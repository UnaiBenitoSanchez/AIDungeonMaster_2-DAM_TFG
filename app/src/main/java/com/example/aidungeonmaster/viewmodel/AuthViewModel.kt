package com.example.aidungeonmaster.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.aidungeonmaster.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance() // Instancia de Firebase

    // Función para comprobar si el usuario ya está logueado
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    private val repository = AuthRepository()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        isLoading = true
        repository.login(
            email,
            password,
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

    fun register(email: String, password: String, onSuccess: () -> Unit) {

        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email y contraseña obligatorios"
            return
        }

        if (password.length < 6) {
            errorMessage = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        isLoading = true

        repository.register(
            email,
            password,
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

    fun clearError() {
        errorMessage = null
    }
}
