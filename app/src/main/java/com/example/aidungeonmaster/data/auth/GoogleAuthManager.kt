package com.example.aidungeonmaster.data.auth

import android.app.Activity
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CredentialManager
import com.example.aidungeonmaster.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

// Gestor encargado de google auth.
class GoogleAuthManager {

    // Obtiene google id token.
    suspend fun getGoogleIdToken(activity: Activity): Result<String> {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Falta GOOGLE_WEB_CLIENT_ID en local.properties. Añádelo antes de usar Google Login."
                )
            )
        }

        return runCatching {
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            extractGoogleIdToken(result)
        }.recoverCatching { throwable ->
            when (throwable) {
                is GetCredentialException -> throw IllegalStateException(
                    throwable.message ?: "No se pudo obtener la credencial de Google."
                )
                else -> throw throwable
            }
        }
    }

    // Ejecuta la lógica de extract google id token.
    private fun extractGoogleIdToken(result: GetCredentialResponse): String {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                return GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (e: GoogleIdTokenParsingException) {
                throw IllegalStateException("La respuesta de Google no se pudo interpretar.", e)
            }
        }

        throw IllegalStateException("La credencial devuelta no es válida para Google Login.")
    }
}
