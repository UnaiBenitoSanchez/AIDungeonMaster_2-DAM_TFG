package com.example.aidungeonmaster.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.repository.AuthRepository
import com.example.aidungeonmaster.data.repository.PasswordResetAvailability
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

// ViewModel que coordina el estado y la lógica de auth.
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val socialRepository = SocialRepository()

    var isLoading by mutableStateOf(false)
        private set

    var isCheckingPasswordResetAvailability by mutableStateOf(false)
        private set

    var isSendingPasswordReset by mutableStateOf(false)
        private set

    var isChangingPassword by mutableStateOf(false)
        private set

    var passwordResetAvailability by mutableStateOf(PasswordResetAvailability.EMPTY_EMAIL)
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

    // Comprueba si el correo escrito es apto para enviar recuperación genérica.
    fun refreshPasswordResetAvailability(email: String) {
        val trimmed = email.trim()
        passwordResetAvailability = when {
            trimmed.isBlank() -> PasswordResetAvailability.EMPTY_EMAIL
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> PasswordResetAvailability.INVALID_EMAIL
            else -> PasswordResetAvailability.AVAILABLE
        }
        isCheckingPasswordResetAvailability = false
    }

    // Envía un correo de recuperación de contraseña.
    fun sendPasswordReset(email: String, onSuccess: (String) -> Unit) {
        val trimmed = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            errorMessage = "Introduce un correo electrónico válido."
            return
        }

        isSendingPasswordReset = true
        errorMessage = null

        viewModelScope.launch {
            runCatching {
                repository.sendPasswordReset(trimmed)
            }.onSuccess {
                passwordResetAvailability = PasswordResetAvailability.AVAILABLE
                onSuccess("Si existe una cuenta compatible con recuperación para ese correo, te hemos enviado un email.")
            }.onFailure {
                errorMessage = it.message ?: "No se pudo enviar el correo de recuperación."
            }
            isSendingPasswordReset = false
        }
    }

    // Cambia la contraseña del usuario autenticado.
    fun changeCurrentUserPassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        if (currentPassword.isBlank()) {
            errorMessage = "Introduce tu contraseña actual."
            return
        }

        if (newPassword.length < 6) {
            errorMessage = "La nueva contraseña debe tener al menos 6 caracteres."
            return
        }

        isChangingPassword = true
        errorMessage = null

        viewModelScope.launch {
            runCatching {
                repository.updateCurrentUserPassword(currentPassword, newPassword)
            }.onSuccess {
                onSuccess()
            }.onFailure { throwable ->
                errorMessage = mapPasswordChangeError(throwable)
            }
            isChangingPassword = false
        }
    }

    // Indica si el usuario actual puede cambiar la contraseña desde la app.
    fun canCurrentUserChangePassword(): Boolean = repository.currentUserSupportsPasswordAuth()

    // Indica si el usuario actual ha accedido solo con Google.
    fun isCurrentUserGoogleOnly(): Boolean = repository.currentUserIsGoogleOnly()

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

    // Traduce errores de actualización de contraseña a mensajes de negocio.
    private fun mapPasswordChangeError(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> "La contraseña actual no es correcta."
            is FirebaseAuthWeakPasswordException -> throwable.reason ?: "La nueva contraseña es demasiado débil."
            is FirebaseAuthRecentLoginRequiredException -> "Necesitas volver a autenticarte antes de cambiar la contraseña."
            is FirebaseNetworkException -> "No hay conexión estable para actualizar la contraseña."
            else -> throwable.message ?: "No se pudo actualizar la contraseña."
        }
    }
}
