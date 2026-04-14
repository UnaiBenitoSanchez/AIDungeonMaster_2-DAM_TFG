package com.example.aidungeonmaster.data.model

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromDisplayName: String = "",
    val fromUsername: String = "",
    val toDisplayName: String = "",
    val toUsername: String = "",
    val status: String = "pending", // pending, accepted, rejected, cancelled
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)