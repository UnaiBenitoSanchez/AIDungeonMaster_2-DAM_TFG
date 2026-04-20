package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.LocationLifeState
import com.example.aidungeonmaster.data.model.WorldLocation
import com.example.aidungeonmaster.data.model.WorldMapState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

class WorldMapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()

    private val _worldMapState = MutableStateFlow(WorldMapState())
    val worldMapState = _worldMapState.asStateFlow()

    /**
     * "{userId}_{characterName}"
     */
    private var currentCharId: String = ""

    private val locationIcons = mapOf(
        "ciudad" to "🏰",
        "pueblo" to "🏘️",
        "mazmorra" to "⚔️",
        "bosque" to "🌲",
        "montaña" to "⛰️",
        "mar" to "🌊",
        "océano" to "🌐",
        "desierto" to "🏜️",
        "cueva" to "🕳️",
        "taberna" to "🍺",
        "templo" to "⛩️",
        "ruina" to "🏚️",
        "torre" to "🗼",
        "lago" to "💧",
        "llanura" to "🌾",
        "lugar" to "📍"
    )

    fun loadMap(charId: String) {
        currentCharId = charId
        viewModelScope.launch {
            try {
                val doc = db.collection("partidas")
                    .document(currentCharId)
                    .collection("worldMap")
                    .document("state")
                    .get()
                    .await()

                if (doc.exists()) {
                    val locationsRaw = doc.get("locations") as? List<Map<String, Any>>
                    val locations = locationsRaw?.map { it.toWorldLocation() } ?: emptyList()
                    val mapName = doc.getString("mapName") ?: "Mundo Desconocido"
                    val currentId = doc.getString("currentLocationId") ?: ""

                    val statesRaw = doc.get("locationStates") as? Map<*, *>
                    val locationStates = statesRaw
                        ?.mapNotNull { (key, value) ->
                            val locationId = key as? String ?: return@mapNotNull null
                            val rawState = value as? Map<String, Any> ?: return@mapNotNull null
                            locationId to rawState.toLocationLifeState(locationId)
                        }
                        ?.toMap()
                        ?: emptyMap()

                    _worldMapState.value = WorldMapState(
                        locations = locations,
                        currentLocationId = currentId,
                        mapName = mapName,
                        locationStates = locationStates
                    )

                    Log.d(
                        "WORLDMAP",
                        "Mapa cargado: ${locations.size} ubicaciones, ${locationStates.size} estados"
                    )
                }
            } catch (e: Exception) {
                Log.w("WORLDMAP", "Error cargando mapa: ${e.message}")
            }
        }
    }

    private fun saveMap() {
        if (currentCharId.isBlank()) return

        viewModelScope.launch {
            try {
                val state = _worldMapState.value
                val data = mapOf(
                    "mapName" to state.mapName,
                    "currentLocationId" to state.currentLocationId,
                    "locations" to state.locations.map { it.toMap() },
                    "locationStates" to state.locationStates.mapValues { (_, value) -> value.toMap() }
                )

                db.collection("partidas")
                    .document(currentCharId)
                    .collection("worldMap")
                    .document("state")
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.w("WORLDMAP", "Error guardando mapa: ${e.message}")
            }
        }
    }

    fun processAdventureStep(storyText: String, locationJson: String?) {
        viewModelScope.launch {
            val location = parseLocationFromJson(locationJson)
                ?: detectLocationFromText(storyText)

            if (location != null) {
                updateCurrentLocation(location)
            }

            val targetLocationId = location?.id ?: _worldMapState.value.currentLocationId
            if (targetLocationId.isNotBlank()) {
                applyStoryConsequences(storyText, targetLocationId)
            }
        }
    }

    private fun parseLocationFromJson(json: String?): WorldLocation? {
        if (json.isNullOrBlank()) return null

        return try {
            val raw = gson.fromJson(json, Map::class.java)
            val name = raw["name"] as? String ?: return null
            val desc = raw["description"] as? String ?: ""
            val type = normalizeLocationType(raw["type"] as? String, "$name $desc")

            WorldLocation(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                description = desc,
                type = type,
                icon = locationIcons[type] ?: "📍",
                x = (raw["x"] as? Double)?.toFloat() ?: generateX(name),
                y = (raw["y"] as? Double)?.toFloat() ?: generateY(name)
            )
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private fun detectLocationFromText(text: String): WorldLocation? {
        val locationPatterns = listOf(
            Regex("llegas? (?:a|al|a la|a los|a las) ([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)", RegexOption.MULTILINE),
            Regex("entras? (?:en|a|al) (?:el |la |los |las )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)", RegexOption.MULTILINE),
            Regex("(?:te encuentras?|estas?) en (?:el |la |los |las )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)", RegexOption.MULTILINE),
            Regex("(?:el |la )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,25}?) (?:se alza|aparece|surge|yace) ante ti", RegexOption.MULTILINE)
        )

        for (pattern in locationPatterns) {
            val match = pattern.find(text) ?: continue
            val rawName = match.groupValues[1].trim()
            if (rawName.length < 3) continue

            val type = detectType(rawName, text)
            return WorldLocation(
                id = rawName.lowercase().replace(" ", "_"),
                name = rawName,
                description = extractSentenceContaining(text, rawName),
                type = type,
                icon = locationIcons[type] ?: "📍",
                x = generateX(rawName),
                y = generateY(rawName)
            )
        }

        return null
    }

    private fun detectType(name: String, context: String): String {
        return normalizeLocationType(name, context)
    }

    private fun extractSentenceContaining(text: String, keyword: String): String {
        return text.split(Regex("[.!?]"))
            .firstOrNull { it.contains(keyword, ignoreCase = true) }
            ?.trim()
            ?: text.take(100)
    }

    private fun generateX(name: String): Float {
        val hash = name.hashCode()
        return ((hash and 0xFF).toFloat() / 255f) * 0.8f + 0.1f
    }

    private fun generateY(name: String): Float {
        val hash = name.hashCode()
        return (((hash shr 8) and 0xFF).toFloat() / 255f) * 0.8f + 0.1f
    }

    private fun updateCurrentLocation(newLocation: WorldLocation) {
        val state = _worldMapState.value
        val alreadyExists = state.locations.any { it.id == newLocation.id }

        val updatedLocations = if (alreadyExists) {
            state.locations.map { loc ->
                loc.copy(isCurrentLocation = loc.id == newLocation.id)
            }
        } else {
            state.locations.map { it.copy(isCurrentLocation = false) } +
                    newLocation.copy(isCurrentLocation = true)
        }

        val updatedStates = if (state.locationStates.containsKey(newLocation.id)) {
            state.locationStates
        } else {
            state.locationStates + (newLocation.id to initialLifeStateFor(newLocation))
        }

        _worldMapState.value = state.copy(
            locations = updatedLocations,
            currentLocationId = newLocation.id,
            locationStates = updatedStates
        )

        Log.d(
            "WORLDMAP",
            "Ubicación actualizada: ${newLocation.name} (total: ${updatedLocations.size})"
        )
        saveMap()
    }

    private fun initialLifeStateFor(location: WorldLocation): LocationLifeState {
        val normalizedType = normalizeLocationType(location.type, "${location.name} ${location.description}")

        return when (normalizedType) {
            "ciudad" -> LocationLifeState(
                locationId = location.id,
                prosperity = 65,
                security = 60,
                danger = 20,
                corruption = 10,
                mood = "activo"
            )
            "pueblo" -> LocationLifeState(
                locationId = location.id,
                prosperity = 55,
                security = 55,
                danger = 25,
                corruption = 8,
                mood = "tranquilo"
            )
            "taberna", "tienda" -> LocationLifeState(
                locationId = location.id,
                prosperity = 60,
                security = 50,
                danger = 18,
                corruption = 6,
                mood = "animado"
            )
            "bosque" -> LocationLifeState(
                locationId = location.id,
                prosperity = 35,
                security = 30,
                danger = 45,
                corruption = 15,
                mood = "salvaje"
            )
            "cueva", "mazmorra", "ruina" -> LocationLifeState(
                locationId = location.id,
                prosperity = 10,
                security = 8,
                danger = 75,
                corruption = 35,
                mood = "hostil"
            )
            "montaña" -> LocationLifeState(
                locationId = location.id,
                prosperity = 18,
                security = 25,
                danger = 60,
                corruption = 12,
                mood = "inhóspito"
            )
            "mar", "océano", "lago" -> LocationLifeState(
                locationId = location.id,
                prosperity = 20,
                security = 20,
                danger = 50,
                corruption = 8,
                mood = "inestable"
            )
            else -> LocationLifeState(
                locationId = location.id,
                prosperity = 40,
                security = 40,
                danger = 35,
                corruption = 10,
                mood = "estable"
            )
        }
    }

    private fun applyStoryConsequences(storyText: String, locationId: String) {
        val text = normalizeText(storyText)
        if (text.isBlank()) return

        var prosperityDelta = 0
        var securityDelta = 0
        var dangerDelta = 0
        var corruptionDelta = 0
        val notes = mutableListOf<String>()

        if (text.containsAnyWorld(
                "salvas", "rescatas", "defiendes", "proteges",
                "ayudas", "liberas", "limpias", "purificas", "reconstruyes"
            )
        ) {
            prosperityDelta += 6
            securityDelta += 8
            dangerDelta -= 6
            corruptionDelta -= 4
            notes += "La zona mejora gracias a tus actos."
        }

        if (text.containsAnyWorld(
                "saqueo", "arrasado", "incendio", "asedio",
                "bandidos", "ataque", "plaga", "maldicion",
                "corrompido", "cultistas"
            )
        ) {
            prosperityDelta -= 5
            securityDelta -= 7
            dangerDelta += 9
            corruptionDelta += 8
            notes += "La zona sufre una crisis."
        }

        if (text.containsAnyWorld(
                "mercado", "comercio", "caravana", "festival",
                "fiesta", "taberna", "abastecido"
            )
        ) {
            prosperityDelta += 7
            notes += "La economía local florece."
        }

        if (text.containsAnyWorld(
                "sombras", "tenebroso", "muertos",
                "demonio", "mal oscuro", "ritual"
            )
        ) {
            dangerDelta += 5
            corruptionDelta += 6
            notes += "Una presencia oscura se intensifica."
        }

        if (prosperityDelta == 0 && securityDelta == 0 && dangerDelta == 0 && corruptionDelta == 0) {
            return
        }

        val state = _worldMapState.value
        val current = state.locationStates[locationId] ?: LocationLifeState(locationId = locationId)

        val updatedProsperity = clampStat(current.prosperity + prosperityDelta)
        val updatedSecurity = clampStat(current.security + securityDelta)
        val updatedDanger = clampStat(current.danger + dangerDelta)
        val updatedCorruption = clampStat(current.corruption + corruptionDelta)

        val updated = current.copy(
            prosperity = updatedProsperity,
            security = updatedSecurity,
            danger = updatedDanger,
            corruption = updatedCorruption,
            mood = resolveMood(
                prosperity = updatedProsperity,
                security = updatedSecurity,
                danger = updatedDanger,
                corruption = updatedCorruption
            ),
            lastEventSummary = notes.joinToString(" ").ifBlank { current.lastEventSummary },
            lastUpdatedAt = System.currentTimeMillis()
        )

        _worldMapState.value = state.copy(
            locationStates = state.locationStates + (locationId to updated)
        )

        saveMap()
    }

    private fun resolveMood(
        prosperity: Int,
        security: Int,
        danger: Int,
        corruption: Int
    ): String {
        return when {
            corruption >= 70 -> "corrupto"
            danger >= 70 -> "asediado"
            prosperity >= 70 && security >= 60 -> "próspero"
            security <= 25 -> "inseguro"
            prosperity <= 20 -> "decadente"
            else -> "estable"
        }
    }

    private fun clampStat(value: Int): Int = value.coerceIn(0, 100)

    private fun normalizeLocationType(rawType: String?, context: String = ""): String {
        val raw = listOfNotNull(rawType, context)
            .joinToString(" ")
            .lowercase()
            .trim()

        if (raw.isBlank()) return "lugar"

        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        return when {
            listOf("ciudad", "metropoli", "capital").any { it in normalized } -> "ciudad"
            listOf("pueblo", "aldea", "villa").any { it in normalized } -> "pueblo"
            listOf("mazmorra", "cripta", "calabozo", "dungeon").any { it in normalized } -> "mazmorra"
            listOf("bosque", "selva", "arboleda").any { it in normalized } -> "bosque"
            listOf("montana", "pico", "cordillera").any { it in normalized } -> "montaña"
            listOf("oceano", "alta mar", "mar abierto").any { it in normalized } -> "océano"
            listOf("mar", "playa", "costa", "litoral", "bahia", "puerto", "muelle").any { it in normalized } -> "mar"
            listOf("lago", "rio", "laguna", "arroyo", "estanque").any { it in normalized } -> "lago"
            listOf("desierto", "duna", "arena", "arido").any { it in normalized } -> "desierto"
            listOf("cueva", "gruta", "caverna", "cavidad").any { it in normalized } -> "cueva"
            listOf("taberna", "posada", "meson", "cantina").any { it in normalized } -> "taberna"
            listOf("templo", "santuario", "altar").any { it in normalized } -> "templo"
            listOf("ruina", "ruinas").any { it in normalized } -> "ruina"
            "torre" in normalized -> "torre"
            listOf("llanura", "pradera", "campo").any { it in normalized } -> "llanura"
            else -> "lugar"
        }
    }

    private fun WorldLocation.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "x" to x,
        "y" to y,
        "icon" to icon,
        "type" to type,
        "isCurrentLocation" to isCurrentLocation,
        "discoveredAt" to discoveredAt
    )

    private fun Map<String, Any>.toWorldLocation(): WorldLocation = WorldLocation(
        id = this["id"] as? String ?: "",
        name = this["name"] as? String ?: "",
        description = this["description"] as? String ?: "",
        x = (this["x"] as? Number)?.toFloat() ?: 0.5f,
        y = (this["y"] as? Number)?.toFloat() ?: 0.5f,
        icon = this["icon"] as? String ?: "📍",
        type = normalizeLocationType(
            this["type"] as? String,
            "${this["name"] as? String ?: ""} ${this["description"] as? String ?: ""}"
        ),
        isCurrentLocation = this["isCurrentLocation"] as? Boolean ?: false,
        discoveredAt = (this["discoveredAt"] as? Number)?.toLong() ?: 0L
    )
}

private fun normalizeText(value: String): String =
    Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

private fun String.containsAnyWorld(vararg options: String): Boolean =
    options.any { this.contains(it) }

private fun LocationLifeState.toMap(): Map<String, Any> = mapOf(
    "locationId" to locationId,
    "prosperity" to prosperity,
    "security" to security,
    "danger" to danger,
    "corruption" to corruption,
    "mood" to mood,
    "controllingFactionId" to controllingFactionId,
    "lastEventSummary" to lastEventSummary,
    "lastUpdatedAt" to lastUpdatedAt
)

private fun Map<String, Any>.toLocationLifeState(locationId: String): LocationLifeState =
    LocationLifeState(
        locationId = this["locationId"] as? String ?: locationId,
        prosperity = (this["prosperity"] as? Number)?.toInt() ?: 50,
        security = (this["security"] as? Number)?.toInt() ?: 50,
        danger = (this["danger"] as? Number)?.toInt() ?: 20,
        corruption = (this["corruption"] as? Number)?.toInt() ?: 0,
        mood = this["mood"] as? String ?: "estable",
        controllingFactionId = this["controllingFactionId"] as? String ?: "",
        lastEventSummary = this["lastEventSummary"] as? String ?: "",
        lastUpdatedAt = (this["lastUpdatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )