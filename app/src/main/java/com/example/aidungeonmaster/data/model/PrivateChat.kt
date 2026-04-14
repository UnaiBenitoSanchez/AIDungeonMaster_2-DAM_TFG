package com.example.aidungeonmaster.data.model

data class PrivateChat(
    val id: String = "",
    val members: List<String> = emptyList(),
    val friendshipId: String = "",
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderUid: String = ""
)