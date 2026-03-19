package com.example.aidungeonmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Item
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

// ============================================================
//  ENUMS Y DATA CLASSES
// ============================================================

// Contador global — IDs únicos para entradas del log de combate.
// NUNCA usar System.currentTimeMillis() como key en LazyColumn:
// si dos mensajes llegan en el mismo ms el crash es inmediato.
private val logIdCounter = AtomicLong(0L)

enum class CombatPhase {
    INTRO,          // Animación de entrada
    PLAYER_TURN,    // El jugador elige acción
    ROLLING,        // Animación de dado activa
    ENEMY_TURN,     // El enemigo actúa
    VICTORY,        // Jugador ganó
    DEFEAT          // Jugador perdió / huyó
}

enum class LogType {
    SYSTEM,
    PLAYER_HIT, PLAYER_MISS, PLAYER_CRIT,
    ENEMY_HIT, ENEMY_MISS,
    HEAL, SPECIAL
}

data class CombatLogEntry(
    val text: String,
    val type: LogType,
    val id: Long = logIdCounter.incrementAndGet()
)

data class DiceAnimState(
    val diceLabel: String = "",       // "1d20", "2d6+3"
    val actionLabel: String = "",     // "Ataque con Espada", "Bola de Fuego"
    val rolls: List<Int> = emptyList(),
    val total: Int = 0,
    val isVisible: Boolean = false,
    val isCrit: Boolean = false,
    val isFumble: Boolean = false,
    val isHeal: Boolean = false
)

enum class AbilityType { DAMAGE, HEAL, BUFF_DEFENSE, BUFF_ATTACK, SPECIAL_FLEE }

data class ClassAbility(
    val id: String,
    val name: String,
    val description: String,
    val diceExpression: String,       // "2d6+3", "heal:1d8+2", "flee", "adv:5"
    val type: AbilityType,
    val emoji: String,
    val cooldownTurns: Int = 0
)

// ============================================================
//  VIEWMODEL PRINCIPAL
// ============================================================

class CombatViewModel(
    val enemy: Enemy,
    val playerCharacter: Character,
    private val onHpUpdate: (Int) -> Unit
) : ViewModel() {

    // --- Estados públicos ---
    private val _enemyHp      = MutableStateFlow(enemy.hpCurrent)
    val enemyHp               = _enemyHp.asStateFlow()

    private val _playerHp     = MutableStateFlow(playerCharacter.hpCurrent)
    val playerHp              = _playerHp.asStateFlow()

    private val _phase        = MutableStateFlow(CombatPhase.INTRO)
    val phase                 = _phase.asStateFlow()

    private val _log          = MutableStateFlow<List<CombatLogEntry>>(emptyList())
    val log                   = _log.asStateFlow()

    private val _dice         = MutableStateFlow(DiceAnimState())
    val dice                  = _dice.asStateFlow()

    private val _cooldowns    = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cooldowns             = _cooldowns.asStateFlow()

    // --- Buffs temporales ---
    private var defenseBonus   = 0
    private var hasAdvantage   = false

    // --- Modificadores de stats ---
    private fun statMod(stat: String): Int {
        val v = playerCharacter.stats[stat] ?: 10
        return (v - 10) / 2
    }

    private val strMod   get() = statMod("Fuerza")
    private val dexMod   get() = statMod("Destreza")
    private val intMod   get() = statMod("Inteligencia")
    private val wisMod   get() = statMod("Sabiduría")
    private val profBonus = 2   // niveles 1-4

    // CA calculada del enemigo (12 base + bonus por HP máx)
    private val enemyAC: Int = (10 + (enemy.hpMax / 12).coerceIn(0, 5))
    // CA del jugador
    private val playerAC: Int get() = (10 + dexMod + defenseBonus).coerceIn(8, 22)

    // Habilidades basadas en clase
    val classAbilities: List<ClassAbility> = abilitiesForClass(playerCharacter.characterClass)

    // Armas del inventario (items de tipo arma o con dados de daño en el efecto)
    val weapons: List<Item> = playerCharacter.inventory.filter { item ->
        item.type.contains("arma", ignoreCase = true) ||
                item.effect.contains("d", ignoreCase = true) ||
                item.description.uppercase().contains("\\dD\\d".toRegex())
    }.ifEmpty {
        // Si no hay armas, dar un puñetazo básico
        listOf(Item(id = "fist", name = "Puñetazo", description = "Ataque desarmado", type = "arma", effect = "1d4"))
    }

    // ============================================================
    //  INICIO DEL COMBATE
    // ============================================================
    init {
        viewModelScope.launch {
            delay(400)
            log("⚔️ ¡${enemy.name} aparece ante ti!", LogType.SYSTEM)
            log("📊 CA enemiga: $enemyAC | HP: ${enemy.hpMax}", LogType.SYSTEM)
            delay(600)
            log("🎲 Tu turno — elige tu acción", LogType.SYSTEM)
            _phase.value = CombatPhase.PLAYER_TURN
        }
    }

    // ============================================================
    //  ACCIONES DEL JUGADOR
    // ============================================================

    fun attackWithWeapon(weapon: Item) {
        if (_phase.value != CombatPhase.PLAYER_TURN) return
        _phase.value = CombatPhase.ROLLING

        viewModelScope.launch {
            // 1. Tirada de ataque d20
            val rollA1 = roll(20)
            val rollA2 = if (hasAdvantage) roll(20) else rollA1
            val attackRoll = if (hasAdvantage) maxOf(rollA1, rollA2) else rollA1
            hasAdvantage = false

            val isCrit   = attackRoll == 20
            val isFumble = attackRoll == 1
            val totalAtk = attackRoll + strMod + profBonus

            showDice(
                diceLabel   = if (hasAdvantage) "2d20 ventaja" else "1d20",
                actionLabel = "Ataque con ${weapon.name}",
                rolls       = listOf(attackRoll),
                total       = totalAtk,
                isCrit      = isCrit,
                isFumble    = isFumble
            )
            delay(2000)
            hideDice()

            when {
                isFumble -> {
                    log("💀 ¡Fallo crítico con ${weapon.name}! Tu arma se escapa de tu mano.", LogType.PLAYER_MISS)
                    endPlayerTurn()
                }
                isCrit || totalAtk >= enemyAC -> {
                    // 2. Tirada de daño
                    val diceExpr = weapon.effect.ifBlank { weapon.description }
                    val (cnt, sides, bonus) = parseDice(diceExpr)
                    val count       = if (isCrit) cnt * 2 else cnt
                    val damageRolls = List(count) { roll(sides) }
                    val rawDamage   = damageRolls.sum() + bonus + strMod
                    val damage      = rawDamage.coerceAtLeast(1)

                    showDice(
                        diceLabel   = "${count}d${sides}${if (bonus > 0) "+$bonus" else ""}",
                        actionLabel = if (isCrit) "⚡ GOLPE CRÍTICO — Daño" else "Daño",
                        rolls       = damageRolls,
                        total       = damage,
                        isCrit      = isCrit
                    )
                    delay(2000)
                    hideDice()

                    log(
                        "${if (isCrit) "⚡" else "🗡️"} ${weapon.name} golpea a ${enemy.name} " +
                                "por $damage de daño${if (isCrit) " (¡CRÍTICO!)" else ""}!",
                        if (isCrit) LogType.PLAYER_CRIT else LogType.PLAYER_HIT
                    )

                    val newEnemyHp = (_enemyHp.value - damage).coerceAtLeast(0)
                    _enemyHp.value = newEnemyHp

                    if (newEnemyHp <= 0) {
                        log("💀 ¡${enemy.name} ha sido derrotado!", LogType.SYSTEM)
                        delay(600)
                        _phase.value = CombatPhase.VICTORY
                        return@launch
                    }
                    endPlayerTurn()
                }
                else -> {
                    log(
                        "🛡️ Fallas el ataque contra ${enemy.name} " +
                                "(necesitabas $enemyAC, obtuviste $totalAtk).",
                        LogType.PLAYER_MISS
                    )
                    endPlayerTurn()
                }
            }
        }
    }

    fun useAbility(ability: ClassAbility) {
        if (_phase.value != CombatPhase.PLAYER_TURN) return
        val cd = _cooldowns.value[ability.id] ?: 0
        if (cd > 0) return
        _phase.value = CombatPhase.ROLLING

        viewModelScope.launch {
            when (ability.type) {
                AbilityType.DAMAGE -> {
                    val isCrit = roll(20) == 20 // Chance de crítico en habilidades mágicas
                    val (cnt, sides, bonus) = parseDice(ability.diceExpression)
                    val count  = if (isCrit) cnt * 2 else cnt
                    val rolls  = List(count) { roll(sides) }
                    val damage = (rolls.sum() + bonus).coerceAtLeast(1)

                    showDice(
                        diceLabel   = "${count}d${sides}${if (bonus > 0) "+$bonus" else ""}",
                        actionLabel = ability.name,
                        rolls       = rolls,
                        total       = damage,
                        isCrit      = isCrit
                    )
                    delay(2000)
                    hideDice()

                    log("${ability.emoji} ${ability.name}: $damage de daño a ${enemy.name}${if (isCrit) " (¡CRÍTICO!)" else ""}!", LogType.SPECIAL)

                    val newEnemyHp = (_enemyHp.value - damage).coerceAtLeast(0)
                    _enemyHp.value = newEnemyHp

                    if (newEnemyHp <= 0) {
                        log("💀 ¡${enemy.name} ha sido derrotado!", LogType.SYSTEM)
                        delay(600)
                        _phase.value = CombatPhase.VICTORY
                        if (ability.cooldownTurns > 0) addCooldown(ability)
                        return@launch
                    }
                }

                AbilityType.HEAL -> {
                    val (cnt, sides, bonus) = parseDice(ability.diceExpression)
                    val rolls = List(cnt) { roll(sides) }
                    val heal  = (rolls.sum() + bonus).coerceAtLeast(1)

                    showDice(
                        diceLabel   = "${cnt}d${sides}${if (bonus > 0) "+$bonus" else ""}",
                        actionLabel = "${ability.name} — Curación",
                        rolls       = rolls,
                        total       = heal,
                        isHeal      = true
                    )
                    delay(2000)
                    hideDice()

                    val newHp = (_playerHp.value + heal).coerceAtMost(playerCharacter.hpMax)
                    _playerHp.value = newHp
                    onHpUpdate(newHp)
                    log("${ability.emoji} ${ability.name}: recuperas $heal HP (${newHp}/${playerCharacter.hpMax}).", LogType.HEAL)
                }

                AbilityType.BUFF_DEFENSE -> {
                    val bonus = ability.diceExpression.toIntOrNull() ?: 2
                    defenseBonus = bonus
                    showDice("—", ability.name, emptyList(), bonus)
                    delay(1400)
                    hideDice()
                    log("${ability.emoji} ${ability.name}: ¡+$bonus a tu CA este turno! (CA actual: ${playerAC})", LogType.SPECIAL)
                }

                AbilityType.BUFF_ATTACK -> {
                    hasAdvantage = true
                    showDice("—", ability.name, emptyList(), 0)
                    delay(1400)
                    hideDice()
                    log("${ability.emoji} ${ability.name}: ¡Ventaja en tu siguiente ataque!", LogType.SPECIAL)
                }

                AbilityType.SPECIAL_FLEE -> {
                    val r = roll(20)
                    val total = r + dexMod
                    showDice("1d20", "Huida (CD 12)", listOf(r), total)
                    delay(2000)
                    hideDice()

                    if (total >= 12) {
                        log("💨 ¡Logras huir del combate! ($total ≥ 12)", LogType.SYSTEM)
                        delay(500)
                        _phase.value = CombatPhase.DEFEAT   // huida = fin de combate
                    } else {
                        log("🚫 Fallas la huida ($total < 12). ¡${enemy.name} te corta el paso!", LogType.PLAYER_MISS)
                        if (ability.cooldownTurns > 0) addCooldown(ability)
                        endPlayerTurn()
                        return@launch
                    }

                    if (ability.cooldownTurns > 0) addCooldown(ability)
                    return@launch
                }
            }

            if (ability.cooldownTurns > 0) addCooldown(ability)
            if (_phase.value == CombatPhase.ROLLING) endPlayerTurn()
        }
    }

    // ============================================================
    //  TURNO DEL ENEMIGO
    // ============================================================

    private fun endPlayerTurn() {
        viewModelScope.launch {
            delay(600)
            if (_phase.value == CombatPhase.VICTORY || _phase.value == CombatPhase.DEFEAT) return@launch
            _phase.value = CombatPhase.ENEMY_TURN
            log("🔴 Turno de ${enemy.name}...", LogType.SYSTEM)
            delay(900)
            enemyAttack()
        }
    }

    private fun enemyAttack() {
        viewModelScope.launch {
            val attackRoll = roll(20)
            val isCrit     = attackRoll == 20
            val isFumble   = attackRoll == 1
            val totalAtk   = attackRoll + 3  // modificador simple de enemigo

            showDice(
                diceLabel   = "1d20",
                actionLabel = "${enemy.name} ataca",
                rolls       = listOf(attackRoll),
                total       = totalAtk,
                isCrit      = isCrit,
                isFumble    = isFumble
            )
            delay(2000)
            hideDice()

            when {
                isFumble -> {
                    log("💫 ¡${enemy.name} falla estrepitosamente!", LogType.ENEMY_MISS)
                }
                isCrit || totalAtk >= playerAC -> {
                    val (cnt, sides, bonus) = parseDice(enemy.attackDamage)
                    val count  = if (isCrit) cnt * 2 else cnt
                    val rolls  = List(count) { roll(sides) }
                    val damage = (rolls.sum() + bonus).coerceAtLeast(1)

                    showDice(
                        diceLabel   = "${count}d${sides}${if (bonus > 0) "+$bonus" else ""}",
                        actionLabel = "${enemy.name}${if (isCrit) " — CRÍTICO" else ""} — Daño",
                        rolls       = rolls,
                        total       = damage,
                        isCrit      = isCrit
                    )
                    delay(2000)
                    hideDice()

                    val newHp = (_playerHp.value - damage).coerceAtLeast(0)
                    _playerHp.value = newHp
                    onHpUpdate(newHp)

                    log(
                        "${if (isCrit) "💥" else "🔴"} ${enemy.name} te golpea por $damage de daño" +
                                "${if (isCrit) " (¡CRÍTICO!)" else ""}! (HP: $newHp/${playerCharacter.hpMax})",
                        if (isCrit) LogType.ENEMY_HIT else LogType.ENEMY_HIT
                    )

                    if (newHp <= 0) {
                        log("💀 Has caído en combate...", LogType.SYSTEM)
                        delay(600)
                        _phase.value = CombatPhase.DEFEAT
                        return@launch
                    }
                }
                else -> {
                    log("🛡️ Bloqueas el ataque de ${enemy.name} (CA ${playerAC}, tirada: $totalAtk).", LogType.ENEMY_MISS)
                }
            }

            // Reducir cooldowns al final del turno
            defenseBonus = 0
            _cooldowns.value = _cooldowns.value
                .mapValues { (_, v) -> v - 1 }
                .filter { (_, v) -> v > 0 }

            if (_phase.value == CombatPhase.ENEMY_TURN) {
                delay(500)
                log("🎲 Tu turno — elige tu acción", LogType.SYSTEM)
                _phase.value = CombatPhase.PLAYER_TURN
            }
        }
    }

    // ============================================================
    //  UTILIDADES PRIVADAS
    // ============================================================

    private fun roll(sides: Int): Int = (1..sides).random()

    /** Parsea "2d6+3", "1D8", "D4" → Triple(count, sides, bonus) */
    private fun parseDice(expr: String): Triple<Int, Int, Int> {
        val clean = expr.uppercase().trim()
        val rx    = """(\d*)D(\d+)(?:\+(\d+))?""".toRegex()
        val m     = rx.find(clean) ?: return Triple(1, 6, 0)
        val cnt   = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
        val sides = m.groupValues[2].toIntOrNull() ?: 6
        val bonus = m.groupValues[3].toIntOrNull() ?: 0
        return Triple(cnt, sides, bonus)
    }

    private fun log(text: String, type: LogType) {
        _log.value = _log.value + CombatLogEntry(text, type)
    }

    private suspend fun showDice(
        diceLabel: String,
        actionLabel: String,
        rolls: List<Int>,
        total: Int,
        isCrit: Boolean = false,
        isFumble: Boolean = false,
        isHeal: Boolean = false
    ) {
        _dice.value = DiceAnimState(diceLabel, actionLabel, rolls, total, true, isCrit, isFumble, isHeal)
    }

    private fun hideDice() { _dice.value = DiceAnimState() }

    private fun addCooldown(ability: ClassAbility) {
        _cooldowns.value = _cooldowns.value + (ability.id to ability.cooldownTurns)
    }

    // ============================================================
    //  HABILIDADES POR CLASE
    // ============================================================

    private fun abilitiesForClass(charClass: String): List<ClassAbility> =
        when (charClass.lowercase().trim()) {
            "guerrero", "fighter", "luchador" -> listOf(
                ClassAbility("second_wind",   "Segunda Oportunidad",  "Recupera fuerzas en batalla",   "1d10+3",  AbilityType.HEAL,        "💚", cooldownTurns = 3),
                ClassAbility("power_attack",  "Ataque Poderoso",      "Golpe devastador con desventaja","2d6+3",   AbilityType.DAMAGE,      "⚔️"),
                ClassAbility("shield_bash",   "Golpe de Escudo",      "Impacto y +2 CA este turno",    "1d6+2",   AbilityType.DAMAGE,      "🛡️"),
                ClassAbility("battle_cry",    "Grito de Batalla",     "Ventaja en el siguiente ataque", "adv",     AbilityType.BUFF_ATTACK, "📢", cooldownTurns = 2),
            )
            "bárbaro", "barbaro", "berserker" -> listOf(
                ClassAbility("rage_strike",   "Furia Berserker",      "Golpe cargado de rabia",        "2d12",    AbilityType.DAMAGE,      "🔥", cooldownTurns = 2),
                ClassAbility("reckless",      "Ataque Imprudente",    "Ventaja pero enemigo también",  "adv",     AbilityType.BUFF_ATTACK, "💪"),
                ClassAbility("brutal_hit",    "Golpe Brutal",         "Daño masivo a costa de defensa","3d6+4",   AbilityType.DAMAGE,      "🪓", cooldownTurns = 2),
                ClassAbility("endurance",     "Resistencia",          "+4 CA un turno",                "4",       AbilityType.BUFF_DEFENSE,"🦺", cooldownTurns = 3),
            )
            "mago", "wizard", "hechicero", "sorcerer" -> listOf(
                ClassAbility("magic_missile", "Proyectil Mágico",     "3 dardos de energía (siempre impactan)", "3d4+3", AbilityType.DAMAGE, "✨"),
                ClassAbility("fireball",      "Bola de Fuego",        "Explosión devastadora",         "8d6",     AbilityType.DAMAGE,      "🔥", cooldownTurns = 3),
                ClassAbility("shield_spell",  "Escudo Arcano",        "+5 CA hasta tu próximo turno",  "5",       AbilityType.BUFF_DEFENSE,"🔮", cooldownTurns = 2),
                ClassAbility("ray_frost",     "Rayo de Escarcha",     "Ralentiza y daña",              "1d8",     AbilityType.DAMAGE,      "❄️"),
            )
            "pícaro", "picaro", "rogue", "asesino" -> listOf(
                ClassAbility("sneak_attack",  "Ataque Furtivo",       "Daño extra flanqueando",        "2d6",     AbilityType.DAMAGE,      "🗡️"),
                ClassAbility("poison_blade",  "Hoja Envenenada",      "Veneno de acción lenta",        "1d4+2",   AbilityType.DAMAGE,      "☠️"),
                ClassAbility("smoke_bomb",    "Bomba de Humo",        "Desaparece, gana ventaja",      "adv",     AbilityType.BUFF_ATTACK, "💨", cooldownTurns = 2),
                ClassAbility("flee",          "Huir",                 "Intenta escapar del combate",   "flee",    AbilityType.SPECIAL_FLEE,"🏃"),
            )
            "clérigo", "clerigo", "cleric", "sacerdote" -> listOf(
                ClassAbility("sacred_flame",  "Llama Sagrada",        "Fuego divino radiante",         "1d8",     AbilityType.DAMAGE,      "✝️"),
                ClassAbility("heal_word",     "Palabra Sanadora",     "Cura a distancia",              "1d8+3",   AbilityType.HEAL,        "💊", cooldownTurns = 2),
                ClassAbility("divine_smite",  "Golpe Divino",         "Daño sagrado concentrado",      "2d8+3",   AbilityType.DAMAGE,      "⚡", cooldownTurns = 2),
                ClassAbility("lay_on_hands",  "Imposición de Manos",  "Cura directa por contacto",     "2d6+4",   AbilityType.HEAL,        "🙏", cooldownTurns = 3),
            )
            "paladín", "paladin" -> listOf(
                ClassAbility("divine_smite",  "Golpe Divino",         "Energía sagrada en el arma",    "2d8+3",   AbilityType.DAMAGE,      "⚡", cooldownTurns = 2),
                ClassAbility("lay_on_hands",  "Imposición de Manos",  "Cura directa por fe",           "2d6+4",   AbilityType.HEAL,        "🙏", cooldownTurns = 3),
                ClassAbility("aura_prot",     "Aura de Protección",   "+3 CA este turno",              "3",       AbilityType.BUFF_DEFENSE,"🛡️", cooldownTurns = 2),
                ClassAbility("smite_evil",    "Venganza del Cielo",   "Golpe masivo de luz",           "3d8",     AbilityType.DAMAGE,      "🌟", cooldownTurns = 3),
            )
            "druida", "druid" -> listOf(
                ClassAbility("heal_spores",   "Esporas Sanadoras",    "Curación natural",              "1d8+2",   AbilityType.HEAL,        "🌿", cooldownTurns = 2),
                ClassAbility("entangle",      "Enredar",              "Ralentiza al enemigo",          "1d4",     AbilityType.DAMAGE,      "🌱"),
                ClassAbility("call_lightning","Llamar Relámpago",     "Rayo desde las nubes",          "3d10",    AbilityType.DAMAGE,      "⚡", cooldownTurns = 3),
                ClassAbility("shillelagh",    "Macana Arcana",        "Golpe imbuido de magia",        "1d8+3",   AbilityType.DAMAGE,      "🪄"),
            )
            "bardo", "bard" -> listOf(
                ClassAbility("vicious_mock",  "Insulto Hiriente",     "Daño psíquico por humillación", "2d6",     AbilityType.DAMAGE,      "🎭"),
                ClassAbility("inspire",       "Inspiración Barda",    "Ventaja en el siguiente ataque","adv",     AbilityType.BUFF_ATTACK, "🎵", cooldownTurns = 2),
                ClassAbility("healing_word",  "Himno de Curación",    "Canción restauradora",          "1d6+3",   AbilityType.HEAL,        "🎶", cooldownTurns = 2),
                ClassAbility("dissonant",     "Susurros Disonantes",  "Terror psíquico",               "3d6",     AbilityType.DAMAGE,      "😱", cooldownTurns = 2),
            )
            "monje", "monk" -> listOf(
                ClassAbility("flurry",        "Tormenta de Golpes",   "4 ataques veloces",             "4d4",     AbilityType.DAMAGE,      "👊"),
                ClassAbility("ki_strike",     "Golpe Ki",             "Energía interior concentrada",  "2d6+2",   AbilityType.DAMAGE,      "⚡"),
                ClassAbility("patient_def",   "Defensa Paciente",     "+3 CA este turno",              "3",       AbilityType.BUFF_DEFENSE,"🧘", cooldownTurns = 2),
                ClassAbility("step_wind",     "Paso del Viento",      "Esquivar y contraatacar",       "adv",     AbilityType.BUFF_ATTACK, "🌬️"),
            )
            // ── Clases personalizadas ──────────────────────────────────────────
            "corsario" -> listOf(
                ClassAbility("pistol_shot",   "Disparo de Pistola",   "Proyectil a quemarropa",        "2d6",     AbilityType.DAMAGE,      "🔫"),
                ClassAbility("boarding_axe",  "Hacha de Abordaje",    "Golpe brutal de pirata",        "1d8+2",   AbilityType.DAMAGE,      "⚓"),
                ClassAbility("sea_roll",       "Tiro de Mar",          "Ventaja con el viento",         "adv",     AbilityType.BUFF_ATTACK, "🌊", cooldownTurns = 2),
                ClassAbility("rum_flask",      "Trago de Ron",         "Cura rápida de combate",        "1d6+2",   AbilityType.HEAL,        "🍺", cooldownTurns = 3),
            )
            "caballero de la muerte", "caballero_muerte", "death knight" -> listOf(
                ClassAbility("death_strike",  "Golpe Mortal",         "Ataque imbuido de muerte",      "2d8+4",   AbilityType.DAMAGE,      "💀", cooldownTurns = 2),
                ClassAbility("unholy_smite",  "Golpe Profano",        "Energía oscura concentrada",    "3d6",     AbilityType.DAMAGE,      "🖤", cooldownTurns = 2),
                ClassAbility("dark_shield",   "Escudo Sombrío",       "+4 CA hasta el próximo turno",  "4",       AbilityType.BUFF_DEFENSE,"🛡️", cooldownTurns = 2),
                ClassAbility("soul_drain",    "Drenar Alma",          "Daño y recupera HP",            "1d8+3",   AbilityType.HEAL,        "💜", cooldownTurns = 3),
            )
            "exorcista" -> listOf(
                ClassAbility("holy_burst",    "Explosión Sagrada",    "Luz que quema lo impuro",       "2d6+2",   AbilityType.DAMAGE,      "✨"),
                ClassAbility("banish",        "Destierro",            "Expulsa entidades oscuras",     "3d8",     AbilityType.DAMAGE,      "🔮", cooldownTurns = 3),
                ClassAbility("seal_ward",     "Sello Protector",      "+3 CA, resistencia mágica",     "3",       AbilityType.BUFF_DEFENSE,"📿", cooldownTurns = 2),
                ClassAbility("purify",        "Purificación",         "Sana heridas con luz sagrada",  "2d6+3",   AbilityType.HEAL,        "🕊️", cooldownTurns = 2),
            )
            "chamán", "chaman", "shaman" -> listOf(
                ClassAbility("spirit_strike", "Golpe Espiritual",     "Los ancestros guían el golpe",  "1d10+2",  AbilityType.DAMAGE,      "👻"),
                ClassAbility("storm_call",    "Llamada a la Tormenta","Truenos del mundo espiritual",  "3d6",     AbilityType.DAMAGE,      "⛈️", cooldownTurns = 3),
                ClassAbility("spirit_heal",   "Sanación Espiritual",  "Los espíritus restauran vida",  "2d6+4",   AbilityType.HEAL,        "🌀", cooldownTurns = 2),
                ClassAbility("totem_guard",   "Tótem Guardián",       "+3 CA bendita por el tótem",    "3",       AbilityType.BUFF_DEFENSE,"🗿", cooldownTurns = 2),
            )
            else -> listOf(
                ClassAbility("basic_att",     "Golpe Básico",         "Ataque cuerpo a cuerpo",        "1d6",     AbilityType.DAMAGE,      "⚔️"),
                ClassAbility("quick_def",     "Postura Defensiva",    "+2 CA un turno",                "2",       AbilityType.BUFF_DEFENSE,"🛡️", cooldownTurns = 2),
                ClassAbility("flee",          "Huir",                 "Intenta escapar",               "flee",    AbilityType.SPECIAL_FLEE,"🏃"),
            )
        }
}

// ============================================================
//  FACTORY
// ============================================================

class CombatViewModelFactory(
    private val enemy: Enemy,
    private val character: Character,
    private val onHpUpdate: (Int) -> Unit
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CombatViewModel(enemy, character, onHpUpdate) as T
}