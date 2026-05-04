package com.example.aidungeonmaster.data.model

// Clase que encapsula la lógica de guild boss participant.
data class GuildBossParticipant(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val selectedCharacterDocId: String = "",
    val selectedCharacterName: String = "",
    val selectedCharacterClass: String = "",
    val hpMax: Int = 0,
    val hpCurrent: Int = 0,
    val attackMin: Int = 1,
    val attackMax: Int = 4,
    val attackBonus: Int = 0,
    val armorClass: Int = 10,
    val ready: Boolean = false,
    val alive: Boolean = true,
    val cooldowns: Map<String, Int> = emptyMap(),
    val defenseBonus: Int = 0,
    val advantageCharges: Int = 0,
    val joinedAt: Long = 0L,
    val updatedAt: Long = 0L
)
