package com.example.aidungeonmaster.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.aidungeonmaster.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Generación de retratos de personaje via Cloudflare Workers AI.
 *
 * Modelo  : @cf/stabilityai/stable-diffusion-xl-base-1.0
 * Límite  : 10.000 imágenes / día GRATIS (sin tarjeta de crédito)
 * Endpoint: POST https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/{model}
 * Auth    : Bearer {API_TOKEN}
 * Respuesta: bytes PNG binarios directos (no JSON)
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    // Modelos en orden de preferencia (calidad → velocidad como fallback)
    private val MODELS = listOf(
        "@cf/stabilityai/stable-diffusion-xl-base-1.0",
        "@cf/lykon/dreamshaper-8-lcm",
        "@cf/bytedance/stable-diffusion-xl-lightning"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Genera un retrato de personaje RPG y devuelve la imagen en base64 (PNG).
     */
    suspend fun generatePortraitBase64(
        raceEs: String,
        clazzEs: String,
        subclazzEs: String,
        physicalTraitsEs: String
    ): String = withContext(Dispatchers.IO) {

        val accountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID
        val apiToken  = BuildConfig.CLOUDFLARE_API_TOKEN

        if (accountId.isBlank() || apiToken.isBlank()) {
            throw Exception(
                "Faltan las credenciales de Cloudflare.\n" +
                        "Añade CLOUDFLARE_ACCOUNT_ID y CLOUDFLARE_API_TOKEN en local.properties."
            )
        }

        // 1. Obtener traducciones directas de los mapas
        val raceEn = RAZA_MAP.getOrDefault(raceEs, raceEs)
        val clazzEn = CLASE_MAP.getOrDefault(clazzEs, clazzEs)
        val subclazzEn = SUBCLASE_MAP.getOrDefault(subclazzEs, subclazzEs)

        // 2. Traducir la apariencia física con Llama 3
        val translatedTraits = translateToEnglish(physicalTraitsEs)

        // 3. Construir el prompt
        val prompt = buildPromptWithSubclass(raceEn, clazzEn, subclazzEn, translatedTraits)
        Log.d(TAG, "Generando retrato... Prompt final: $prompt")

        // Variable para guardar el último error en caso de que fallen los modelos
        var lastError: Exception? = null

        for (model in MODELS) {
            try {
                val base64 = callCloudflare(accountId, apiToken, model, prompt)
                Log.d(TAG, "Retrato generado con $model ✓")
                return@withContext base64
            } catch (e: Exception) {
                Log.w(TAG, "Fallo con $model: ${e.message}")
                lastError = e
            }
        }

        throw lastError ?: Exception("Todos los modelos de Cloudflare fallaron")
    }

    /**
     * Construye un prompt detallado y puramente en inglés, incluyendo la subclase.
     */
    private fun buildPromptWithSubclass(
        raceEn: String,
        clazzEn: String,
        subclazzEn: String,
        physicalTraitsEn: String
    ): String {
        // Evitamos añadir "subclass" al prompt si la subclase está vacía
        val subclassText = if (subclazzEn.isNotBlank()) "$subclazzEn subclass, " else ""

        val raw = "solo character portrait, single person, one character only, " +
                "fantasy RPG concept art of a $raceEn $clazzEn, $subclassText" +
                "$physicalTraitsEn, " +
                "detailed face, digital painting, D&D illustration, " +
                "dramatic cinematic lighting, high detail, sharp focus, " +
                "professional illustration, 4k quality"

        return if (raw.length > 500) raw.take(500) else raw
    }

    // --- DICCIONARIOS DE TRADUCCIÓN ---

    private val RAZA_MAP = mapOf(
        "Aarakocras" to "Aarakocra", "Aasimar" to "Aasimar", "Cambiantes" to "Shifter",
        "Centauro" to "Centaur", "Chico pollo" to "Chicken boy", "Chico Slime" to "Slime boy",
        "Deidad" to "God deity", "Demonio" to "Demon", "Dracónidos" to "Dragonborn",
        "Elemental" to "Elemental", "Elfo oscuro" to "Drow", "Elfos" to "Elf",
        "Enanos" to "Dwarf", "Espectro" to "Wraith", "Espíritu" to "Spirit",
        "Etergénito" to "Aetherborn", "Firbolgs" to "Firbolg", "Forjados" to "Warforged",
        "Genasi" to "Genasi", "Gith" to "Githyanki", "Gnomos" to "Gnome",
        "Goblins" to "Goblin", "Golem" to "Golem", "Goliats" to "Goliath",
        "Grungs" to "Grung", "Híbridos Simic" to "Simic Hybrid", "Hobgoblins" to "Hobgoblin",
        "Hombre lobo" to "Werewolf", "Hombres lagarto" to "Lizardfolk", "Humanos" to "Human",
        "Huecos" to "Hollow One", "Ilusión" to "Illusion", "Kalashtar" to "Kalashtar",
        "Kenkus" to "Kenku", "Kobolds" to "Kobold", "Locathah" to "Locathah",
        "Loxodon" to "Loxodon", "Medianos" to "Halfling", "Minotauros" to "Minotaur",
        "Mutadores" to "Changeling", "Orcos" to "Orc", "Orcos de Eberron" to "Eberron Orc",
        "Osgos" to "Bugbear", "Polimorfo" to "Doppelganger", "Quimera" to "Chimera",
        "Rápido" to "Tabaxi", "Semielfos" to "Half-Elf", "Semiorcos" to "Half-Orc",
        "Sátiro" to "Satyr", "Tabaxis" to "Tabaxi", "Tiflin" to "Tiefling",
        "Tortogas" to "Tortle", "Trasgo" to "Goblin", "Tritones" to "Triton",
        "Vedalken" to "Vedalken", "Verdan" to "Verdan", "Vampiro" to "Vampire",
        "Yuan-Ti Purasangres" to "Yuan-Ti Pureblood", "Zombie" to "Zombie"
    )

    private val CLASE_MAP = mapOf(
        "Artífice" to "Artificer", "Bardo" to "Bard", "Bárbaro" to "Barbarian",
        "Brujo" to "Warlock", "Caballero de la Muerte" to "Death Knight",
        "Chamán" to "Shaman", "Clérigo" to "Cleric", "Corsario" to "Corsair",
        "Druida" to "Druid", "Exorcista" to "Exorcist", "Explorador" to "Ranger",
        "Guerrero" to "Fighter", "Hechicero" to "Sorcerer", "Mago" to "Wizard",
        "Monje" to "Monk", "Paladín" to "Paladin", "Pícaro" to "Rogue"
    )

    // He incluido las más comunes; si falta alguna el código usará el nombre original
    private val SUBCLASE_MAP = mapOf(
        "El Celestial" to "Celestial", "El Hexblade" to "Hexblade", "El Archihada" to "Archfey",
        "Senda del Berserker" to "Berserker", "Círculo de la Luna" to "Circle of the Moon",
        "Juramento de Venganza" to "Oath of Vengeance", "Asesino" to "Assassin"
    )

    private fun callCloudflare(
        accountId: String,
        apiToken: String,
        model: String,
        prompt: String
    ): String {
        val url = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/$model"

        // Cloudflare Workers AI acepta JSON con el prompt
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("negative_prompt",
                "multiple characters, two people, duo, group, crowd, split image, " +
                        "collage, bad anatomy, deformed, ugly, blurry, watermark, text, " +
                        "extra limbs, bad hands, low quality, nsfw")
            put("num_steps", 20)
            put("guidance", 8.5)
            put("width", 512)
            put("height", 768)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val bytes = response.body?.bytes() ?: throw Exception("Respuesta vacía de Cloudflare")

        if (!response.isSuccessful) {
            // Cloudflare devuelve JSON con el error
            val errorText = try {
                JSONObject(String(bytes))
                    .optJSONArray("errors")
                    ?.optJSONObject(0)
                    ?.optString("message", "Error desconocido")
                    ?: "HTTP ${response.code}"
            } catch (_: Exception) {
                "HTTP ${response.code}"
            }
            throw Exception("Cloudflare error: $errorText")
        }

        // La respuesta es PNG binario directamente — verificar magic bytes
        if (bytes.size < 8) {
            throw Exception("Respuesta demasiado pequeña (${bytes.size} bytes)")
        }

        val isPng  = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()
        val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()

        if (!isPng && !isJpeg) {
            // Puede ser JSON de error con código 200 — intentar parsear
            val maybeError = try {
                JSONObject(String(bytes)).optString("error", "")
            } catch (_: Exception) { "" }

            val msg = if (maybeError.isNotBlank()) maybeError
            else "Respuesta no es una imagen válida (${bytes.size} bytes)"
            throw Exception(msg)
        }

        Log.d(TAG, "Imagen recibida: ${bytes.size} bytes")
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun buildPrompt(raceEn: String, clazzEn: String, physicalTraitsEn: String): String {
        val raw = "solo character portrait, single person, one character only, " +
                "fantasy RPG $raceEn $clazzEn, " +
                "$physicalTraitsEn, " +
                "detailed face closeup, digital painting, D&D concept art, " +
                "dramatic cinematic lighting, high detail, sharp focus, " +
                "dark fantasy background, professional illustration, 4k quality"

        return if (raw.length > 500) raw.take(500) else raw
    }

    /**
     * Convierte una cadena base64 (PNG/JPEG) a Bitmap para mostrar en Compose.
     */
    fun base64ToBitmap(base64: String): Bitmap {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw Exception("No se pudo decodificar la imagen recibida")
    }

    /**
     * Traduce el texto de apariencia del español al inglés usando Llama 3 en Cloudflare.
     */
    private suspend fun translateToEnglish(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        val accountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID
        val apiToken  = BuildConfig.CLOUDFLARE_API_TOKEN
        // Usamos Llama 3 8B, que es rápido y excelente siguiendo instrucciones
        val model = "@cf/meta/llama-3-8b-instruct"

        val url = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/$model"

        // Estructura de mensajes para el modelo conversacional
        val body = JSONObject().apply {
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a translator. Translate the following Spanish text describing a fantasy character's physical appearance into English. Output ONLY the English translation. No explanations, no quotes, just the translated text.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: throw Exception("Respuesta vacía")

            if (!response.isSuccessful) {
                Log.e(TAG, "Error en traducción HTTP ${response.code}: $responseString")
                return@withContext text // Fallback: devolvemos el original si hay error HTTP
            }

            // Cloudflare devuelve el texto generado dentro de result -> response
            val jsonResponse = JSONObject(responseString)
            val translatedText = jsonResponse.optJSONObject("result")?.optString("response", text) ?: text

            Log.d(TAG, "Prompt traducido: '$text' -> '${translatedText.trim()}'")
            return@withContext translatedText.trim()

        } catch (e: Exception) {
            Log.w(TAG, "Excepción al traducir, usando texto original: ${e.message}")
            return@withContext text // Fallback seguro para no bloquear la app
        }
    }
}