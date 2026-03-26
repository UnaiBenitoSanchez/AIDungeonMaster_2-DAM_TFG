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

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Verificamos si ya validó el correo
                    if (user?.isEmailVerified == true) {
                        onSuccess() // ¡Adelante, entra a la aventura!
                    } else {
                        // No le dejamos entrar y cerramos la sesión temporal que hizo Firebase
                        auth.signOut()
                        errorMessage = "¡Alto ahí! Aún no has validado tu pergamino mágico (correo)."
                    }
                } else {
                    errorMessage = "Credenciales incorrectas."
                }
            }
    }

    fun register(email: String, pass: String, onSuccess: () -> Unit) {
        // 1. Creamos el aventurero en Firebase
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 2. Si se creó con éxito, obtenemos el usuario actual
                    val user = auth.currentUser

                    // 3. Enviamos la lechuza (correo de verificación)
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                // Correo enviado correctamente, llamamos al callback de éxito
                                onSuccess()
                            } else {
                                errorMessage = "El pergamino de verificación se perdió en el camino."
                            }
                        }
                } else {
                    // Manejo de errores (correo ya en uso, mala conexión, etc.)
                    errorMessage = task.exception?.localizedMessage ?: "Fallo al crear el personaje."
                }
            }
    }

    fun clearError() {
        errorMessage = null
    }
}
