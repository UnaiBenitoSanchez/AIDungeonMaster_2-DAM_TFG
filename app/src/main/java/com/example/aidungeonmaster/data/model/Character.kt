package com.example.aidungeonmaster.data.model

// 1. Definimos qué es un Objeto
data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "consumible", // "arma", "armadura", "pocion", "pergamino"
    val effect: String = ""          // Ej: "Daño 1d8", "Cura 2d4"
)

// 2. Personaje con sistema de niveles
data class Character(
    val id: String = "",
    val name: String = "",
    val race: String = "",
    val characterClass: String = "",
    val stats: Map<String, Int> = emptyMap(),
    val physicalTraits: String = "",
    val gameTheme: String? = null,

    // --- CAMPOS DE SISTEMA ---
    val hpMax: Int = 20,
    val hpCurrent: Int = 20,
    val inventory: List<Item> = emptyList(),
    val portraitUrl: String = "",
    val lastPlayed: Long = 0L,

    // --- PROGRESIÓN ---
    val xp: Int = 0,
    val level: Int = 1,

    // --- ECONOMÍA ---
    val coins: Int = 0
) {
    /** XP necesario para pasar al siguiente nivel */
    val xpToNextLevel: Int get() = level * 100

    /** Porcentaje de progreso hacia el siguiente nivel (0.0 – 1.0) */
    val xpProgress: Float get() =
        if (xpToNextLevel > 0) (xp.toFloat() / xpToNextLevel).coerceIn(0f, 1f) else 0f

    /** Bonus de competencia según nivel (igual que D&D) */
    val profBonus: Int get() = when {
        level >= 17 -> 6
        level >= 13 -> 5
        level >= 9  -> 4
        level >= 5  -> 3
        else        -> 2
    }
}