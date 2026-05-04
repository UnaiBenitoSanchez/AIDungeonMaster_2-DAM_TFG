package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.BestiaryEntry
import com.example.aidungeonmaster.data.model.BestiaryLoot
import com.example.aidungeonmaster.data.model.MonsterStatSnapshot
import com.example.aidungeonmaster.utils.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel que coordina el estado y la lógica de bestiary.
class BestiaryViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _entries = MutableStateFlow<List<BestiaryEntry>>(emptyList())
    val entries: MutableStateFlow<List<BestiaryEntry>> = _entries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = _isLoading

    private val _selectedEntry = MutableStateFlow<BestiaryEntry?>(null)
    val selectedEntry: MutableStateFlow<BestiaryEntry?> = _selectedEntry

    private val _imageGenerationMonsterId = MutableStateFlow<String?>(null)
    val imageGenerationMonsterId: MutableStateFlow<String?> = _imageGenerationMonsterId

    // Selecciona entry.
    fun selectEntry(entry: BestiaryEntry?) {
        _selectedEntry.value = entry
    }

    // Carga bestiary.
    fun loadBestiary(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("partidas")
                    .document(gameId)
                    .collection("bestiary")
                    .get()
                    .await()

                _entries.value = snapshot.documents.mapNotNull { doc ->
                    try {
                        parseBestiaryEntry(doc.id, doc.data ?: emptyMap<String, Any>())
                    } catch (e: Exception) {
                        Log.e("BESTIARY_ERROR", "Fallo parseando doc=${doc.id}: ${e.message}", e)
                        null
                    }
                }.sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "loadBestiary: ${e.message}", e)
                _entries.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Registra encounter.
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
        tags: List<String> = emptyList(),
        observedWeaknesses: List<String> = emptyList(),
        observedResistances: List<String> = emptyList()
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
                } else null

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
                        ),
                        observedWeaknesses = mergeDistinct(inferWeaknesses(name, tags), observedWeaknesses),
                        observedResistances = mergeDistinct(inferResistances(name, tags), observedResistances)
                    )
                } else {
                    existingEntry.copy(
                        name = if (existingEntry.name.isBlank()) name.ifBlank { monsterId.toDisplayMonsterName() } else existingEntry.name,
                        description = pickLonger(existingEntry.description, description),
                        imageUrl = existingEntry.imageUrl.ifBlank { imageUrl },
                        lastSeenAt = now,
                        timesEncountered = existingEntry.timesEncountered + 1,
                        locationsSeen = mergeDistinct(existingEntry.locationsSeen, listOf(locationName)),
                        tags = mergeDistinct(existingEntry.tags, tags),
                        observedWeaknesses = mergeDistinct(
                            existingEntry.observedWeaknesses,
                            inferWeaknesses(existingEntry.name.ifBlank { name }, mergeDistinct(existingEntry.tags, tags)),
                            observedWeaknesses
                        ),
                        observedResistances = mergeDistinct(
                            existingEntry.observedResistances,
                            inferResistances(existingEntry.name.ifBlank { name }, mergeDistinct(existingEntry.tags, tags)),
                            observedResistances
                        ),
                        lastObservedStats = existingEntry.lastObservedStats.copy(
                            hpMaxObserved = maxOf(existingEntry.lastObservedStats.hpMaxObserved, hpMaxObserved),
                            armorClassObserved = armorClassObserved ?: existingEntry.lastObservedStats.armorClassObserved,
                            damageNotes = mergeDistinct(existingEntry.lastObservedStats.damageNotes, damageNotes),
                            abilitiesSeen = mergeDistinct(existingEntry.lastObservedStats.abilitiesSeen, abilitiesSeen)
                        )
                    )
                }

                docRef.set(bestiaryEntryToMap(mergedEntry), SetOptions.merge()).await()

                if (mergedEntry.imageUrl.isBlank()) {
                    generateAndPersistMonsterImage(
                        gameId = gameId,
                        monsterId = monsterId,
                        monsterName = mergedEntry.name,
                        description = mergedEntry.description,
                        tags = mergedEntry.tags
                    )
                } else {
                    loadBestiary(gameId)
                }
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "registerEncounter: ${e.message}", e)
            }
        }
    }

    // Registra defeat.
    fun registerDefeat(
        gameId: String,
        monsterId: String,
        knownLoot: List<String> = emptyList(),
        detailedLoot: List<BestiaryLoot> = emptyList(),
        observedWeaknesses: List<String> = emptyList(),
        observedResistances: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas")
                    .document(gameId)
                    .collection("bestiary")
                    .document(monsterId)

                val existing = docRef.get().await()
                if (!existing.exists()) return@launch

                val entry = parseBestiaryEntry(monsterId, existing.data ?: emptyMap<String, Any>()) ?: return@launch
                val updated = entry.copy(
                    timesDefeated = entry.timesDefeated + 1,
                    knownLoot = mergeDistinct(entry.knownLoot, knownLoot),
                    detailedKnownLoot = mergeLoot(entry.detailedKnownLoot, detailedLoot),
                    observedWeaknesses = mergeDistinct(entry.observedWeaknesses, observedWeaknesses),
                    observedResistances = mergeDistinct(entry.observedResistances, observedResistances),
                    lastSeenAt = System.currentTimeMillis()
                )

                docRef.set(bestiaryEntryToMap(updated), SetOptions.merge()).await()
                loadBestiary(gameId)
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "registerDefeat: ${e.message}", e)
            }
        }
    }

    // Ejecuta la lógica de regenerate monster image.
    fun regenerateMonsterImage(gameId: String, entry: BestiaryEntry) {
        viewModelScope.launch {
            generateAndPersistMonsterImage(
                gameId = gameId,
                monsterId = entry.monsterId,
                monsterName = entry.name,
                description = entry.description,
                tags = entry.tags,
                force = true
            )
        }
    }

    // Guarda monster notes.
    fun saveMonsterNotes(gameId: String, monsterId: String, notes: String) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas").document(gameId).collection("bestiary").document(monsterId)
                val existing = docRef.get().await()
                if (!existing.exists()) return@launch

                docRef.set(mapOf("notes" to notes.trim()), SetOptions.merge()).await()
                refreshSelectedEntry(gameId, monsterId)
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "saveMonsterNotes: ${e.message}", e)
            }
        }
    }

    // Guarda monster field list.
    fun saveMonsterFieldList(gameId: String, monsterId: String, field: BestiaryEditableListField, rawValue: String) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("partidas").document(gameId).collection("bestiary").document(monsterId)
                val existing = docRef.get().await()
                if (!existing.exists()) return@launch

                val normalized = rawValue
                    .split(',', '\n', ';')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                docRef.set(mapOf(field.firebaseKey to normalized), SetOptions.merge()).await()
                refreshSelectedEntry(gameId, monsterId)
            } catch (e: Exception) {
                Log.e("BESTIARY_ERROR", "saveMonsterFieldList(${field.firebaseKey}): ${e.message}", e)
            }
        }
    }

    private suspend fun generateAndPersistMonsterImage(
        gameId: String,
        monsterId: String,
        monsterName: String,
        description: String,
        tags: List<String>,
        force: Boolean = false
    ) {
        _imageGenerationMonsterId.value = monsterId
        try {
            val docRef = db.collection("partidas").document(gameId).collection("bestiary").document(monsterId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return

            val existingUrl = snapshot.getString("imageUrl").orEmpty()
            if (existingUrl.isNotBlank() && !force) {
                loadBestiary(gameId)
                return
            }

            val imageDataUrl = ImageUtils.generateMonsterImageDataUrl(
                monsterNameEs = monsterName,
                descriptionEs = description,
                tags = tags
            )
            docRef.set(mapOf("imageUrl" to imageDataUrl), SetOptions.merge()).await()
            refreshSelectedEntry(gameId, monsterId)
        } catch (e: Exception) {
            Log.e("BESTIARY_ERROR", "generateAndPersistMonsterImage: ${e.message}", e)
            loadBestiary(gameId)
        } finally {
            _imageGenerationMonsterId.value = null
        }
    }

    private suspend fun refreshSelectedEntry(gameId: String, monsterId: String) {
        try {
            val doc = db.collection("partidas")
                .document(gameId)
                .collection("bestiary")
                .document(monsterId)
                .get()
                .await()

            val parsed = parseBestiaryEntry(monsterId, doc.data ?: emptyMap<String, Any>())
            _selectedEntry.value = parsed
            loadBestiary(gameId)
        } catch (e: Exception) {
            Log.e("BESTIARY_ERROR", "refreshSelectedEntry: ${e.message}", e)
        }
    }

    // Analiza bestiary entry.
    private fun parseBestiaryEntry(monsterId: String, data: Map<*, *>): BestiaryEntry? {
        return try {
            val statsMap = data["lastObservedStats"] as? Map<*, *>
            val name = (data["name"] as? String).orEmpty().ifBlank { monsterId.toDisplayMonsterName() }
            val tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val statSnapshot = MonsterStatSnapshot(
                hpMaxObserved = (statsMap?.get("hpMaxObserved") as? Number)?.toInt() ?: 0,
                armorClassObserved = (statsMap?.get("armorClassObserved") as? Number)?.toInt(),
                damageNotes = (statsMap?.get("damageNotes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                abilitiesSeen = (statsMap?.get("abilitiesSeen") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
            val knownLootStrings = (data["knownLoot"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            BestiaryEntry(
                monsterId = monsterId,
                name = name,
                description = data["description"] as? String ?: "",
                imageUrl = (data["imageUrl"] as? String).orEmpty(),
                firstSeenAt = (data["firstSeenAt"] as? Number)?.toLong() ?: 0L,
                lastSeenAt = (data["lastSeenAt"] as? Number)?.toLong() ?: 0L,
                timesEncountered = (data["timesEncountered"] as? Number)?.toInt() ?: 0,
                timesDefeated = (data["timesDefeated"] as? Number)?.toInt() ?: 0,
                locationsSeen = (data["locationsSeen"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                tags = tags,
                lastObservedStats = statSnapshot,
                knownLoot = knownLootStrings,
                detailedKnownLoot = parseDetailedLoot(data["detailedKnownLoot"], knownLootStrings),
                observedWeaknesses = (data["observedWeaknesses"] as? List<*>)?.mapNotNull { it as? String }
                    ?: inferWeaknesses(name, tags),
                observedResistances = (data["observedResistances"] as? List<*>)?.mapNotNull { it as? String }
                    ?: inferResistances(name, tags),
                notes = data["notes"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e("BESTIARY_ERROR", "parseBestiaryEntry($monsterId): ${e.message}", e)
            null
        }
    }

    // Ejecuta la lógica de bestiary entry to map.
    private fun bestiaryEntryToMap(entry: BestiaryEntry): Map<String, Any> = mapOf(
        "name" to entry.name,
        "description" to entry.description,
        "imageUrl" to entry.imageUrl,
        "firstSeenAt" to entry.firstSeenAt,
        "lastSeenAt" to entry.lastSeenAt,
        "timesEncountered" to entry.timesEncountered,
        "timesDefeated" to entry.timesDefeated,
        "locationsSeen" to entry.locationsSeen.distinct(),
        "tags" to entry.tags.distinct(),
        "lastObservedStats" to mapOf(
            "hpMaxObserved" to entry.lastObservedStats.hpMaxObserved,
            "armorClassObserved" to entry.lastObservedStats.armorClassObserved,
            "damageNotes" to entry.lastObservedStats.damageNotes.distinct(),
            "abilitiesSeen" to entry.lastObservedStats.abilitiesSeen.distinct()
        ),
        "knownLoot" to entry.knownLoot.distinct(),
        "detailedKnownLoot" to entry.detailedKnownLoot.map { loot ->
            mapOf(
                "name" to loot.name,
                "category" to loot.category,
                "details" to loot.details,
                "quantityObserved" to loot.quantityObserved,
                "timesDropped" to loot.timesDropped
            )
        },
        "observedWeaknesses" to entry.observedWeaknesses.distinct(),
        "observedResistances" to entry.observedResistances.distinct(),
        "notes" to entry.notes
    )

    // Analiza detailed loot.
    private fun parseDetailedLoot(raw: Any?, fallbackStrings: List<String>): List<BestiaryLoot> {
        val fromMaps = (raw as? List<*>)
            ?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val name = map["name"] as? String ?: return@mapNotNull null
                BestiaryLoot(
                    name = name,
                    category = map["category"] as? String ?: classifyLootCategory(name),
                    details = map["details"] as? String ?: "",
                    quantityObserved = (map["quantityObserved"] as? Number)?.toInt() ?: 1,
                    timesDropped = (map["timesDropped"] as? Number)?.toInt() ?: 1
                )
            }
            ?: emptyList()

        if (fromMaps.isNotEmpty()) return fromMaps

        return fallbackStrings.map { rawLoot ->
            val quantity = """(\d+)""".toRegex().find(rawLoot)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            BestiaryLoot(
                name = rawLoot,
                category = classifyLootCategory(rawLoot),
                details = if (rawLoot.contains("moneda", true)) {
                    "Botín monetario registrado al derrotarlo."
                } else {
                    "Observado en combate."
                },
                quantityObserved = quantity,
                timesDropped = 1
            )
        }
    }

    // Ejecuta la lógica de merge loot.
    private fun mergeLoot(current: List<BestiaryLoot>, incoming: List<BestiaryLoot>): List<BestiaryLoot> {
        if (incoming.isEmpty()) return current.distinctBy { it.name.lowercase() }

        val merged = current.associateBy { it.name.lowercase() }.toMutableMap()
        incoming.forEach { loot ->
            val key = loot.name.lowercase()
            val previous = merged[key]
            merged[key] = if (previous == null) {
                loot
            } else {
                previous.copy(
                    category = pickLonger(previous.category, loot.category),
                    details = pickLonger(previous.details, loot.details),
                    quantityObserved = maxOf(previous.quantityObserved, loot.quantityObserved),
                    timesDropped = previous.timesDropped + loot.timesDropped
                )
            }
        }
        return merged.values.sortedBy { it.name.lowercase() }
    }

    // Ejecuta la lógica de classify loot category.
    private fun classifyLootCategory(value: String): String {
        val normalized = value.lowercase()
        return when {
            normalized.contains("moneda") || normalized.contains("oro") -> "moneda"
            normalized.contains("poción") || normalized.contains("pocion") || normalized.contains("elixir") -> "consumible"
            normalized.contains("espada") || normalized.contains("hacha") || normalized.contains("arco") || normalized.contains("daga") || normalized.contains("arma") -> "arma"
            normalized.contains("armadura") || normalized.contains("casco") || normalized.contains("escudo") || normalized.contains("guante") || normalized.contains("bota") -> "armadura"
            normalized.contains("gema") || normalized.contains("cristal") || normalized.contains("reliquia") || normalized.contains("amuleto") || normalized.contains("anillo") -> "tesoro"
            normalized.contains("garra") || normalized.contains("colmillo") || normalized.contains("escama") || normalized.contains("ojo") || normalized.contains("piel") || normalized.contains("veneno") -> "material"
            else -> "desconocido"
        }
    }

    // Ejecuta la lógica de infer weaknesses.
    private fun inferWeaknesses(name: String, tags: List<String>): List<String> {
        val basis = (listOf(name) + tags).joinToString(" ").lowercase()
        val detected = mutableListOf<String>()
        if (basis.contains("hielo") || basis.contains("ice")) detected += "fuego"
        if (basis.contains("fuego") || basis.contains("fire") || basis.contains("lava")) detected += "hielo"
        if (basis.contains("muerto") || basis.contains("undead") || basis.contains("esqueleto") || basis.contains("zombi")) detected += "sagrado"
        if (basis.contains("fantasma") || basis.contains("spirit")) detected += "luz"
        if (basis.contains("bestia") || basis.contains("lobo") || basis.contains("araña")) detected += "fuego"
        return detected.distinct().sorted()
    }

    // Ejecuta la lógica de infer resistances.
    private fun inferResistances(name: String, tags: List<String>): List<String> {
        val basis = (listOf(name) + tags).joinToString(" ").lowercase()
        val detected = mutableListOf<String>()
        if (basis.contains("hielo") || basis.contains("ice")) detected += "frío"
        if (basis.contains("fuego") || basis.contains("fire") || basis.contains("lava")) detected += "fuego"
        if (basis.contains("piedra") || basis.contains("gólem") || basis.contains("golem")) detected += "cortes ligeros"
        if (basis.contains("fantasma") || basis.contains("specter")) detected += "daño físico"
        if (basis.contains("veneno") || basis.contains("araña") || basis.contains("serpiente")) detected += "veneno"
        return detected.distinct().sorted()
    }

    // Ejecuta la lógica de merge distinct.
    private fun mergeDistinct(vararg lists: List<String>): List<String> {
        return lists.flatMap { it }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    // Ejecuta la lógica de pick longer.
    private fun pickLonger(current: String, candidate: String): String = when {
        candidate.isBlank() -> current
        current.isBlank() -> candidate
        candidate.length > current.length -> candidate
        else -> current
    }

    // Ejecuta la lógica de string.
    private fun String.toDisplayMonsterName(): String = replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { ch -> ch.titlecase() }
        }
}

// Clase que encapsula la lógica de bestiary editable list field.
enum class BestiaryEditableListField(val firebaseKey: String) {
    WEAKNESSES("observedWeaknesses"),
    RESISTANCES("observedResistances")
}
