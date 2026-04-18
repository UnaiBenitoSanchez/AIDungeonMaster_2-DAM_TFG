package com.example.aidungeonmaster.data.model

data class GuildChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val senderDisplayName: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val type: String = "text",
    val createdAt: Long = 0L
)