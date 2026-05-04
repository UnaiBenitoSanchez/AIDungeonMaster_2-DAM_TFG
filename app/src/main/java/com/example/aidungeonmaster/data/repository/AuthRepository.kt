package com.example.aidungeonmaster.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import kotlin.random.Random

// Repositorio que centraliza el acceso a datos de auth.
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Ejecuta la lógica de login.
    fun login(
        email: String,
        password: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result -> onSuccess(result.user) }
            .addOnFailureListener {
                onError(it.message ?: "Error al iniciar sesión")
            }
    }

    // Ejecuta la lógica de register.
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
                    "accentColor" to "#D4AF37",
                    "profileBackgroundColor" to "#1E1E1E",
                    "profileAccentColor" to "#D4AF37",
                    "profilePrimaryColor" to "#1E1E1E",
                    "profileSecondaryColor" to "#1E1E1E",
                    "isOnline" to false,
                    "lastSeen" to now,
                    "createdAt" to now,
                    "updatedAt" to now,
                    "characterCount" to 0,
                    "currentGuildId" to ""
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

    // Ejecuta la lógica de sign in with google.
    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: throw IllegalStateException("No se pudo completar el acceso con Google.")
    }

    // Ejecuta la lógica de upsert public user profile.
    suspend fun upsertPublicUserProfile(firebaseUser: FirebaseUser, provider: String) {
        val docRef = db.collection("users").document(firebaseUser.uid)
        val existing = docRef.get().await()
        val now = System.currentTimeMillis()

        val resolvedDisplayName = firebaseUser.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email
                ?.substringBefore("@")
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .orEmpty()
                .ifBlank { "Aventurero" }

        val resolvedDisplayNameLower = resolvedDisplayName.lowercase()
        val resolvedPhotoUrl = firebaseUser.photoUrl?.toString().orEmpty()
        val resolvedEmail = firebaseUser.email.orEmpty().trim()

        if (existing.exists()) {
            val updates = mutableMapOf<String, Any>(
                "email" to resolvedEmail,
                "isOnline" to true,
                "lastSeen" to now,
                "updatedAt" to now,
                "authProvider" to provider
            )

            val currentDisplayName = existing.getString("displayName").orEmpty()
            if (currentDisplayName.isBlank()) {
                updates["displayName"] = resolvedDisplayName
                updates["displayNameLower"] = resolvedDisplayNameLower
            }

            val currentPhotoUrl = existing.getString("photoUrl").orEmpty()
            if (currentPhotoUrl.isBlank() && resolvedPhotoUrl.isNotBlank()) {
                updates["photoUrl"] = resolvedPhotoUrl
            }

            val currentUsername = existing.getString("username").orEmpty()
            if (currentUsername.isBlank()) {
                val generatedUsername = generateUniqueUsername(firebaseUser)
                updates["username"] = generatedUsername
                updates["usernameLower"] = generatedUsername.lowercase()
            }

            docRef.update(updates).await()
            return
        }

        val generatedUsername = generateUniqueUsername(firebaseUser)
        val profile = hashMapOf(
            "uid" to firebaseUser.uid,
            "email" to resolvedEmail,
            "displayName" to resolvedDisplayName,
            "displayNameLower" to resolvedDisplayNameLower,
            "username" to generatedUsername,
            "usernameLower" to generatedUsername.lowercase(),
            "photoUrl" to resolvedPhotoUrl,
            "bio" to "Aventurero recién llegado a la taberna.",
            "accentColor" to "#D4AF37",
            "profileBackgroundColor" to "#1E1E1E",
            "profileAccentColor" to "#D4AF37",
            "profilePrimaryColor" to "#1E1E1E",
            "profileSecondaryColor" to "#1E1E1E",
            "isOnline" to true,
            "lastSeen" to now,
            "createdAt" to now,
            "updatedAt" to now,
            "characterCount" to 0,
            "currentGuildId" to "",
            "authProvider" to provider
        )

        docRef.set(profile).await()
    }

    // Comprueba si user logged.
    fun isUserLogged(): Boolean {
        return auth.currentUser != null
    }

    private suspend fun generateUniqueUsername(user: FirebaseUser): String {
        val emailSeed = user.email?.substringBefore("@").orEmpty()
        val displayNameSeed = user.displayName.orEmpty()
        val baseSeed = sanitizeUsername(
            listOf(emailSeed, displayNameSeed, "aventurero")
                .firstOrNull { sanitizeUsername(it).isNotBlank() }
                .orEmpty()
        ).ifBlank { "aventurero" }

        val shortBase = baseSeed.take(16).ifBlank { "aventurero" }

        repeat(20) { attempt ->
            val candidate = if (attempt == 0) shortBase else "$shortBase${Random.nextInt(1000, 9999)}"
            val exists = db.collection("users")
                .whereEqualTo("usernameLower", candidate.lowercase())
                .limit(1)
                .get()
                .await()
                .documents
                .isNotEmpty()

            if (!exists) return candidate
        }

        return "aventurero${System.currentTimeMillis().toString().takeLast(6)}"
    }

    // Ejecuta la lógica de sanitize username.
    private fun sanitizeUsername(raw: String): String {
        val normalized = Normalizer.normalize(raw.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9_]"), "")

        return normalized
            .takeIf { it.isNotBlank() }
            ?.let { if (it.length >= 3) it else it.padEnd(3, 'x') }
            .orEmpty()
    }
}
