package com.example.aidungeonmaster.data.model

data class Character(
    val id: String = "",
    val name: String = "",
    val race: String = "",
    val characterClass: String = "",
    val stats: Map<String, Int> = emptyMap(),
    val physicalTraits: String = "",
    val gameTheme: String? = null
)