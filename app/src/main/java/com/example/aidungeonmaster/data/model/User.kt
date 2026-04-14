package com.example.aidungeonmaster.data.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val displayNameLower: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)