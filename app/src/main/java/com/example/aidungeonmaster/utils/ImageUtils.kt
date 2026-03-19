package com.example.aidungeonmaster.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ImageUtils {

    private const val API_KEY = "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"

    private val client = OkHttpClient()

    /**
     * Genera imagen usando OpenAI (gpt-image-1)
     * Devuelve base64
     */
    suspend fun generatePortraitBase64(
        race: String,
        clazz: String,
        physicalTraits: String
    ): String = withContext(Dispatchers.IO) {

        val prompt = "fantasy RPG character portrait $race $clazz $physicalTraits " +
                "detailed digital art DnD style dramatic lighting high quality epic fantasy"

        val safePrompt = prompt
            .replace("\"", "")
            .replace("\n", " ")

        val jsonObj = JSONObject().apply {
            put("model", "gpt-image-1")
            put("prompt", safePrompt)
            put("size", "1024x1536")
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/images/generations")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(jsonObj.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw Exception("HTTP ${response.code}: $errorBody")
        }

        val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía")

        val jsonResponse = JSONObject(responseBody)

        return@withContext jsonResponse
            .getJSONArray("data")
            .getJSONObject(0)
            .getString("b64_json")
    }

    /**
     * Convierte base64 a Bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}