package com.example.aidungeonmaster.data.repository

import android.util.Log
import com.example.aidungeonmaster.BuildConfig
import com.example.aidungeonmaster.utils.PushConstants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PushGatewayRepository {

    private val auth = FirebaseAuth.getInstance()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun notifyChatMessage(
        chatId: String,
        targetUid: String,
        senderName: String,
        messagePreview: String,
        eventId: String,
        sentAt: Long
    ) {
        sendEvent(
            JSONObject()
                .put(PushConstants.EXTRA_EVENT_TYPE, PushConstants.EVENT_TYPE_CHAT_MESSAGE)
                .put(PushConstants.EXTRA_CHAT_ID, chatId)
                .put(PushConstants.EXTRA_TARGET_UID, targetUid)
                .put(PushConstants.EXTRA_SENDER_NAME, senderName)
                .put(PushConstants.EXTRA_MESSAGE_PREVIEW, messagePreview)
                .put(PushConstants.EXTRA_EVENT_ID, eventId)
                .put(PushConstants.EXTRA_SENT_AT, sentAt)
        )
    }

    suspend fun notifyFriendRequest(requestId: String) {
        sendEvent(
            JSONObject()
                .put(PushConstants.EXTRA_EVENT_TYPE, PushConstants.EVENT_TYPE_FRIEND_REQUEST)
                .put(PushConstants.EXTRA_REQUEST_ID, requestId)
        )
    }

    suspend fun notifyFriendAccepted(
        requestId: String,
        targetUid: String,
        senderName: String
    ) {
        sendEvent(
            JSONObject()
                .put(PushConstants.EXTRA_EVENT_TYPE, PushConstants.EVENT_TYPE_FRIEND_ACCEPTED)
                .put(PushConstants.EXTRA_REQUEST_ID, requestId)
                .put(PushConstants.EXTRA_TARGET_UID, targetUid)
                .put(PushConstants.EXTRA_SENDER_NAME, senderName)
        )
    }

    suspend fun notifyGuildWaitingRoomJoin(guildId: String) {
        sendEvent(
            JSONObject()
                .put(PushConstants.EXTRA_EVENT_TYPE, PushConstants.EVENT_TYPE_GUILD_WAITING_ROOM)
                .put(PushConstants.EXTRA_GUILD_ID, guildId)
        )
    }

    private suspend fun sendEvent(body: JSONObject) {
        val baseUrl = BuildConfig.PUSH_GATEWAY_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            Log.w(TAG, "PUSH_GATEWAY_URL está vacío. No se enviará el evento push.")
            return
        }

        val idToken = resolveIdToken()
        if (idToken.isBlank()) {
            Log.w(TAG, "No se pudo obtener el ID token del usuario autenticado.")
            return
        }

        val request = Request.Builder()
            .url("$baseUrl/send")
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        Log.d(TAG, "Enviando evento push al gateway: ${body.toString()}")

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(
                    TAG,
                    "El gateway push respondió ${response.code}: $responseBody"
                )
                throw IllegalStateException(
                    "No se pudo enviar el evento push (${response.code}): $responseBody"
                )
            }

            Log.d(TAG, "Gateway push OK: $responseBody")
        }
    }

    private suspend fun resolveIdToken(): String {
        val user = auth.currentUser ?: return ""

        val cached = runCatching { user.getIdToken(false).await().token.orEmpty() }
            .getOrElse {
                Log.w(TAG, "Fallo al obtener ID token cacheado", it)
                ""
            }

        if (cached.isNotBlank()) return cached

        return runCatching { user.getIdToken(true).await().token.orEmpty() }
            .onFailure { Log.e(TAG, "Fallo al refrescar el ID token", it) }
            .getOrElse { "" }
    }

    companion object {
        private const val TAG = "PushGatewayRepository"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}