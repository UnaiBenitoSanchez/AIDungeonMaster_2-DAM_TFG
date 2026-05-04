package com.example.aidungeonmaster.data.model

// Clase que encapsula la lógica de guild boss ability type.
enum class GuildBossAbilityType {
    DAMAGE,
    HEAL,
    BUFF_DEFENSE,
    BUFF_ATTACK,
    SPECIAL_FLEE
}

// Clase que encapsula la lógica de guild boss ability.
data class GuildBossAbility(
    val id: String,
    val name: String,
    val description: String,
    val diceExpression: String,
    val type: GuildBossAbilityType,
    val emoji: String,
    val cooldownTurns: Int = 0
)

// Ejecuta la lógica de guild boss abilities for class.
fun guildBossAbilitiesForClass(charClass: String): List<GuildBossAbility> =
    when (charClass.lowercase().trim()) {
        "guerrero", "fighter", "luchador" -> listOf(
            GuildBossAbility("second_wind", "Segunda Oportunidad", "Recupera fuerzas en batalla", "1d10+3", GuildBossAbilityType.HEAL, "💚", cooldownTurns = 3),
            GuildBossAbility("power_attack", "Ataque Poderoso", "Golpe devastador", "2d6+3", GuildBossAbilityType.DAMAGE, "⚔️"),
            GuildBossAbility("shield_bash", "Golpe de Escudo", "Impacto pesado", "1d6+2", GuildBossAbilityType.DAMAGE, "🛡️"),
            GuildBossAbility("battle_cry", "Grito de Batalla", "Ventaja en el siguiente ataque", "adv", GuildBossAbilityType.BUFF_ATTACK, "📢", cooldownTurns = 2),
        )

        "bárbaro", "barbaro", "berserker" -> listOf(
            GuildBossAbility("rage_strike", "Furia Berserker", "Golpe cargado de rabia", "2d12", GuildBossAbilityType.DAMAGE, "🔥", cooldownTurns = 2),
            GuildBossAbility("reckless", "Ataque Imprudente", "Ventaja en el siguiente ataque", "adv", GuildBossAbilityType.BUFF_ATTACK, "💪"),
            GuildBossAbility("brutal_hit", "Golpe Brutal", "Daño masivo", "3d6+4", GuildBossAbilityType.DAMAGE, "🪓", cooldownTurns = 2),
            GuildBossAbility("endurance", "Resistencia", "+4 CA hasta tu próximo turno", "4", GuildBossAbilityType.BUFF_DEFENSE, "🦺", cooldownTurns = 3),
        )

        "mago", "wizard", "hechicero", "sorcerer" -> listOf(
            GuildBossAbility("magic_missile", "Proyectil Mágico", "Siempre impacta", "3d4+3", GuildBossAbilityType.DAMAGE, "✨"),
            GuildBossAbility("fireball", "Bola de Fuego", "Explosión devastadora", "8d6", GuildBossAbilityType.DAMAGE, "🔥", cooldownTurns = 3),
            GuildBossAbility("shield_spell", "Escudo Arcano", "+5 CA hasta tu próximo turno", "5", GuildBossAbilityType.BUFF_DEFENSE, "🔮", cooldownTurns = 2),
            GuildBossAbility("ray_frost", "Rayo de Escarcha", "Daño de frío", "1d8", GuildBossAbilityType.DAMAGE, "❄️"),
        )

        "pícaro", "picaro", "rogue", "asesino" -> listOf(
            GuildBossAbility("sneak_attack", "Ataque Furtivo", "Daño extra", "2d6", GuildBossAbilityType.DAMAGE, "🗡️"),
            GuildBossAbility("poison_blade", "Hoja Envenenada", "Golpe venenoso", "1d4+2", GuildBossAbilityType.DAMAGE, "☠️"),
            GuildBossAbility("smoke_bomb", "Bomba de Humo", "Ventaja en el siguiente ataque", "adv", GuildBossAbilityType.BUFF_ATTACK, "💨", cooldownTurns = 2),
            GuildBossAbility("flee", "Huir", "No disponible contra el jefe final", "flee", GuildBossAbilityType.SPECIAL_FLEE, "🏃"),
        )

        "clérigo", "clerigo", "cleric", "sacerdote" -> listOf(
            GuildBossAbility("sacred_flame", "Llama Sagrada", "Fuego divino radiante", "1d8", GuildBossAbilityType.DAMAGE, "✝️"),
            GuildBossAbility("heal_word", "Palabra Sanadora", "Cura a distancia", "1d8+3", GuildBossAbilityType.HEAL, "💊", cooldownTurns = 2),
            GuildBossAbility("divine_smite", "Golpe Divino", "Daño sagrado", "2d8+3", GuildBossAbilityType.DAMAGE, "⚡", cooldownTurns = 2),
            GuildBossAbility("lay_on_hands", "Imposición de Manos", "Curación directa", "2d6+4", GuildBossAbilityType.HEAL, "🙏", cooldownTurns = 3),
        )

        "paladín", "paladin" -> listOf(
            GuildBossAbility("divine_smite", "Golpe Divino", "Energía sagrada", "2d8+3", GuildBossAbilityType.DAMAGE, "⚡", cooldownTurns = 2),
            GuildBossAbility("lay_on_hands", "Imposición de Manos", "Curación directa", "2d6+4", GuildBossAbilityType.HEAL, "🙏", cooldownTurns = 3),
            GuildBossAbility("aura_prot", "Aura de Protección", "+3 CA hasta tu próximo turno", "3", GuildBossAbilityType.BUFF_DEFENSE, "🛡️", cooldownTurns = 2),
            GuildBossAbility("smite_evil", "Venganza del Cielo", "Golpe masivo de luz", "3d8", GuildBossAbilityType.DAMAGE, "🌟", cooldownTurns = 3),
        )

        "druida", "druid" -> listOf(
            GuildBossAbility("heal_spores", "Esporas Sanadoras", "Curación natural", "1d8+2", GuildBossAbilityType.HEAL, "🌿", cooldownTurns = 2),
            GuildBossAbility("entangle", "Enredar", "Daño natural", "1d4", GuildBossAbilityType.DAMAGE, "🌱"),
            GuildBossAbility("call_lightning", "Llamar Relámpago", "Rayo desde las nubes", "3d10", GuildBossAbilityType.DAMAGE, "⚡", cooldownTurns = 3),
            GuildBossAbility("shillelagh", "Macana Arcana", "Golpe imbuido de magia", "1d8+3", GuildBossAbilityType.DAMAGE, "🪄"),
        )

        "bardo", "bard" -> listOf(
            GuildBossAbility("vicious_mock", "Insulto Hiriente", "Daño psíquico", "2d6", GuildBossAbilityType.DAMAGE, "🎭"),
            GuildBossAbility("inspire", "Inspiración Barda", "Ventaja en el siguiente ataque", "adv", GuildBossAbilityType.BUFF_ATTACK, "🎵", cooldownTurns = 2),
            GuildBossAbility("healing_word", "Himno de Curación", "Canción restauradora", "1d6+3", GuildBossAbilityType.HEAL, "🎶", cooldownTurns = 2),
            GuildBossAbility("dissonant", "Susurros Disonantes", "Terror psíquico", "3d6", GuildBossAbilityType.DAMAGE, "😱", cooldownTurns = 2),
        )

        "monje", "monk" -> listOf(
            GuildBossAbility("flurry", "Tormenta de Golpes", "Ataques veloces", "4d4", GuildBossAbilityType.DAMAGE, "👊"),
            GuildBossAbility("ki_strike", "Golpe Ki", "Energía interior concentrada", "2d6+2", GuildBossAbilityType.DAMAGE, "⚡"),
            GuildBossAbility("patient_def", "Defensa Paciente", "+3 CA hasta tu próximo turno", "3", GuildBossAbilityType.BUFF_DEFENSE, "🧘", cooldownTurns = 2),
            GuildBossAbility("step_wind", "Paso del Viento", "Ventaja en el siguiente ataque", "adv", GuildBossAbilityType.BUFF_ATTACK, "🌬️"),
        )

        "corsario" -> listOf(
            GuildBossAbility("pistol_shot", "Disparo de Pistola", "Proyectil a quemarropa", "2d6", GuildBossAbilityType.DAMAGE, "🔫"),
            GuildBossAbility("boarding_axe", "Hacha de Abordaje", "Golpe brutal de pirata", "1d8+2", GuildBossAbilityType.DAMAGE, "⚓"),
            GuildBossAbility("sea_roll", "Tiro de Mar", "Ventaja en el siguiente ataque", "adv", GuildBossAbilityType.BUFF_ATTACK, "🌊", cooldownTurns = 2),
            GuildBossAbility("rum_flask", "Trago de Ron", "Cura rápida", "1d6+2", GuildBossAbilityType.HEAL, "🍺", cooldownTurns = 3),
        )

        "caballero de la muerte", "caballero_muerte", "death knight" -> listOf(
            GuildBossAbility("death_strike", "Golpe Mortal", "Ataque imbuido de muerte", "2d8+4", GuildBossAbilityType.DAMAGE, "💀", cooldownTurns = 2),
            GuildBossAbility("unholy_smite", "Golpe Profano", "Energía oscura", "3d6", GuildBossAbilityType.DAMAGE, "🖤", cooldownTurns = 2),
            GuildBossAbility("dark_shield", "Escudo Sombrío", "+4 CA hasta tu próximo turno", "4", GuildBossAbilityType.BUFF_DEFENSE, "🛡️", cooldownTurns = 2),
            GuildBossAbility("soul_drain", "Drenar Alma", "Recupera vida", "1d8+3", GuildBossAbilityType.HEAL, "💜", cooldownTurns = 3),
        )

        "exorcista" -> listOf(
            GuildBossAbility("holy_burst", "Explosión Sagrada", "Luz que quema lo impuro", "2d6+2", GuildBossAbilityType.DAMAGE, "✨"),
            GuildBossAbility("banish", "Destierro", "Expulsa entidades oscuras", "3d8", GuildBossAbilityType.DAMAGE, "🔮", cooldownTurns = 3),
            GuildBossAbility("seal_ward", "Sello Protector", "+3 CA hasta tu próximo turno", "3", GuildBossAbilityType.BUFF_DEFENSE, "📿", cooldownTurns = 2),
            GuildBossAbility("purify", "Purificación", "Sana heridas", "2d6+3", GuildBossAbilityType.HEAL, "🕊️", cooldownTurns = 2),
        )

        "chamán", "chaman", "shaman" -> listOf(
            GuildBossAbility("spirit_strike", "Golpe Espiritual", "Los ancestros guían el golpe", "1d10+2", GuildBossAbilityType.DAMAGE, "👻"),
            GuildBossAbility("storm_call", "Llamada a la Tormenta", "Truenos del mundo espiritual", "3d6", GuildBossAbilityType.DAMAGE, "⛈️", cooldownTurns = 3),
            GuildBossAbility("spirit_heal", "Sanación Espiritual", "Los espíritus restauran vida", "2d6+4", GuildBossAbilityType.HEAL, "🌀", cooldownTurns = 2),
            GuildBossAbility("totem_guard", "Tótem Guardián", "+3 CA hasta tu próximo turno", "3", GuildBossAbilityType.BUFF_DEFENSE, "🗿", cooldownTurns = 2),
        )

        else -> listOf(
            GuildBossAbility("basic_att", "Golpe Básico", "Ataque cuerpo a cuerpo", "1d6", GuildBossAbilityType.DAMAGE, "⚔️"),
            GuildBossAbility("quick_def", "Postura Defensiva", "+2 CA hasta tu próximo turno", "2", GuildBossAbilityType.BUFF_DEFENSE, "🛡️", cooldownTurns = 2),
            GuildBossAbility("flee", "Huir", "No disponible contra el jefe final", "flee", GuildBossAbilityType.SPECIAL_FLEE, "🏃"),
        )
    }

// Comprueba si boss battle consumable.
fun isBossBattleConsumable(item: Item): Boolean {
    val effect = item.effect.lowercase().trim()

    val looksHealingItem =
        item.type.contains("pocion", ignoreCase = true) ||
                item.type.contains("consum", ignoreCase = true) ||
                item.name.contains("poción", ignoreCase = true) ||
                item.name.contains("pocion", ignoreCase = true) ||
                item.name.contains("elixir", ignoreCase = true) ||
                item.description.contains("cura", ignoreCase = true) ||
                item.description.contains("restaura", ignoreCase = true) ||
                item.description.contains("regenera", ignoreCase = true)

    return effect.startsWith("cura:") ||
            effect.startsWith("daño:") ||
            effect.startsWith("dano:") ||
            effect.startsWith("veneno:") ||
            effect.startsWith("explosivo:") ||
            looksHealingItem
}
