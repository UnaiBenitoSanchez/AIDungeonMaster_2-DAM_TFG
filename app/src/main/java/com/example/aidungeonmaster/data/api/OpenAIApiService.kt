package com.example.aidungeonmaster.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class DalleRequest(val prompt: String, val n: Int = 1, val size: String = "512x512")
data class DalleResponse(val data: List<DalleImage>)
data class DalleImage(val url: String)

interface OpenAIApiService {
    @POST("v1/images/generations")
    suspend fun generateImage(
        @Header("Authorization") token: String,
        @Body request: DalleRequest
    ): DalleResponse
}