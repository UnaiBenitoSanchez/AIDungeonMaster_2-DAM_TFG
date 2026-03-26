package com.example.aidungeonmaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * ViewModel que gestiona el mapa del mundo.
 *
 * - Parsea las respuestas del DM para detectar ubicaciones nuevas.
 * - Persiste el mapa en Firestore bajo "partidas/{gameId}/worldMap".
 * - Expone [worldMapState] para que la UI lo observe.
 */
class WorldMapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()

    private val _worldMapState = MutableStateFlow(WorldMapState())
    val worldMapState = _worldMapState.asStateFlow()

    /**
     * Usamos el charId del personaje (formato: "{userId}_{characterName}") como clave
     * para que TODAS las aventuras del mismo personaje compartan el mismo mapa del mundo.
     * Antes usábamos el gameId (que incluía el tema), lo que generaba mapas separados por tema.
     */
    private var currentCharId: String = ""

    // ── TIPOS DE LUGAR Y SUS ICONOS ──────────────────────────────────────────

    private val locationIcons = mapOf(
        "ciudad"    to "🏰",
        "pueblo"    to "🏘️",
        "mazmorra"  to "⚔️",
        "bosque"    to "🌲",
        "montaña"   to "⛰️",
        "mar"       to "🌊",
        "océano"    to "🌐",
        "desierto"  to "🏜️",
        "cueva"     to "🕳️",
        "taberna"   to "🍺",
        "templo"    to "⛩️",
        "ruina"     to "🏚️",
        "torre"     to "🗼",
        "lago"      to "💧",
        "llanura"   to "🌾",
        "lugar"     to "📍"
    )

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

    // ── CARGA / GUARDADO ─────────────────────────────────────────────────────

    /**
     * Carga el mapa del personaje.
     * @param charId  Identificador del personaje: "{userId}_{characterName}" (SIN el tema).
     */
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
                    val mapName   = doc.getString("mapName") ?: "Mundo Desconocido"
                    val currentId = doc.getString("currentLocationId") ?: ""

                    _worldMapState.value = WorldMapState(
                        locations           = locations,
                        currentLocationId   = currentId,
                        mapName             = mapName
                    )
                    Log.d("WORLDMAP", "Mapa cargado: ${locations.size} ubicaciones")
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
                    "mapName"           to state.mapName,
                    "currentLocationId" to state.currentLocationId,
                    "locations"         to state.locations.map { it.toMap() }
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

    // ── DETECCIÓN AUTOMÁTICA DE UBICACIONES A PARTIR DEL TEXTO DEL DM ───────

    /**
     * Analiza la narrativa del DM (story) e intenta extraer una nueva ubicación
     * utilizando una respuesta JSON estructurada que el DM debe incluir.
     *
     * Se llama desde [GameViewModel] después de cada paso de aventura.
     *
     * @param storyText     El texto narrativo del DM
     * @param locationJson  JSON opcional que el DM incluye con datos de la ubicación
     *                      Formato: {"name":"...","type":"...","description":"..."}
     */
    fun processAdventureStep(storyText: String, locationJson: String?) {
        viewModelScope.launch {
            val location = parseLocationFromJson(locationJson)
                ?: detectLocationFromText(storyText)
                ?: return@launch

            updateCurrentLocation(location)
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
                id          = name.lowercase().replace(" ", "_"),
                name        = name,
                description = desc,
                type        = type,
                icon        = locationIcons[type] ?: "📍",
                x           = (raw["x"] as? Double)?.toFloat() ?: generateX(name),
                y           = (raw["y"] as? Double)?.toFloat() ?: generateY(name)
            )
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Heurístico de último recurso: busca patrones de lugar en el texto narrativo.
     */
    private fun detectLocationFromText(text: String): WorldLocation? {
        val locationPatterns = listOf(
            Regex("llegas? (?:a|al|a la|a los|a las) ([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)", RegexOption.MULTILINE),
            Regex("entras? (?:en|a|al) (?:el |la |los |las )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)", RegexOption.MULTILINE),
            Regex("(?:te encuentras?|estás?) en (?:el |la |los |las )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,30}?)(?:[,.]|$)", RegexOption.MULTILINE),
            Regex("(?:el |la )?([A-ZÁÉÍÓÚÑ][\\w\\sÁÉÍÓÚáéíóú]{2,25}?) (?:se alza|aparece|surge|yace) ante ti", RegexOption.MULTILINE)
        )

        for (pattern in locationPatterns) {
            val match = pattern.find(text) ?: continue
            val rawName = match.groupValues[1].trim()
            if (rawName.length < 3) continue

            val type = detectType(rawName, text)
            return WorldLocation(
                id          = rawName.lowercase().replace(" ", "_"),
                name        = rawName,
                description = extractSentenceContaining(text, rawName),
                type        = type,
                icon        = locationIcons[type] ?: "📍",
                x           = generateX(rawName),
                y           = generateY(rawName)
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

    /**
     * Genera una posición X pseudo-aleatoria pero determinista basada en el nombre.
     * Así dos ejecuciones con el mismo lugar producen la misma posición.
     */
    private fun generateX(name: String): Float {
        val hash = name.hashCode()
        return ((hash and 0xFF).toFloat() / 255f) * 0.8f + 0.1f  // Rango: 0.1 – 0.9
    }

    private fun generateY(name: String): Float {
        val hash = name.hashCode()
        return (((hash shr 8) and 0xFF).toFloat() / 255f) * 0.8f + 0.1f
    }

    // ── ACTUALIZAR UBICACIÓN ACTUAL ──────────────────────────────────────────

    private fun updateCurrentLocation(newLocation: WorldLocation) {
        val state = _worldMapState.value
        val alreadyExists = state.locations.any { it.id == newLocation.id }

        val updatedLocations = if (alreadyExists) {
            // Solo actualizar isCurrentLocation
            state.locations.map { loc ->
                loc.copy(isCurrentLocation = loc.id == newLocation.id)
            }
        } else {
            // Añadir nueva ubicación y actualizar flags
            val withNew = state.locations.map { it.copy(isCurrentLocation = false) } +
                    newLocation.copy(isCurrentLocation = true)
            withNew
        }

        _worldMapState.value = state.copy(
            locations           = updatedLocations,
            currentLocationId   = newLocation.id
        )

        Log.d("WORLDMAP", "Ubicación actualizada: ${newLocation.name} (total: ${updatedLocations.size})")
        saveMap()
    }

    // ── UTILS DE SERIALIZACIÓN ───────────────────────────────────────────────

    private fun WorldLocation.toMap(): Map<String, Any> = mapOf(
        "id"                to id,
        "name"              to name,
        "description"       to description,
        "x"                 to x,
        "y"                 to y,
        "icon"              to icon,
        "type"              to type,
        "isCurrentLocation" to isCurrentLocation,
        "discoveredAt"      to discoveredAt
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toWorldLocation(): WorldLocation = WorldLocation(
        id                  = this["id"] as? String ?: "",
        name                = this["name"] as? String ?: "",
        description         = this["description"] as? String ?: "",
        x                   = (this["x"] as? Number)?.toFloat() ?: 0.5f,
        y                   = (this["y"] as? Number)?.toFloat() ?: 0.5f,
        icon                = this["icon"] as? String ?: "📍",
        type                = normalizeLocationType(
            this["type"] as? String,
            "${this["name"] as? String ?: ""} ${this["description"] as? String ?: ""}"
        ),
        isCurrentLocation   = this["isCurrentLocation"] as? Boolean ?: false,
        discoveredAt        = (this["discoveredAt"] as? Number)?.toLong() ?: 0L
    )
}