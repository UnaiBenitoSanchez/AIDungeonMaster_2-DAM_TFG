package com.example.aidungeonmaster.data.model

// Modelo de datos que representa guild.
data class Guild(
    val id: String = "",
    val name: String = "",
    val nameLower: String = "",
    val description: String = "",
    val ownerUid: String = "",
    val ownerDisplayName: String = "",
    val accentColor: String = "#8E24AA",
    val bannerColor: String = "#1F1235",
    val memberCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val joined: Boolean = false
)
