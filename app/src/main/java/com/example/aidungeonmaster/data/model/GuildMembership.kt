package com.example.aidungeonmaster.data.model

data class GuildMembership(
    val guildId: String = "",
    val uid: String = "",
    val role: String = "member",
    val joinedAt: Long = 0L,
    val displayName: String = "",
    val username: String = ""
)