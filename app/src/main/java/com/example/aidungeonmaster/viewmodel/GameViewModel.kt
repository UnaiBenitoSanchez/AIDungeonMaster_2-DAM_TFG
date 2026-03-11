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

// --- DEFINICIONES DE MODELO PARA EL PARSEO ---
data class Item(
    val name: String = "",
    val description: String = "",
    val type: String = ""
)

data class AdventureStep(
    val story: String = "",
    val options: List<String> = emptyList(),
    val damageTaken: Int = 0,
    val itemFound: Item? = null
)

class GameViewModel : ViewModel() {

    private val repository = GameRepository()
    private var currentGameId: String = ""
    private var currentUserId: String = ""
    private val gson = Gson()
    private val chatHistory = mutableListOf<ChatMessage>()

    private val apiService = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(com.example.aidungeonmaster.data.api.GroqApiService::class.java)

    private val apiKey = "Bearer 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"

    private val _messages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<String>>(emptyList())
    val currentOptions = _currentOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun startStory(userId: String, characterName: String, theme: String) {
        currentUserId = userId
        currentGameId = "${userId}_${characterName}_${theme}".replace(" ", "_")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val savedData = repository.loadGame(currentGameId)
                if (savedData != null) {
                    val rawMessages = savedData["displayMessages"] as? List<Map<String, String>>
                    _messages.value = rawMessages?.map { (it["first"] ?: "") to (it["second"] ?: "") } ?: emptyList()
                    _currentOptions.value = (savedData["lastOptions"] as? List<*>)?.map { it.toString() } ?: emptyList()

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
                _messages.value = listOf("DM" to "Error de conexión: ${e.localizedMessage}")
                _isLoading.value = false
            }
        }
    }

    // Maneja botones de opciones
    fun sendPlayerAction(action: String) {
        _messages.value = _messages.value + ("Tú" to action)
        generateNextStep(action)
    }

    // Maneja el texto libre (TextField)
    fun sendCustomAction(action: String) {
        if (action.isBlank()) return
        _messages.value = _messages.value + ("Tú" to action)
        generateNextStep(action)
    }

    // --- EL CORAZÓN DE LA LÓGICA ---
    private fun generateNextStep(action: String) {
        viewModelScope.launch {
            executeGroqCall("El jugador intenta: $action")
        }
    }

    private suspend fun executeGroqCall(promptContent: String) {
        _isLoading.value = true
        try {
            chatHistory.add(ChatMessage(role = "user", content = promptContent))

            val systemPrompt = ChatMessage(role = "system", content = getEnhancedSystemPrompt())
            val request = GroqRequest(
                messages = listOf(systemPrompt) + chatHistory
            )

            val response = apiService.getCompletion(apiKey, request)
            val rawJson = response.choices.firstOrNull()?.message?.content ?: ""

            val adventure = gson.fromJson(rawJson, AdventureStep::class.java)

            chatHistory.add(ChatMessage(role = "assistant", content = adventure.story))
            _messages.value = _messages.value + ("DM" to adventure.story)
            _currentOptions.value = adventure.options

            // Aquí es donde procesaríamos adventure.damageTaken o adventure.itemFound más adelante

            saveCurrentGame()
        } catch (e: Exception) {
            _messages.value = _messages.value + ("DM" to "El DM se ha confundido con las reglas...")
        } finally {
            _isLoading.value = false
        }
    }

    private fun getEnhancedSystemPrompt(): String {
        return """
        Eres un Dungeon Master. Responde SIEMPRE en JSON exacto:
        {
          "story": "descripción",
          "options": ["opción 1", "opción 2"],
          "damageTaken": 0,
          "itemFound": null
        }
        Si el jugador hace algo arriesgado, suma daño en damageTaken.
        """.trimIndent()
    }

    private fun getInitialPrompt(name: String, theme: String): String {
        return "Inicia una aventura de $theme para el héroe $name. Describe el inicio y da opciones."
    }

    private fun saveCurrentGame() {
        viewModelScope.launch {
            try {
                val gameData = mapOf(
                    "userId" to currentUserId,
                    "displayMessages" to _messages.value.map { mapOf("first" to it.first, "second" to it.second) },
                    "chatHistory" to chatHistory.map { mapOf("role" to it.role, "content" to it.content) },
                    "lastOptions" to _currentOptions.value,
                    "timestamp" to System.currentTimeMillis()
                )
                repository.saveGame(currentGameId, gameData)
            } catch (e: Exception) {
                println("Error al guardar: ${e.message}")
            }
        }
    }
}