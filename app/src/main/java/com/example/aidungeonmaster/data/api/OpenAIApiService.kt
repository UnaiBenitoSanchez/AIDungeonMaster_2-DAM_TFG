package com.example.aidungeonmaster.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Modelo de datos que representa dalle request.
data class DalleRequest(val prompt: String, val n: Int = 1, val size: String = "512x512")
// Clase que encapsula la lógica de dalle response.
data class DalleResponse(val data: List<DalleImage>)
// Clase que encapsula la lógica de dalle image.
data class DalleImage(val url: String)

// Servicio que encapsula la lógica de open aiapi.
interface OpenAIApiService {
    @POST("v1/images/generations")
    // Genera image.
    suspend fun generateImage(
        @Header("Authorization") token: String,
        @Body request: DalleRequest
    ): DalleResponse
}
