package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.JournalEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class JournalViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: MutableStateFlow<List<JournalEntry>> = _entries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = _isLoading

    private val _selectedEntry = MutableStateFlow<JournalEntry?>(null)
    val selectedEntry: MutableStateFlow<JournalEntry?> = _selectedEntry

    fun selectEntry(entry: JournalEntry?) {
        _selectedEntry.value = entry
    }

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

                val entry = JournalEntry(
                    id = entryId,
                    title = title.trim(),
                    summary = summary.trim(),
                    fullText = fullText.trim(),
                    timestamp = timestamp,
                    chapter = chapter.trim(),
                    type = type.trim().ifBlank { "story" },
                    tags = normalizeList(tags),
                    locationName = locationName.trim(),
                    enemyName = enemyName.trim(),
                    itemNames = normalizeList(itemNames),
                    hpChange = hpChange,
                    coinsChange = coinsChange,
                    xpGained = xpGained
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
                val normalizedType = type.trim().ifBlank { "story" }

                val existingDoc = snapshot.documents.firstOrNull { doc ->
                    val docTitle = doc.getString("title")?.trim().orEmpty()
                    val docType = doc.getString("type")?.trim().orEmpty()
                    docTitle == normalizedTitle && docType == normalizedType
                }

                if (existingDoc == null) {
                    addEntry(
                        charId = charId,
                        title = title,
                        summary = summary,
                        type = type,
                        tags = tags,
                        locationName = locationName,
                        enemyName = enemyName,
                        itemNames = itemNames,
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
                    summary = pickLonger(existing.summary, summary),
                    fullText = pickLonger(existing.fullText, summary),
                    timestamp = System.currentTimeMillis(),
                    tags = mergeDistinct(existing.tags, tags),
                    locationName = existing.locationName.ifBlank { locationName.trim() },
                    enemyName = existing.enemyName.ifBlank { enemyName.trim() },
                    itemNames = mergeDistinct(existing.itemNames, itemNames),
                    hpChange = existing.hpChange + hpChange,
                    coinsChange = existing.coinsChange + coinsChange,
                    xpGained = existing.xpGained + xpGained
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

                Log.d("JOURNAL_DEBUG", "Entrada fusionada: ${merged.title}")
            } catch (e: Exception) {
                Log.e("JOURNAL_ERROR", "addOrMergeSimpleEntry: ${e.message}", e)
            }
        }
    }

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
                type = data["type"] as? String ?: "story",
                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                locationName = data["locationName"] as? String ?: "",
                enemyName = data["enemyName"] as? String ?: "",
                itemNames = (data["itemNames"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                hpChange = (data["hpChange"] as? Number)?.toInt() ?: 0,
                coinsChange = (data["coinsChange"] as? Number)?.toInt() ?: 0,
                xpGained = (data["xpGained"] as? Number)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e("JOURNAL_ERROR", "parseJournalEntry($id): ${e.message}", e)
            null
        }
    }

    private fun journalEntryToMap(entry: JournalEntry): Map<String, Any> {
        return mapOf(
            "title" to entry.title,
            "summary" to entry.summary,
            "fullText" to entry.fullText,
            "timestamp" to entry.timestamp,
            "chapter" to entry.chapter,
            "type" to entry.type,
            "tags" to entry.tags,
            "locationName" to entry.locationName,
            "enemyName" to entry.enemyName,
            "itemNames" to entry.itemNames,
            "hpChange" to entry.hpChange,
            "coinsChange" to entry.coinsChange,
            "xpGained" to entry.xpGained
        )
    }

    private fun normalizeList(values: List<String>): List<String> {
        return values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun mergeDistinct(
        base: List<String>,
        incoming: List<String>
    ): List<String> {
        return (base + incoming)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun pickLonger(current: String, incoming: String): String {
        val a = current.trim()
        val b = incoming.trim()
        return if (b.length > a.length) b else a
    }
}