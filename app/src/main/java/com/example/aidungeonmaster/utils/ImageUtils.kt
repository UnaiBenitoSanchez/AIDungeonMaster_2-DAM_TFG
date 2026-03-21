package com.example.aidungeonmaster.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object ImageUtils {
    private const val TAG = "ImageUtils"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Genera un retrato usando Pollinations.ai (sin API key).
     * Prueba los modelos "turbo" → "flux" → "flux-realism" en orden.
     * Si uno devuelve HTTP 500, pasa automáticamente al siguiente.
     */
    suspend fun generatePortraitBase64(
        race: String,
        clazz: String,
        physicalTraits: String
    ): String = withContext(Dispatchers.IO) {

        val fullPrompt = buildString {
            append("Fantasy RPG character portrait, ")
            append("$race $clazz, ")
            append(physicalTraits)
            append(", masterwork digital painting, D&D style, dramatic lighting, ")
            append("detailed face, high resolution, artstation quality")
        }
        val encodedPrompt = java.net.URLEncoder.encode(fullPrompt, "UTF-8")

        // Probamos modelos en orden: turbo es el más rápido y estable
        val models = listOf("turbo", "flux", "flux-realism")
        var lastError: Exception? = null

        for ((index, model) in models.withIndex()) {
            try {
                if (index > 0) {
                    Log.d(TAG, "Fallback al modelo: $model")
                    delay(2000L)
                }

                val seed = Random.nextInt(0, 999999)
                val url = "https://image.pollinations.ai/prompt/$encodedPrompt" +
                        "?width=512&height=512&model=$model&seed=$seed&nologo=true"

                Log.d(TAG, "Petición con modelo=$model seed=$seed")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AIDungeonMaster/1.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 500 -> {
                            // Error del servidor con este modelo — intentar el siguiente
                            throw Exception("HTTP 500 con modelo $model")
                        }
                        response.code == 429 -> {
                            throw Exception("Servidor ocupado (429). Espera unos segundos e inténtalo de nuevo.")
                        }
                        response.code == 503 -> {
                            throw Exception("Servicio no disponible (503). El servidor de imágenes está caído.")
                        }
                        !response.isSuccessful -> {
                            throw Exception("Error del servidor: HTTP ${response.code}")
                        }
                        else -> {
                            val contentType = response.header("Content-Type") ?: ""
                            val bytes = response.body?.bytes()
                                ?: throw Exception("Respuesta vacía del servidor")

                            if (bytes.size < 100) {
                                throw Exception("Datos inválidos recibidos (${bytes.size} bytes)")
                            }

                            // Verificar magic bytes: PNG o JPEG
                            val isValidImage =
                                (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) ||
                                        (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) ||
                                        contentType.startsWith("image/")

                            if (!isValidImage) {
                                val preview = String(bytes.copyOf(50), Charsets.UTF_8)
                                    .filter { it.isLetterOrDigit() || it == ' ' }
                                Log.w(TAG, "Respuesta no es imagen: $preview")
                                throw Exception("El servidor no devolvió una imagen válida")
                            }

                            Log.d(TAG, "Imagen OK con modelo=$model: ${bytes.size} bytes")
                            return@withContext Base64.encodeToString(bytes, Base64.DEFAULT)
                        }
                    }
                }

            } catch (e: java.net.SocketTimeoutException) {
                lastError = Exception("Tiempo de espera agotado con modelo $model. Inténtalo de nuevo.")
                Log.w(TAG, "Timeout con modelo $model: ${e.message}")
            } catch (e: java.net.UnknownHostException) {
                throw Exception("Sin conexión a internet. Comprueba tu red.")
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Error con modelo $model: ${e.message}")
            }
        }

        throw lastError ?: Exception("Todos los modelos fallaron. Inténtalo de nuevo más tarde.")
    }

    fun base64ToBitmap(base64: String): Bitmap {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw Exception("No se pudo decodificar la imagen recibida")
    }
}