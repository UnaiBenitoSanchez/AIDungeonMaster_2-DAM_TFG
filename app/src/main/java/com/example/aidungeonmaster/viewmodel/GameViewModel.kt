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
import kotlinx.coroutines.flow.MutableStateFlow
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
    val attackDamage: String = "1d6"
)

data class AdventureStep(
    val story: String = "",
    val options: List<String> = emptyList(),
    val damageTaken: Int = 0,
    val itemFound: Item? = null,
    val combatStarted: Boolean = false,
    val enemy: Enemy? = null
)

// ── VIEWMODEL ────────────────────────────────────────────────────────────────

class GameViewModel : ViewModel() {

    private val repository = GameRepository()
    private var currentGameId: String = ""
    private var currentUserId: String = ""
    private val gson = Gson()
    private val chatHistory = mutableListOf<ChatMessage>()
    private val db = FirebaseFirestore.getInstance()

    // Máximo de mensajes en el historial que se envía a Groq.
    // Mantener bajo evita que el contexto se llene y el modelo aluciné.
    private val MAX_HISTORY = 14   // 7 pares usuario/asistente

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

    // ── INICIO / CARGA ────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun startStory(userId: String, characterName: String, theme: String) {
        currentUserId = userId
        currentGameId = "${userId}_${characterName}_${theme}".replace(" ", "_")

        viewModelScope.launch {
            // Aseguramos que la clase del personaje esté disponible en el documento de partida
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
            trimHistory()   // evitar desbordamiento de contexto

            val request = GroqRequest(
                messages = listOf(ChatMessage(role = "system", content = buildSystemPrompt())) +
                        chatHistory.toList()
            )

            val rawContent = apiService.getCompletion(apiKey, request)
                .choices.firstOrNull()?.message?.content.orEmpty()

            Log.d("GM_RAW", "Intento $attempt — respuesta cruda: ${rawContent.take(300)}")

            val adventure = parseAdventureStep(rawContent)

            if (adventure == null) {
                // ── Reintento automático ──────────────────────────────────
                if (attempt < 3) {
                    Log.w("GM_PARSE", "Fallo de parseo en intento $attempt, reintentando...")
                    // Añadimos un mensaje de corrección al historial y reintentamos
                    chatHistory.add(ChatMessage(
                        role    = "assistant",
                        content = rawContent.take(200)   // guardar lo que vino
                    ))
                    chatHistory.add(ChatMessage(
                        role    = "user",
                        content = "Tu respuesta no era JSON válido. Por favor, responde SOLO con el JSON especificado, sin ningún texto adicional."
                    ))
                    delay(600L * attempt)   // back-off progresivo
                    executeGroqCall(promptContent, attempt + 1)
                } else {
                    // ── Fallback tras 3 intentos ──────────────────────────
                    Log.e("GM_PARSE", "3 intentos fallidos. Usando fallback.")
                    val fallback = extractStoryFallback(rawContent)
                    applyAdventureStep(fallback)
                }
                return
            }

            chatHistory.add(ChatMessage(role = "assistant", content = adventure.story))
            applyAdventureStep(adventure)
            saveCurrentGame()

        } catch (e: Exception) {
            Log.e("GM_ERROR", "executeGroqCall error: ${e.message}", e)
            if (attempt < 3) {
                delay(1000L * attempt)
                // Quitamos el último mensaje de usuario para no duplicar en el reintento
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

    /**
     * Extrae el JSON de la respuesta del modelo, aunque venga envuelto en:
     *   - Bloques Markdown:  ```json { ... } ```
     *   - Texto previo:      "Aquí tienes: { ... }"
     *   - Caracteres BOM o espacios extra
     *
     * Devuelve null si no se puede parsear tras todos los intentos de limpieza.
     */
    private fun parseAdventureStep(raw: String): AdventureStep? {
        if (raw.isBlank()) return null

        // 1. Quitar bloques Markdown ```json ... ``` o ``` ... ```
        val stripped = raw
            .replace(Regex("```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        // 2. Extraer el primer objeto JSON completo (desde { hasta el } de cierre)
        val jsonCandidate = extractJsonObject(stripped) ?: stripped

        // 3. Intentar parsear directamente
        try {
            val step = gson.fromJson(jsonCandidate, AdventureStep::class.java)
            if (step != null && step.story.isNotBlank()) return step
        } catch (e: JsonSyntaxException) {
            Log.w("GM_PARSE", "Fallo JSON estricto, intentando reparación...")
        }

        // 4. Reparación básica: a veces el modelo omite comas o cierra mal
        val repaired = repairJson(jsonCandidate)
        return try {
            val step = gson.fromJson(repaired, AdventureStep::class.java)
            if (step?.story?.isNotBlank() == true) step else null
        } catch (e: Exception) {
            null
        }
    }

    /** Encuentra el substring desde el primer '{' hasta el '}' de cierre balanceado */
    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null

        var depth = 0
        var inString = false
        var escape = false

        for (i in start until text.length) {
            val c = text[i]
            when {
                escape          -> escape = false
                c == '\\'       -> if (inString) escape = true
                c == '"'        -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    /** Reparaciones menores de JSON malformado */
    private fun repairJson(raw: String): String {
        var s = raw.trim()
        // Asegurar apertura y cierre
        if (!s.startsWith("{")) s = "{$s"
        if (!s.endsWith("}"))  s = "$s}"
        // Quitar comas finales antes de } o ]
        s = s.replace(Regex(",\\s*}"), "}")
        s = s.replace(Regex(",\\s*]"), "]")
        return s
    }

    /**
     * Último recurso: si no podemos parsear el JSON, intentamos extraer
     * la narrativa en texto plano para no bloquear al jugador.
     */
    private fun extractStoryFallback(raw: String): AdventureStep {
        // Intentar extraer el valor de "story" con regex
        val storyRegex = Regex(""""story"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val storyMatch = storyRegex.find(raw)
        val story = storyMatch?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: raw.replace(Regex("[{}\\[\\]\":]"), " ")  // texto plano en último caso
                .lines().firstOrNull { it.length > 20 }
            ?: "El Dungeon Master reflexiona sobre tu acción..."

        return AdventureStep(
            story   = story,
            options = listOf("Continuar", "Observar el entorno", "Descansar")
        )
    }

    // ── APLICAR STEP AL ESTADO ───────────────────────────────────────────────

    private fun applyAdventureStep(step: AdventureStep) {
        _messages.value = _messages.value + ("DM" to step.story)
        _currentOptions.value = step.options.filter { it.isNotBlank() }
        _currentAdventureStep.value = step
    }

    private fun showError(msg: String) {
        _messages.value = _messages.value + ("DM" to msg)
    }

    // ── GESTIÓN DEL HISTORIAL ────────────────────────────────────────────────

    /**
     * Recorta el historial para no superar MAX_HISTORY mensajes.
     * Siempre preserva los primeros 2 mensajes (inicio de la historia)
     * para mantener el contexto de quién es el personaje y el tema.
     */
    private fun trimHistory() {
        if (chatHistory.size <= MAX_HISTORY) return
        val keep = chatHistory.take(2) +
                chatHistory.takeLast(MAX_HISTORY - 2)
        chatHistory.clear()
        chatHistory.addAll(keep)
        Log.d("GM_TRIM", "Historial recortado a ${chatHistory.size} mensajes")
    }

    // ── PROMPTS ───────────────────────────────────────────────────────────────

    private fun buildSystemPrompt(): String = """
Eres un Dungeon Master de rol. Responde ÚNICAMENTE con un objeto JSON válido, sin texto adicional, sin bloques de código markdown, sin explicaciones. Solo el JSON.

Estructura EXACTA (respeta los nombres de campo):
{"story":"narración aquí","options":["opción 1","opción 2","opción 3"],"damageTaken":0,"itemFound":null,"combatStarted":false,"enemy":null}

Reglas:
- "story": narrativa inmersiva en español, 2-4 frases.
- "options": exactamente 2-4 opciones cortas para el jugador.
- "combatStarted": true SOLO si hay combate activo. Entonces "enemy" debe tener {"name":"...","hpMax":N,"hpCurrent":N,"attackDamage":"XdY"}.
- "damageTaken": daño recibido fuera de combate (0 si ninguno).
- "itemFound": null o {"name":"...","type":"arma/pocion/armadura","description":"...","effect":"..."}.
- NO uses comillas simples. NO añadas campos extra. NO envuelvas en markdown.
""".trimIndent()

    /**
     * Busca la clase e HP del personaje en la colección de usuarios y los escribe
     * en el documento de partida (colección "partidas") con merge, para que
     * CombatViewModel pueda leer la clase correcta al iniciar el combate.
     */
    private suspend fun saveCharacterClassToGame(userId: String, characterName: String) {
        try {
            val snap = db.collection("users")
                .document(userId)
                .collection("characters")
                .whereEqualTo("name", characterName)
                .get().await()

            val charDoc = snap.documents.firstOrNull() ?: return
            val charClass = charDoc.getString("characterClass") ?: ""
            val hpMax     = charDoc.getLong("hpMax")?.toInt() ?: 20

            if (charClass.isNotBlank()) {
                db.collection("partidas")
                    .document(currentGameId)
                    .set(
                        mapOf(
                            "characterClass" to charClass,
                            "characterName"  to characterName,
                            "userId"         to userId,
                            "hpMax"          to hpMax,
                            "hpCurrent"      to hpMax
                        ),
                        SetOptions.merge()
                    ).await()
                Log.d("GM_CHAR", "Clase '$charClass' guardada en partida $currentGameId")
            }
        } catch (e: Exception) {
            Log.w("GM_CHAR", "saveCharacterClassToGame: ${e.message}")
        }
    }

    private fun getInitialPrompt(name: String, theme: String): String =
        "Inicia la aventura de $theme para el héroe llamado $name. Presenta el escenario de forma épica y ofrece 3 opciones iniciales."

    // ── GUARDADO ──────────────────────────────────────────────────────────────

    private fun saveCurrentGame() {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "userId"          to currentUserId,
                    "displayMessages" to _messages.value.map { mapOf("first" to it.first, "second" to it.second) },
                    "chatHistory"     to chatHistory.map { mapOf("role" to it.role, "content" to it.content) },
                    "lastOptions"     to _currentOptions.value,
                    "timestamp"       to System.currentTimeMillis()
                )
                repository.saveGame(currentGameId, data)
            } catch (e: Exception) {
                Log.e("GM_SAVE", "Error guardando: ${e.message}")
            }
        }
    }
}