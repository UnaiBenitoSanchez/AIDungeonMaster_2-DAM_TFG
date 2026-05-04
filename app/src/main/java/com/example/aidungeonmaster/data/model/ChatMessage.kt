package com.example.aidungeonmaster.data.model

// Modelo de datos que representa chat message.
data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val type: String = "text",
    val createdAt: Long = 0L,
    val seenBy: List<String> = emptyList()
)
