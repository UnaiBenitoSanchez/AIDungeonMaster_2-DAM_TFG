package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.JournalEntry
import com.example.aidungeonmaster.python.PythonJournalBridge
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// ViewModel que coordina el estado y la lógica de journal.
class JournalViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: MutableStateFlow<List<JournalEntry>> = _entries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = _isLoading

    private val _selectedEntry = MutableStateFlow<JournalEntry?>(null)
    val selectedEntry: MutableStateFlow<JournalEntry?> = _selectedEntry

    // Selecciona entry.
    fun selectEntry(entry: JournalEntry?) {
        _selectedEntry.value = entry
    }

    // Carga journal.
    fun loadJournal(charId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("partidas")
                    .document(charId)
                    .collection("journal")
                    .get()
                    .await()

                val loaded = snapshot.documents.mapNotNull { doc ->
                    try {
                        parseJournalEntry(doc.id, doc.data ?: emptyMap<String, Any>())
                    } catch (e: Exception) {
                        Log.e("JOURNAL_ERROR", "parse doc=${doc.id}: ${e.message}", e)
                        null
                    }
                }.sortedByDescending { it.timestamp }

                _entries.value = loaded
                Log.d("JOURNAL_DEBUG", "Diario cargado: ${loaded.size} entradas")
            } catch (e: Exception) {
                Log.e("JOURNAL_ERROR", "loadJournal: ${e.message}", e)
                _entries.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Ejecuta la lógica de add entry.
    fun addEntry(
        charId: String,
        title: String,
        summary: String,
        fullText: String = "",
        chapter: String = "",
        type: String = "story",
        tags: List<String> = emptyList(),
        locationName: String = "",
        enemyName: String = "",
        itemNames: List<String> = emptyList(),
        hpChange: Int = 0,
        coinsChange: Int = 0,
        xpGained: Int = 0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                val entryId = UUID.randomUUID().toString()

                val existingEntries = _entries.value
                val (autoChapterIndex, autoChapterTitle) = resolveNextChapter(existingEntries)

                val normalizedTitle = title.trim()
                val normalizedSummary = summary.trim()
                val normalizedType = type.trim().ifBlank { "story" }
                val normalizedLocation = locationName.trim()
                val normalizedEnemy = enemyName.trim()

                val entry = JournalEntry(
                    id = entryId,
                    title = normalizedTitle,
                    summary = normalizedSummary,
                    fullText = fullText.trim(),
                    timestamp = timestamp,

                    chapter = chapter.trim().ifBlank { autoChapterTitle },
                    chapterIndex = autoChapterIndex,
                    sceneIndex = existingEntries.size + 1,

                    type = normalizedType,
                    tags = normalizeList(tags),

                    locationName = normalizedLocation,
                    enemyName = normalizedEnemy,
                    itemNames = normalizeList(itemNames),

                    hpChange = hpChange,
                    coinsChange = coinsChange,
                    xpGained = xpGained,

                    repeatGroupKey = buildRepeatGroupKey(
                        title = normalizedTitle,
                        type = normalizedType,
                        locationName = normalizedLocation,
                        enemyName = normalizedEnemy
                    ),
                    repeatCount = 1,
                    toneVersion = "normal",
                    epicText = ""
                )

                db.collection("partidas")
                    .document(charId)
                    .collection("journal")
                    .document(entryId)
                    .set(journalEntryToMap(entry), SetOptions.merge())
                    .await()

                _entries.value = listOf(entry) + _entries.value
                Log.d("JOURNAL_DEBUG", "Entrada añadida: ${entry.title}")
            } catch (e: Exception) {
                Log.e("JOURNAL_ERROR", "addEntry: ${e.message}", e)
            }
        }
    }

    // Ejecuta la lógica de add or merge simple entry.
    fun addOrMergeSimpleEntry(
        charId: String,
        title: String,
        summary: String,
        type: String,
        tags: List<String> = emptyList(),
        locationName: String = "",
        enemyName: String = "",
        itemNames: List<String> = emptyList(),
        hpChange: Int = 0,
        coinsChange: Int = 0,
        xpGained: Int = 0
    ) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("partidas")
                    .document(charId)
                    .collection("journal")
                    .get()
                    .await()

                val normalizedTitle = title.trim()
                val normalizedSummary = summary.trim()
                val normalizedType = type.trim().ifBlank { "story" }
                val normalizedLocation = locationName.trim()
                val normalizedEnemy = enemyName.trim()
                val normalizedTags = normalizeList(tags)
                val normalizedItems = normalizeList(itemNames)

                val groupKey = buildRepeatGroupKey(
                    title = normalizedTitle,
                    type = normalizedType,
                    locationName = normalizedLocation,
                    enemyName = normalizedEnemy
                )

                val existingDoc = snapshot.documents.firstOrNull { doc ->
                    val docGroupKey = doc.getString("repeatGroupKey")?.trim().orEmpty()
                    docGroupKey == groupKey && groupKey.isNotBlank()
                }

                if (existingDoc == null) {
                    addEntry(
                        charId = charId,
                        title = normalizedTitle,
                        summary = normalizedSummary,
                        type = normalizedType,
                        tags = normalizedTags,
                        locationName = normalizedLocation,
                        enemyName = normalizedEnemy,
                        itemNames = normalizedItems,
                        hpChange = hpChange,
                        coinsChange = coinsChange,
                        xpGained = xpGained
                    )
                    return@launch
                }

                val existing = parseJournalEntry(
                    existingDoc.id,
                    existingDoc.data ?: emptyMap<String, Any>()
                ) ?: return@launch

                val merged = existing.copy(
                    summary = pickLonger(existing.summary, normalizedSummary),
                    fullText = pickLonger(existing.fullText, normalizedSummary),
                    timestamp = System.currentTimeMillis(),
                    tags = mergeDistinct(existing.tags, normalizedTags),
                    locationName = existing.locationName.ifBlank { normalizedLocation },
                    enemyName = existing.enemyName.ifBlank { normalizedEnemy },
                    itemNames = mergeDistinct(existing.itemNames, normalizedItems),
                    hpChange = existing.hpChange + hpChange,
                    coinsChange = existing.coinsChange + coinsChange,
                    xpGained = existing.xpGained + xpGained,
                    repeatGroupKey = existing.repeatGroupKey.ifBlank { groupKey },
                    repeatCount = existing.repeatCount + 1
                )

                db.collection("partidas")
                    .document(charId)
                    .collection("journal")
                    .document(existing.id)
                    .set(journalEntryToMap(merged), SetOptions.merge())
                    .await()

                _entries.value = _entries.value
                    .map { if (it.id == merged.id) merged else it }
                    .sortedByDescending { it.timestamp }

                if (_selectedEntry.value?.id == merged.id) {
                    _selectedEntry.value = merged
                }

                Log.d("JOURNAL_DEBUG", "Entrada fusionada: ${merged.title}")
            } catch (e: Exception) {
                Log.e("JOURNAL_ERROR", "addOrMergeSimpleEntry: ${e.message}", e)
            }
        }
    }

    // Ejecuta la lógica de rewrite entry epic.
    fun rewriteEntryEpic(charId: String, entry: JournalEntry) {
        viewModelScope.launch {
            try {
                val epic = PythonJournalBridge.rewriteEpic(
                    mapOf(
                        "title" to entry.title,
                        "summary" to entry.summary,
                        "fullText" to entry.fullText,
                        "locationName" to entry.locationName,
                        "enemyName" to entry.enemyName,
                        "type" to entry.type,
                        "chapter" to entry.chapter,
                        "tags" to entry.tags,
                        "itemNames" to entry.itemNames,
                        "timestamp" to entry.timestamp
                    )
                )

                val updated = entry.copy(
                    epicText = epic,
                    toneVersion = "epic"
                )

                db.collection("partidas")
                    .document(charId)
                    .collection("journal")
                    .document(entry.id)
                    .set(journalEntryToMap(updated), SetOptions.merge())
                    .await()

                _entries.value = _entries.value.map {
                    if (it.id == updated.id) updated else it
                }

                if (_selectedEntry.value?.id == updated.id) {
                    _selectedEntry.value = updated
                }

                Log.d("JOURNAL_DEBUG", "Entrada reescrita en tono épico: ${updated.title}")
            } catch (e: Exception) {
                Log.e("JOURNAL_ERROR", "rewriteEntryEpic: ${e.message}", e)
            }
        }
    }

    // Elimina entry.
    fun deleteEntry(charId: String, entryId: String) {
        viewModelScope.launch {
            try {
                db.collection("partidas")
                    .document(charId)
                    .collection("journal")
                    .document(entryId)
                    .delete()
                    .await()

                _entries.value = _entries.value.filterNot { it.id == entryId }
                if (_selectedEntry.value?.id == entryId) {
                    _selectedEntry.value = null
                }

                Log.d("JOURNAL_DEBUG", "Entrada eliminada: $entryId")
            } catch (e: Exception) {
                Log.e("JOURNAL_ERROR", "deleteEntry: ${e.message}", e)
            }
        }
    }

    // Analiza journal entry.
    private fun parseJournalEntry(
        id: String,
        data: Map<*, *>
    ): JournalEntry? {
        return try {
            JournalEntry(
                id = id,
                title = data["title"] as? String ?: "",
                summary = data["summary"] as? String ?: "",
                fullText = data["fullText"] as? String ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                chapter = data["chapter"] as? String ?: "",
                chapterIndex = (data["chapterIndex"] as? Number)?.toInt() ?: 0,
                sceneIndex = (data["sceneIndex"] as? Number)?.toInt() ?: 0,
                type = data["type"] as? String ?: "story",
                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                locationName = data["locationName"] as? String ?: "",
                enemyName = data["enemyName"] as? String ?: "",
                itemNames = (data["itemNames"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                hpChange = (data["hpChange"] as? Number)?.toInt() ?: 0,
                coinsChange = (data["coinsChange"] as? Number)?.toInt() ?: 0,
                xpGained = (data["xpGained"] as? Number)?.toInt() ?: 0,
                repeatGroupKey = data["repeatGroupKey"] as? String ?: "",
                repeatCount = (data["repeatCount"] as? Number)?.toInt() ?: 1,
                toneVersion = data["toneVersion"] as? String ?: "normal",
                epicText = data["epicText"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e("JOURNAL_ERROR", "parseJournalEntry($id): ${e.message}", e)
            null
        }
    }

    // Ejecuta la lógica de journal entry to map.
    private fun journalEntryToMap(entry: JournalEntry): Map<String, Any> {
        return mapOf(
            "title" to entry.title,
            "summary" to entry.summary,
            "fullText" to entry.fullText,
            "timestamp" to entry.timestamp,
            "chapter" to entry.chapter,
            "chapterIndex" to entry.chapterIndex,
            "sceneIndex" to entry.sceneIndex,
            "type" to entry.type,
            "tags" to entry.tags,
            "locationName" to entry.locationName,
            "enemyName" to entry.enemyName,
            "itemNames" to entry.itemNames,
            "hpChange" to entry.hpChange,
            "coinsChange" to entry.coinsChange,
            "xpGained" to entry.xpGained,
            "repeatGroupKey" to entry.repeatGroupKey,
            "repeatCount" to entry.repeatCount,
            "toneVersion" to entry.toneVersion,
            "epicText" to entry.epicText
        )
    }

    // Ejecuta la lógica de normalize list.
    private fun normalizeList(values: List<String>): List<String> {
        return values.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Ejecuta la lógica de merge distinct.
    private fun mergeDistinct(
        base: List<String>,
        incoming: List<String>
    ): List<String> {
        return (base + incoming)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Ejecuta la lógica de pick longer.
    private fun pickLonger(current: String, incoming: String): String {
        val a = current.trim()
        val b = incoming.trim()
        return if (b.length > a.length) b else a
    }

    // Ejecuta la lógica de resolve next chapter.
    private fun resolveNextChapter(entries: List<JournalEntry>): Pair<Int, String> {
        if (entries.isEmpty()) return 1 to "Capítulo 1"

        val latest = entries.maxByOrNull { it.timestamp }
        val latestIndex = latest?.chapterIndex ?: 1

        val currentChapterEntries = entries.count { it.chapterIndex == latestIndex }
        val shouldOpenNewChapter = currentChapterEntries >= 8

        val nextIndex = if (shouldOpenNewChapter) latestIndex + 1 else latestIndex
        return nextIndex to "Capítulo $nextIndex"
    }

    // Construye repeat group key.
    private fun buildRepeatGroupKey(
        title: String,
        type: String,
        locationName: String,
        enemyName: String
    ): String {
        val normalizedType = type.trim().lowercase().ifBlank { "story" }
        val normalizedLocation = locationName.trim().lowercase()
        val normalizedEnemy = enemyName.trim().lowercase()
        val normalizedTitle = title.trim().lowercase()

        return when (normalizedType) {
            "combat" -> "combat|$normalizedLocation|${normalizedEnemy.ifBlank { normalizedTitle }}"
            "loot" -> "loot|$normalizedLocation|$normalizedTitle"
            "location" -> "location|${normalizedLocation.ifBlank { normalizedTitle }}"
            else -> "$normalizedType|$normalizedLocation|$normalizedTitle"
        }
    }
}
