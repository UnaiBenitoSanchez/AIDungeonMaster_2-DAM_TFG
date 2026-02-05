package com.example.aidungeonmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.api.ChatMessage
import com.example.aidungeonmaster.data.api.GroqRequest
import com.example.aidungeonmaster.ui.game.GameRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Clase para mapear el JSON de la aventura
data class AdventureStep(
    val story: String = "",
    val options: List<String> = emptyList()
)

class GameViewModel : ViewModel() {

    private val repository = GameRepository()
    private var currentGameId: String = ""
    private var currentUserId: String = ""

    private val gson = Gson()

    // 1. Configuración de Retrofit
    private val apiService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(com.example.aidungeonmaster.data.api.GroqApiService::class.java)

    private val apiKey = "Bearer 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"

    // 2. Estados para la UI
    private val _messages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<String>>(emptyList())
    val currentOptions = _currentOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 3. Inicio de la historia
    fun startStory(userId: String, characterName: String, theme: String) {

        currentUserId = userId
        currentGameId = "${userId}_${characterName}_${theme}".replace(" ", "_")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val savedData = repository.loadGame(currentGameId)

                if (savedData != null) {
                    // CASO A: Restaurar partida existente
                    val rawMessages = savedData["displayMessages"] as? List<Map<String, String>>
                    _messages.value = rawMessages?.map {
                        (it["first"] ?: "") to (it["second"] ?: "")
                    } ?: emptyList()

                    _currentOptions.value = (savedData["lastOptions"] as? List<*>)?.map { it.toString() } ?: emptyList()

                    // Restaurar historial técnico
                    val savedHistory = savedData["chatHistory"] as? List<Map<String, String>>
                    chatHistory.clear()
                    savedHistory?.forEach {
                        chatHistory.add(ChatMessage(role = it["role"] ?: "user", content = it["content"] ?: ""))
                    }

                    _isLoading.value = false
                } else {
                    // CASO B: No hay datos, iniciamos historia nueva
                    val prompt = """
                    Eres un Dungeon Master experto. Inicia una aventura de $theme para el héroe $characterName.
                    Describe la escena inicial y da 3 opciones de acción.
                    Responde SIEMPRE en este formato JSON:
                    { "story": "texto de la historia", "options": ["opcion1", "opcion2", "opcion3"] }
                """.trimIndent()

                    executeGroqCall(prompt)
                    // isLoading se pondrá en false dentro de executeGroqCall o en el finally
                }
            } catch (e: Exception) {
                _messages.value = listOf("DM" to "Error al conectar con la base de datos: ${e.localizedMessage}")
                _isLoading.value = false
            }
        }
    }

    // 4. Acción del jugador
    fun sendPlayerAction(action: String) {
        _messages.value = _messages.value + ("Tú" to action)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prompt = "El jugador ha elegido: $action. Continúa la historia manteniendo el formato JSON anterior."
                executeGroqCall(prompt)
            } catch (e: Exception) {
                _messages.value = _messages.value + ("DM" to "Error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 5. Lógica de red y parseo
    private val chatHistory = mutableListOf<ChatMessage>()

    private suspend fun executeGroqCall(promptContent: String) {
        _isLoading.value = true
        try {
            // Añadimos el mensaje del usuario al historial
            chatHistory.add(ChatMessage(role = "user", content = promptContent))

            val request = GroqRequest(
                messages = listOf(ChatMessage(role = "system", content = "Eres un DM que responde exclusivamente en JSON.")) + chatHistory
            )

            val response = apiService.getCompletion(apiKey, request)
            val rawJson = response.choices.firstOrNull()?.message?.content ?: ""

            // Limpieza y parseo
            val cleanJson = rawJson.substringAfter("{").substringBeforeLast("}")
            val adventure = gson.fromJson("{$cleanJson}", AdventureStep::class.java)

            // IMPORTANTE: Guardamos solo la historia en el historial para la IA, no el JSON crudo
            chatHistory.add(ChatMessage(role = "assistant", content = adventure.story))

            _messages.value = _messages.value + ("DM" to adventure.story)
            _currentOptions.value = adventure.options

            saveCurrentGame()

        } catch (e: Exception) {
            // Si falla el JSON, intentamos al menos mostrar lo que dijo la IA o un error amigable
            _messages.value = _messages.value + ("DM" to "El DM se ha quedado sin palabras... (Error de formato)")
            _currentOptions.value = listOf("Reintentar", "Cargar partida")
        } finally {
            _isLoading.value = false
        }
    }

    private fun saveCurrentGame() {
        viewModelScope.launch {
            try {
                // Ya no necesitas safeId porque currentGameId ya viene limpio de startStory
                val nameFromId = currentGameId.split("_").getOrNull(1) ?: "Heroe"

                val gameData = mapOf(
                    "userId" to currentUserId,
                    "characterName" to nameFromId,
                    "displayMessages" to _messages.value.map { mapOf("first" to it.first, "second" to it.second) },
                    "chatHistory" to chatHistory.map { mapOf("role" to it.role, "content" to it.content) },
                    "lastOptions" to _currentOptions.value,
                    "timestamp" to System.currentTimeMillis()
                )

                // Usamos un ID de documento sin espacios
                val safeId = currentGameId.replace(" ", "_")
                repository.saveGame(currentGameId, gameData)
                println("DEBUG: Guardado solicitado para $currentGameId")
            } catch (e: Exception) {
                println("DEBUG: Error al intentar guardar: ${e.message}")
            }
        }
    }

    // En GameRepository.kt
    suspend fun saveGame(gameId: String, gameData: Map<String, Any>) {
        try {
            // Esto creará la colección "partidas" automáticamente si no existe
            FirebaseFirestore.getInstance()
                .collection("partidas")
                .document(gameId)
                .set(gameData)
                .await()
            println("DEBUG: Guardado con éxito en Firebase")
        } catch (e: Exception) {
            println("DEBUG: Error al guardar: ${e.message}")
        }
    }
}