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

    val equippedStatBonuses: Map<String, Int>
        get() = equipment
            .allEquipped()
            .flatMap { item ->
                item.statBonuses.entries.map { canonicalStatName(it.key) to it.value }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

    fun totalStat(name: String): Int {
        val canonical = canonicalStatName(name)
        return baseStatValue(canonical) + (equippedStatBonuses[canonical] ?: 0)
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

    val meleeAttackBonus: Int
        get() = strMod + profBonus

    val rangedAttackBonus: Int
        get() = dexMod + profBonus

    val weaponDamageBonus: Int
        get() = strMod

    val initiativeBonus: Int
        get() = dexMod

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
                        item.isShield && item.armorBonus == 0 -> 2
                        else -> item.armorBonus
                    }
                }

            return if (chestArmor?.armorBase != null) {
                val dexContribution = chestArmor.maxDexBonus?.let { maxDex ->
                    dexMod.coerceAtMost(maxDex)
                } ?: dexMod

                (chestArmor.armorBase + dexContribution + chestArmor.armorBonus + extraArmorBonus)
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