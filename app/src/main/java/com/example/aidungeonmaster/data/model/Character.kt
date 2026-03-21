package com.example.aidungeonmaster.data.model

// 1. Definimos qué es un Objeto
data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "consumible", // "arma", "armadura", "pocion", "pergamino"
    val effect: String = ""          // Ej: "Daño 1d8", "Cura 2d4"
)

// 2. Actualizamos el Personaje para que tenga Mochila y Vida real
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
    val lastPlayed: Long = 0L   // timestamp Unix ms — para ordenar por última partida
)