package com.example.aidungeonmaster.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.repository.AuthRepository
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = AuthRepository()
    private val socialRepository = SocialRepository()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    fun isUserLoggedIn(): Boolean {
        // BUG FIX: Solo se considera logueado si el email está verificado.
        // Antes devolvía true aunque el usuario no hubiera verificado el correo,
        // lo que provocaba que el registro metiera al usuario directamente en la app.
        val user = auth.currentUser
        return user != null && user.isEmailVerified
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        viewModelScope.launch { socialRepository.updatePresence(true) }
                        onSuccess()
                    } else {
                        auth.signOut()
                        errorMessage = "¡Alto ahí! Aún no has validado tu pergamino mágico (correo)."
                    }
                } else {
                    errorMessage = "Credenciales incorrectas."
                }
            }
    }

    fun register(
        email: String,
        pass: String,
        displayName: String,
        username: String,
        onSuccess: () -> Unit
    ) {
        repository.register(
            email = email,
            password = pass,
            displayName = displayName,
            username = username,
            onSuccess = {
                // BUG FIX: Se elimina la llamada a updatePresence aquí.
                // El usuario acaba de registrarse pero su email NO está verificado todavía,
                // así que no debe establecerse como "online" ni entrar en la app.
                // onSuccess solo muestra el Toast y hace popBackStack al Login.
                onSuccess()
            },
            onError = {
                errorMessage = it
            }
        )
    }

    fun clearError() {
        errorMessage = null
    }
}