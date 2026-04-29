package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.GuildBossAbility
import com.example.aidungeonmaster.data.model.GuildBossAbilityType
import com.example.aidungeonmaster.data.model.GuildBossParticipant
import com.example.aidungeonmaster.data.model.GuildBossRoom
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.data.model.guildBossAbilitiesForClass
import com.example.aidungeonmaster.data.repository.GuildRaidRepository
import com.example.aidungeonmaster.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuildBossBattleScreen(
    guildId: String,
    onBack: () -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val guild by viewModel.selectedGuild.collectAsState()
    val bossRoom by viewModel.guildBossRoom.collectAsState()
    val bossParticipants by viewModel.guildBossParticipants.collectAsState()
    val isGuildBossActing by viewModel.isGuildBossActing.collectAsState()
    val bossConsumables by viewModel.guildBossConsumables.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserUid = viewModel.currentUserUid()
    val myParticipant = bossParticipants.firstOrNull { it.uid == currentUserUid }
    val isOwner = guild?.let { viewModel.isGuildOwner(it) } == true
    val listState = rememberLazyListState()

    var initialized by rememberSaveable { mutableStateOf(false) }

    // FIX NAVEGACIÓN: al salir marcamos el flag para que GuildDetailsScreen
    // no vuelva a redirigir automáticamente al usuario a la batalla.
    val handleBack: () -> Unit = {
        viewModel.markUserLeftBattle()
        onBack()
    }

    LaunchedEffect(guildId) {
        if (!initialized) {
            initialized = true
            viewModel.startGuildsListener()
            viewModel.openGuildDetailsById(guildId)
            viewModel.refreshBossConsumables()
        }
    }

    // BUG FIX TURNO DEL JEFE: El líder resuelve el turno del jefe automáticamente
    LaunchedEffect(bossRoom?.status, bossRoom?.currentTurnUid) {
        if (
            bossRoom?.status == "battle" &&
            bossRoom?.currentTurnUid == GuildRaidRepository.BOSS_TURN_UID
        ) {
            viewModel.resolveBossTurnIfNeeded()
        }
    }

    LaunchedEffect(bossRoom?.battleLog?.size) {
        val size = bossRoom?.battleLog?.size ?: 0
        if (size > 0) {
            listState.animateScrollToItem(size - 1)
        }
    }

    // Mostrar mensajes de error/info
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    // BUG FIX NAVEGACIÓN: BackHandler llama a handleBack para marcar la salida
    BackHandler { handleBack() }

    val backgroundBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF1A0B0B),
            Color(0xFF090909)
        )
    )

    val room = bossRoom
    val isMyTurn = room?.status == "battle" &&
            room.currentTurnUid == currentUserUid &&
            myParticipant?.alive == true

    // Habilidades del personaje del jugador actual
    val myAbilities = remember(myParticipant?.selectedCharacterClass) {
        guildBossAbilitiesForClass(myParticipant?.selectedCharacterClass ?: "")
            .filter { it.type != GuildBossAbilityType.SPECIAL_FLEE }
    }

    // Cooldowns actuales
    val myCooldowns = myParticipant?.cooldowns ?: emptyMap()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Jefe final")
                        Text(
                            guild?.name ?: "Gremio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    // FIX NAVEGACIÓN: usa handleBack para marcar la salida explícita
                    TextButton(onClick = handleBack) {
                        Text("←")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            when {
                room == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD54F))
                            Text(
                                "Cargando sala del jefe...",
                                color = Color.White
                            )
                        }
                    }
                }

                room.status == "finished" -> {
                    FinishedBossBattleContent(
                        room = room,
                        participants = bossParticipants,
                        currentUserUid = currentUserUid,
                        onBack = handleBack
                    )
                }

                room.status != "battle" || room.bossHpMax <= 0 -> {
                    WaitingBossBattleContent(
                        room = room,
                        participants = bossParticipants,
                        myParticipant = myParticipant,
                        isOwner = isOwner,
                        onBack = handleBack
                    )
                }

                else -> {
                    ActiveBossBattleContent(
                        room = room,
                        participants = bossParticipants,
                        currentUserUid = currentUserUid,
                        isMyTurn = isMyTurn,
                        isOwner = isOwner,
                        isGuildBossActing = isGuildBossActing,
                        myAbilities = myAbilities,
                        myCooldowns = myCooldowns,
                        myConsumables = bossConsumables,
                        listState = listState,
                        onAttack = { viewModel.performBossAttack() },
                        onUseAbility = { ability -> viewModel.useBossAbility(ability) },
                        onUseConsumable = { item -> viewModel.useBossConsumable(item) },
                        onForceEnd = { viewModel.forceEndBattle() },
                        onBack = handleBack
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingBossBattleContent(
    room: GuildBossRoom,
    participants: List<GuildBossParticipant>,
    myParticipant: GuildBossParticipant?,
    isOwner: Boolean,
    onBack: () -> Unit
) {
    val allReady = participants.isNotEmpty() && participants.all { it.ready }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFFFFD54F))

            Text(
                "Esperando que comience la batalla...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Estado de la sala",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Participantes: ${participants.size}",
                        color = Color.White
                    )

                    Text(
                        when {
                            myParticipant == null -> "Todavía no te has unido a la pelea con un personaje."
                            myParticipant.ready -> "Tu personaje está listo."
                            else -> "Has elegido personaje, pero aún no estás listo."
                        },
                        color = Color.White
                    )

                    Text(
                        when {
                            participants.isEmpty() -> "Aún no hay participantes."
                            allReady && isOwner -> "Todos están listos. Como líder, ya puedes iniciar la pelea desde la pantalla del gremio."
                            allReady -> "Todos están listos. Esperando a que el líder inicie la pelea."
                            else -> "Falta que todos los participantes marquen listo."
                        },
                        color = Color.White
                    )

                    if (room.battleLog.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            room.battleLog.last(),
                            color = Color.LightGray
                        )
                    }
                }
            }

            OutlinedButton(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}

@Composable
private fun FinishedBossBattleContent(
    room: GuildBossRoom,
    participants: List<GuildBossParticipant>,
    currentUserUid: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF221111)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (room.winner == "guild") "🏆 Victoria del gremio" else "☠️ El jefe ha ganado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (room.winner == "guild") Color(0xFFFFD54F) else Color.Red
                )

                Text(
                    room.bossName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                if (room.battleLog.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = room.battleLog.last(),
                            modifier = Modifier.padding(12.dp),
                            color = Color.White
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            "Estado final del grupo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(participants) { participant ->
                ParticipantCard(
                    participant = participant,
                    currentUserUid = currentUserUid,
                    currentTurnUid = "",
                    showReadyState = false
                )
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 12.dp)
        ) {
            Text("Volver al gremio")
        }
    }
}

@Composable
private fun ActiveBossBattleContent(
    room: GuildBossRoom,
    participants: List<GuildBossParticipant>,
    currentUserUid: String,
    isMyTurn: Boolean,
    isOwner: Boolean,
    isGuildBossActing: Boolean,
    myAbilities: List<GuildBossAbility>,
    myCooldowns: Map<String, Int>,
    myConsumables: List<Item>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAttack: () -> Unit,
    onUseAbility: (GuildBossAbility) -> Unit,
    onUseConsumable: (Item) -> Unit,
    onForceEnd: () -> Unit,
    onBack: () -> Unit
) {
    val hpProgress = if (room.bossHpMax <= 0) {
        0f
    } else {
        (room.bossHpCurrent.toFloat() / room.bossHpMax.toFloat()).coerceIn(0f, 1f)
    }

    // Estado de expansión de paneles
    var showAbilities by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- Panel del Jefe ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF221111)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    room.bossName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF3A3A3A)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(hpProgress)
                                .height(22.dp)
                                .background(Color(0xFFD32F2F))
                        )
                    }
                }

                Text(
                    "HP ${room.bossHpCurrent}/${room.bossHpMax}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    when {
                        room.currentTurnUid == GuildRaidRepository.BOSS_TURN_UID -> "Turno del jefe"
                        room.currentTurnUid == currentUserUid -> "Es tu turno"
                        else -> "Turno de otro miembro"
                    },
                    color = Color.White
                )

                Text(
                    "Ronda ${room.round}",
                    color = Color.LightGray
                )
            }
        }

        HorizontalDivider()

        // --- Log y participantes ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Registro de batalla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(room.battleLog) { line ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = line,
                        modifier = Modifier.padding(12.dp),
                        color = Color.White
                    )
                }
            }

            item {
                Text(
                    "Grupo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(participants) { participant ->
                ParticipantCard(
                    participant = participant,
                    currentUserUid = currentUserUid,
                    currentTurnUid = room.currentTurnUid,
                    showReadyState = false
                )
            }
        }

        HorizontalDivider()

        // --- Panel de acciones ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                isMyTurn -> {
                    // BUG FIX ATAQUE: El botón de ataque estaba correctamente configurado
                    // pero la función performBossAttack() devolvía sin hacer nada si
                    // _selectedGuild.value era null. Ahora el LaunchedEffect inicializa
                    // correctamente el selectedGuild mediante openGuildDetailsById.
                    Button(
                        onClick = onAttack,
                        enabled = !isGuildBossActing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD54F),
                            contentColor = Color.Black
                        )
                    ) {
                        if (isGuildBossActing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("⚔️ Atacar al jefe", fontWeight = FontWeight.Bold)
                        }
                    }

                    // --- Botón habilidades ---
                    if (myAbilities.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                showAbilities = !showAbilities
                                if (showAbilities) showItems = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGuildBossActing
                        ) {
                            Text(
                                if (showAbilities) "✨ Ocultar habilidades" else "✨ Habilidades (${myAbilities.size})",
                                color = Color(0xFFFFD54F)
                            )
                        }

                        AnimatedVisibility(
                            visible = showAbilities,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            AbilitiesPanel(
                                abilities = myAbilities,
                                cooldowns = myCooldowns,
                                isActing = isGuildBossActing,
                                onUseAbility = {
                                    onUseAbility(it)
                                    showAbilities = false
                                }
                            )
                        }
                    }

                    // --- Botón objetos ---
                    if (myConsumables.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                showItems = !showItems
                                if (showItems) showAbilities = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGuildBossActing
                        ) {
                            Text(
                                if (showItems) "🎒 Ocultar objetos" else "🎒 Objetos (${myConsumables.size})",
                                color = Color(0xFF90CAF9)
                            )
                        }

                        AnimatedVisibility(
                            visible = showItems,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            ConsumablesPanel(
                                items = myConsumables,
                                isActing = isGuildBossActing,
                                onUseItem = {
                                    onUseConsumable(it)
                                    showItems = false
                                }
                            )
                        }
                    }
                }

                room.currentTurnUid == GuildRaidRepository.BOSS_TURN_UID -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "El jefe está actuando. El líder resolverá este turno automáticamente.",
                            modifier = Modifier.padding(14.dp),
                            color = Color.White
                        )
                    }
                }

                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "Espera tu turno para actuar.",
                            modifier = Modifier.padding(14.dp),
                            color = Color.White
                        )
                    }
                }
            }

            // Botón "Terminar pelea" solo visible para el líder durante batalla activa
            if (isOwner) {
                OutlinedButton(
                    onClick = onForceEnd,
                    enabled = !isGuildBossActing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("🚩 Terminar pelea", fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }
}

// --- Panel de habilidades expandible ---
@Composable
private fun AbilitiesPanel(
    abilities: List<GuildBossAbility>,
    cooldowns: Map<String, Int>,
    isActing: Boolean,
    onUseAbility: (GuildBossAbility) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Habilidades de clase",
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            abilities.forEach { ability ->
                val cooldown = cooldowns[ability.id] ?: 0
                val onCooldown = cooldown > 0
                AbilityButton(
                    ability = ability,
                    cooldownTurns = cooldown,
                    isActing = isActing || onCooldown,
                    onClick = { onUseAbility(ability) }
                )
            }
        }
    }
}

@Composable
private fun AbilityButton(
    ability: GuildBossAbility,
    cooldownTurns: Int,
    isActing: Boolean,
    onClick: () -> Unit
) {
    val onCooldown = cooldownTurns > 0
    val typeColor = when (ability.type) {
        GuildBossAbilityType.DAMAGE -> Color(0xFFEF5350)
        GuildBossAbilityType.HEAL -> Color(0xFF66BB6A)
        GuildBossAbilityType.BUFF_DEFENSE -> Color(0xFF42A5F5)
        GuildBossAbilityType.BUFF_ATTACK -> Color(0xFFFFCA28)
        GuildBossAbilityType.SPECIAL_FLEE -> Color.Gray
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActing, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (onCooldown)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        else
            typeColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${ability.emoji} ${ability.name}",
                    color = if (onCooldown) Color.Gray else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    ability.description,
                    color = if (onCooldown) Color.Gray else Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onCooldown) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3A3A3A)
                ) {
                    Text(
                        "$cooldownTurns",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.25f)
                ) {
                    Text(
                        ability.diceExpression,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = typeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- Panel de objetos consumibles ---
@Composable
private fun ConsumablesPanel(
    items: List<Item>,
    isActing: Boolean,
    onUseItem: (Item) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0D1B2A)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Objetos consumibles",
                color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            items.forEach { item ->
                ConsumableButton(
                    item = item,
                    isActing = isActing,
                    onClick = { onUseItem(item) }
                )
            }
        }
    }
}

@Composable
private fun ConsumableButton(
    item: Item,
    isActing: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActing, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF90CAF9).copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🧪 ${item.name}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (item.description.isNotBlank()) {
                    Text(
                        item.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF90CAF9).copy(alpha = 0.2f)
            ) {
                Text(
                    "Usar",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ParticipantCard(
    participant: GuildBossParticipant,
    currentUserUid: String,
    currentTurnUid: String,
    showReadyState: Boolean
) {
    val isMe = participant.uid == currentUserUid
    val stateText = when {
        !participant.alive -> "KO"
        showReadyState && participant.ready -> "Listo"
        showReadyState && !participant.ready -> "Esperando"
        currentTurnUid == participant.uid -> "Turno"
        else -> "Esperando"
    }

    val stateColor = when {
        !participant.alive -> Color.Red
        currentTurnUid == participant.uid -> Color(0xFFFFD54F)
        participant.ready -> Color(0xFF66BB6A)
        else -> Color.LightGray
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                buildString {
                    append(participant.displayName.ifBlank { participant.username })
                    if (isMe) append(" (Tú)")
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                "${participant.selectedCharacterName} • ${participant.selectedCharacterClass}",
                color = Color.LightGray
            )

            // Barra de vida del participante
            val hpFraction = if (participant.hpMax <= 0) 0f
            else (participant.hpCurrent.toFloat() / participant.hpMax.toFloat()).coerceIn(0f, 1f)
            val hpColor = when {
                !participant.alive -> Color.Red
                hpFraction < 0.3f -> Color(0xFFFF7043)
                hpFraction < 0.6f -> Color(0xFFFFCA28)
                else -> Color(0xFF66BB6A)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF3A3A3A)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(hpFraction)
                                .height(8.dp)
                                .background(hpColor)
                        )
                    }
                }
                Text(
                    "HP ${participant.hpCurrent}/${participant.hpMax}",
                    color = hpColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Cooldowns activos (solo si hay alguno)
            if (participant.cooldowns.isNotEmpty()) {
                Text(
                    "Recargas: ${participant.cooldowns.entries.joinToString(" | ") { "${it.key}: ${it.value}t" }}",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp
                )
            }

            Text(
                stateText,
                color = stateColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}