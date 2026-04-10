package com.example.aidungeonmaster.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.aidungeonmaster.utils.CombatMusicEngine
import com.example.aidungeonmaster.viewmodel.*
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.aidungeonmaster.utils.ImageUtils

private val CombatBg = Color(0xFF080808)
private val EnemyRed = Color(0xFFCC2222)
private val PlayerGreen = Color(0xFF22CC55)
private val GoldAccent = Color(0xFFFFD700)
private val CritYellow = Color(0xFFFFEE00)
private val HealGreen = Color(0xFF44FF88)
private val MissGray = Color(0xFF888888)
private val EnemyHitRed = Color(0xFFFF4444)
private val BorderDark = Color(0xFF333333)
private val PanelBg = Color(0xFF111111)
private val AbilityBg = Color(0xFF1A1A2E)
private val WeaponBg = Color(0xFF2A1010)

@Composable
fun CombatScreen(
    gameViewModel: GameViewModel,
    inventoryViewModel: InventoryViewModel,
    gameId: String,
    onCombatEnd: (victory: Boolean, xpGained: Int) -> Unit,
    achievementViewModel: AchievementViewModel? = null
) {
    val step by gameViewModel.currentAdventureStep.collectAsState()
    val character by inventoryViewModel.character.collectAsState()

    val enemy = step?.enemy
    val charReady = character

    if (enemy == null || charReady == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(CombatBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = GoldAccent)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Preparando el combate...",
                    color = GoldAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    CombatScreenReady(
        enemy = enemy,
        character = charReady,
        gameId = gameId,
        inventoryViewModel = inventoryViewModel,
        onCombatEnd = onCombatEnd,
        achievementViewModel = achievementViewModel
    )
}

@Composable
private fun CombatScreenReady(
    enemy: Enemy,
    character: com.example.aidungeonmaster.data.model.Character,
    gameId: String,
    inventoryViewModel: InventoryViewModel,
    onCombatEnd: (victory: Boolean, xpGained: Int) -> Unit,
    achievementViewModel: AchievementViewModel? = null
) {
    val combatVm: CombatViewModel = viewModel(
        key = "combat_${enemy.name}_${enemy.hpMax}",
        factory = CombatViewModelFactory(
            enemy = enemy,
            character = character,
            onHpUpdate = { newHp -> inventoryViewModel.updateHp(gameId, newHp) },
            achievementViewModel = achievementViewModel,
            charId = gameId
        )
    )

    CombatContent(
        combatVm = combatVm,
        enemyHpMax = enemy.hpMax,
        playerHpMax = character.hpMax,
        inventoryViewModel = inventoryViewModel,
        gameId = gameId,
        onCombatEnd = onCombatEnd
    )
}

@Composable
private fun CombatContent(
    combatVm: CombatViewModel,
    enemyHpMax: Int,
    playerHpMax: Int,
    inventoryViewModel: InventoryViewModel,
    gameId: String,
    onCombatEnd: (victory: Boolean, xpGained: Int) -> Unit
) {
    val phase by combatVm.phase.collectAsState()
    val enemyHp by combatVm.enemyHp.collectAsState()
    val playerHp by combatVm.playerHp.collectAsState()
    val log by combatVm.log.collectAsState()
    val dice by combatVm.dice.collectAsState()
    val cooldowns by combatVm.cooldowns.collectAsState()
    var activeTab by remember { mutableStateOf(0) }

    val enemyImageUrl by combatVm.enemyImageUrl.collectAsState()

    val musicScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        CombatMusicEngine.start(musicScope)
        onDispose {
            CombatMusicEngine.stop()
        }
    }

    LaunchedEffect(phase) {
        if (phase == CombatPhase.VICTORY || phase == CombatPhase.DEFEAT) {
            CombatMusicEngine.fadeOutAndStop(musicScope)
            delay(2200)
            val xpGained = if (phase == CombatPhase.VICTORY)
                (combatVm.enemy.hpMax / 2).coerceAtLeast(5)
            else 0
            onCombatEnd(phase == CombatPhase.VICTORY, xpGained)
        }
    }

    LaunchedEffect(Unit) {
        combatVm.coinsReward.collect { coins ->
            if (coins > 0) inventoryViewModel.addCoins(gameId, coins)
        }
    }

    Box(Modifier.fillMaxSize().background(CombatBg)) {
        Column(Modifier.fillMaxSize()) {
            EnemyZone(
                enemyName = combatVm.enemy.name,
                imageUrl = enemyImageUrl,
                hpCurrent = enemyHp,
                hpMax = enemyHpMax,
                phase = phase,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.28f)
            )
            HorizontalDivider(color = EnemyRed.copy(alpha = 0.4f), thickness = 1.dp)
            CombatLogPanel(
                entries = log,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.30f)
            )
            HorizontalDivider(color = BorderDark, thickness = 1.dp)
            PlayerZone(
                hpCurrent = playerHp,
                hpMax = playerHpMax,
                phase = phase,
                weapons = combatVm.weapons,
                abilities = combatVm.classAbilities,
                cooldowns = cooldowns,
                activeTab = activeTab,
                onTabChange = { activeTab = it },
                onWeaponUse = { combatVm.attackWithWeapon(it) },
                onAbilityUse = { combatVm.useAbility(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
            )
        }

        if (dice.isVisible) DiceOverlay(dice = dice)
        if (phase == CombatPhase.VICTORY || phase == CombatPhase.DEFEAT) {
            CombatEndOverlay(victory = phase == CombatPhase.VICTORY)
        }
        TurnIndicator(
            phase = phase,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
}

@Composable
private fun EnemyZone(
    enemyName: String,
    imageUrl: String,
    hpCurrent: Int,
    hpMax: Int,
    phase: CombatPhase,
    modifier: Modifier = Modifier
) {
    val shakeAnim = remember { Animatable(0f) }
    val prevHp = remember { mutableStateOf(hpCurrent) }

    LaunchedEffect(hpCurrent) {
        if (hpCurrent < prevHp.value) {
            repeat(4) {
                shakeAnim.animateTo(if (it % 2 == 0) 8f else -8f, tween(60))
            }
            shakeAnim.animateTo(0f, tween(60))
        }
        prevHp.value = hpCurrent
    }

    val enemyGlow by animateColorAsState(
        if (phase == CombatPhase.ENEMY_TURN) EnemyRed.copy(0.3f) else Color.Transparent,
        tween(400),
        label = "glow"
    )

    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF200000), CombatBg)))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = shakeAnim.value.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, EnemyRed.copy(0.5f), RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A0000)),
                contentAlignment = Alignment.Center
            ) {
                val enemyBitmap = remember(imageUrl) {
                    try {
                        when {
                            imageUrl.startsWith("data:image", ignoreCase = true) -> {
                                val base64Part = imageUrl.substringAfter("base64,", "")
                                if (base64Part.isNotBlank()) ImageUtils.base64ToBitmap(base64Part) else null
                            }
                            else -> null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                if (enemyBitmap != null) {
                    Image(
                        bitmap = enemyBitmap.asImageBitmap(),
                        contentDescription = enemyName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text(enemyEmoji(enemyName), fontSize = 40.sp, textAlign = TextAlign.Center)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    enemyName.uppercase(),
                    color = EnemyRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                CombatHpBar(
                    hpCurrent,
                    hpMax,
                    EnemyRed,
                    Color(0xFF3D0000),
                    "❤️ $hpCurrent / $hpMax"
                )
            }
        }
        Box(Modifier.fillMaxSize().background(enemyGlow))
    }
}

private fun enemyEmoji(name: String): String = when {
    name.contains("dragón", true) || name.contains("dragon", true) -> "🐉"
    name.contains("esqueleto", true) || name.contains("skeleton", true) -> "💀"
    name.contains("orco", true) || name.contains("orc", true) -> "👹"
    name.contains("lobo", true) || name.contains("wolf", true) -> "🐺"
    name.contains("araña", true) || name.contains("spider", true) -> "🕷️"
    name.contains("troll", true) -> "🧌"
    name.contains("demonio", true) || name.contains("demon", true) -> "😈"
    name.contains("bandido", true) || name.contains("bandit", true) -> "🥷"
    name.contains("fantasma", true) || name.contains("ghost", true) -> "👻"
    name.contains("goblin", true) -> "👺"
    else -> "⚔️"
}

@Composable
private fun CombatLogPanel(entries: List<CombatLogEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    Box(modifier.background(PanelBg).padding(horizontal = 12.dp, vertical = 6.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(entries, key = { it.id }) { CombatLogLine(it) }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(PanelBg, Color.Transparent)))
        )
    }
}

@Composable
private fun CombatLogLine(entry: CombatLogEntry) {
    val color = when (entry.type) {
        LogType.PLAYER_HIT -> Color(0xFF88FF88)
        LogType.PLAYER_CRIT -> CritYellow
        LogType.PLAYER_MISS -> MissGray
        LogType.ENEMY_HIT -> EnemyHitRed
        LogType.ENEMY_MISS -> Color(0xFF558855)
        LogType.HEAL -> HealGreen
        LogType.SPECIAL -> Color(0xFFBB88FF)
        LogType.SYSTEM -> GoldAccent
    }

    Text(
        entry.text,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlayerZone(
    hpCurrent: Int,
    hpMax: Int,
    phase: CombatPhase,
    weapons: List<com.example.aidungeonmaster.data.model.Item>,
    abilities: List<ClassAbility>,
    cooldowns: Map<String, Int>,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onWeaponUse: (com.example.aidungeonmaster.data.model.Item) -> Unit,
    onAbilityUse: (ClassAbility) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlayerTurn = phase == CombatPhase.PLAYER_TURN
    val playerGlow by animateColorAsState(
        if (isPlayerTurn) PlayerGreen.copy(0.08f) else Color.Transparent,
        tween(400),
        label = "pglow"
    )
    val hpRatio = if (hpMax > 0) hpCurrent.toFloat() / hpMax else 0f
    val barColor = when {
        hpRatio > 0.5f -> PlayerGreen
        hpRatio > 0.25f -> Color(0xFFFFAA00)
        else -> EnemyRed
    }

    Column(
        modifier
            .background(Brush.verticalGradient(listOf(CombatBg, Color(0xFF001A00))))
            .background(playerGlow)
            .padding(12.dp)
    ) {
        CombatHpBar(
            hpCurrent,
            hpMax,
            barColor,
            Color(0xFF003300),
            "💚 $hpCurrent / $hpMax  (Tu personaje)"
        )
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("⚔️ Armas", "✨ Habilidades").forEachIndexed { idx, lbl ->
                OutlinedButton(
                    onClick = { onTabChange(idx) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (activeTab == idx) Color(0xFF1F1F1F) else Color.Transparent,
                        contentColor = if (activeTab == idx) GoldAccent else MissGray
                    ),
                    border = BorderStroke(1.dp, if (activeTab == idx) GoldAccent else BorderDark),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(lbl, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTab == 0) {
                weapons.forEach { weapon ->
                    WeaponButton(weapon, isPlayerTurn) {
                        onWeaponUse(weapon)
                    }
                }
            } else {
                abilities.forEach { ability ->
                    val cd = cooldowns[ability.id] ?: 0
                    AbilityButton(ability, cd, isPlayerTurn && cd == 0) {
                        onAbilityUse(ability)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponButton(
    weapon: com.example.aidungeonmaster.data.model.Item,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val diceInfo = remember(weapon) {
        val src = weapon.effect.ifBlank { weapon.description }
        val m = Regex("""(\d*)[dD](\d+)""").find(src)
        m?.value?.uppercase() ?: "1D4"
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(108.dp)
            .height(78.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WeaponBg,
            disabledContainerColor = Color(0xFF110808)
        ),
        border = BorderStroke(1.dp, if (enabled) EnemyRed.copy(0.8f) else BorderDark),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(5.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚔️", fontSize = 20.sp)
            Text(
                weapon.name,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                color = if (enabled) Color.White else MissGray,
                maxLines = 2
            )
            Text(
                diceInfo,
                fontSize = 10.sp,
                color = if (enabled) EnemyRed else MissGray,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AbilityButton(
    ability: ClassAbility,
    cooldown: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(108.dp)
            .height(78.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (ability.type == AbilityType.HEAL) Color(0xFF0A1F0A) else AbilityBg,
            disabledContainerColor = Color(0xFF0A0A15)
        ),
        border = BorderStroke(
            1.dp,
            when {
                !enabled && cooldown > 0 -> Color(0xFF555555)
                ability.type == AbilityType.HEAL -> HealGreen.copy(0.7f)
                else -> Color(0xFF5544AA)
            }
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(5.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (cooldown > 0) "⏳" else ability.emoji, fontSize = 20.sp)
            Text(
                ability.name,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                color = if (enabled) Color.White else MissGray,
                maxLines = 2
            )
            if (cooldown > 0) {
                Text(
                    "$cooldown turnos",
                    fontSize = 9.sp,
                    color = MissGray,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    ability.diceExpression.uppercase().take(6),
                    fontSize = 10.sp,
                    color = if (enabled) Color(0xFF88AAFF) else MissGray,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CombatHpBar(
    hpCurrent: Int,
    hpMax: Int,
    barColor: Color,
    trackColor: Color,
    label: String
) {
    val ratio = if (hpMax > 0) hpCurrent.toFloat() / hpMax else 0f
    val anim by animateFloatAsState(ratio, tween(600, easing = EaseOutCubic), label = "hp")
    val animColor by animateColorAsState(barColor, tween(400), label = "hpc")

    Column {
        Text(
            label,
            color = animColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(13.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            drawRect(trackColor, size = this.size)
            drawRect(animColor, size = androidx.compose.ui.geometry.Size(this.size.width * anim, this.size.height))
            val seg = this.size.width / 10f
            repeat(9) { i ->
                drawRect(
                    Color.Black.copy(0.5f),
                    topLeft = Offset((i + 1) * seg, 0f),
                    size = androidx.compose.ui.geometry.Size(1.5f, this.size.height)
                )
            }
        }
    }
}

@Composable
private fun DiceOverlay(dice: DiceAnimState) {
    val inf = rememberInfiniteTransition(label = "d_anim")
    val rot by inf.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "rot"
    )
    val pulse by inf.animateFloat(
        0.95f,
        1.05f,
        infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pls"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.83f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                .border(
                    2.dp,
                    if (dice.isCrit) CritYellow else if (dice.isHeal) HealGreen else GoldAccent,
                    RoundedCornerShape(12.dp)
                )
                .padding(28.dp)
        ) {
            Text(
                dice.actionLabel,
                color = GoldAccent,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text("🎲", fontSize = 54.sp, modifier = Modifier.rotate(rot).scale(pulse))
            Spacer(Modifier.height(8.dp))
            if (dice.rolls.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    dice.rolls.take(8).forEach {
                        Text(
                            "[$it]",
                            color = Color.White.copy(0.8f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (dice.rolls.size > 8) {
                        Text("...", color = MissGray, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            val totalColor = when {
                dice.isCrit -> CritYellow
                dice.isFumble -> EnemyRed
                dice.isHeal -> HealGreen
                else -> Color.White
            }

            Text(
                when {
                    dice.isCrit -> "⚡ CRÍTICO = ${dice.total}"
                    dice.isFumble -> "💀 FALLO CRÍTICO"
                    dice.isHeal -> "💚 +${dice.total} HP"
                    else -> "= ${dice.total}"
                },
                color = totalColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                dice.diceLabel,
                color = MissGray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CombatEndOverlay(victory: Boolean) {
    val alpha by animateFloatAsState(1f, tween(900), label = "eof")
    Box(
        Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    if (victory) listOf(Color(0xFF001500), Color.Black)
                    else listOf(Color(0xFF200000), Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (victory) "🏆" else "💀", fontSize = 68.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                if (victory) "¡VICTORIA!" else "HAS CAÍDO",
                color = if (victory) GoldAccent else EnemyRed,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (victory) "Regresando a la aventura..." else "Game over",
                color = Color.White.copy(0.6f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TurnIndicator(phase: CombatPhase, modifier: Modifier = Modifier) {
    val (lbl, col) = when (phase) {
        CombatPhase.PLAYER_TURN -> "TU TURNO" to PlayerGreen
        CombatPhase.ENEMY_TURN -> "ENEMIGO" to EnemyRed
        CombatPhase.ROLLING -> "TIRANDO…" to GoldAccent
        CombatPhase.VICTORY -> "VICTORIA" to GoldAccent
        CombatPhase.DEFEAT -> "DERROTA" to EnemyRed
        else -> "" to Color.Transparent
    }

    if (lbl.isEmpty()) return

    val pulse by rememberInfiniteTransition(label = "ti").animateFloat(
        0.6f,
        1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "p"
    )

    Box(
        modifier
            .alpha(if (phase == CombatPhase.ROLLING) 1f else pulse)
            .background(col.copy(0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, col.copy(0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            lbl,
            color = col,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PixelTransitionOverlay(onComplete: () -> Unit) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(900, easing = LinearEasing))
        delay(80)
        onComplete()
    }

    Canvas(Modifier.fillMaxSize()) {
        val px = 30f
        val cols = (size.width / px).toInt() + 1
        val rows = (size.height / px).toInt() + 1
        val total = (cols * rows).toFloat()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val delay = (row * cols + col) / total * 0.85f
                if (progress.value >= delay) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset(col * px, row * px),
                        size = Size(px + 1f, px + 1f)
                    )
                }
            }
        }
    }
}