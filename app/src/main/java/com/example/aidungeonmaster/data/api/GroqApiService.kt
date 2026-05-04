package com.example.aidungeonmaster.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Servicio que encapsula la lógica de groq api.
interface GroqApiService {
    @POST("v1/chat/completions")
    // Obtiene completion.
    suspend fun getCompletion(
        @Header("Authorization") token: String,
        @Body request: GroqRequest
    ): GroqResponse
}

// Modelos para la petición
data class GroqRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat = ResponseFormat()
)

// Modelo de datos que representa chat message.
data class ChatMessage(val role: String, val content: String)
// Clase que encapsula la lógica de response format.
data class ResponseFormat(val type: String = "json_object")

// Modelos para la respuesta
data class GroqResponse(val choices: List<Choice>)
// Clase que encapsula la lógica de choice.
data class Choice(val message: ChatMessage)
