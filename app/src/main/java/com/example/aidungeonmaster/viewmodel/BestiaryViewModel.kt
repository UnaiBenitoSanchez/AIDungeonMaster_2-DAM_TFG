package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.BestiaryEntry
import com.example.aidungeonmaster.data.model.MonsterStatSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BestiaryViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _entries = MutableStateFlow<List<BestiaryEntry>>(emptyList())
    val entries: MutableStateFlow<List<BestiaryEntry>> = _entries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = _isLoading

    private val _selectedEntry = MutableStateFlow<BestiaryEntry?>(null)
    val selectedEntry: MutableStateFlow<BestiaryEntry?> = _selectedEntry

    fun selectEntry(entry: BestiaryEntry?) {
        _selectedEntry.value = entry
    }

    fun loadBestiary(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("partidas")
                    .document(gameId)
                    .collection("bestiary")
                    .get()
                    .await()

                Log.d("BESTIARY_DEBUG", "Documentos encontrados: ${snapshot.documents.size}")

                val loaded = snapshot.documents.mapNotNull { doc ->
                    try {
                        val parsed = parseBestiaryEntry(doc.id, doc.data ?: emptyMap<String, Any>())
                        Log.d("BESTIARY_DEBUG", "Parseado bestiario doc=${doc.id} -> ${parsed?.name}")
                        parsed
                    } catch (e: Exception) {
                        Log.e("BESTIARY_ERROR", "Fallo parseando doc=${doc.id}: ${e.message}", e)
                        null
                    }
                }.sortedBy { it.name.lowercase() }

                _entries.value = loaded
                Log.d("BESTIARY_DEBUG", "Bestiario cargado correctamente: ${loaded.size} entradas")
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "loadBestiary: ${e.message}", e)
                _entries.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerEncounter(
        gameId: String,
        monsterId: String,
        name: String,
        description: String = "",
        imageUrl: String = "",
        locationName: String = "",
        hpMaxObserved: Int = 0,
        armorClassObserved: Int? = null,
        damageNotes: List<String> = emptyList(),
        abilitiesSeen: List<String> = emptyList(),
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas")
                    .document(gameId)
                    .collection("bestiary")
                    .document(monsterId)

                val existing = docRef.get().await()
                val now = System.currentTimeMillis()

                val existingEntry = if (existing.exists()) {
                    parseBestiaryEntry(monsterId, existing.data ?: emptyMap<String, Any>())
                } else {
                    null
                }

                val mergedEntry = if (existingEntry == null) {
                    BestiaryEntry(
                        monsterId = monsterId,
                        name = name.ifBlank { monsterId.toDisplayMonsterName() },
                        description = description,
                        imageUrl = imageUrl,
                        firstSeenAt = now,
                        lastSeenAt = now,
                        timesEncountered = 1,
                        timesDefeated = 0,
                        locationsSeen = listOfNotNull(locationName.takeIf { it.isNotBlank() }),
                        tags = tags.distinct(),
                        lastObservedStats = MonsterStatSnapshot(
                            hpMaxObserved = hpMaxObserved,
                            armorClassObserved = armorClassObserved,
                            damageNotes = damageNotes.distinct(),
                            abilitiesSeen = abilitiesSeen.distinct()
                        )
                    )
                } else {
                    existingEntry.copy(
                        name = if (existingEntry.name.isBlank()) {
                            name.ifBlank { monsterId.toDisplayMonsterName() }
                        } else {
                            existingEntry.name
                        },
                        description = pickLonger(existingEntry.description, description),
                        imageUrl = if (existingEntry.imageUrl.isBlank()) imageUrl else existingEntry.imageUrl,
                        lastSeenAt = now,
                        timesEncountered = existingEntry.timesEncountered + 1,
                        locationsSeen = mergeDistinct(existingEntry.locationsSeen, listOf(locationName)),
                        tags = mergeDistinct(existingEntry.tags, tags),
                        lastObservedStats = existingEntry.lastObservedStats.copy(
                            hpMaxObserved = maxOf(
                                existingEntry.lastObservedStats.hpMaxObserved,
                                hpMaxObserved
                            ),
                            armorClassObserved = armorClassObserved
                                ?: existingEntry.lastObservedStats.armorClassObserved,
                            damageNotes = mergeDistinct(
                                existingEntry.lastObservedStats.damageNotes,
                                damageNotes
                            ),
                            abilitiesSeen = mergeDistinct(
                                existingEntry.lastObservedStats.abilitiesSeen,
                                abilitiesSeen
                            )
                        )
                    )
                }

                docRef.set(bestiaryEntryToMap(mergedEntry), SetOptions.merge()).await()
                loadBestiary(gameId)
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "registerEncounter: ${e.message}", e)
            }
        }
    }

    fun registerDefeat(
        gameId: String,
        monsterId: String,
        knownLoot: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas")
                    .document(gameId)
                    .collection("bestiary")
                    .document(monsterId)

                val existing = docRef.get().await()
                if (!existing.exists()) return@launch

                val entry = parseBestiaryEntry(monsterId, existing.data ?: emptyMap<String, Any>())
                    ?: return@launch

                val updated = entry.copy(
                    timesDefeated = entry.timesDefeated + 1,
                    knownLoot = mergeDistinct(entry.knownLoot, knownLoot),
                    lastSeenAt = System.currentTimeMillis()
                )

                docRef.set(bestiaryEntryToMap(updated), SetOptions.merge()).await()
                loadBestiary(gameId)
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "registerDefeat: ${e.message}", e)
            }
        }
    }

    fun addMonsterNote(
        gameId: String,
        monsterId: String,
        note: String
    ) {
        if (note.isBlank()) return

        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas")
                    .document(gameId)
                    .collection("bestiary")
                    .document(monsterId)

                val existing = docRef.get().await()
                if (!existing.exists()) return@launch

                val entry = parseBestiaryEntry(monsterId, existing.data ?: emptyMap<String, Any>())
                    ?: return@launch

                val updated = entry.copy(notes = note.trim())

                docRef.set(bestiaryEntryToMap(updated), SetOptions.merge()).await()
                loadBestiary(gameId)
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "addMonsterNote: ${e.message}", e)
            }
        }
    }

    private fun parseBestiaryEntry(
        monsterId: String,
        data: Map<*, *>
    ): BestiaryEntry? {
        return try {
            val statsMap = data["lastObservedStats"] as? Map<*, *>

            val statSnapshot = MonsterStatSnapshot(
                hpMaxObserved = (statsMap?.get("hpMaxObserved") as? Number)?.toInt() ?: 0,
                armorClassObserved = (statsMap?.get("armorClassObserved") as? Number)?.toInt(),
                damageNotes = (statsMap?.get("damageNotes") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                abilitiesSeen = (statsMap?.get("abilitiesSeen") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList()
            )

            val name = (data["name"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: monsterId.toDisplayMonsterName()

            BestiaryEntry(
                monsterId = monsterId,
                name = name,
                description = data["description"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String ?: "",
                firstSeenAt = (data["firstSeenAt"] as? Number)?.toLong() ?: 0L,
                lastSeenAt = (data["lastSeenAt"] as? Number)?.toLong() ?: 0L,
                timesEncountered = (data["timesEncountered"] as? Number)?.toInt() ?: 0,
                timesDefeated = (data["timesDefeated"] as? Number)?.toInt() ?: 0,
                locationsSeen = (data["locationsSeen"] as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                lastObservedStats = statSnapshot,
                knownLoot = (data["knownLoot"] as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                notes = data["notes"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e("BESTIARY_ERROR", "parseBestiaryEntry($monsterId): ${e.message}", e)
            null
        }
    }

    private fun bestiaryEntryToMap(entry: BestiaryEntry): Map<String, Any> {
        val statsMap = mutableMapOf<String, Any>(
            "hpMaxObserved" to entry.lastObservedStats.hpMaxObserved,
            "damageNotes" to entry.lastObservedStats.damageNotes,
            "abilitiesSeen" to entry.lastObservedStats.abilitiesSeen
        )

        entry.lastObservedStats.armorClassObserved?.let {
            statsMap["armorClassObserved"] = it
        }

        return mapOf(
            "name" to entry.name,
            "description" to entry.description,
            "imageUrl" to entry.imageUrl,
            "firstSeenAt" to entry.firstSeenAt,
            "lastSeenAt" to entry.lastSeenAt,
            "timesEncountered" to entry.timesEncountered,
            "timesDefeated" to entry.timesDefeated,
            "locationsSeen" to entry.locationsSeen,
            "tags" to entry.tags,
            "lastObservedStats" to statsMap,
            "knownLoot" to entry.knownLoot,
            "notes" to entry.notes
        )
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

    private fun String.toDisplayMonsterName(): String {
        return replace("_", " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
            .ifBlank { "Criatura desconocida" }
    }
}