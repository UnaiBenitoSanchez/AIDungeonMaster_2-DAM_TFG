package com.example.aidungeonmaster.ui.game

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Achievement
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.data.model.Quest
import com.example.aidungeonmaster.navigation.Screen
import com.example.aidungeonmaster.ui.achievements.AchievementToast
import com.example.aidungeonmaster.ui.achievements.QuestCompletedToast
import com.example.aidungeonmaster.viewmodel.GameViewModel
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.example.aidungeonmaster.viewmodel.WorldMapViewModel
import com.example.aidungeonmaster.utils.AdventureMusicEngine
import com.example.aidungeonmaster.viewmodel.AchievementViewModel
import kotlinx.coroutines.launch
import com.example.aidungeonmaster.utils.SupermarketProximityManager
import androidx.compose.ui.platform.LocalContext
import com.example.aidungeonmaster.viewmodel.JournalViewModel

import androidx.compose.material.icons.filled.MenuBook

import androidx.lifecycle.compose.LifecycleResumeEffect

import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    navController: androidx.navigation.NavHostController,
    userId: String,
    characterName: String,
    theme: String,
    viewModel: GameViewModel = viewModel(),
    inventoryViewModel: InventoryViewModel = viewModel(),
    mapViewModel: WorldMapViewModel = viewModel(),
    achievementViewModel: AchievementViewModel = viewModel(),
    journalViewModel: JournalViewModel = viewModel()
) {
    val listState = rememberLazyListState()
    val messages  by viewModel.messages.collectAsState()
    val options   by viewModel.currentOptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val characterState by inventoryViewModel.character.collectAsState()
    val mapState by mapViewModel.worldMapState.collectAsState()

    val hpCurrent = characterState?.hpCurrent ?: 20
    val hpMax     = characterState?.hpMax     ?: 20

    var customAction by remember { mutableStateOf("") }

    val gameId = "${userId}_${characterName}_${theme}".replace(" ", "_")
    val charId = "${userId}_${characterName}"

    val currentStep by viewModel.currentAdventureStep.collectAsState()
    var showPixelTransition by remember { mutableStateOf(false) }

    var levelUpDialogLevel by remember { mutableStateOf<Int?>(null) }

    // ── MÚSICA ───────────────────────────────────────────────────────────────
    LifecycleResumeEffect(theme) {
        AdventureMusicEngine.enterGameplay(theme)

        onPauseOrDispose {
            // No parar aquí.  a
            // El stop total lo controla la navegación al salir del flujo de aventura.
        }
    }

    // ── DETECCIÓN DE SUPERMERCADOS EN TIEMPO REAL ─────────────────────────
    // Se activa mientras el jugador está en la pantalla de juego y se detiene
    // automáticamente al salir (onDispose). El Worker del background sigue
    // funcionando cuando la app está cerrada.
//    val context = LocalContext.current
//
//    DisposableEffect(Unit) {
//        val proximityManager = SupermarketProximityManager(context)
//        proximityManager.start()
//        onDispose { proximityManager.stop() }
//    }

    // ── CARGA INICIAL ─────────────────────────────────────────────────────────
    LaunchedEffect(charId) {
        inventoryViewModel.loadInventory(charId)
        journalViewModel.loadJournal(charId)

        viewModel.worldMapViewModel = mapViewModel

        mapViewModel.onLocationDiscoveredForAchievements = { discoveredCharId, locationCount ->
            achievementViewModel.onLocationDiscovered(discoveredCharId, locationCount)
        }

        mapViewModel.loadMap(charId)
        viewModel.startStory(userId, characterName, theme)
        achievementViewModel.loadForCharacter(charId)

        journalViewModel.addOrMergeSimpleEntry(
            charId = charId,
            title = "Comienzo de la aventura",
            summary = "$characterName ha iniciado una nueva aventura con temática \"$theme\".",
            type = "story",
            tags = listOf("inicio", "aventura", theme)
        )
    }

    var toastAchievement by remember { mutableStateOf<Achievement?>(null) }
    var toastQuest       by remember { mutableStateOf<Quest?>(null) }

    LaunchedEffect(Unit) {
        launch { achievementViewModel.newAchievement.collect { toastAchievement = it } }
        launch { achievementViewModel.completedQuest.collect  { toastQuest       = it } }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            achievementViewModel.onMessageSent(charId)
        }
    }

    LaunchedEffect(Unit) {
        achievementViewModel.pendingAchievementXp.collect { xp ->
            if (xp > 0) {
                viewModel.addPendingXp(xp)
                achievementViewModel.consumeAchievementXp()
            }
        }
    }

    // ── MONEDAS POR COMPLETAR MISIONES ───────────────────────────────────────
    LaunchedEffect(Unit) {
        achievementViewModel.pendingAchievementCoins.collect { coins ->
            if (coins > 0) {
                inventoryViewModel.addCoins(charId, coins)
                achievementViewModel.consumeAchievementCoins()
            }
        }
    }

    // ── EFECTOS REALES DEL DM (daño + curación + ítems) ─────────────────────
    LaunchedEffect(Unit) {
        viewModel.stepEffect.collect { step ->
            val current = inventoryViewModel.character.value

            if (step.damageTaken > 0 && current != null) {
                val newHp = (current.hpCurrent - step.damageTaken).coerceAtLeast(0)
                inventoryViewModel.updateHp(charId, newHp)

                journalViewModel.addEntry(
                    charId = charId,
                    title = "Has recibido daño",
                    summary = "Has sufrido ${step.damageTaken} puntos de daño.",
                    fullText = "Durante la aventura, $characterName recibió ${step.damageTaken} puntos de daño.",
                    type = "system",
                    tags = listOf("daño", "hp"),
                    hpChange = -step.damageTaken
                )
            }

            if (step.healingReceived > 0 && current != null) {
                val newHp = (current.hpCurrent + step.healingReceived).coerceAtMost(current.hpMax)
                inventoryViewModel.updateHp(charId, newHp)

                journalViewModel.addEntry(
                    charId = charId,
                    title = "Has recuperado salud",
                    summary = "Has recuperado ${step.healingReceived} puntos de vida.",
                    fullText = "Durante la aventura, $characterName recuperó ${step.healingReceived} puntos de vida.",
                    type = "system",
                    tags = listOf("curacion", "hp"),
                    hpChange = step.healingReceived
                )
            }

            step.itemFound?.let { item ->
                inventoryViewModel.addItemToInventory(charId, item)

                journalViewModel.addEntry(
                    charId = charId,
                    title = "Has encontrado un objeto",
                    summary = "Has obtenido ${item.name}.",
                    fullText = buildString {
                        append("$characterName encontró ${item.name}.")
                        if (item.description.isNotBlank()) {
                            append(" ")
                            append(item.description)
                        }
                    },
                    type = "loot",
                    tags = listOf("objeto", "botin", item.type),
                    itemNames = listOf(item.name)
                )
            }

            if (step.coinsFound > 0) {
                val currentLocationState = mapState.locationStates[mapState.currentLocationId]
                val adjustedCoins = adjustCoinsFoundByWorld(step.coinsFound, currentLocationState)

                inventoryViewModel.addCoins(charId, adjustedCoins)

                journalViewModel.addEntry(
                    charId = charId,
                    title = "Has encontrado monedas",
                    summary = "Has conseguido $adjustedCoins monedas de oro.",
                    fullText = "$characterName encontró $adjustedCoins monedas de oro durante la aventura.",
                    type = "loot",
                    tags = listOf("oro", "monedas", "botin"),
                    coinsChange = adjustedCoins
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pendingXp.collect { xp ->
            if (xp > 0) {
                inventoryViewModel.addXp(charId, xp)
                viewModel.consumePendingXp()
            }
        }
    }

    LaunchedEffect(Unit) {
        inventoryViewModel.levelUpEvent.collect { newLevel ->
            levelUpDialogLevel = newLevel
            achievementViewModel.onLevelUp(charId, newLevel)
        }
    }

    // ── DETECTAR COMBATE ──────────────────────────────────────────────────────
    LaunchedEffect(currentStep?.combatStarted) {
        if (currentStep?.combatStarted == true && currentStep?.enemy != null) {
            val enemy = currentStep?.enemy
            if (enemy != null) {
                journalViewModel.addEntry(
                    charId = charId,
                    title = "Comienza un combate",
                    summary = "Te enfrentas a ${enemy.name}.",
                    fullText = "$characterName entra en combate contra ${enemy.name}.",
                    type = "combat",
                    tags = listOf("combate", enemy.name.lowercase()),
                    enemyName = enemy.name
                )
            }
            showPixelTransition = true
        }
    }

    // Scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val isDead = characterState != null && hpCurrent <= 0

    MedievalBackground {
        Box(modifier = Modifier.fillMaxSize()) {

            Scaffold(
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f))) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                MedievalTitle(text = "Aventura: $theme", modifier = Modifier.padding(vertical = 4.dp))
                            }
                            IconButton(onClick = { navController.navigate("bestiary/$charId") }) {
                                Icon(
                                    Icons.Default.AutoStories,
                                    contentDescription = "Bestiario",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            IconButton(onClick = { navController.navigate(Screen.Journal.createRoute(charId)) }) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = "Diario",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            IconButton(onClick = { navController.navigate("inventory/$charId") }) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = "Mochila",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        PlayerStatsHeader(hpCurrent = hpCurrent, hpMax = hpMax)
                    }
                },
                bottomBar = {
                    Surface(tonalElevation = 8.dp, color = Color.Black.copy(alpha = 0.9f)) {
                        Column {
                            if (options.isNotEmpty() && !isLoading) {
                                LazyRow(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(options) { opcion ->
                                        AssistChip(
                                            onClick = { viewModel.sendPlayerAction(opcion) },
                                            label   = { Text(opcion, color = Color.Cyan) },
                                            colors  = AssistChipDefaults.assistChipColors(
                                                labelColor = Color.Cyan, containerColor = Color(0xFF1E1E1E))
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth().navigationBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { navController.navigate("qr_scanner/$charId") }) {
                                    Icon(Icons.Default.QrCodeScanner, "Scan QR", tint = Color.Yellow)
                                }
                                TextField(
                                    value = customAction, onValueChange = { customAction = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("¿Qué quieres hacer?", color = Color.Gray) },
                                    maxLines = 2,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor   = Color(0xFF1E1E1E),
                                        unfocusedContainerColor = Color(0xFF1E1E1E),
                                        focusedTextColor        = Color.White,
                                        unfocusedTextColor      = Color.White,
                                        cursorColor             = Color.Cyan
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (customAction.isNotBlank()) {
                                            val actionText = customAction.trim()

                                            viewModel.sendCustomAction(actionText)

                                            journalViewModel.addEntry(
                                                charId = charId,
                                                title = "Has tomado una decisión",
                                                summary = actionText,
                                                fullText = "$characterName decidió: $actionText",
                                                type = "story",
                                                tags = listOf("decision", "accion")
                                            )

                                            customAction = ""
                                        }
                                    },
                                    enabled = !isLoading && customAction.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Send, "Enviar",
                                        tint = if (customAction.isNotBlank()) Color.Cyan else Color.Gray)
                                }
                            }
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(messages) { GameMessageBubble(author = it.first, text = it.second) }
                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFFFFD700))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }

            // ── BOTÓN FLOTANTE DEL MAPA ──────────────────────────────────────
            // Posicionado sobre la barra inferior, a la izquierda.
            WorldMapFab(
                mapViewModel = mapViewModel,
                modifier     = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 110.dp),
                onOpenAR     = {                                          // ← NUEVO
                    navController.navigate(Screen.ARMap.createRoute(charId))
                }
            )

            // ── TRANSICIÓN AL COMBATE ────────────────────────────────────────
            if (showPixelTransition) {
                PixelTransitionOverlay {
                    showPixelTransition = false
                    AdventureMusicEngine.stopNow()
                    navController.navigate("combat/$charId")
                }
            }

            // ── PANTALLA DE MUERTE ───────────────────────────────────────────
            if (isDead) {
                DeathScreen(
                    onRestart = {
                        inventoryViewModel.resetCharacter(charId)
                        viewModel.resetStory()
                    },
                    onGoHome = {
                        AdventureMusicEngine.stopNow()
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }

            Column(modifier = Modifier.align(Alignment.TopCenter)) {
                AchievementToast(achievement = toastAchievement) { toastAchievement = null }
                QuestCompletedToast(quest = toastQuest) { toastQuest = null }
            }

        } // fin Box

        levelUpDialogLevel?.let { lvl ->
            LevelUpDialog(
                newLevel       = lvl,
                characterClass = characterState?.characterClass ?: "",
                onDismiss      = { levelUpDialogLevel = null }
            )
        }
    } // fin MedievalBackground
}

// ── PANTALLA DE MUERTE ────────────────────────────────────────────────────────

@Composable
private fun DeathScreen(onRestart: () -> Unit, onGoHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("💀", fontSize = 80.sp)
            Text(
                text = "Has caído en batalla",
                color = Color.Red,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tu aventura ha llegado a su fin...\nPero los héroes siempre pueden volver a levantarse.",
                color = Color.LightGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B0000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄  Nueva Historia", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("🏠  Volver al Inicio", fontSize = 16.sp, color = Color.LightGray)
            }
        }
    }
}

// ── BURBUJAS DE CHAT ──────────────────────────────────────────────────────────

@Composable
fun GameMessageBubble(author: String, text: String) {
    val isAI = author == "DM"
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isAI) Alignment.Start else Alignment.End
    ) {
        Text(
            text = author,
            style = MaterialTheme.typography.labelSmall,
            color = if (isAI) Color(0xFFBB86FC) else Color(0xFF03DAC5),
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = if (isAI) Color(0xFF1E1E1E) else Color(0xFF2C2C2C),
            shape = RoundedCornerShape(
                topStart = 12.dp, topEnd = 12.dp,
                bottomStart = if (isAI) 0.dp else 12.dp,
                bottomEnd   = if (isAI) 12.dp else 0.dp
            ),
            tonalElevation = 2.dp
        ) {
            Text(text = text, modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

// ── BARRA DE VIDA ─────────────────────────────────────────────────────────────

@Composable
fun PlayerStatsHeader(hpCurrent: Int, hpMax: Int) {
    Surface(color = Color.Black.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HP", color = Color.Red, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { if (hpMax > 0) hpCurrent.toFloat() / hpMax.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color      = when {
                    hpCurrent <= hpMax * 0.25f -> Color.Red
                    hpCurrent <= hpMax * 0.5f  -> Color(0xFFFF9800)
                    else                        -> Color.Green
                },
                trackColor = Color.DarkGray
            )
        }
    }
}

private fun adjustCoinsFoundByWorld(
    baseCoins: Int,
    state: com.example.aidungeonmaster.data.model.LocationLifeState?
): Int {
    if (state == null) return baseCoins

    var multiplier = 1f

    if (state.prosperity >= 70) multiplier += 0.15f
    if (state.prosperity <= 20) multiplier -= 0.10f

    if (state.danger >= 70) multiplier += 0.20f
    else if (state.danger >= 50) multiplier += 0.10f

    if (state.corruption >= 60) multiplier += 0.10f

    return (baseCoins * multiplier).roundToInt().coerceAtLeast(1)
}