package com.example.aidungeonmaster.data.model

data class PersonalRoomPlacedDecoration(
    val decorationId: String = "",
    val slotId: String = ""
)

data class PersonalRoomState(
    val ownedDecorationIds: List<String> = emptyList(),
    val placedDecorations: List<PersonalRoomPlacedDecoration> = emptyList(),
    val roomTheme: String = "fortaleza",
    val updatedAt: Long = System.currentTimeMillis()
)

data class PersonalRoomDecoration(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val emoji: String,
    val allowedSlots: List<String>
)

data class PersonalRoomSlot(
    val id: String,
    val label: String,
    val x: Float,
    val z: Float
)

val PERSONAL_ROOM_SLOTS = listOf(
    PersonalRoomSlot("tile_a1", "Baldosa A1", -3.3f, -3.3f),
    PersonalRoomSlot("tile_a2", "Baldosa A2", -1.1f, -3.3f),
    PersonalRoomSlot("tile_a3", "Baldosa A3",  1.1f, -3.3f),
    PersonalRoomSlot("tile_a4", "Baldosa A4",  3.3f, -3.3f),

    PersonalRoomSlot("tile_b1", "Baldosa B1", -3.3f, -1.1f),
    PersonalRoomSlot("tile_b2", "Baldosa B2", -1.1f, -1.1f),
    PersonalRoomSlot("tile_b3", "Baldosa B3",  1.1f, -1.1f),
    PersonalRoomSlot("tile_b4", "Baldosa B4",  3.3f, -1.1f),

    PersonalRoomSlot("tile_c1", "Baldosa C1", -3.3f,  1.1f),
    PersonalRoomSlot("tile_c2", "Baldosa C2", -1.1f,  1.1f),
    PersonalRoomSlot("tile_c3", "Baldosa C3",  1.1f,  1.1f),
    PersonalRoomSlot("tile_c4", "Baldosa C4",  3.3f,  1.1f),

    PersonalRoomSlot("tile_d1", "Baldosa D1", -3.3f,  3.3f),
    PersonalRoomSlot("tile_d2", "Baldosa D2", -1.1f,  3.3f),
    PersonalRoomSlot("tile_d3", "Baldosa D3",  1.1f,  3.3f),
    PersonalRoomSlot("tile_d4", "Baldosa D4",  3.3f,  3.3f)
)

val PERSONAL_ROOM_ALL_SLOT_IDS = PERSONAL_ROOM_SLOTS.map { it.id }

val PERSONAL_ROOM_CATALOG = listOf(
    PersonalRoomDecoration(
        id = "banner_royal",
        name = "Estandarte Real",
        description = "Un gran estandarte para dar presencia a tu sala.",
        price = 120,
        emoji = "🚩",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "torch_pair",
        name = "Antorchas Gemelas",
        description = "Iluminación cálida para una fortaleza con carácter.",
        price = 90,
        emoji = "🔥",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "weapon_rack",
        name = "Armero",
        description = "Un soporte con armas para decorar tu base.",
        price = 150,
        emoji = "⚔️",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "treasure_chest",
        name = "Cofre del Botín",
        description = "Un cofre robusto perfecto para exhibir tus riquezas.",
        price = 140,
        emoji = "🧰",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "red_rug",
        name = "Alfombra Carmesí",
        description = "Hace la sala más noble y acogedora.",
        price = 110,
        emoji = "🟥",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "crystal_orb",
        name = "Orbe Arcano",
        description = "Una reliquia brillante para dar un toque mágico.",
        price = 170,
        emoji = "🔮",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "book_stack",
        name = "Biblioteca de Guerra",
        description = "Libros, mapas y saber acumulado de tus aventuras.",
        price = 130,
        emoji = "📚",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "potted_tree",
        name = "Árbol en Maceta",
        description = "Un detalle vivo que rompe la piedra de la fortaleza.",
        price = 80,
        emoji = "🌿",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "war_table",
        name = "Mesa de Estrategia",
        description = "Una gran mesa para preparar campañas y planes.",
        price = 190,
        emoji = "🗺️",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    ),
    PersonalRoomDecoration(
        id = "throne_seat",
        name = "Asiento del Héroe",
        description = "Un asiento imponente para presidir tu refugio.",
        price = 220,
        emoji = "🪑",
        allowedSlots = PERSONAL_ROOM_ALL_SLOT_IDS
    )
)

fun personalRoomDecorationById(id: String): PersonalRoomDecoration? =
    PERSONAL_ROOM_CATALOG.firstOrNull { it.id == id }

fun personalRoomSlotById(id: String): PersonalRoomSlot? =
    PERSONAL_ROOM_SLOTS.firstOrNull { it.id == id }