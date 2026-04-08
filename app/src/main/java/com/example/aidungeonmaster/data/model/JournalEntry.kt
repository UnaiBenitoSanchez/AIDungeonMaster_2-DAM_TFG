package com.example.aidungeonmaster.data.model

data class JournalEntry(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val fullText: String = "",

    val timestamp: Long = 0L,
    val chapter: String = "",

    val type: String = "", // "story", "combat", "loot", "location", "quest", "system"
    val tags: List<String> = emptyList(),

    val locationName: String = "",
    val enemyName: String = "",
    val itemNames: List<String> = emptyList(),

    val hpChange: Int = 0,
    val coinsChange: Int = 0,
    val xpGained: Int = 0
)