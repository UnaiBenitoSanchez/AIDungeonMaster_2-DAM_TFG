package com.example.aidungeonmaster.utils

object RaceModifiers {

    fun apply(race: String, stats: Map<String, Int>): Map<String, Int> {
        return when (race) {
            "Humano" -> stats.mapValues { it.value + 1 }
            "Elfo" -> stats + ("dexterity" to stats["dexterity"]!! + 2)
            "Enano" -> stats + ("constitution" to stats["constitution"]!! + 2)
            else -> stats
        }
    }
}
