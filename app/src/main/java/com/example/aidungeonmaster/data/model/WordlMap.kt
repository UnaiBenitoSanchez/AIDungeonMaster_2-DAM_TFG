package com.example.aidungeonmaster.data.model

/**
 * Representa una ubicación descubierta en el mapa del mundo.
 *
 * @param id         Identificador único (ej: "bosque_oscuro_1")
 * @param name       Nombre del lugar (ej: "Bosque Oscuro")
 * @param description Descripción breve del lugar
 * @param x          Posición horizontal en el mapa (0.0 – 1.0, relativo al ancho)
 * @param y          Posición vertical en el mapa (0.0 – 1.0, relativo al alto)
 * @param icon       Emoji o símbolo que representa el lugar en el mapa
 * @param type       Tipo de lugar: "ciudad", "mazmorra", "bosque", "montaña", "mar", "pueblo", etc.
 * @param isCurrentLocation  true si el jugador está aquí ahora mismo
 * @param discoveredAt  Timestamp de cuando fue descubierto
 */
data class WorldLocation(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val icon: String = "📍",
    val type: String = "lugar",
    val isCurrentLocation: Boolean = false,
    val discoveredAt: Long = System.currentTimeMillis()
)

/**
 * Estado completo del mapa del mundo para una partida.
 */
data class WorldMapState(
    val locations: List<WorldLocation> = emptyList(),
    val currentLocationId: String = "",
    val mapName: String = "Mundo Desconocido"
)