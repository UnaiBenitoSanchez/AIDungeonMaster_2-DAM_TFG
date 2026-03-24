package com.example.aidungeonmaster.data.model

// ─────────────────────────────────────────────────────────────────────────────
//  LOGROS
// ─────────────────────────────────────────────────────────────────────────────

enum class AchievementCategory(val label: String, val emoji: String) {
    COMBAT("Combate", "⚔️"),
    EXPLORATION("Exploración", "🗺️"),
    PROGRESSION("Progresión", "⭐"),
    SOCIAL("Social", "🤝"),
    COLLECTOR("Coleccionista", "🎒")
}

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: AchievementCategory = AchievementCategory.COMBAT,
    val emoji: String = "🏆",
    val xpReward: Int = 50,
    val isSecret: Boolean = false,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)

/** Catálogo completo de logros del juego */
object AchievementCatalog {
    val all: List<Achievement> = listOf(

        // ── COMBATE ──────────────────────────────────────────────────────────
        Achievement(
            id = "first_blood",
            title = "Primera Sangre",
            description = "Gana tu primer combate.",
            category = AchievementCategory.COMBAT,
            emoji = "⚔️",
            xpReward = 50
        ),
        Achievement(
            id = "ten_victories",
            title = "Guerrero Veterano",
            description = "Gana 10 combates.",
            category = AchievementCategory.COMBAT,
            emoji = "🛡️",
            xpReward = 150
        ),
        Achievement(
            id = "critical_strike",
            title = "Golpe Crítico",
            description = "Consigue un golpe crítico en combate.",
            category = AchievementCategory.COMBAT,
            emoji = "💥",
            xpReward = 75
        ),
        Achievement(
            id = "survive_low_hp",
            title = "Al Filo de la Muerte",
            description = "Sobrevive a un combate con 1 HP.",
            category = AchievementCategory.COMBAT,
            emoji = "💀",
            xpReward = 100
        ),

        // ── EXPLORACIÓN ──────────────────────────────────────────────────────
        Achievement(
            id = "first_location",
            title = "Explorador Novato",
            description = "Descubre tu primera ubicación en el mapa.",
            category = AchievementCategory.EXPLORATION,
            emoji = "🗺️",
            xpReward = 30
        ),
        Achievement(
            id = "five_locations",
            title = "Trotamundos",
            description = "Descubre 5 ubicaciones distintas.",
            category = AchievementCategory.EXPLORATION,
            emoji = "🧭",
            xpReward = 100
        ),
        Achievement(
            id = "ten_locations",
            title = "Cartógrafo Real",
            description = "Descubre 10 ubicaciones distintas.",
            category = AchievementCategory.EXPLORATION,
            emoji = "🌍",
            xpReward = 200
        ),

        // ── PROGRESIÓN ───────────────────────────────────────────────────────
        Achievement(
            id = "level_5",
            title = "Aventurero Consagrado",
            description = "Alcanza el nivel 5.",
            category = AchievementCategory.PROGRESSION,
            emoji = "⭐",
            xpReward = 100
        ),
        Achievement(
            id = "level_10",
            title = "Héroe de Leyenda",
            description = "Alcanza el nivel 10.",
            category = AchievementCategory.PROGRESSION,
            emoji = "🌟",
            xpReward = 250
        ),
        Achievement(
            id = "first_levelup",
            title = "¡Subiste de Nivel!",
            description = "Sube de nivel por primera vez.",
            category = AchievementCategory.PROGRESSION,
            emoji = "🆙",
            xpReward = 50
        ),

        // ── COLECCIONISTA ────────────────────────────────────────────────────
        Achievement(
            id = "first_item",
            title = "Mochila Llena",
            description = "Encuentra tu primer objeto.",
            category = AchievementCategory.COLLECTOR,
            emoji = "🎒",
            xpReward = 30
        ),
        Achievement(
            id = "five_items",
            title = "Comerciante Astuto",
            description = "Acumula 5 objetos en el inventario.",
            category = AchievementCategory.COLLECTOR,
            emoji = "💼",
            xpReward = 80
        ),

        // ── SECRETOS ─────────────────────────────────────────────────────────
        Achievement(
            id = "qr_scan",
            title = "Código Misterioso",
            description = "Escanea un código QR en el mundo real.",
            category = AchievementCategory.EXPLORATION,
            emoji = "🔲",
            xpReward = 120,
            isSecret = true
        )
    )

    fun getById(id: String): Achievement? = all.firstOrNull { it.id == id }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MISIONES
// ─────────────────────────────────────────────────────────────────────────────

enum class QuestStatus { AVAILABLE, IN_PROGRESS, COMPLETED, FAILED }

enum class QuestObjectiveType {
    WINS,           // Ganar N combates
    LOCATIONS,      // Descubrir N lugares
    ITEMS,          // Encontrar N objetos
    MESSAGES,       // Enviar N acciones al DM
    LEVEL_REACH,    // Alcanzar nivel N
    XP_EARN         // Ganar N XP total
}

data class QuestObjective(
    val type: QuestObjectiveType = QuestObjectiveType.WINS,
    val targetValue: Int = 1,
    val currentValue: Int = 0,
    val description: String = ""
) {
    val isCompleted: Boolean get() = currentValue >= targetValue
    val progress: Float get() = (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}

data class Quest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val emoji: String = "📜",
    val xpReward: Int = 100,
    val objectives: List<QuestObjective> = emptyList(),
    val status: QuestStatus = QuestStatus.AVAILABLE,
    val acceptedAt: Long = 0L,
    val completedAt: Long = 0L
) {
    val isCompleted: Boolean get() = objectives.all { it.isCompleted }
    val overallProgress: Float get() = if (objectives.isEmpty()) 0f
    else objectives.map { it.progress }.average().toFloat()
}

/** Misiones predefinidas disponibles en el juego */
object QuestCatalog {
    val all: List<Quest> = listOf(
        Quest(
            id = "intro_quest",
            title = "Primeros Pasos",
            description = "Comienza tu aventura enviando tus primeras acciones al Dungeon Master.",
            emoji = "👣",
            xpReward = 50,
            objectives = listOf(
                QuestObjective(
                    type = QuestObjectiveType.MESSAGES,
                    targetValue = 5,
                    description = "Envía 5 acciones al DM"
                )
            )
        ),
        Quest(
            id = "warrior_path",
            title = "El Camino del Guerrero",
            description = "Demuestra tu valor en combate ganando batallas.",
            emoji = "⚔️",
            xpReward = 150,
            objectives = listOf(
                QuestObjective(
                    type = QuestObjectiveType.WINS,
                    targetValue = 3,
                    description = "Gana 3 combates"
                )
            )
        ),
        Quest(
            id = "explorer_path",
            title = "Tierras Desconocidas",
            description = "Explora el mundo y descubre nuevos lugares.",
            emoji = "🗺️",
            xpReward = 120,
            objectives = listOf(
                QuestObjective(
                    type = QuestObjectiveType.LOCATIONS,
                    targetValue = 3,
                    description = "Descubre 3 ubicaciones"
                )
            )
        ),
        Quest(
            id = "fortune_hunter",
            title = "Cazador de Tesoros",
            description = "Reúne objetos valiosos en tus aventuras.",
            emoji = "💎",
            xpReward = 100,
            objectives = listOf(
                QuestObjective(
                    type = QuestObjectiveType.ITEMS,
                    targetValue = 3,
                    description = "Encuentra 3 objetos"
                )
            )
        ),
        Quest(
            id = "veteran_hero",
            title = "El Héroe Veterano",
            description = "Una misión épica para verdaderos aventureros.",
            emoji = "🦅",
            xpReward = 300,
            objectives = listOf(
                QuestObjective(
                    type = QuestObjectiveType.WINS,
                    targetValue = 5,
                    description = "Gana 5 combates"
                ),
                QuestObjective(
                    type = QuestObjectiveType.LOCATIONS,
                    targetValue = 5,
                    description = "Descubre 5 ubicaciones"
                ),
                QuestObjective(
                    type = QuestObjectiveType.LEVEL_REACH,
                    targetValue = 5,
                    description = "Alcanza el nivel 5"
                )
            )
        )
    )

    fun getById(id: String): Quest? = all.firstOrNull { it.id == id }
}