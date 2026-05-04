package com.example.aidungeonmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aidungeonmaster.data.model.BestiaryLoot
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.utils.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

// ============================================================
//  ENUMS Y DATA CLASSES
// ============================================================

private val logIdCounter = AtomicLong(0L)

// Clase que encapsula la lógica de combat phase.
enum class CombatPhase {
    INTRO,
    PLAYER_TURN,
    ROLLING,
    ENEMY_TURN,
    VICTORY,
    DEFEAT,
    FLED
}

// Clase que encapsula la lógica de log type.
enum class LogType {
    SYSTEM,
    PLAYER_HIT, PLAYER_MISS, PLAYER_CRIT,
    ENEMY_HIT, ENEMY_MISS,
    HEAL, SPECIAL
}

// Modelo de datos que representa combat log entry.
data class CombatLogEntry(
    val text: String,
    val type: LogType,
    val id: Long = logIdCounter.incrementAndGet()
)

// Clase que encapsula la lógica de dice anim state.
data class DiceAnimState(
    val diceLabel: String = "",
    val actionLabel: String = "",
    val rolls: List<Int> = emptyList(),
    val total: Int = 0,
    val isVisible: Boolean = false,
    val isCrit: Boolean = false,
    val isFumble: Boolean = false,
    val isHeal: Boolean = false,
)

// Clase que encapsula la lógica de ability type.
enum class AbilityType { DAMAGE, HEAL, BUFF_DEFENSE, BUFF_ATTACK, SPECIAL_FLEE }

// Clase que encapsula la lógica de class ability.
data class ClassAbility(
    val id: String,
    val name: String,
    val description: String,
    val diceExpression: String,
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
    private val onHpUpdate: (Int) -> Unit,
    // ── NUEVO: AchievementViewModel para disparar logros desde el combate ──
    val achievementViewModel: AchievementViewModel? = null,
    // ── NUEVO: charId necesario para las funciones del AchievementViewModel ──
    private val charId: String = ""
) : ViewModel() {

    // ============================================================
    //  DIARIO DE AVENTURA
    // ============================================================

    private suspend fun addJournalEntry(
        title: String,
        summary: String,
        fullText: String = "",
        type: String = "combat",
        tags: List<String> = emptyList(),
        enemyName: String = "",
        hpChange: Int = 0,
        coinsChange: Int = 0,
        xpGained: Int = 0
    ) {
        try {
            if (charId.isBlank()) return

            val entryId = UUID.randomUUID().toString()

            val payload = mapOf(
                "title" to title,
                "summary" to summary,
                "fullText" to fullText,
                "timestamp" to System.currentTimeMillis(),
                "chapter" to "",
                "type" to type,
                "tags" to tags.distinct(),
                "locationName" to "",
                "enemyName" to enemyName,
                "itemNames" to emptyList<String>(),
                "hpChange" to hpChange,
                "coinsChange" to coinsChange,
                "xpGained" to xpGained
            )

            db.collection("partidas")
                .document(charId)
                .collection("journal")
                .document(entryId)
                .set(payload, SetOptions.merge())
                .await()
        } catch (_: Exception) {
            // no rompemos el combate por un fallo del diario
        }
    }

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

    private val _xpReward = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val xpReward = _xpReward.asSharedFlow()

    private val _coinsReward = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val coinsReward = _coinsReward.asSharedFlow()

    private val _enemyImageUrl = MutableStateFlow("")
    val enemyImageUrl = _enemyImageUrl.asStateFlow()

    private val db = FirebaseFirestore.getInstance()

    private val bestiaryMonsterId: String
        get() = enemy.name
            .trim()
            .lowercase()
            .replace("[^a-z0-9áéíóúñ ]".toRegex(), "")
            .replace("\\s+".toRegex(), "_")
            .ifBlank { "monstruo_desconocido" }

    private var defenseBonus = 0
    private var hasAdvantage = false

    private val strMod get() = playerCharacter.strMod
    private val dexMod get() = playerCharacter.dexMod
    private val intMod get() = playerCharacter.intMod
    private val wisMod get() = playerCharacter.wisMod
    private val profBonus get() = playerCharacter.profBonus

    private val enemyAC: Int = (10 + (enemy.hpMax / 12).coerceIn(0, 5))
    private val playerAC: Int
        get() = (playerCharacter.armorClass + defenseBonus).coerceIn(8, 32)

    // Ejecuta la lógica de resolve active weapon.
    private fun resolveActiveWeapon(): Item {
        return playerCharacter.equippedWeapon ?: Item(
            id = "fist",
            name = "Puñetazo",
            description = "Ataque desarmado",
            type = "arma",
            weaponDamage = "1d4"
        )
    }

    val classAbilities: List<ClassAbility> = abilitiesForClass(playerCharacter.characterClass)

    val weapons: List<Item>
        get() = listOf(resolveActiveWeapon())

    // ============================================================
    //  INICIO DEL COMBATE
    // ============================================================
    init {
        if (playerCharacter.hpCurrent <= 0) {
            _playerHp.value = 0
            _phase.value = CombatPhase.DEFEAT
            log("💀 Tu personaje ya no puede continuar combatiendo.", LogType.SYSTEM)
        } else {
            viewModelScope.launch {
                registerEncounterInBestiary()

                log("📚 Guardando bestiario en partida: $charId", LogType.SYSTEM)
                android.util.Log.d("BESTIARY_DEBUG", "registerEncounterInBestiary charId=$charId enemy=${enemy.name}")

                delay(400)
                log("⚔️ ¡${enemy.name} aparece ante ti!", LogType.SYSTEM)
                log("📊 CA enemiga: $enemyAC | HP: ${enemy.hpMax}", LogType.SYSTEM)
                delay(600)
                log("🎲 Tu turno — elige tu acción", LogType.SYSTEM)
                _phase.value = CombatPhase.PLAYER_TURN
            }
        }
    }


    // Ejecuta la lógica de infer enemy tags.
    private fun inferEnemyTags(): List<String> {
        val basis = enemy.name.lowercase()
        val tags = mutableListOf<String>()
        if (basis.contains("esqueleto") || basis.contains("zombi") || basis.contains("fantasma") || basis.contains("no muerto")) tags += "no-muerto"
        if (basis.contains("fuego") || basis.contains("lava")) tags += "fuego"
        if (basis.contains("hielo") || basis.contains("ice")) tags += "hielo"
        if (basis.contains("veneno") || basis.contains("araña") || basis.contains("serpiente")) tags += "veneno"
        if (basis.contains("gólem") || basis.contains("golem") || basis.contains("piedra")) tags += "constructo"
        return tags.distinct()
    }

    // Ejecuta la lógica de infer observed weaknesses.
    private fun inferObservedWeaknesses(triggerSource: String? = null): List<String> {
        val basis = listOf(enemy.name, triggerSource.orEmpty()).joinToString(" ").lowercase()
        val weaknesses = mutableListOf<String>()
        if (basis.contains("hielo") || basis.contains("ice")) weaknesses += "fuego"
        if (basis.contains("fuego") || basis.contains("lava")) weaknesses += "hielo"
        if (basis.contains("esqueleto") || basis.contains("zombi") || basis.contains("fantasma") || basis.contains("no muerto")) weaknesses += "sagrado"
        if (basis.contains("araña") || basis.contains("bestia") || basis.contains("maleza")) weaknesses += "fuego"
        if (basis.contains("gólem") || basis.contains("golem") || basis.contains("piedra")) weaknesses += "contundente"
        if (basis.contains("holy") || basis.contains("sagrado")) weaknesses += "sagrado"
        return weaknesses.distinct()
    }

    // Ejecuta la lógica de infer observed resistances.
    private fun inferObservedResistances(): List<String> {
        val basis = enemy.name.lowercase()
        val resistances = mutableListOf<String>()
        if (basis.contains("hielo") || basis.contains("ice")) resistances += "frío"
        if (basis.contains("fuego") || basis.contains("lava")) resistances += "fuego"
        if (basis.contains("gólem") || basis.contains("golem") || basis.contains("piedra")) resistances += "daño cortante leve"
        if (basis.contains("fantasma") || basis.contains("espectro")) resistances += "daño físico"
        if (basis.contains("araña") || basis.contains("veneno") || basis.contains("serpiente")) resistances += "veneno"
        return resistances.distinct()
    }

    // Construye detailed loot from victory.
    private fun buildDetailedLootFromVictory(coinsGained: Int, finisherName: String): List<BestiaryLoot> {
        val loot = mutableListOf<BestiaryLoot>()
        if (coinsGained > 0) {
            loot += BestiaryLoot(
                name = "Monedas de oro",
                category = "moneda",
                details = "Cantidad observada al rematar a ${enemy.name} con $finisherName.",
                quantityObserved = coinsGained,
                timesDropped = 1
            )
        }
        val basis = enemy.name.lowercase()
        when {
            basis.contains("araña") -> loot += BestiaryLoot("Saco de veneno", "material", "Residuo extraído de una araña derrotada.", 1, 1)
            basis.contains("lobo") || basis.contains("bestia") -> loot += BestiaryLoot("Piel curtible", "material", "Material orgánico aprovechable tras el combate.", 1, 1)
            basis.contains("esqueleto") || basis.contains("zombi") -> loot += BestiaryLoot("Restos óseos", "material", "Fragmentos recogidos del enemigo abatido.", 1, 1)
            basis.contains("gólem") || basis.contains("golem") || basis.contains("piedra") -> loot += BestiaryLoot("Fragmento de núcleo", "tesoro", "Pieza sólida hallada en el cuerpo del constructo.", 1, 1)
        }
        return loot.distinctBy { it.name.lowercase() }
    }

    // Ejecuta la lógica de merge detailed loot.
    private fun mergeDetailedLoot(current: List<BestiaryLoot>, incoming: List<BestiaryLoot>): List<BestiaryLoot> {
        if (incoming.isEmpty()) return current
        val merged = current.associateBy { it.name.lowercase() }.toMutableMap()
        incoming.forEach { loot ->
            val previous = merged[loot.name.lowercase()]
            merged[loot.name.lowercase()] = if (previous == null) loot else previous.copy(
                category = if (loot.category.length > previous.category.length) loot.category else previous.category,
                details = if (loot.details.length > previous.details.length) loot.details else previous.details,
                quantityObserved = maxOf(previous.quantityObserved, loot.quantityObserved),
                timesDropped = previous.timesDropped + loot.timesDropped
            )
        }
        return merged.values.sortedBy { it.name.lowercase() }
    }

    // ============================================================
    //  ACCIONES DEL JUGADOR
    // ============================================================

    fun attackWithWeapon(weapon: Item) {
        if (_phase.value != CombatPhase.PLAYER_TURN) return
        _phase.value = CombatPhase.ROLLING

        viewModelScope.launch {
            val activeWeapon = resolveActiveWeapon()
            val usedAdvantage = hasAdvantage

            val rollA1 = roll(20)
            val rollA2 = if (usedAdvantage) roll(20) else rollA1
            val attackRoll = if (usedAdvantage) maxOf(rollA1, rollA2) else rollA1
            hasAdvantage = false

            val isCrit = attackRoll == 20
            val isFumble = attackRoll == 1
            val attackBonus = playerCharacter.meleeAttackBonus
            val totalAtk = attackRoll + attackBonus

            showDice(
                diceLabel = if (usedAdvantage) "2d20 ventaja" else "1d20",
                actionLabel = "Ataque con ${activeWeapon.name}",
                rolls = if (usedAdvantage) listOf(rollA1, rollA2) else listOf(attackRoll),
                total = totalAtk,
                isCrit = isCrit,
                isFumble = isFumble
            )
            delay(2000)
            hideDice()

            when {
                isFumble -> {
                    log("💀 ¡Fallo crítico con ${weapon.name}! Tu arma se escapa de tu mano.", LogType.PLAYER_MISS)
                    endPlayerTurn()
                }
                isCrit || totalAtk >= enemyAC -> {
                    val (cnt, sides, bonus) = parseDice(activeWeapon.resolvedWeaponDamage)
                    val count = if (isCrit) cnt * 2 else cnt
                    val damageRolls = List(count) { roll(sides) }
                    val rawDamage = damageRolls.sum() + bonus + playerCharacter.weaponDamageBonus
                    val damage      = rawDamage.coerceAtLeast(1)

                    // ── LOGRO: Golpe Crítico ─────────────────────────────────
                    if (isCrit) {
                        achievementViewModel?.onCriticalHit()
                    }

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
                        "${if (isCrit) "⚡" else "🗡️"} ${activeWeapon.name} golpea a ${enemy.name} " +
                                "por $damage de daño${if (isCrit) " (¡CRÍTICO!)" else ""}!",
                        if (isCrit) LogType.PLAYER_CRIT else LogType.PLAYER_HIT
                    )

                    val newEnemyHp = (_enemyHp.value - damage).coerceAtLeast(0)
                    _enemyHp.value = newEnemyHp

                    if (newEnemyHp <= 0) {
                        log("💀 ¡${enemy.name} ha sido derrotado!", LogType.SYSTEM)

                        val xpGained = (enemy.hpMax / 2).coerceAtLeast(5)
                        log("⭐ +$xpGained XP ganado", LogType.SYSTEM)
                        _xpReward.emit(xpGained)

                        val coinsGained = if (enemy.goldCoins > 0) enemy.goldCoins
                        else ((enemy.hpMax / 4) + (1..10).random()).coerceAtLeast(5)
                        log("🪙 +$coinsGained monedas de oro", LogType.SYSTEM)
                        _coinsReward.emit(coinsGained)

                        registerDefeatInBestiary(
                            damageNotes = listOf(activeWeapon.name),
                            knownLoot = if (coinsGained > 0) listOf("$coinsGained monedas de oro") else emptyList(),
                            detailedLoot = buildDetailedLootFromVictory(
                                coinsGained = coinsGained,
                                finisherName = activeWeapon.name
                            ),
                            observedWeaknesses = inferObservedWeaknesses(activeWeapon.name),
                            observedResistances = inferObservedResistances()
                        )

                        addJournalEntry(
                            title = "Victoria en combate",
                            summary = "Has derrotado a ${enemy.name} con ${weapon.name}.",
                            fullText = buildString {
                                append("El combate terminó en victoria. ")
                                append("${enemy.name} fue derrotado usando ${activeWeapon.name}. ")
                                append("Recompensa obtenida: $xpGained XP")
                                if (coinsGained > 0) append(" y $coinsGained monedas")
                                append(".")
                            },
                            type = "combat",
                            tags = listOf("combate", "victoria", enemy.name.lowercase(), weapon.name.lowercase()),
                            enemyName = enemy.name,
                            coinsChange = coinsGained,
                            xpGained = xpGained
                        )

                        // ── LOGRO: Victoria en combate ───────────────────────
                        achievementViewModel?.onCombatWon(charId)

                        // ── LOGRO: Sobrevivir con 1 HP ───────────────────────
                        if (_playerHp.value <= 1) {
                            achievementViewModel?.onSurvivedLowHp()
                        }

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

    // Ejecuta la lógica de use ability.
    fun useAbility(ability: ClassAbility) {
        if (_phase.value != CombatPhase.PLAYER_TURN) return
        val cd = _cooldowns.value[ability.id] ?: 0
        if (cd > 0) return
        _phase.value = CombatPhase.ROLLING

        viewModelScope.launch {
            when (ability.type) {
                AbilityType.DAMAGE -> {
                    val isCrit = roll(20) == 20
                    val (cnt, sides, bonus) = parseDice(ability.diceExpression)
                    val count  = if (isCrit) cnt * 2 else cnt
                    val rolls  = List(count) { roll(sides) }
                    val damage = (rolls.sum() + bonus).coerceAtLeast(1)

                    // ── LOGRO: Golpe Crítico con habilidad ───────────────────
                    if (isCrit) {
                        achievementViewModel?.onCriticalHit()
                    }

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

                        val xpGained = (enemy.hpMax / 2).coerceAtLeast(5)
                        log("⭐ +$xpGained XP ganado", LogType.SYSTEM)
                        _xpReward.emit(xpGained)

                        val coinsGained = if (enemy.goldCoins > 0) enemy.goldCoins
                        else ((enemy.hpMax / 4) + (1..10).random()).coerceAtLeast(5)
                        log("🪙 +$coinsGained monedas de oro", LogType.SYSTEM)
                        _coinsReward.emit(coinsGained)

                        registerDefeatInBestiary(
                            abilitiesSeen = listOf(ability.name),
                            knownLoot = if (coinsGained > 0) listOf("$coinsGained monedas de oro") else emptyList()
                        )

                        addJournalEntry(
                            title = "Victoria en combate",
                            summary = "Has derrotado a ${enemy.name} con la habilidad ${ability.name}.",
                            fullText = buildString {
                                append("El combate terminó en victoria. ")
                                append("${enemy.name} cayó ante la habilidad ${ability.name}. ")
                                append("Recompensa obtenida: $xpGained XP")
                                if (coinsGained > 0) append(" y $coinsGained monedas")
                                append(".")
                            },
                            type = "combat",
                            tags = listOf("combate", "victoria", enemy.name.lowercase(), ability.name.lowercase()),
                            enemyName = enemy.name,
                            coinsChange = coinsGained,
                            xpGained = xpGained
                        )

                        // ── LOGRO: Victoria en combate ───────────────────────
                        achievementViewModel?.onCombatWon(charId)

                        // ── LOGRO: Sobrevivir con 1 HP ───────────────────────
                        if (_playerHp.value <= 1) {
                            achievementViewModel?.onSurvivedLowHp()
                        }

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

                    addJournalEntry(
                        title = "Has usado una habilidad de curación",
                        summary = "Usaste ${ability.name} y recuperaste $heal puntos de vida.",
                        fullText = "${ability.name} permitió recuperar $heal puntos de vida durante el combate contra ${enemy.name}.",
                        type = "combat",
                        tags = listOf("combate", "curacion", ability.name.lowercase()),
                        enemyName = enemy.name,
                        hpChange = heal
                    )
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
                        _phase.value = CombatPhase.FLED
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
            if (
                _phase.value == CombatPhase.VICTORY ||
                _phase.value == CombatPhase.DEFEAT ||
                _phase.value == CombatPhase.FLED
            ) return@launch
            _phase.value = CombatPhase.ENEMY_TURN
            log("🔴 Turno de ${enemy.name}...", LogType.SYSTEM)
            delay(900)
            enemyAttack()
        }
    }

    // Ejecuta la lógica de enemy attack.
    private fun enemyAttack() {
        viewModelScope.launch {
            val attackRoll = roll(20)
            val isCrit     = attackRoll == 20
            val isFumble   = attackRoll == 1
            val totalAtk   = attackRoll + 3

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

                        addJournalEntry(
                            title = "Has sido derrotado",
                            summary = "Has caído en combate contra ${enemy.name}.",
                            fullText = "El combate terminó en derrota. ${enemy.name} venció al héroe.",
                            type = "combat",
                            tags = listOf("combate", "derrota", enemy.name.lowercase()),
                            enemyName = enemy.name
                        )

                        delay(600)
                        _phase.value = CombatPhase.DEFEAT
                        return@launch
                    }
                }
                else -> {
                    log("🛡️ Bloqueas el ataque de ${enemy.name} (CA ${playerAC}, tirada: $totalAtk).", LogType.ENEMY_MISS)
                }
            }

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
    //  BESTIARIO
    // ============================================================

    private suspend fun registerEncounterInBestiary() {
        try {
            if (charId.isBlank()) return

            val docRef = db.collection("partidas")
                .document(charId)
                .collection("bestiary")
                .document(bestiaryMonsterId)

            val snapshot = docRef.get().await()
            val now = System.currentTimeMillis()
            val previousTimesEncountered = snapshot.getLong("timesEncountered")?.toInt() ?: 0
            val previousTimesDefeated = snapshot.getLong("timesDefeated")?.toInt() ?: 0
            val previousLocations = (snapshot.get("locationsSeen") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val previousTags = (snapshot.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val previousDamageNotes = ((snapshot.get("lastObservedStats") as? Map<*, *>)?.get("damageNotes") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()
            val previousAbilitiesSeen = ((snapshot.get("lastObservedStats") as? Map<*, *>)?.get("abilitiesSeen") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()
            val firstSeenAt = snapshot.getLong("firstSeenAt") ?: now
            val mergedTags = (previousTags + inferEnemyTags() + listOf("combate")).distinct()
            val mergedWeaknesses = ((snapshot.get("observedWeaknesses") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()) + inferObservedWeaknesses()
            val mergedResistances = ((snapshot.get("observedResistances") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()) + inferObservedResistances()

            var imageUrl = snapshot.getString("imageUrl") ?: ""
            if (imageUrl.isBlank()) {
                imageUrl = try {
                    ImageUtils.generateMonsterImageDataUrl(
                        monsterNameEs = enemy.name,
                        descriptionEs = "Enemigo encontrado en combate.",
                        tags = mergedTags
                    )
                } catch (_: Exception) {
                    ""
                }
            }

            val payload = mapOf(
                "name" to enemy.name,
                "description" to (snapshot.getString("description") ?: "Enemigo encontrado en combate."),
                "imageUrl" to imageUrl,
                "firstSeenAt" to firstSeenAt,
                "lastSeenAt" to now,
                "timesEncountered" to (previousTimesEncountered + 1),
                "timesDefeated" to previousTimesDefeated,
                "locationsSeen" to previousLocations.distinct(),
                "tags" to mergedTags,
                "lastObservedStats" to mapOf(
                    "hpMaxObserved" to enemy.hpMax,
                    "armorClassObserved" to enemyAC,
                    "damageNotes" to previousDamageNotes.distinct(),
                    "abilitiesSeen" to previousAbilitiesSeen.distinct()
                ),
                "knownLoot" to ((snapshot.get("knownLoot") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()),
                "detailedKnownLoot" to ((snapshot.get("detailedKnownLoot") as? List<*>) ?: emptyList<Any>()),
                "observedWeaknesses" to mergedWeaknesses.distinct(),
                "observedResistances" to mergedResistances.distinct(),
                "notes" to (snapshot.getString("notes") ?: "")
            )

            docRef.set(payload, SetOptions.merge()).await()
            _enemyImageUrl.value = imageUrl
        } catch (e: Exception) {
            log("⚠️ No se pudo registrar ${enemy.name} en el bestiario.", LogType.SYSTEM)
        }
    }

    private suspend fun registerDefeatInBestiary(
        damageNotes: List<String> = emptyList(),
        abilitiesSeen: List<String> = emptyList(),
        knownLoot: List<String> = emptyList(),
        detailedLoot: List<BestiaryLoot> = emptyList(),
        observedWeaknesses: List<String> = emptyList(),
        observedResistances: List<String> = emptyList()
    ) {
        try {
            if (charId.isBlank()) return

            val docRef = db.collection("partidas")
                .document(charId)
                .collection("bestiary")
                .document(bestiaryMonsterId)

            val snapshot = docRef.get().await()
            val now = System.currentTimeMillis()
            val previousTimesEncountered = snapshot.getLong("timesEncountered")?.toInt() ?: 1
            val previousTimesDefeated = snapshot.getLong("timesDefeated")?.toInt() ?: 0
            val previousKnownLoot = (snapshot.get("knownLoot") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val previousDamageNotes = ((snapshot.get("lastObservedStats") as? Map<*, *>)?.get("damageNotes") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()
            val previousAbilitiesSeen = ((snapshot.get("lastObservedStats") as? Map<*, *>)?.get("abilitiesSeen") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()
            val previousDetailedLoot = (snapshot.get("detailedKnownLoot") as? List<*>)
                ?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val name = map["name"] as? String ?: return@mapNotNull null
                    BestiaryLoot(
                        name = name,
                        category = map["category"] as? String ?: "desconocido",
                        details = map["details"] as? String ?: "",
                        quantityObserved = (map["quantityObserved"] as? Number)?.toInt() ?: 1,
                        timesDropped = (map["timesDropped"] as? Number)?.toInt() ?: 1
                    )
                } ?: emptyList()
            val firstSeenAt = snapshot.getLong("firstSeenAt") ?: now
            val mergedDetailedLoot = mergeDetailedLoot(previousDetailedLoot, detailedLoot)

            val payload = mapOf(
                "name" to enemy.name,
                "description" to (snapshot.getString("description") ?: "Enemigo encontrado en combate."),
                "imageUrl" to (snapshot.getString("imageUrl") ?: ""),
                "firstSeenAt" to firstSeenAt,
                "lastSeenAt" to now,
                "timesEncountered" to previousTimesEncountered,
                "timesDefeated" to (previousTimesDefeated + 1),
                "locationsSeen" to ((snapshot.get("locationsSeen") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()),
                "tags" to ((snapshot.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: listOf("combate")),
                "lastObservedStats" to mapOf(
                    "hpMaxObserved" to enemy.hpMax,
                    "armorClassObserved" to enemyAC,
                    "damageNotes" to (previousDamageNotes + damageNotes).distinct(),
                    "abilitiesSeen" to (previousAbilitiesSeen + abilitiesSeen).distinct()
                ),
                "knownLoot" to (previousKnownLoot + knownLoot).distinct(),
                "detailedKnownLoot" to mergedDetailedLoot.map { loot ->
                    mapOf(
                        "name" to loot.name,
                        "category" to loot.category,
                        "details" to loot.details,
                        "quantityObserved" to loot.quantityObserved,
                        "timesDropped" to loot.timesDropped
                    )
                },
                "observedWeaknesses" to (((snapshot.get("observedWeaknesses") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()) + observedWeaknesses).distinct(),
                "observedResistances" to (((snapshot.get("observedResistances") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()) + observedResistances).distinct(),
                "notes" to (snapshot.getString("notes") ?: "")
            )

            docRef.set(payload, SetOptions.merge()).await()
            _enemyImageUrl.value = snapshot.getString("imageUrl") ?: ""
        } catch (e: Exception) {
            log("⚠️ No se pudo actualizar la derrota de ${enemy.name} en el bestiario.", LogType.SYSTEM)
        }
    }

    // ============================================================
    //  UTILIDADES PRIVADAS
    // ============================================================

    private fun roll(sides: Int): Int = (1..sides).random()

    // Analiza dice.
    private fun parseDice(expr: String): Triple<Int, Int, Int> {
        val clean = expr.uppercase().trim()
        val rx    = """(\d*)D(\d+)(?:\+(\d+))?""".toRegex()
        val m     = rx.find(clean) ?: return Triple(1, 6, 0)
        val cnt   = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
        val sides = m.groupValues[2].toIntOrNull() ?: 6
        val bonus = m.groupValues[3].toIntOrNull() ?: 0
        return Triple(cnt, sides, bonus)
    }

    // Ejecuta la lógica de log.
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

    // Ejecuta la lógica de hide dice.
    private fun hideDice() { _dice.value = DiceAnimState() }

    // Ejecuta la lógica de add cooldown.
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
    private val onHpUpdate: (Int) -> Unit,
    // ── NUEVO: pasar el AchievementViewModel y charId al factory ──
    private val achievementViewModel: AchievementViewModel? = null,
    private val charId: String = ""
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CombatViewModel(enemy, character, onHpUpdate, achievementViewModel, charId) as T
}
