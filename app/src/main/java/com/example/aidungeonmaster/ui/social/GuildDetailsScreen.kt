package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.aidungeonmaster.data.model.GuildChatMessage
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.data.repository.GuildRaidRepository
import com.example.aidungeonmaster.ui.accessibility.VoiceFormAction
import com.example.aidungeonmaster.ui.accessibility.VoiceFormField
import com.example.aidungeonmaster.ui.accessibility.VoiceFormRegistry
import com.example.aidungeonmaster.ui.accessibility.VoiceFormScreen
import com.example.aidungeonmaster.ui.accessibility.VoiceInputType
import com.example.aidungeonmaster.ui.accessibility.findVoiceNamedColor
import com.example.aidungeonmaster.ui.accessibility.guildVoiceColorHelp
import com.example.aidungeonmaster.ui.accessibility.findBestVoiceOption
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_GUILD_MEMBERS = 15

private enum class GuildDetailsScreenTab {
    RESUMEN, CHAT, MIEMBROS, JEFE_FINAL
}

private fun String.toGuildDetailsTab(): GuildDetailsScreenTab {
    return when (trim().lowercase(Locale.getDefault())) {
        "chat" -> GuildDetailsScreenTab.CHAT
        "miembros" -> GuildDetailsScreenTab.MIEMBROS
        "jefe_final", "jefe-final", "jefe final", "raid" -> GuildDetailsScreenTab.JEFE_FINAL
        else -> GuildDetailsScreenTab.RESUMEN
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuildDetailsScreen(
    guildId: String,
    initialTab: String = "resumen",
    onBack: () -> Unit,
    onOpenMemberChat: (String, String, String) -> Unit,
    onOpenBossBattle: (String) -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val guild by viewModel.selectedGuild.collectAsState()
    val members by viewModel.selectedGuildMembers.collectAsState()
    val guildChatMessages by viewModel.selectedGuildChat.collectAsState()
    val isMembersLoading by viewModel.isGuildMembersLoading.collectAsState()
    val isGuildChatLoading by viewModel.isGuildChatLoading.collectAsState()
    val isSendingGuildMessage by viewModel.isSendingGuildMessage.collectAsState()

    val bossRoom by viewModel.guildBossRoom.collectAsState()
    val bossParticipants by viewModel.guildBossParticipants.collectAsState()
    val bossCharacters by viewModel.guildBossCharacters.collectAsState()
    val isGuildBossLoading by viewModel.isGuildBossLoading.collectAsState()
    val isGuildBossActing by viewModel.isGuildBossActing.collectAsState()

    val guildLeaveCompleted by viewModel.guildLeaveCompleted.collectAsState()

    LaunchedEffect(guildLeaveCompleted) {
        if (guildLeaveCompleted) {
            viewModel.consumeGuildLeaveCompleted()
            onBack()
        }
    }

    LaunchedEffect(guildId) {
        viewModel.startGuildsListener()
        viewModel.openGuildDetailsById(guildId)
    }

    BackHandler { onBack() }

    if (guild == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gremio") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("â†") } }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Cargando gremio...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val currentGuild = guild!!
    val banner = guildDetailsParseColor(currentGuild.bannerColor)
    val accent = guildDetailsParseColor(currentGuild.accentColor)
    val currentUserUid = viewModel.currentUserUid()
    val canLeave = viewModel.canLeaveGuild(currentGuild)
    val isOwner = viewModel.isGuildOwner(currentGuild)
    val isFull = currentGuild.memberCount >= MAX_GUILD_MEMBERS

    var selectedTab by remember(currentGuild.id, initialTab) {
        mutableStateOf(initialTab.toGuildDetailsTab())
    }
    var messageText by remember(currentGuild.id) { mutableStateOf("") }
    val chatListState = rememberLazyListState()
    val chatScope = rememberCoroutineScope()

    var hasNavigatedToBossBattle by rememberSaveable(currentGuild.id) { mutableStateOf(false) }
    val userExplicitlyLeftBattle by viewModel.userExplicitlyLeftBattle.collectAsState()

    LaunchedEffect(guildChatMessages.size, selectedTab) {
        if (selectedTab == GuildDetailsScreenTab.CHAT && guildChatMessages.isNotEmpty()) {
            chatScope.launch {
                chatListState.animateScrollToItem(guildChatMessages.lastIndex)
            }
        }
    }

    var lastGuildVoiceFeedback by remember(currentGuild.id) { mutableStateOf("") }

    fun setGuildColorByVoice(value: String, isAccent: Boolean) {
        val color = findVoiceNamedColor(value)

        if (color == null) {
            lastGuildVoiceFeedback = "No reconozco ese color. Puedes decir: ${guildVoiceColorHelp()}."
            return
        }

        val nextAccent = if (isAccent) color.hex else currentGuild.accentColor
        val nextBanner = if (isAccent) currentGuild.bannerColor else color.hex

        viewModel.updateGuildColors(
            guildId = currentGuild.id,
            accentColor = nextAccent,
            bannerColor = nextBanner
        )

        lastGuildVoiceFeedback = if (isAccent) {
            "Color de acento cambiado a ${color.displayName}."
        } else {
            "Color de banner cambiado a ${color.displayName}."
        }
    }

    DisposableEffect(
        currentGuild.id,
        currentGuild.joined,
        isOwner,
        selectedTab,
        currentGuild.accentColor,
        currentGuild.bannerColor,
        messageText,
        isSendingGuildMessage,
        isGuildBossActing,
        bossRoom?.status,
        bossRoom?.bossHpMax,
        bossParticipants.size
    ) {
        val tabActions = listOf(
            VoiceFormAction(
                label = "abrir resumen del gremio",
                aliases = listOf(
                    "abrir resumen del gremio",
                    "abre resumen del gremio",
                    "resumen del gremio",
                    "abre resumen",
                    "ir a resumen"
                ),
                onRun = { selectedTab = GuildDetailsScreenTab.RESUMEN },
                feedback = "Abriendo resumen del gremio."
            ),
            VoiceFormAction(
                label = "abrir chat del gremio",
                aliases = listOf(
                    "abrir chat del gremio",
                    "abre chat del gremio",
                    "chat del gremio",
                    "abre chat",
                    "ir al chat"
                ),
                onRun = { selectedTab = GuildDetailsScreenTab.CHAT },
                feedback = "Abriendo chat del gremio."
            ),
            VoiceFormAction(
                label = "abrir miembros del gremio",
                aliases = listOf(
                    "abrir miembros del gremio",
                    "abre miembros del gremio",
                    "miembros del gremio",
                    "ver miembros",
                    "abre integrantes",
                    "ir a miembros"
                ),
                onRun = { selectedTab = GuildDetailsScreenTab.MIEMBROS },
                feedback = "Abriendo miembros del gremio."
            ),
            VoiceFormAction(
                label = "abrir jefe final del gremio",
                aliases = listOf(
                    "abrir jefe final",
                    "abre jefe final",
                    "jefe final",
                    "boss del gremio",
                    "jefe del gremio",
                    "raid del gremio",
                    "batalla del gremio"
                ),
                onRun = { selectedTab = GuildDetailsScreenTab.JEFE_FINAL },
                feedback = "Abriendo jefe final del gremio."
            )
        )

        val chatFields = if (currentGuild.joined) {
            listOf(
                VoiceFormField(
                    label = "mensaje del gremio",
                    aliases = listOf(
                        "mensaje",
                        "mensaje del gremio",
                        "chat",
                        "escribir en chat",
                        "enviar mensaje"
                    ),
                    inputType = VoiceInputType.TEXT,
                    onValue = { value ->
                        messageText = value
                        lastGuildVoiceFeedback = "Mensaje preparado."
                    },
                    feedback = { lastGuildVoiceFeedback }
                )
            )
        } else {
            emptyList()
        }

        val memberRequestFields = if (currentGuild.joined && members.isNotEmpty()) {
            listOf(
                VoiceFormField(
                    label = "solicitud de amistad",
                    aliases = listOf(
                        "enviar solicitud a",
                        "mandar solicitud a",
                        "agregar amigo",
                        "anadir amigo",
                        "aÃ±adir amigo"
                    ),
                    inputType = VoiceInputType.TEXT,
                    onValue = { value ->
                        val candidates = members
                            .filter { it.uid != currentUserUid }
                        val options = candidates.flatMap { member ->
                            listOf(
                                member.displayName.ifBlank { member.username },
                                member.username,
                                "@${member.username}"
                            )
                        }
                        val selected = findBestVoiceOption(value, options)
                        val targetMember = candidates.firstOrNull { member ->
                            listOf(
                                member.displayName.ifBlank { member.username },
                                member.username,
                                "@${member.username}"
                            ).any { it == selected }
                        }

                        if (targetMember == null) {
                            lastGuildVoiceFeedback = "No encontrÃ© a ese miembro."
                        } else {
                            viewModel.sendFriendRequest(
                                AppUser(
                                    uid = targetMember.uid,
                                    displayName = targetMember.displayName.ifBlank { targetMember.username },
                                    username = targetMember.username
                                )
                            )
                            lastGuildVoiceFeedback = "Enviando solicitud a ${targetMember.displayName.ifBlank { targetMember.username }}."
                        }
                    },
                    feedback = { lastGuildVoiceFeedback }
                )
            )
        } else {
            emptyList()
        }

        val colorFields = if (isOwner) {
            listOf(
                VoiceFormField(
                    label = "color de acento",
                    aliases = listOf(
                        "color de acento",
                        "color principal",
                        "color del gremio",
                        "acento"
                    ),
                    inputType = VoiceInputType.TEXT,
                    onValue = { value -> setGuildColorByVoice(value, isAccent = true) },
                    feedback = { lastGuildVoiceFeedback }
                ),
                VoiceFormField(
                    label = "color de banner",
                    aliases = listOf(
                        "color de banner",
                        "color del banner",
                        "color de fondo",
                        "banner",
                        "fondo"
                    ),
                    inputType = VoiceInputType.TEXT,
                    onValue = { value -> setGuildColorByVoice(value, isAccent = false) },
                    feedback = { lastGuildVoiceFeedback }
                )
            )
        } else {
            emptyList()
        }

        val leaderVoiceFields = if (isOwner) {
            listOf(
                VoiceFormField(
                    label = "nombrar lÃ­der",
                    aliases = listOf(
                        "nombrar lider",
                        "nombrar lÃ­der",
                        "hacer lider a",
                        "hacer lÃ­der a",
                        "dar liderazgo a",
                        "transferir liderazgo a"
                    ),
                    inputType = VoiceInputType.TEXT,
                    onValue = { value ->
                        val candidates = members.filter { it.uid != currentUserUid }
                        val options = candidates.flatMap { member ->
                            listOf(
                                member.displayName.ifBlank { member.username },
                                member.username,
                                "@${member.username}"
                            )
                        }
                        val selected = findBestVoiceOption(value, options)
                        val targetMember = candidates.firstOrNull { member ->
                            listOf(
                                member.displayName.ifBlank { member.username },
                                member.username,
                                "@${member.username}"
                            ).any { it == selected }
                        }

                        if (targetMember == null) {
                            lastGuildVoiceFeedback = "No encontrÃ© a ese miembro para liderazgo."
                        } else {
                            viewModel.transferLeadership(currentGuild, targetMember.uid)
                            lastGuildVoiceFeedback =
                                "Transfiriendo liderazgo a ${targetMember.displayName.ifBlank { targetMember.username }}."
                        }
                    },
                    feedback = { lastGuildVoiceFeedback }
                )
            )
        } else {
            emptyList()
        }

        val myParticipant = bossParticipants.firstOrNull { it.uid == currentUserUid }
        val canReady = currentGuild.joined && myParticipant != null && bossRoom?.status != "battle" && !isGuildBossActing
        val canJoinBattle = bossRoom?.status == "battle" && (bossRoom?.bossHpMax ?: 0) > 0
        val canLeaveRoom = currentGuild.joined && bossRoom?.status != "battle" && !isGuildBossActing
        val canStartBattle = isOwner &&
                (bossRoom?.status == "waiting" || bossRoom?.status == "finished") &&
                bossParticipants.isNotEmpty() &&
                bossParticipants.all {
                    it.ready && it.alive && it.hpCurrent > 0 && it.hpCurrent == it.hpMax
                } &&
                !isGuildBossActing

        val extendedActions = buildList {
            addAll(tabActions)

            add(
                VoiceFormAction(
                    label = "enviar mensaje al gremio",
                    aliases = listOf(
                        "enviar mensaje",
                        "manda mensaje",
                        "publicar mensaje",
                        "enviar al gremio"
                    ),
                    enabled = {
                        currentGuild.joined && selectedTab == GuildDetailsScreenTab.CHAT &&
                                !isSendingGuildMessage && messageText.trim().isNotBlank()
                    },
                    disabledFeedback = "Primero abre el chat y dicta un mensaje.",
                    onRun = {
                        val text = messageText.trim()
                        if (text.isNotBlank()) {
                            viewModel.sendGuildMessage(text)
                            messageText = ""
                        }
                    },
                    feedback = "Mensaje enviado al gremio."
                )
            )

            add(
                VoiceFormAction(
                    label = "unirme a sala de espera",
                    aliases = listOf(
                        "unirme a sala de espera",
                        "unirme a la sala",
                        "entrar a sala de espera",
                        "ponerme listo",
                        "listo"
                    ),
                    enabled = { canReady },
                    disabledFeedback = "No puedes ponerte listo ahora mismo.",
                    onRun = {
                        selectedTab = GuildDetailsScreenTab.JEFE_FINAL
                        viewModel.setBossReady(true)
                    },
                    feedback = "Te he puesto listo en la sala de espera."
                )
            )

            add(
                VoiceFormAction(
                    label = "no listo en sala de espera",
                    aliases = listOf(
                        "no listo",
                        "poner no listo",
                        "quitar listo",
                        "quitar el listo",
                        "desmarcar listo",
                        "no estoy listo"
                    ),
                    enabled = { canReady },
                    disabledFeedback = "No puedes cambiar a no listo ahora mismo.",
                    onRun = {
                        selectedTab = GuildDetailsScreenTab.JEFE_FINAL
                        viewModel.setBossReady(false)
                    },
                    feedback = "Te he marcado como no listo."
                )
            )

            add(
                VoiceFormAction(
                    label = "entrar en pelea del gremio",
                    aliases = listOf(
                        "entrar en pelea",
                        "unirme a la pelea",
                        "abrir pelea del gremio"
                    ),
                    enabled = { currentGuild.joined && canJoinBattle },
                    disabledFeedback = "La pelea no estÃ¡ disponible ahora.",
                    onRun = { onOpenBossBattle(currentGuild.id) },
                    feedback = "Entrando en la pelea del gremio."
                )
            )

            if (isOwner) {
                add(
                    VoiceFormAction(
                        label = "empezar pelea del gremio",
                        aliases = listOf(
                            "empezar pelea",
                            "iniciar pelea",
                            "comenzar pelea del gremio"
                        ),
                        enabled = { canStartBattle },
                        disabledFeedback = "AÃºn no se puede empezar la pelea.",
                        onRun = {
                            selectedTab = GuildDetailsScreenTab.JEFE_FINAL
                            viewModel.startBossBattle()
                        },
                        feedback = "Iniciando pelea del gremio."
                    )
                )
            }

            add(
                VoiceFormAction(
                    label = "salir de sala de espera",
                    aliases = listOf(
                        "salir de sala",
                        "abandonar sala",
                        "dejar sala de espera"
                    ),
                    enabled = { canLeaveRoom },
                    disabledFeedback = "No puedes salir de la sala ahora mismo.",
                    onRun = {
                        selectedTab = GuildDetailsScreenTab.JEFE_FINAL
                        viewModel.leaveBossRoom()
                    },
                    feedback = "Saliendo de la sala de espera."
                )
            )

            if (canLeave) {
                add(
                    VoiceFormAction(
                        label = "salir del gremio",
                        aliases = listOf(
                            "salir del gremio",
                            "abandonar gremio",
                            "dejar gremio"
                        ),
                        onRun = { viewModel.leaveGuild(currentGuild) },
                        feedback = "Saliendo del gremio."
                    )
                )
            }

            add(
                VoiceFormAction(
                    label = "enviar solicitud de amistad",
                    aliases = listOf(
                        "enviar solicitud de amistad",
                        "mandar solicitud de amistad"
                    ),
                    enabled = {
                        currentGuild.joined &&
                                selectedTab == GuildDetailsScreenTab.MIEMBROS &&
                                members.count { it.uid != currentUserUid } == 1
                    },
                    disabledFeedback = "Di el nombre del miembro. Por ejemplo: enviar solicitud a Luna.",
                    onRun = {
                        val candidate = members.firstOrNull { it.uid != currentUserUid } ?: return@VoiceFormAction
                        viewModel.sendFriendRequest(
                            AppUser(
                                uid = candidate.uid,
                                displayName = candidate.displayName.ifBlank { candidate.username },
                                username = candidate.username
                            )
                        )
                    },
                    feedback = "Enviando solicitud de amistad."
                )
            )
        }

        val registration = VoiceFormRegistry.register(
            VoiceFormScreen(
                screenName = "detalle de gremio",
                fields = colorFields + chatFields + memberRequestFields + leaderVoiceFields,
                actions = extendedActions
            )
        )

        onDispose { registration.dispose() }
    }

    LaunchedEffect(currentGuild.id, bossRoom?.status, bossRoom?.currentTurnUid) {
        if (
            bossRoom?.status == "battle" &&
            bossRoom?.currentTurnUid == GuildRaidRepository.BOSS_TURN_UID
        ) {
            viewModel.resolveBossTurnIfNeeded()
        }
    }

    LaunchedEffect(
        currentGuild.id,
        bossRoom?.status,
        bossRoom?.bossHpMax,
        bossRoom?.updatedAt,
        bossParticipants.size
    ) {
        val myUid = currentUserUid
        val amParticipant = bossParticipants.any { it.uid == myUid }
        val amOwner = currentGuild.ownerUid == myUid

        // FIX NAVEGACIÃ“N: no redirigimos si el usuario acaba de pulsar "Volver"
        val shouldEnterBattle =
            bossRoom?.status == "battle" &&
                    (bossRoom?.bossHpMax ?: 0) > 0 &&
                    (amParticipant || amOwner) &&
                    !userExplicitlyLeftBattle

        if (shouldEnterBattle && !hasNavigatedToBossBattle) {
            hasNavigatedToBossBattle = true
            onOpenBossBattle(currentGuild.id)
        }

        // Limpiar flags cuando la batalla termina
        if (bossRoom?.status != "battle") {
            hasNavigatedToBossBattle = false
            viewModel.clearUserLeftBattle()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentGuild.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (currentGuild.joined) "PÃ¡gina del gremio" else "Vista previa del gremio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("â†") }
                },
                actions = {
                    when {
                        !currentGuild.joined -> {
                            Button(
                                onClick = { viewModel.joinGuild(currentGuild) },
                                enabled = !isFull
                            ) {
                                Text(if (isFull) "Completo" else "Unirme")
                            }
                        }
                        canLeave -> {
                            TextButton(onClick = { viewModel.leaveGuild(currentGuild) }) {
                                Text("Salir")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = banner)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(banner, accent.copy(alpha = 0.88f))
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        currentGuild.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        currentGuild.description.ifBlank { "Sin descripciÃ³n todavÃ­a." },
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GuildDetailsMiniBadge(
                                text = "${currentGuild.memberCount}/$MAX_GUILD_MEMBERS miembros",
                                background = Color.White.copy(alpha = 0.18f),
                                content = Color.White
                            )

                            if (currentGuild.joined) {
                                GuildDetailsMiniBadge(
                                    text = "Mi gremio",
                                    background = Color.White.copy(alpha = 0.14f),
                                    content = Color.White
                                )
                            }
                        }

                        GuildDetailsMiniBadge(
                            text = "Líder: ${currentGuild.ownerDisplayName.ifBlank { "?" }}",
                            background = Color.Black.copy(alpha = 0.16f),
                            content = Color.White
                        )

                        if (!currentGuild.joined) {
                            GuildDetailsMiniBadge(
                                text = if (isFull) "Completo" else "Explorar",
                                background = Color.White.copy(alpha = 0.14f),
                                content = Color.White
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!currentGuild.joined) {
                            Button(
                                onClick = { viewModel.joinGuild(currentGuild) },
                                enabled = !isFull,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.White.copy(alpha = 0.55f),
                                    disabledContentColor = Color.Black.copy(alpha = 0.7f)
                                )
                            ) {
                                Text(if (isFull) "Gremio completo" else "Unirme al gremio")
                            }
                        } else {
                            OutlinedButton(onClick = { selectedTab = GuildDetailsScreenTab.CHAT }) {
                                Text("Abrir chat")
                            }
                            OutlinedButton(onClick = { selectedTab = GuildDetailsScreenTab.MIEMBROS }) {
                                Text("Ver miembros")
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GuildDetailsTabChip(
                    title = "Resumen",
                    selected = selectedTab == GuildDetailsScreenTab.RESUMEN,
                    accent = accent,
                    onClick = { selectedTab = GuildDetailsScreenTab.RESUMEN }
                )
                GuildDetailsTabChip(
                    title = "Chat",
                    selected = selectedTab == GuildDetailsScreenTab.CHAT,
                    accent = accent,
                    onClick = { selectedTab = GuildDetailsScreenTab.CHAT }
                )
                GuildDetailsTabChip(
                    title = "Miembros",
                    selected = selectedTab == GuildDetailsScreenTab.MIEMBROS,
                    accent = accent,
                    onClick = { selectedTab = GuildDetailsScreenTab.MIEMBROS }
                )
                GuildDetailsTabChip(
                    title = "Jefe final",
                    selected = selectedTab == GuildDetailsScreenTab.JEFE_FINAL,
                    accent = accent,
                    onClick = { selectedTab = GuildDetailsScreenTab.JEFE_FINAL }
                )
            }

            when (selectedTab) {
                GuildDetailsScreenTab.RESUMEN -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Vista general",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GuildDetailsInfoPill(
                                    label = "Miembros",
                                    value = "${currentGuild.memberCount}/$MAX_GUILD_MEMBERS"
                                )
                                GuildDetailsInfoPill(
                                    label = "LÃ­der",
                                    value = currentGuild.ownerDisplayName.ifBlank { "?" }
                                )
                            }

                            Text(
                                if (currentGuild.joined) {
                                    "Desde aquÃ­ puedes hablar con el gremio, revisar sus miembros y abrir chats privados con ellos."
                                } else {
                                    "Puedes revisar el gremio y sus integrantes. Si te unes, desbloquearÃ¡s el chat del gremio y el acceso rÃ¡pido a los chats privados."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider()

                            Text(
                                "Integrantes destacados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (isMembersLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (members.isEmpty()) {
                                GuildDetailsEmptyBlock("No hay integrantes visibles todavÃ­a.")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    members.take(4).forEach { member ->
                                        GuildDetailsMemberRow(
                                            member = member,
                                            accentColor = accent,
                                            canPromote = false,
                                            canOpenChat = currentGuild.joined && member.uid != currentUserUid,
                                            onPromote = {},
                                            onOpenChat = {
                                                onOpenMemberChat(
                                                    member.uid,
                                                    member.displayName.ifBlank { member.username },
                                                    currentGuild.id
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                GuildDetailsScreenTab.CHAT -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Chat del gremio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (!currentGuild.joined) {
                                GuildDetailsEmptyBlock("Debes unirte al gremio para leer y escribir en el chat.")
                            } else {
                                if (isGuildChatLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (guildChatMessages.isEmpty()) {
                                    GuildDetailsEmptyBlock("TodavÃ­a no hay mensajes. Rompe el hielo.")
                                } else {
                                    LazyColumn(
                                        state = chatListState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp, max = 420.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(guildChatMessages, key = { it.id }) { msg ->
                                            GuildDetailsChatBubble(
                                                message = msg,
                                                isMine = msg.senderUid == currentUserUid
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    OutlinedTextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Escribe al gremio...") },
                                        minLines = 1,
                                        maxLines = 4
                                    )

                                    Button(
                                        onClick = {
                                            val text = messageText.trim()
                                            if (text.isNotBlank()) {
                                                viewModel.sendGuildMessage(text)
                                                messageText = ""
                                            }
                                        },
                                        enabled = !isSendingGuildMessage
                                    ) {
                                        Text(if (isSendingGuildMessage) "..." else "Enviar")
                                    }
                                }
                            }
                        }
                    }
                }

                GuildDetailsScreenTab.MIEMBROS -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Miembros del gremio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Pulsa sobre un miembro para abrir chat privado con Ã©l. Si eres lÃ­der, tambiÃ©n puedes transferir el liderazgo.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isMembersLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (members.isEmpty()) {
                                GuildDetailsEmptyBlock("No hay miembros para mostrar.")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    members.forEach { member ->
                                        GuildDetailsMemberRow(
                                            member = member,
                                            accentColor = accent,
                                            canPromote = isOwner && member.uid != currentUserUid,
                                            canOpenChat = currentGuild.joined && member.uid != currentUserUid,
                                            onPromote = {
                                                viewModel.transferLeadership(currentGuild, member.uid)
                                            },
                                            onOpenChat = {
                                                onOpenMemberChat(
                                                    member.uid,
                                                    member.displayName.ifBlank { member.username },
                                                    currentGuild.id
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                GuildDetailsScreenTab.JEFE_FINAL -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Jefe final del gremio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (!currentGuild.joined) {
                                GuildDetailsEmptyBlock("Debes unirte al gremio para participar.")
                            } else {
                                val myUid = currentUserUid
                                val myParticipant = bossParticipants.firstOrNull { it.uid == myUid }
                                val allParticipantsReady = bossParticipants.isNotEmpty() && bossParticipants.all { it.ready }
                                val canStartBattle = isOwner &&
                                        (bossRoom?.status == "waiting" || bossRoom?.status == "finished") &&
                                        bossParticipants.isNotEmpty() &&
                                        bossParticipants.all {
                                            it.ready && it.alive && it.hpCurrent > 0 && it.hpCurrent == it.hpMax
                                        } &&
                                        !isGuildBossActing

                                val roomStatusText = when (bossRoom?.status) {
                                    "battle" -> "Batalla en curso"
                                    "finished" -> "Batalla terminada"
                                    else -> "Sala de espera"
                                }

                                val turnLabel = when (bossRoom?.currentTurnUid) {
                                    GuildRaidRepository.BOSS_TURN_UID -> "Turno del jefe"
                                    myUid -> "Es tu turno"
                                    null, "" -> "Sin turno activo"
                                    else -> "Turno de otro miembro"
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            turnLabel,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            "Estado: $roomStatusText",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (bossRoom?.status == "battle") {
                                            Text(
                                                "Ronda ${bossRoom?.round ?: 1}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                "Jefe: ${bossRoom?.bossName.orEmpty()}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                "HP ${bossRoom?.bossHpCurrent ?: 0}/${bossRoom?.bossHpMax ?: 0}",
                                                color = accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (bossRoom?.winner == "guild") {
                                            Text(
                                                "Victoria del gremio",
                                                color = accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else if (bossRoom?.winner == "boss") {
                                            Text(
                                                "El jefe ha ganado",
                                                color = Color.Red,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    "1) Elige personaje. 2) Pulsa listo. 3) El lÃ­der inicia la pelea cuando todos estÃ©n listos.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider()

                                Text(
                                    "Tus personajes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (isGuildBossLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (bossCharacters.isEmpty()) {
                                    GuildDetailsEmptyBlock("No tienes personajes disponibles.")
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        bossCharacters.forEach { character ->
                                            val isSelected = myParticipant?.selectedCharacterDocId == character.id

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(
                                                        enabled = bossRoom?.status != "battle" && !isGuildBossActing
                                                    ) {
                                                        viewModel.selectBossCharacter(character)
                                                    },
                                                shape = RoundedCornerShape(18.dp),
                                                color = if (isSelected) {
                                                    accent.copy(alpha = 0.18f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) accent
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            character.name,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )

                                                        if (isSelected) {
                                                            Text(
                                                                "Seleccionado",
                                                                color = accent,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        "${character.race} â€¢ ${character.characterClass}",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    Text(
                                                        "Nivel ${character.level} â€¢ HP ${character.hpMax}",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider()

                                Text(
                                    "Participantes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (bossParticipants.isEmpty()) {
                                    GuildDetailsEmptyBlock("AÃºn no hay nadie en la sala.")
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        bossParticipants.forEach { participant ->
                                            val isMe = participant.uid == myUid

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isMe) accent.copy(alpha = 0.45f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                buildString {
                                                                    append(participant.displayName.ifBlank { participant.username })
                                                                    if (isMe) append(" (TÃº)")
                                                                },
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                "${participant.selectedCharacterName} â€¢ ${participant.selectedCharacterClass}",
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        GuildDetailsMiniBadge(
                                                            text = when {
                                                                !participant.alive -> "KO"
                                                                participant.ready -> "Listo"
                                                                else -> "Esperando"
                                                            },
                                                            background = when {
                                                                !participant.alive -> Color.Red.copy(alpha = 0.18f)
                                                                participant.ready -> accent.copy(alpha = 0.18f)
                                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                                            },
                                                            content = if (!participant.alive) Color.Red else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Text(
                                                        "HP ${participant.hpCurrent}/${participant.hpMax}",
                                                        color = if (participant.alive) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        } else {
                                                            Color.Red
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider()

                                if (bossRoom?.status == "waiting" || bossRoom?.status == "finished") {
                                    Text(
                                        when {
                                            bossRoom?.status == "finished" && canStartBattle ->
                                                "La pelea anterior ha terminado. Todos han vuelto a pulsar Listo y el lÃ­der ya puede iniciar otra."
                                            bossRoom?.status == "finished" ->
                                                "La pelea anterior ha terminado. Cada participante debe pulsar Listo otra vez para restaurar su estado."
                                            allParticipantsReady ->
                                                "Todos los participantes estÃ¡n listos. El lÃ­der puede iniciar la pelea."
                                            else ->
                                                "TodavÃ­a no estÃ¡n todos listos."
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setBossReady(true) },
                                        enabled = myParticipant != null &&
                                                bossRoom?.status != "battle" &&
                                                !isGuildBossActing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Listo")
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.setBossReady(false) },
                                        enabled = myParticipant != null &&
                                                bossRoom?.status != "battle" &&
                                                !isGuildBossActing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("No listo")
                                    }
                                }

                                if (isOwner && (bossRoom?.status == "waiting" || bossRoom?.status == "finished")) {
                                    Button(
                                        onClick = { viewModel.startBossBattle() },
                                        enabled = canStartBattle,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (bossRoom?.status == "finished") "Empezar otra pelea" else "Empezar pelea")
                                    }
                                }

                                if (bossRoom?.status == "battle" && (bossRoom?.bossHpMax ?: 0) > 0) {
                                    Button(
                                        onClick = { onOpenBossBattle(currentGuild.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Entrar en pelea")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.leaveBossRoom() },
                                        enabled = bossRoom?.status != "battle" && !isGuildBossActing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Salir de sala")
                                    }

                                    if (isOwner) {
                                        OutlinedButton(
                                            onClick = { viewModel.resetBossRoom() },
                                            enabled = !isGuildBossActing,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reiniciar")
                                        }
                                    }
                                }

                                if (!bossRoom?.battleLog.isNullOrEmpty()) {
                                    HorizontalDivider()

                                    Text(
                                        "Registro de batalla",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        bossRoom!!.battleLog.takeLast(12).forEach { line ->
                                            Text(
                                                line,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuildDetailsMemberRow(
    member: GuildMemberSummary,
    accentColor: Color,
    canPromote: Boolean,
    canOpenChat: Boolean,
    onPromote: () -> Unit,
    onOpenChat: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuildDetailsMemberAvatar(
                photoUrl = member.photoUrl,
                displayName = member.displayName.ifBlank { member.username },
                size = 52.dp,
                accent = accentColor
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.displayName.ifBlank { member.username },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "@${member.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (member.role.isNotBlank()) {
                    Text(
                        when (member.role.lowercase()) {
                            "owner" -> "LÃ­der"
                            else -> "Miembro"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canOpenChat) {
                    OutlinedButton(onClick = onOpenChat) {
                        Text("Chat")
                    }
                }
                if (canPromote) {
                    Button(onClick = onPromote) {
                        Text("Liderar")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuildDetailsChatBubble(
    message: GuildChatMessage,
    isMine: Boolean
) {
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val avatarAccent = if (isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            SocialUserAvatar(
                photoUrl = message.senderPhotoUrl,
                displayName = message.senderDisplayName.ifBlank { "Jugador" },
                size = 38.dp,
                accent = avatarAccent
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 18.dp
            ),
            color = bubbleColor,
            tonalElevation = 1.dp,
            modifier = Modifier
                .width(280.dp)
                .fillMaxWidth(0.88f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    message.senderDisplayName.ifBlank { "Jugador" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Text(
                    guildDetailsFormatTimestamp(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        if (isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            SocialUserAvatar(
                photoUrl = message.senderPhotoUrl,
                displayName = message.senderDisplayName.ifBlank { "Jugador" },
                size = 38.dp,
                accent = avatarAccent
            )
        }
    }
}

@Composable
private fun GuildDetailsTabChip(
    title: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GuildDetailsMiniBadge(
    text: String,
    background: Color,
    content: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun GuildDetailsInfoPill(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GuildDetailsEmptyBlock(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun GuildDetailsMemberAvatar(
    photoUrl: String,
    displayName: String,
    size: Dp,
    accent: Color
) {
    SocialUserAvatar(
        photoUrl = photoUrl,
        displayName = displayName,
        size = size,
        accent = accent
    )
}

private fun guildDetailsFormatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}

private fun guildDetailsParseColor(value: String?): Color {
    return try {
        if (value.isNullOrBlank()) Color(0xFF6750A4)
        else Color(android.graphics.Color.parseColor(value))
    } catch (_: Exception) {
        Color(0xFF6750A4)
    }
}



