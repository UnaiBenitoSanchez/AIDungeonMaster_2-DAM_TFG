package com.example.aidungeonmaster.data.repository

import android.content.Context
import android.util.Log
import com.example.aidungeonmaster.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class PushTokenRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val prefs = appContext.getSharedPreferences(PREFS_PUSH_TOKENS, Context.MODE_PRIVATE)

    suspend fun syncCurrentToken() {
        val token = FirebaseMessaging.getInstance().token.await().orEmpty().trim()
        if (token.isBlank()) {
            throw IllegalStateException("Firebase Messaging devolvió un token vacío")
        }
        syncProvidedToken(token)
    }

    suspend fun syncProvidedToken(token: String) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No hay usuario autenticado para registrar el token FCM")

        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            throw IllegalArgumentException("No se puede registrar un token FCM vacío")
        }

        val tokenHash = sha256(cleanToken)
        val previousHash = prefs.getString(prefKeyTokenHash(uid), null)
        val now = System.currentTimeMillis()
        val userRef = db.collection("users").document(uid)

        try {
            val rootPayload = hashMapOf<String, Any>(
                "pushPrimaryToken" to cleanToken,
                "pushPrimaryTokenHash" to tokenHash,
                "pushPrimaryPlatform" to "android",
                "pushPrimaryUpdatedAt" to now,
                "pushLastSyncAt" to now,
                "pushLastSyncOk" to true,
                "pushLastSyncError" to "",
                "pushAppVersion" to BuildConfig.VERSION_NAME
            )

            userRef.set(rootPayload, SetOptions.merge()).await()

            var subcollectionSynced = true
            val tokenDocPayload = hashMapOf<String, Any>(
                "token" to cleanToken,
                "platform" to "android",
                "updatedAt" to now,
                "appVersion" to BuildConfig.VERSION_NAME
            )

            runCatching {
                userRef.collection("device_tokens")
                    .document(tokenHash)
                    .set(tokenDocPayload, SetOptions.merge())
                    .await()
            }.onFailure { error ->
                subcollectionSynced = false
                Log.e(TAG, "No se pudo escribir users/$uid/device_tokens/$tokenHash", error)
            }

            if (!previousHash.isNullOrBlank() && previousHash != tokenHash) {
                runCatching {
                    userRef.collection("device_tokens")
                        .document(previousHash)
                        .delete()
                        .await()
                }.onFailure { error ->
                    Log.w(TAG, "No se pudo borrar el token anterior de la subcolección", error)
                }
            }

            userRef.set(
                hashMapOf<String, Any>(
                    "pushLastSubcollectionSyncOk" to subcollectionSynced,
                    "pushLastSubcollectionSyncAt" to now
                ),
                SetOptions.merge()
            ).await()

            prefs.edit()
                .putString(prefKeyTokenHash(uid), tokenHash)
                .apply()

            Log.d(TAG, "Token FCM sincronizado correctamente para uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando el token FCM para uid=$uid", e)

            runCatching {
                userRef.set(
                    hashMapOf<String, Any>(
                        "pushLastSyncAt" to now,
                        "pushLastSyncOk" to false,
                        "pushLastSyncError" to (e.message ?: "error desconocido")
                    ),
                    SetOptions.merge()
                ).await()
            }

            throw e
        }
    }

    suspend fun unregisterCurrentTokenForCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        val tokenHash = prefs.getString(prefKeyTokenHash(uid), null).orEmpty()
        val userRef = db.collection("users").document(uid)

        if (tokenHash.isNotBlank()) {
            runCatching {
                userRef.collection("device_tokens")
                    .document(tokenHash)
                    .delete()
                    .await()
            }.onFailure { error ->
                Log.w(TAG, "No se pudo borrar el token de la subcolección al cerrar sesión", error)
            }
        }

        runCatching {
            userRef.update(
                mapOf(
                    "pushPrimaryToken" to FieldValue.delete(),
                    "pushPrimaryTokenHash" to FieldValue.delete(),
                    "pushPrimaryPlatform" to FieldValue.delete(),
                    "pushPrimaryUpdatedAt" to FieldValue.delete(),
                    "pushLastSyncAt" to System.currentTimeMillis(),
                    "pushLastSyncOk" to false,
                    "pushLastSyncError" to "token eliminado en logout",
                    "pushAppVersion" to FieldValue.delete()
                )
            ).await()
        }.onFailure { error ->
            Log.w(TAG, "No se pudo limpiar el token raíz del usuario al cerrar sesión", error)
        }

        prefs.edit().remove(prefKeyTokenHash(uid)).apply()
    }

    fun clearLocalStateForUser(uid: String) {
        prefs.edit().remove(prefKeyTokenHash(uid)).apply()
    }

    private fun prefKeyTokenHash(uid: String) = "push_token_hash_$uid"

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "PushTokenRepository"
        private const val PREFS_PUSH_TOKENS = "push_token_registry"
    }
}