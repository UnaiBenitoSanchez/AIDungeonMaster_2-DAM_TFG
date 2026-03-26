package com.example.aidungeonmaster.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.api.ChatMessage
import com.example.aidungeonmaster.data.api.GroqRequest
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.ui.game.GameRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.aidungeonmaster.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ── MODELOS DE DATOS ─────────────────────────────────────────────────────────

data class Enemy(
    val name: String = "Enemigo",
    val hpMax: Int = 20,
    val hpCurrent: Int = 20,
    val attackDamage: String = "1d6",
    val goldCoins: Int = 0   // Monedas que suelta al ser derrotado
)

data class AdventureStep(
    val story: String = "",
    val options: List<String> = emptyList(),
    val damageTaken: Int = 0,
    val healingReceived: Int = 0,
    val itemFound: Item? = null,
    val combatStarted: Boolean = false,
    val enemy: Enemy? = null,
    val coinsFound: Int = 0,   // Monedas encontradas en la aventura (sin combate)
    // ── NUEVO: JSON de la ubicación actual del jugador ──────────────────────
    // El DM lo rellena cuando el jugador llega a un lugar nuevo.
    // Formato: {"name":"...","type":"ciudad|bosque|...","description":"..."}
    val locationJson: String? = null
)

// ── VIEWMODEL ────────────────────────────────────────────────────────────────

class GameViewModel : ViewModel() {

    private val repository = GameRepository()
    private var currentGameId: String = ""
    private var currentCharId: String = ""
    private var currentUserId: String = ""
    private var currentCharacterName: String = ""
    private var currentTheme: String = ""
    private val gson = Gson()
    private val chatHistory = mutableListOf<ChatMessage>()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Referencia al WorldMapViewModel para notificarle ubicaciones nuevas.
     * Se inyecta desde GamePlayScreen/AppNavigation.
     */
    var worldMapViewModel: WorldMapViewModel? = null

    private val MAX_HISTORY = 14

    private val apiService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(com.example.aidungeonmaster.data.api.GroqApiService::class.java)

    private val apiKey = "Bearer ${BuildConfig.GROQ_API_KEY}"

    private val _messages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<String>>(emptyList())
    val currentOptions = _currentOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentAdventureStep = MutableStateFlow<AdventureStep?>(null)
    val currentAdventureStep = _currentAdventureStep.asStateFlow()

    private val _stepEffect = MutableSharedFlow<AdventureStep>(replay = 0, extraBufferCapacity = 1)
    val stepEffect = _stepEffect.asSharedFlow()

    // ── INICIO / CARGA ────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun startStory(userId: String, characterName: String, theme: String) {
        currentUserId        = userId
        currentCharacterName = characterName
        currentTheme         = theme
        currentGameId        = "${userId}_${characterName}_${theme}".replace(" ", "_")
        currentCharId        = "${userId}_${characterName}"

        viewModelScope.launch {
            saveCharacterClassToGame(userId, characterName)
            _isLoading.value = true
            try {
                val savedData = repository.loadGame(currentGameId)
                if (savedData != null) {
                    val rawMessages = savedData["displayMessages"] as? List<Map<String, String>>
                    _messages.value = rawMessages
                        ?.map { (it["first"] ?: "") to (it["second"] ?: "") }
                        ?: emptyList()
                    _currentOptions.value =
                        (savedData["lastOptions"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    val savedHistory = savedData["chatHistory"] as? List<Map<String, String>>
                    chatHistory.clear()
                    savedHistory?.forEach {
                        chatHistory.add(ChatMessage(role = it["role"] ?: "user", content = it["content"] ?: ""))
                    }
                    _isLoading.value = false
                } else {
                    executeGroqCall(getInitialPrompt(characterName, theme))
                }
            } catch (e: Exception) {
                showError("Error de conexión: ${e.localizedMessage}")
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun resetStory() {
        viewModelScope.launch {
            try {
                db.collection("partidas").document(currentGameId).delete().await()
                Log.d("GM_RESET", "Historia borrada: $currentGameId")
            } catch (e: Exception) {
                Log.w("GM_RESET", "Error borrando historia: ${e.message}")
            }
            _messages.value       = emptyList()
            _currentOptions.value = emptyList()
            _currentAdventureStep.value = null
            chatHistory.clear()
            startStory(currentUserId, currentCharacterName, currentTheme)
        }
    }

    // ── ACCIONES DEL JUGADOR ─────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun sendPlayerAction(action: String) {
        _messages.value = _messages.value + ("Tú" to action)
        viewModelScope.launch { executeGroqCall("El jugador decide: $action") }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun sendCustomAction(action: String) {
        if (action.isBlank()) return
        _messages.value = _messages.value + ("Tú" to action)
        viewModelScope.launch { executeGroqCall("El jugador decide: $action") }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun notifyCombatEnd(victory: Boolean, enemyName: String) {
        val msg = if (victory)
            "He derrotado a $enemyName en combate épico."
        else
            "He sido derrotado por $enemyName pero consigo escapar malherido."
        sendPlayerAction(msg)
        _currentAdventureStep.value = _currentAdventureStep.value
            ?.copy(combatStarted = false, enemy = null)
    }

    // ── NÚCLEO: LLAMADA A GROQ CON REINTENTOS ────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun executeGroqCall(promptContent: String, attempt: Int = 1) {
        _isLoading.value = true
        try {
            chatHistory.add(ChatMessage(role = "user", content = promptContent))
            trimHistory()

            val request = GroqRequest(
                messages = listOf(ChatMessage(role = "system", content = buildSystemPrompt())) +
                        chatHistory.toList()
            )

            val rawContent = apiService.getCompletion(apiKey, request)
                .choices.firstOrNull()?.message?.content.orEmpty()

            Log.d("GM_RAW", "Intento $attempt — respuesta cruda: ${rawContent.take(300)}")

            val adventure = parseAdventureStep(rawContent)

            if (adventure == null) {
                if (attempt < 3) {
                    Log.w("GM_PARSE", "Fallo de parseo en intento $attempt, reintentando...")
                    chatHistory.add(ChatMessage(role = "assistant", content = rawContent.take(200)))
                    chatHistory.add(ChatMessage(
                        role    = "user",
                        content = "Tu respuesta no era JSON válido. Por favor, responde SOLO con el JSON especificado, sin ningún texto adicional."
                    ))
                    delay(600L * attempt)
                    executeGroqCall(promptContent, attempt + 1)
                } else {
                    Log.e("GM_PARSE", "3 intentos fallidos. Usando fallback.")
                    val fallback = extractStoryFallback(rawContent)
                    applyAdventureStep(fallback, isNew = true)
                }
                return
            }

            chatHistory.add(ChatMessage(role = "assistant", content = adventure.story))
            applyAdventureStep(adventure, isNew = true)
            saveCurrentGame()

        } catch (e: Exception) {
            Log.e("GM_ERROR", "executeGroqCall error: ${e.message}", e)
            if (attempt < 3) {
                delay(1000L * attempt)
                if (chatHistory.isNotEmpty()) chatHistory.removeLast()
                executeGroqCall(promptContent, attempt + 1)
            } else {
                showError("El DM necesita un momento para pensar... (intenta de nuevo)")
            }
        } finally {
            _isLoading.value = false
        }
    }

    // ── PARSEO ROBUSTO ───────────────────────────────────────────────────────

    private fun parseAdventureStep(raw: String): AdventureStep? {
        if (raw.isBlank()) return null
        val stripped = raw
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        val jsonCandidate = extractJsonObject(stripped) ?: stripped
        try {
            val step = gson.fromJson(jsonCandidate, AdventureStep::class.java)
            if (step != null && step.story.isNotBlank()) return step
        } catch (e: JsonSyntaxException) {
            Log.w("GM_PARSE", "Fallo JSON estricto, intentando reparación...")
        }
        val repaired = repairJson(jsonCandidate)
        return try {
            val step = gson.fromJson(repaired, AdventureStep::class.java)
            if (step?.story?.isNotBlank() == true) step else null
        } catch (e: Exception) { null }
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0; var inString = false; var escape = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escape          -> escape = false
                c == '\\'       -> if (inString) escape = true
                c == '"'        -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> { depth--; if (depth == 0) return text.substring(start, i + 1) }
            }
        }
        return null
    }

    private fun repairJson(raw: String): String {
        var s = raw.trim()
        if (!s.startsWith("{")) s = "{$s"
        if (!s.endsWith("}"))  s = "$s}"
        s = s.replace(Regex(",\\s*}"), "}").replace(Regex(",\\s*]"), "]")
        return s
    }

    private fun extractStoryFallback(raw: String): AdventureStep {
        val storyRegex = Regex(""""story"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val storyMatch = storyRegex.find(raw)
        val story = storyMatch?.groupValues?.get(1)
            ?.replace("\\n", "\n")?.replace("\\\"", "\"")
            ?: raw.replace(Regex("[{}\\[\\]\":]"), " ")
                .lines().firstOrNull { it.length > 20 }
            ?: "El Dungeon Master reflexiona sobre tu acción..."
        return AdventureStep(story = story, options = listOf("Continuar", "Observar el entorno", "Descansar"))
    }

    // ── APLICAR STEP AL ESTADO ───────────────────────────────────────────────

    private fun applyAdventureStep(step: AdventureStep, isNew: Boolean = false) {
        _messages.value = _messages.value + ("DM" to step.story)
        _currentOptions.value = step.options.filter { it.isNotBlank() }
        _currentAdventureStep.value = step
        if (isNew && (step.damageTaken > 0 || step.healingReceived > 0 || step.itemFound != null || step.coinsFound > 0)) {
            viewModelScope.launch { _stepEffect.emit(step) }
        }

        // ── NUEVO: Notificar al mapa sobre la ubicación ───────────────────
        if (isNew) {
            worldMapViewModel?.processAdventureStep(step.story, step.locationJson)
        }
    }

    private fun showError(msg: String) {
        _messages.value = _messages.value + ("DM" to msg)
    }

    // ── GESTIÓN DEL HISTORIAL ────────────────────────────────────────────────

    private fun trimHistory() {
        if (chatHistory.size <= MAX_HISTORY) return
        val keep = chatHistory.take(2) + chatHistory.takeLast(MAX_HISTORY - 2)
        chatHistory.clear(); chatHistory.addAll(keep)
        Log.d("GM_TRIM", "Historial recortado a ${chatHistory.size} mensajes")
    }

    // ── PROMPTS ───────────────────────────────────────────────────────────────

    private fun buildSystemPrompt(): String = """
Eres un Dungeon Master de rol. Responde ÚNICAMENTE con un objeto JSON válido, sin texto adicional, sin bloques de código markdown, sin explicaciones. Solo el JSON.

Estructura EXACTA (respeta los nombres de campo):
{"story":"narración aquí","options":["opción 1","opción 2","opción 3"],"damageTaken":0,"healingReceived":0,"itemFound":null,"combatStarted":false,"enemy":null,"coinsFound":0,"locationJson":null}

Reglas:
- "story": narrativa inmersiva en español, 2-4 frases.
- "options": exactamente 2-4 opciones cortas para el jugador.
- "combatStarted": true SOLO si hay combate activo. Entonces "enemy" debe tener {"name":"...","hpMax":N,"hpCurrent":N,"attackDamage":"XdY","goldCoins":N} donde goldCoins son las monedas que suelta el enemigo al morir (entre 5 y 50 según su nivel de dificultad).
- "damageTaken": daño recibido fuera de combate (0 si ninguno).
- "healingReceived": puntos de vida recuperados fuera de combate. 0 si ninguno.
- "itemFound": null o {"id":"","name":"...","type":"arma/pocion/armadura","description":"...","effect":"..."}.
- "coinsFound": monedas de oro encontradas en este paso fuera de combate (0 si ninguna). Úsalo cuando el jugador encuentra un tesoro, saquea un cofre, recibe una recompensa, etc. Valor entre 1 y 100.
- "locationJson": null O un JSON en STRING escapado cuando el jugador llega a un lugar NUEVO o diferente al anterior. Formato: "{\"name\":\"Nombre\",\"type\":\"ciudad|pueblo|mazmorra|bosque|montaña|cueva|taberna|templo|ruina|llanura|desierto|lago|mar|océano\",\"description\":\"Descripción breve del lugar\"}". Inclúyelo SOLO al cambiar de ubicación.
- Usa tipos canónicos. Si el lugar es océano, mar abierto o alta mar usa "océano". Si es un puerto, playa, costa o bahía usa "mar". Si es un río, arroyo, estanque o laguna usa "lago". Si es una gruta o caverna usa "cueva".
- NO uses comillas simples. NO añadas campos extra. NO envuelvas en markdown.
""".trimIndent()

    private suspend fun saveCharacterClassToGame(userId: String, characterName: String) {
        try {
            val snap = db.collection("users").document(userId)
                .collection("characters").whereEqualTo("name", characterName).get().await()
            val charDoc  = snap.documents.firstOrNull() ?: return
            val charClass = charDoc.getString("characterClass") ?: ""
            val hpMax     = charDoc.getLong("hpMax")?.toInt() ?: 20
            if (charClass.isNotBlank()) {
                db.collection("partidas").document(currentGameId)
                    .set(mapOf(
                        "characterClass" to charClass,
                        "characterName"  to characterName,
                        "userId"         to userId,
                        "hpMax"          to hpMax,
                        "hpCurrent"      to hpMax
                    ), SetOptions.merge()).await()
                Log.d("GM_CHAR", "Clase '$charClass' guardada en partida $currentGameId")
            }
        } catch (e: Exception) {
            Log.w("GM_CHAR", "saveCharacterClassToGame: ${e.message}")
        }
    }

    private fun getInitialPrompt(name: String, theme: String): String =
        "Inicia la aventura de $theme para el héroe llamado $name. Presenta el escenario de forma épica y ofrece 3 opciones iniciales. Si el inicio tiene un lugar concreto, inclúyelo en locationJson."

    // ── GUARDADO ──────────────────────────────────────────────────────────────

    private fun saveCurrentGame() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val data = mapOf(
                    "userId"          to currentUserId,
                    "displayMessages" to _messages.value.map { mapOf("first" to it.first, "second" to it.second) },
                    "chatHistory"     to chatHistory.map { mapOf("role" to it.role, "content" to it.content) },
                    "lastOptions"     to _currentOptions.value,
                    "timestamp"       to now,
                    "lastPlayed"      to now
                )
                repository.saveGame(currentGameId, data)

                if (currentCharId.isNotBlank()) {
                    db.collection("partidas").document(currentCharId)
                        .update("lastPlayed", now)
                        .await()
                }
            } catch (e: Exception) {
                Log.e("GM_SAVE", "Error guardando: ${e.message}")
            }
        }
    }

    private val _pendingXp = MutableStateFlow(0)
    val pendingXp = _pendingXp.asStateFlow()
    fun addPendingXp(xp: Int) { _pendingXp.value += xp }
    fun consumePendingXp() { _pendingXp.value = 0 }
}