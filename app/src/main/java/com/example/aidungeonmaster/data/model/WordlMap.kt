package com.example.aidungeonmaster.data.model

// Clase que encapsula la lógica de world location.
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

// Clase que encapsula la lógica de location life state.
data class LocationLifeState(
    val locationId: String = "",
    val prosperity: Int = 50,
    val security: Int = 50,
    val danger: Int = 20,
    val corruption: Int = 0,
    val mood: String = "estable",
    val controllingFactionId: String = "",
    val lastEventSummary: String = "",
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

// Clase que encapsula la lógica de world map state.
data class WorldMapState(
    val locations: List<WorldLocation> = emptyList(),
    val currentLocationId: String = "",
    val mapName: String = "Mundo Desconocido",
    val locationStates: Map<String, LocationLifeState> = emptyMap(),
    val recentWorldEvents: List<String> = emptyList(),
    val lastWorldSimulationAt: Long = 0L
)
