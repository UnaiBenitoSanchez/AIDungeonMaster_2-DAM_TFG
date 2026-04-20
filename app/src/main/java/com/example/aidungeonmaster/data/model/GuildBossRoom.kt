package com.example.aidungeonmaster.data.model

data class GuildBossRoom(
    val guildId: String = "",
    val status: String = "waiting", // waiting | battle | finished
    val bossName: String = "Señor del Abismo",
    val bossHpMax: Int = 0,
    val bossHpCurrent: Int = 0,
    val bossAttackMin: Int = 0,
    val bossAttackMax: Int = 0,
    val currentTurnUid: String = "",
    val turnOrder: List<String> = emptyList(),
    val turnIndex: Int = 0,
    val round: Int = 1,
    val winner: String = "", // boss | guild | ""
    val battleLog: List<String> = emptyList(),
    val createdBy: String = "",
    val updatedAt: Long = 0L
)