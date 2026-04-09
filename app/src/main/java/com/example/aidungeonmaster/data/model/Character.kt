package com.example.aidungeonmaster.data.model

private val DICE_REGEX = Regex("""\d+d\d+(?:\+\d+)?""", RegexOption.IGNORE_CASE)

fun normalizeEquipSlot(raw: String): String {
    return raw.trim()
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .let { slot ->
            when (slot) {
                "cabeza" -> "head"
                "pecho", "torso" -> "chest"
                "piernas" -> "legs"
                "pies" -> "feet"
                "manos" -> "hands"
                "mano_principal", "mano principal", "arma" -> "main_hand"
                "mano_secundaria", "mano secundaria", "escudo" -> "off_hand"
                "anillo" -> "ring"
                "anillo1" -> "ring"
                "anillo2" -> "ring2"
                "amuleto" -> "amulet"
                else -> slot
            }
        }
}

data class ItemEnchantment(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val statBonuses: Map<String, Int> = emptyMap(),
    val armorBonus: Int = 0,
    val attackBonus: Int = 0,
    val weaponDamageBonus: Int = 0
)

data class EquipmentSetBonus(
    val piecesRequired: Int,
    val statBonuses: Map<String, Int> = emptyMap(),
    val armorBonus: Int = 0,
    val attackBonus: Int = 0,
    val weaponDamageBonus: Int = 0,
    val initiativeBonus: Int = 0
)

data class EquipmentSetDefinition(
    val id: String,
    val name: String,
    val bonuses: List<EquipmentSetBonus>
)

// ─────────────────────────────────────────────────────────────────────────────
// ITEM
// ─────────────────────────────────────────────────────────────────────────────
data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "consumible",
    val effect: String = "",

    val equipSlot: String = "",
    val weaponDamage: String = "",
    val armorBase: Int? = null,
    val armorBonus: Int = 0,
    val maxDexBonus: Int? = null,
    val handedness: String = "one_hand",
    val statBonuses: Map<String, Int> = emptyMap(),

    // ── NUEVO: rareza / encantamientos / sets ─────────────────────────────
    val rarity: String = "common",   // common, uncommon, rare, epic, legendary
    val enchantments: List<ItemEnchantment> = emptyList(),
    val setId: String = "",
    val setName: String = ""
) {
    val isWeapon: Boolean
        get() = type.equals("arma", ignoreCase = true)

    val isArmor: Boolean
        get() = type.equals("armadura", ignoreCase = true)

    val isShield: Boolean
        get() = resolvedEquipSlot == "off_hand" &&
                (isArmor || name.contains("escudo", ignoreCase = true))

    val isEquippable: Boolean
        get() = isWeapon || isArmor || resolvedEquipSlot.isNotBlank()

    val resolvedEquipSlot: String
        get() = when {
            equipSlot.isNotBlank() -> normalizeEquipSlot(equipSlot)
            isWeapon -> "main_hand"
            isArmor && name.contains("escudo", ignoreCase = true) -> "off_hand"
            isArmor -> "chest"
            name.contains("anillo", ignoreCase = true) -> "ring"
            name.contains("amuleto", ignoreCase = true) -> "amulet"
            type.contains("anillo", ignoreCase = true) -> "ring"
            type.contains("amuleto", ignoreCase = true) -> "amulet"
            else -> ""
        }

    val resolvedWeaponDamage: String
        get() = when {
            weaponDamage.isNotBlank() -> weaponDamage
            isWeapon -> extractDiceExpression(effect)
                .ifBlank { extractDiceExpression(description) }
                .ifBlank { "1d4" }
            else -> ""
        }

    val enchantmentStatBonuses: Map<String, Int>
        get() = enchantments
            .flatMap { ench -> ench.statBonuses.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum() }

    val totalStatBonuses: Map<String, Int>
        get() = (statBonuses.keys + enchantmentStatBonuses.keys)
            .associateWith { key ->
                (statBonuses[key] ?: 0) + (enchantmentStatBonuses[key] ?: 0)
            }
            .filterValues { it != 0 }

    val totalArmorBonus: Int
        get() = armorBonus + enchantments.sumOf { it.armorBonus }

    val totalAttackBonus: Int
        get() = enchantments.sumOf { it.attackBonus }

    val totalWeaponDamageBonus: Int
        get() = enchantments.sumOf { it.weaponDamageBonus }

    companion object {
        private fun extractDiceExpression(text: String): String {
            return DICE_REGEX.find(text)?.value ?: ""
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EQUIPO EQUIPADO
// ─────────────────────────────────────────────────────────────────────────────
data class EquippedItems(
    val head: Item? = null,
    val chest: Item? = null,
    val legs: Item? = null,
    val feet: Item? = null,
    val hands: Item? = null,
    val mainHand: Item? = null,
    val offHand: Item? = null,

    val ring: Item? = null,
    val ring2: Item? = null,

    val amulet: Item? = null
) {
    fun allEquipped(): List<Item> = listOfNotNull(
        head, chest, legs, feet, hands, mainHand, offHand, ring, ring2, amulet
    )

    fun itemInSlot(slot: String): Item? = when (normalizeEquipSlot(slot)) {
        "head" -> head
        "chest" -> chest
        "legs" -> legs
        "feet" -> feet
        "hands" -> hands
        "main_hand" -> mainHand
        "off_hand" -> offHand
        "ring" -> ring
        "ring2" -> ring2
        "amulet" -> amulet
        else -> null
    }

    fun withItem(slot: String, item: Item?): EquippedItems = when (normalizeEquipSlot(slot)) {
        "head" -> copy(head = item)
        "chest" -> copy(chest = item)
        "legs" -> copy(legs = item)
        "feet" -> copy(feet = item)
        "hands" -> copy(hands = item)
        "main_hand" -> copy(mainHand = item)
        "off_hand" -> copy(offHand = item)
        "ring" -> copy(ring = item)
        "ring2" -> copy(ring2 = item)
        "amulet" -> copy(amulet = item)
        else -> this
    }
}

private fun normalizeStatKey(raw: String): String {
    return raw.trim()
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
}

private fun canonicalStatName(raw: String): String = when (normalizeStatKey(raw)) {
    "fuerza" -> "Fuerza"
    "destreza" -> "Destreza"
    "constitucion" -> "Constitución"
    "inteligencia" -> "Inteligencia"
    "sabiduria" -> "Sabiduría"
    "carisma" -> "Carisma"
    else -> raw
}

private val BASIC_EQUIPMENT_SETS = listOf(
    EquipmentSetDefinition(
        id = "guardian",
        name = "Set del Guardián",
        bonuses = listOf(
            EquipmentSetBonus(
                piecesRequired = 2,
                armorBonus = 1
            ),
            EquipmentSetBonus(
                piecesRequired = 3,
                armorBonus = 2,
                statBonuses = mapOf("Constitución" to 2)
            )
        )
    ),
    EquipmentSetDefinition(
        id = "berserker",
        name = "Set del Berserker",
        bonuses = listOf(
            EquipmentSetBonus(
                piecesRequired = 2,
                attackBonus = 1
            ),
            EquipmentSetBonus(
                piecesRequired = 3,
                attackBonus = 2,
                weaponDamageBonus = 2,
                statBonuses = mapOf("Fuerza" to 2)
            )
        )
    ),
    EquipmentSetDefinition(
        id = "shadow",
        name = "Set de las Sombras",
        bonuses = listOf(
            EquipmentSetBonus(
                piecesRequired = 2,
                initiativeBonus = 2
            ),
            EquipmentSetBonus(
                piecesRequired = 3,
                initiativeBonus = 3,
                statBonuses = mapOf("Destreza" to 2)
            )
        )
    )
)

data class Character(
    val id: String = "",
    val name: String = "",
    val race: String = "",
    val characterClass: String = "",
    val stats: Map<String, Int> = emptyMap(),
    val physicalTraits: String = "",
    val gameTheme: String? = null,
    val hpMax: Int = 20,
    val hpCurrent: Int = 20,
    val inventory: List<Item> = emptyList(),
    val portraitUrl: String = "",
    val lastPlayed: Long = 0L,
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 0,
    val equipment: EquippedItems = EquippedItems()
) {
    val xpToNextLevel: Int
        get() = level * 100

    val xpProgress: Float
        get() = if (xpToNextLevel > 0) {
            (xp.toFloat() / xpToNextLevel).coerceIn(0f, 1f)
        } else {
            0f
        }

    val profBonus: Int
        get() = when {
            level >= 17 -> 6
            level >= 13 -> 5
            level >= 9 -> 4
            level >= 5 -> 3
            else -> 2
        }

    private fun baseStatValue(name: String): Int {
        val canonical = canonicalStatName(name)
        return stats[canonical] ?: 10
    }

    val activeSetPieceCounts: Map<String, Int>
        get() = equipment.allEquipped()
            .mapNotNull { item ->
                item.setId.takeIf { it.isNotBlank() }?.let { it to item }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, pieces) -> pieces.size }

    val activeSetBonuses: List<EquipmentSetBonus>
        get() = BASIC_EQUIPMENT_SETS.flatMap { definition ->
            val count = activeSetPieceCounts[definition.id] ?: 0
            definition.bonuses.filter { count >= it.piecesRequired }
        }

    val activeSetNames: List<String>
        get() = BASIC_EQUIPMENT_SETS
            .filter { (activeSetPieceCounts[it.id] ?: 0) > 0 }
            .map { set ->
                val pieces = activeSetPieceCounts[set.id] ?: 0
                "${set.name} ($pieces)"
            }

    val equippedStatBonuses: Map<String, Int>
        get() = equipment
            .allEquipped()
            .flatMap { item ->
                item.totalStatBonuses.entries.map { canonicalStatName(it.key) to it.value }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

    val setStatBonuses: Map<String, Int>
        get() = activeSetBonuses
            .flatMap { bonus ->
                bonus.statBonuses.entries.map { canonicalStatName(it.key) to it.value }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

    fun totalStat(name: String): Int {
        val canonical = canonicalStatName(name)
        return baseStatValue(canonical) +
                (equippedStatBonuses[canonical] ?: 0) +
                (setStatBonuses[canonical] ?: 0)
    }

    fun statModifier(name: String): Int = (totalStat(name) - 10) / 2

    val strTotal get() = totalStat("Fuerza")
    val dexTotal get() = totalStat("Destreza")
    val conTotal get() = totalStat("Constitución")
    val intTotal get() = totalStat("Inteligencia")
    val wisTotal get() = totalStat("Sabiduría")
    val chaTotal get() = totalStat("Carisma")

    val strMod get() = statModifier("Fuerza")
    val dexMod get() = statModifier("Destreza")
    val conMod get() = statModifier("Constitución")
    val intMod get() = statModifier("Inteligencia")
    val wisMod get() = statModifier("Sabiduría")
    val chaMod get() = statModifier("Carisma")

    val setAttackBonus: Int
        get() = activeSetBonuses.sumOf { it.attackBonus }

    val setWeaponDamageBonus: Int
        get() = activeSetBonuses.sumOf { it.weaponDamageBonus }

    val setArmorBonus: Int
        get() = activeSetBonuses.sumOf { it.armorBonus }

    val setInitiativeBonus: Int
        get() = activeSetBonuses.sumOf { it.initiativeBonus }

    val meleeAttackBonus: Int
        get() = strMod + profBonus + setAttackBonus + (equippedWeapon?.totalAttackBonus ?: 0)

    val rangedAttackBonus: Int
        get() = dexMod + profBonus + setAttackBonus + (equippedWeapon?.totalAttackBonus ?: 0)

    val weaponDamageBonus: Int
        get() = strMod + setWeaponDamageBonus + (equippedWeapon?.totalWeaponDamageBonus ?: 0)

    val initiativeBonus: Int
        get() = dexMod + setInitiativeBonus

    val finalStats: Map<String, Int>
        get() = linkedMapOf(
            "Fuerza" to strTotal,
            "Destreza" to dexTotal,
            "Constitución" to conTotal,
            "Inteligencia" to intTotal,
            "Sabiduría" to wisTotal,
            "Carisma" to chaTotal
        )

    val armorClass: Int
        get() {
            val chestArmor = equipment.chest

            val extraArmorBonus = equipment
                .allEquipped()
                .filterNot { it == chestArmor && it.armorBase != null }
                .sumOf { item ->
                    when {
                        item.isShield && item.totalArmorBonus == 0 -> 2
                        else -> item.totalArmorBonus
                    }
                } + setArmorBonus

            return if (chestArmor?.armorBase != null) {
                val dexContribution = chestArmor.maxDexBonus?.let { maxDex ->
                    dexMod.coerceAtMost(maxDex)
                } ?: dexMod

                (chestArmor.armorBase + dexContribution + chestArmor.totalArmorBonus + extraArmorBonus)
                    .coerceAtLeast(1)
            } else {
                (10 + dexMod + extraArmorBonus).coerceAtLeast(1)
            }
        }

    val equippedWeapon: Item?
        get() = when {
            equipment.mainHand?.isWeapon == true -> equipment.mainHand
            equipment.offHand?.isWeapon == true -> equipment.offHand
            else -> null
        }
}