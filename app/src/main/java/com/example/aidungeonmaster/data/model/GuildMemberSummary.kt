package com.example.aidungeonmaster.data.model

data class GuildMemberSummary(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val role: String = "member",
    val joinedAt: Long = 0L,
    val characterCount: Int = 0,
    val isOwner: Boolean = false
)