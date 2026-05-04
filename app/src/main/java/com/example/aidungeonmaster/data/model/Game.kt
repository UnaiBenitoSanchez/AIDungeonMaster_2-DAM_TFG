package com.example.aidungeonmaster.data.model

// Modelo de datos que representa game.
data class Game(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val style: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
