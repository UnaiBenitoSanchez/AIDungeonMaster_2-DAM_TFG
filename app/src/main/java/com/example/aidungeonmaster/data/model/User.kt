package com.example.aidungeonmaster.data.model

// Modelo de datos que representa app user.
data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val displayNameLower: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val accentColor: String = "#D4AF37",
    val profileBackgroundColor: String = "#1E1E1E",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val characterCount: Int = 0,
    val currentGuildId: String = ""
)
