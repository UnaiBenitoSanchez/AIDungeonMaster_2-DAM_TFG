package com.example.aidungeonmaster.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.repository.AuthRepository
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

// ViewModel que coordina el estado y la lógica de auth.
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val socialRepository = SocialRepository()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    // Comprueba si user logged in.
    fun isUserLoggedIn(): Boolean {
        return canAccessApp(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser)
    }

    // Ejecuta la lógica de login.
    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        isLoading = true
        errorMessage = null

        repository.login(
            email = email,
            password = pass,
            onSuccess = { user ->
                if (canAccessApp(user)) {
                    viewModelScope.launch {
                        runCatching { socialRepository.updatePresence(true) }
                        isLoading = false
                        onSuccess()
                    }
                } else {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    isLoading = false
                    errorMessage = "¡Alto ahí! Aún no has validado tu pergamino mágico (correo)."
                }
            },
            onError = {
                isLoading = false
                errorMessage = "Credenciales incorrectas."
            }
        )
    }

    // Ejecuta la lógica de login with google.
    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            runCatching {
                val user = repository.signInWithGoogle(idToken)
                repository.upsertPublicUserProfile(user, provider = GoogleAuthProvider.PROVIDER_ID)
                socialRepository.updatePresence(true)
            }.onSuccess {
                isLoading = false
                onSuccess()
            }.onFailure { throwable ->
                isLoading = false
                errorMessage = when (throwable) {
                    is FirebaseAuthUserCollisionException ->
                        "Ya existe una cuenta con este correo. Entra primero con correo y contraseña para enlazar Google más adelante."
                    else -> throwable.message ?: "No se pudo iniciar sesión con Google."
                }
            }
        }
    }

    // Ejecuta la lógica de register.
    fun register(
        email: String,
        pass: String,
        displayName: String,
        username: String,
        onSuccess: () -> Unit
    ) {
        isLoading = true
        errorMessage = null

        repository.register(
            email = email,
            password = pass,
            displayName = displayName,
            username = username,
            onSuccess = {
                isLoading = false
                onSuccess()
            },
            onError = {
                isLoading = false
                errorMessage = it
            }
        )
    }

    // Limpia error.
    fun clearError() {
        errorMessage = null
    }

    // Comprueba si access app.
    private fun canAccessApp(user: FirebaseUser?): Boolean {
        if (user == null) return false

        val hasGoogleProvider = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        return user.isEmailVerified || hasGoogleProvider
    }
}
