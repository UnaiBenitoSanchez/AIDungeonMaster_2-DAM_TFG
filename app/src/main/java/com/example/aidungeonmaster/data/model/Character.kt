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

    // --- NUEVOS CAMPOS (Añade estos) ---
    val hpMax: Int = 20,
    val hpCurrent: Int = 20,
    // Una lista de objetos que el personaje posee
    val inventory: List<Item> = emptyList()
)