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
import kotlin.math.absoluteValue

class WorldMapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()

    private val _worldMapState = MutableStateFlow(WorldMapState())
    val worldMapState = _worldMapState.asStateFlow()

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
        "rio" to "🏞️",
        "cascada" to "💦",
        "tienda" to "🛒",
        "cabana" to "🪵",
        "llanura" to "🌾",
        "asediada" to "🔥",
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
                    val locationsRaw = doc.get("locations") as? List<*>
                    val locations = locationsRaw
                        ?.mapNotNull { raw ->
                            (raw as? Map<String, Any>)?.toWorldLocation()
                        }
                        ?: emptyList()

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

                    val recentEvents = (doc.get("recentWorldEvents") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()

                    val lastSimulationAt = doc.getLong("lastWorldSimulationAt") ?: 0L

                    _worldMapState.value = WorldMapState(
                        locations = locations,
                        currentLocationId = currentId,
                        mapName = mapName,
                        locationStates = locationStates,
                        recentWorldEvents = recentEvents,
                        lastWorldSimulationAt = lastSimulationAt
                    )

                    Log.d(
                        "WORLDMAP",
                        "Mapa cargado: ${locations.size} ubicaciones, ${locationStates.size} estados"
                    )

                    simulateWorldIfNeeded()
                } else {
                    _worldMapState.value = WorldMapState(
                        lastWorldSimulationAt = System.currentTimeMillis()
                    )
                    saveMap()
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
                    "locationStates" to state.locationStates.mapValues { (_, value) -> value.toMap() },
                    "recentWorldEvents" to state.recentWorldEvents,
                    "lastWorldSimulationAt" to state.lastWorldSimulationAt
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
            simulateWorldIfNeeded()

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
            val raw = gson.fromJson(json, Map::class.java) ?: return null

            val name = raw["name"] as? String ?: return null
            val desc = raw["description"] as? String ?: ""
            val type = normalizeLocationType(raw["type"] as? String, "$name $desc")

            WorldLocation(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                description = desc,
                type = type,
                icon = locationIcons[type] ?: "📍",
                x = extractFloat(raw["x"]) ?: generateX(name),
                y = extractFloat(raw["y"]) ?: generateY(name)
            )
        } catch (_: JsonSyntaxException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun detectLocationFromText(text: String): WorldLocation? {
        val locationPatterns = listOf(
            Regex(
                "llegas? (?:a|al|a la|a los|a las) ([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)",
                RegexOption.MULTILINE
            ),
            Regex(
                "entras? (?:en|a|al) (?:el |la |los |las )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)",
                RegexOption.MULTILINE
            ),
            Regex(
                "(?:te encuentras?|estás?) en (?:el |la |los |las )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)",
                RegexOption.MULTILINE
            ),
            Regex(
                "(?:el |la )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,25}?) (?:se alza|aparece|surge|yace) ante ti",
                RegexOption.MULTILINE
            )
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
                if (loc.id == newLocation.id) {
                    loc.copy(
                        name = newLocation.name,
                        description = newLocation.description,
                        icon = newLocation.icon,
                        type = newLocation.type,
                        x = newLocation.x,
                        y = newLocation.y,
                        isCurrentLocation = true
                    )
                } else {
                    loc.copy(isCurrentLocation = false)
                }
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

            "taberna", "tienda", "cabana" -> LocationLifeState(
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

            "cueva", "mazmorra", "ruina", "asediada" -> LocationLifeState(
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

            "mar", "océano", "lago", "rio", "cascada" -> LocationLifeState(
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
                "salvas", "rescatas", "defiendes", "proteges", "ayudas",
                "liberas", "limpias", "purificas", "reconstruyes"
            )
        ) {
            prosperityDelta += 6
            securityDelta += 8
            dangerDelta -= 6
            corruptionDelta -= 4
            notes += "La zona mejora gracias a tus actos."
        }

        if (text.containsAnyWorld(
                "saqueo", "arrasado", "incendio", "asedio", "bandidos",
                "ataque", "plaga", "maldicion", "corrompido", "cultistas"
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

        if (prosperityDelta == 0 &&
            securityDelta == 0 &&
            dangerDelta == 0 &&
            corruptionDelta == 0
        ) {
            return
        }

        val state = _worldMapState.value
        val current = state.locationStates[locationId]
            ?: LocationLifeState(locationId = locationId)

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

    private fun simulateWorldIfNeeded() {
        val state = _worldMapState.value
        val now = System.currentTimeMillis()
        val anchor = if (state.lastWorldSimulationAt > 0L) state.lastWorldSimulationAt else now
        val elapsed = (now - anchor).coerceAtLeast(0L)
        val ticks = (elapsed / WORLD_TICK_MS).toInt().coerceAtMost(12)

        if (ticks <= 0) {
            if (state.lastWorldSimulationAt == 0L) {
                _worldMapState.value = state.copy(lastWorldSimulationAt = now)
                saveMap()
            }
            return
        }

        var updatedState = state
        val news = updatedState.recentWorldEvents.toMutableList()
        val baseTickIndex = (anchor / WORLD_TICK_MS).toInt()

        repeat(ticks) { tickOffset ->
            val tickIndex = baseTickIndex + tickOffset + 1
            updatedState = evolveOneWorldTick(updatedState, tickIndex, news)
        }

        updatedState = updatedState.copy(
            recentWorldEvents = news.takeLast(8),
            lastWorldSimulationAt = anchor + ticks * WORLD_TICK_MS
        )

        _worldMapState.value = updatedState
        saveMap()
    }

    private fun evolveOneWorldTick(
        state: WorldMapState,
        tickIndex: Int,
        news: MutableList<String>
    ): WorldMapState {
        val updatedStates = state.locationStates.toMutableMap()

        state.locations.forEach { location ->
            if (location.id == state.currentLocationId) return@forEach

            val current = updatedStates[location.id] ?: initialLifeStateFor(location)
            val evolved = evolveLocation(location, current, tickIndex)
            updatedStates[location.id] = evolved

            if (evolved.lastEventSummary.isNotBlank() &&
                evolved.lastEventSummary != current.lastEventSummary
            ) {
                news += "${location.name}: ${evolved.lastEventSummary}"
            }
        }

        return state.copy(locationStates = updatedStates)
    }

    private fun evolveLocation(
        location: WorldLocation,
        state: LocationLifeState,
        tickIndex: Int
    ): LocationLifeState {
        val biome = normalizeLocationType(location.type, "${location.name} ${location.description}")
        val seed = ("${location.id}:$tickIndex").hashCode().absoluteValue

        val prosperityDrift = drift(seed, biome, DriftChannel.PROSPERITY)
        val securityDrift = drift(seed + 17, biome, DriftChannel.SECURITY)
        val dangerDrift = drift(seed + 29, biome, DriftChannel.DANGER)
        val corruptionDrift = drift(seed + 43, biome, DriftChannel.CORRUPTION)

        val newProsperity = clampStat(state.prosperity + prosperityDrift)
        val newSecurity = clampStat(state.security + securityDrift)
        val newDanger = clampStat(state.danger + dangerDrift)
        val newCorruption = clampStat(state.corruption + corruptionDrift)

        return state.copy(
            prosperity = newProsperity,
            security = newSecurity,
            danger = newDanger,
            corruption = newCorruption,
            mood = resolveMood(
                prosperity = newProsperity,
                security = newSecurity,
                danger = newDanger,
                corruption = newCorruption
            ),
            lastEventSummary = worldEventSummary(
                location.name,
                biome,
                prosperityDrift,
                securityDrift,
                dangerDrift,
                corruptionDrift
            ),
            lastUpdatedAt = System.currentTimeMillis()
        )
    }

    private fun drift(seed: Int, biome: String, channel: DriftChannel): Int {
        val roll = seed.absoluteValue % 100

        return when (channel) {
            DriftChannel.PROSPERITY -> when (biome) {
                "ciudad", "pueblo", "taberna", "templo", "tienda" -> when {
                    roll < 20 -> 2
                    roll < 32 -> 1
                    roll > 92 -> -2
                    roll > 82 -> -1
                    else -> 0
                }

                "bosque", "cueva", "mazmorra", "ruina", "asediada" -> when {
                    roll < 12 -> 1
                    roll > 86 -> -2
                    roll > 76 -> -1
                    else -> 0
                }

                else -> when {
                    roll < 18 -> 1
                    roll > 88 -> -1
                    else -> 0
                }
            }

            DriftChannel.SECURITY -> when (biome) {
                "ciudad", "pueblo", "tienda", "taberna", "cabana" -> when {
                    roll < 18 -> 2
                    roll < 28 -> 1
                    roll > 92 -> -2
                    roll > 80 -> -1
                    else -> 0
                }

                "cueva", "mazmorra", "ruina", "bosque", "asediada" -> when {
                    roll < 10 -> 1
                    roll > 72 -> -2
                    roll > 58 -> -1
                    else -> 0
                }

                else -> when {
                    roll < 14 -> 1
                    roll > 84 -> -1
                    else -> 0
                }
            }

            DriftChannel.DANGER -> when (biome) {
                "cueva", "mazmorra", "ruina", "bosque", "asediada" -> when {
                    roll < 16 -> -1
                    roll > 68 -> 2
                    roll > 54 -> 1
                    else -> 0
                }

                "ciudad", "pueblo", "taberna", "templo", "tienda", "cabana" -> when {
                    roll < 25 -> -1
                    roll > 90 -> 2
                    roll > 76 -> 1
                    else -> 0
                }

                else -> when {
                    roll < 18 -> -1
                    roll > 80 -> 1
                    else -> 0
                }
            }

            DriftChannel.CORRUPTION -> when (biome) {
                "cueva", "mazmorra", "ruina", "asediada" -> when {
                    roll < 15 -> -1
                    roll > 74 -> 2
                    roll > 60 -> 1
                    else -> 0
                }

                "ciudad", "pueblo", "tienda", "taberna" -> when {
                    roll < 18 -> -1
                    roll > 86 -> 1
                    else -> 0
                }

                else -> when {
                    roll < 15 -> -1
                    roll > 84 -> 1
                    else -> 0
                }
            }
        }
    }

    private fun worldEventSummary(
        locationName: String,
        biome: String,
        prosperityDrift: Int,
        securityDrift: Int,
        dangerDrift: Int,
        corruptionDrift: Int
    ): String {
        return when {
            dangerDrift >= 2 -> "Nuevas amenazas acechan en las cercanías."
            corruptionDrift >= 2 -> "Una sombra turbia se extiende por la zona."
            prosperityDrift >= 2 -> "La actividad local crece incluso sin tu presencia."
            securityDrift >= 2 -> "Los habitantes refuerzan sus defensas."
            dangerDrift <= -1 && securityDrift >= 1 -> "Las patrullas mantienen el peligro a raya."
            prosperityDrift <= -2 -> if (biome == "ciudad" || biome == "pueblo") {
                "El comercio de $locationName se enfría un poco."
            } else {
                "La zona se vuelve más áspera y estéril."
            }

            corruptionDrift <= -1 -> "La influencia oscura retrocede lentamente."
            else -> "El lugar sigue cambiando mientras estás lejos."
        }
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

    private fun normalizeText(value: String): String =
        Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    private fun String.containsAnyWorld(vararg options: String): Boolean =
        options.any { this.contains(it) }

    private fun normalizeLocationType(rawType: String?, context: String = ""): String {
        val raw = listOfNotNull(rawType, context)
            .joinToString(" ")
            .lowercase()
            .trim()

        if (raw.isBlank()) return "lugar"

        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        return when {
            listOf("asediada", "asediado", "asedio").any { it in normalized } -> "asediada"
            listOf("cascada", "catarata", "salto de agua").any { it in normalized } -> "cascada"
            listOf("rio", "arroyo", "quebrada", "afluente", "ribera").any { it in normalized } -> "rio"
            listOf("tienda", "mercado", "puesto", "comercio", "almacen", "shop", "store").any { it in normalized } -> "tienda"
            listOf("cabana", "choza", "refugio", "casita", "cottage", "hut").any { it in normalized } -> "cabana"
            listOf("ciudad", "metropoli", "capital").any { it in normalized } -> "ciudad"
            listOf("pueblo", "aldea", "villa").any { it in normalized } -> "pueblo"
            listOf("mazmorra", "cripta", "calabozo", "dungeon").any { it in normalized } -> "mazmorra"
            listOf("bosque", "selva", "arboleda").any { it in normalized } -> "bosque"
            listOf("montana", "pico", "cordillera").any { it in normalized } -> "montaña"
            listOf("oceano", "alta mar", "mar abierto").any { it in normalized } -> "océano"
            listOf("mar", "playa", "costa", "litoral", "bahia", "puerto", "muelle").any { it in normalized } -> "mar"
            listOf("lago", "laguna", "estanque").any { it in normalized } -> "lago"
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

    private fun extractFloat(value: Any?): Float? {
        return when (value) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull()
            else -> null
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
            lastUpdatedAt = (this["lastUpdatedAt"] as? Number)?.toLong()
                ?: System.currentTimeMillis()
        )
}

private const val WORLD_TICK_MS = 6L * 60L * 60L * 1000L

private enum class DriftChannel {
    PROSPERITY,
    SECURITY,
    DANGER,
    CORRUPTION
}