package com.example.aidungeonmaster.data.model

// Modelo de datos que representa guild membership.
data class GuildMembership(
    val guildId: String = "",
    val uid: String = "",
    val role: String = "member",
    val joinedAt: Long = 0L,
    val displayName: String = "",
    val username: String = ""
)
