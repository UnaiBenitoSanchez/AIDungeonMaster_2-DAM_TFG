package com.example.aidungeonmaster.data.model

data class MonsterStatSnapshot(
    val hpMaxObserved: Int = 0,
    val armorClassObserved: Int? = null,
    val damageNotes: List<String> = emptyList(),
    val abilitiesSeen: List<String> = emptyList()
)

data class BestiaryLoot(
    val name: String = "",
    val category: String = "desconocido",
    val details: String = "",
    val quantityObserved: Int = 1,
    val timesDropped: Int = 1
)

data class BestiaryEntry(
    val monsterId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",

    val firstSeenAt: Long = 0L,
    val lastSeenAt: Long = 0L,

    val timesEncountered: Int = 0,
    val timesDefeated: Int = 0,

    val locationsSeen: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    val lastObservedStats: MonsterStatSnapshot = MonsterStatSnapshot(),

    val knownLoot: List<String> = emptyList(),
    val detailedKnownLoot: List<BestiaryLoot> = emptyList(),

    val observedWeaknesses: List<String> = emptyList(),
    val observedResistances: List<String> = emptyList(),

    val notes: String = ""
)