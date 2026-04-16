package com.example.aidungeonmaster.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(it.message ?: "Error al iniciar sesión")
            }
    }

    fun register(
        email: String,
        password: String,
        displayName: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val usernameTrimmed = username.trim()
        val displayNameTrimmed = displayName.trim()
        val usernameLower = usernameTrimmed.lowercase()
        val displayNameLower = displayNameTrimmed.lowercase()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    onError("No se pudo obtener el usuario recién creado.")
                    return@addOnSuccessListener
                }

                val now = System.currentTimeMillis()

                val profile = hashMapOf(
                    "uid" to user.uid,
                    "email" to email.trim(),
                    "displayName" to displayNameTrimmed,
                    "displayNameLower" to displayNameLower,
                    "username" to usernameTrimmed,
                    "usernameLower" to usernameLower,
                    "photoUrl" to "",
                    "bio" to "Aventurero recién llegado a la taberna.",

                    // Esquema usado por la app
                    "accentColor" to "#D4AF37",
                    "profileBackgroundColor" to "#1E1E1E",

                    // Esquema esperado por tus reglas
                    "profileAccentColor" to "#D4AF37",
                    "profilePrimaryColor" to "#1E1E1E",
                    "profileSecondaryColor" to "#1E1E1E",

                    "isOnline" to false,
                    "lastSeen" to now,
                    "createdAt" to now,
                    "updatedAt" to now
                )

                db.collection("users")
                    .document(user.uid)
                    .set(profile)
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            ?.addOnSuccessListener { onSuccess() }
                            ?.addOnFailureListener {
                                onError("El usuario se creó, pero no se pudo enviar el correo de verificación.")
                            }
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "No se pudo guardar el perfil del usuario.")
                    }
            }
            .addOnFailureListener {
                onError(it.message ?: "Error al registrarse")
            }
    }

    fun isUserLogged(): Boolean {
        return auth.currentUser != null
    }
}