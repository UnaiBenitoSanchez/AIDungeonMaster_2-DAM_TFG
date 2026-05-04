package com.example.aidungeonmaster.data.model

// Clase que encapsula la lógica de friendship.
data class Friendship(
    val id: String = "",
    val userA: String = "",
    val userB: String = "",
    val createdAt: Long = 0L,
    val createdBy: String = ""
)
