package com.example.aidungeonmaster.data.model

private val DICE_REGEX = Regex("""\d+d\d+(?:\+\d+)?""", RegexOption.IGNORE_CASE)

// ─────────────────────────────────────────────────────────────────────────────
// ITEM
// ─────────────────────────────────────────────────────────────────────────────
data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "consumible", // "arma", "armadura", "pocion", "pergamino", etc.
    val effect: String = "",

    // ── NUEVO: CAMPOS DE EQUIPAMIENTO ───────────────────────────────────────
    val equipSlot: String = "",      // "head", "chest", "legs", "feet", "main_hand", "off_hand", "ring", "amulet"
    val weaponDamage: String = "",   // "1d8", "2d6", etc.
    val armorBase: Int? = null,      // Base de CA si es armadura equipada (ej: 11, 14, 16...)
    val armorBonus: Int = 0,         // Bonificación plana (ej: escudo +2)
    val maxDexBonus: Int? = null,    // null = sin límite; 0 = armadura pesada
    val handedness: String = "one_hand", // "one_hand" o "two_hand"
    val statBonuses: Map<String, Int> = emptyMap()
) {
    val isWeapon: Boolean
        get() = type.equals("arma", ignoreCase = true)

    val isArmor: Boolean
        get() = type.equals("armadura", ignoreCase = true)

    val isShield: Boolean
        get() = resolvedEquipSlot == "off_hand" &&
                (isArmor || name.contains("escudo", ignoreCase = true))

    val isEquippable: Boolean
        get() = isWeapon || isArmor || equipSlot.isNotBlank()

    val resolvedEquipSlot: String
        get() = when {
            equipSlot.isNotBlank() -> equipSlot.lowercase()
            isWeapon -> "main_hand"
            isArmor && name.contains("escudo", ignoreCase = true) -> "off_hand"
            isArmor -> "chest"
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
    val amulet: Item? = null
) {
    fun allEquipped(): List<Item> = listOfNotNull(
        head, chest, legs, feet, hands, mainHand, offHand, ring, amulet
    )

    fun itemInSlot(slot: String): Item? = when (slot.lowercase()) {
        "head" -> head
        "chest" -> chest
        "legs" -> legs
        "feet" -> feet
        "hands" -> hands
        "main_hand" -> mainHand
        "off_hand" -> offHand
        "ring" -> ring
        "amulet" -> amulet
        else -> null
    }

    fun withItem(slot: String, item: Item?): EquippedItems = when (slot.lowercase()) {
        "head" -> copy(head = item)
        "chest" -> copy(chest = item)
        "legs" -> copy(legs = item)
        "feet" -> copy(feet = item)
        "hands" -> copy(hands = item)
        "main_hand" -> copy(mainHand = item)
        "off_hand" -> copy(offHand = item)
        "ring" -> copy(ring = item)
        "amulet" -> copy(amulet = item)
        else -> this
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PERSONAJE
// ─────────────────────────────────────────────────────────────────────────────
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
    val coins: Int = 0,

    // --- NUEVO: EQUIPAMIENTO REAL ---
    val equipment: EquippedItems = EquippedItems()
) {
    /** XP necesario para pasar al siguiente nivel */
    val xpToNextLevel: Int
        get() = level * 100

    /** Porcentaje de progreso hacia el siguiente nivel (0.0 – 1.0) */
    val xpProgress: Float
        get() = if (xpToNextLevel > 0) {
            (xp.toFloat() / xpToNextLevel).coerceIn(0f, 1f)
        } else {
            0f
        }

    /** Bonus de competencia según nivel (igual que D&D) */
    val profBonus: Int
        get() = when {
            level >= 17 -> 6
            level >= 13 -> 5
            level >= 9 -> 4
            level >= 5 -> 3
            else -> 2
        }

    private fun statValue(name: String): Int = stats[name] ?: 10

    private val dexMod: Int
        get() = (statValue("Destreza") - 10) / 2

    val equippedStatBonuses: Map<String, Int>
        get() = equipment
            .allEquipped()
            .flatMap { it.statBonuses.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum() }

    /**
     * Clase de armadura real del personaje:
     * - Sin armadura equipada: 10 + mod DEX
     * - Con armadura equipada en chest: armorBase + DEX (limitada por maxDexBonus si aplica)
     * - Escudos / bonus planos: armorBonus
     */
    val armorClass: Int
        get() {
            val chestArmor = equipment.chest

            val shieldBonus = listOfNotNull(equipment.mainHand, equipment.offHand)
                .filter { it.isShield }
                .sumOf { shield ->
                    if (shield.armorBonus != 0) shield.armorBonus else 2
                }

            return if (chestArmor != null && chestArmor.armorBase != null) {
                val dexContribution = chestArmor.maxDexBonus?.let { maxDex ->
                    dexMod.coerceAtMost(maxDex)
                } ?: dexMod

                (chestArmor.armorBase + dexContribution + chestArmor.armorBonus + shieldBonus)
                    .coerceAtLeast(1)
            } else {
                (10 + dexMod + shieldBonus).coerceAtLeast(1)
            }
        }

    val equippedWeapon: Item?
        get() = equipment.mainHand ?: inventory.firstOrNull { it.isWeapon }
}