package com.example.aidungeonmaster.ui.social

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildChatMessage
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.BorderStroke

private enum class GuildDetailsTab {
    RESUMEN, CHAT, MIEMBROS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuildsScreen(
    onBack: () -> Unit,
    onOpenGuildDetails: (String) -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val myGuilds by viewModel.myGuilds.collectAsState()
    val searchResults by viewModel.guildSearchResults.collectAsState()
    val message by viewModel.message.collectAsState()

    val selectedGuild by viewModel.selectedGuild.collectAsState()
    val selectedGuildMembers by viewModel.selectedGuildMembers.collectAsState()
    val selectedGuildChat by viewModel.selectedGuildChat.collectAsState()

    val isGuildMembersLoading by viewModel.isGuildMembersLoading.collectAsState()
    val isGuildChatLoading by viewModel.isGuildChatLoading.collectAsState()
    val isSendingGuildMessage by viewModel.isSendingGuildMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var pendingTransferMember by remember { mutableStateOf<GuildMemberSummary?>(null) }

    LaunchedEffect(Unit) {
        viewModel.startGuildsListener()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopGuildsListener() }
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = selectedGuild != null) {
        viewModel.closeGuildDetails()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gremios") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                actions = {
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text("Crear")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Mis gremios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (myGuilds.isEmpty()) {
                    EmptyGuildBlock("Todavía no perteneces a ningún gremio.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(0.42f, fill = false)
                    ) {
                        items(myGuilds, key = { it.id }) { guild ->
                            GuildCard(
                                guild = guild,
                                showJoinButton = false,
                                onJoin = {},
                                onOpenDetails = { onOpenGuildDetails(guild.id) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchGuilds(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar gremios") },
                    supportingText = { Text("Solo puedes pertenecer a un gremio a la vez") }
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults, key = { it.id }) { guild ->
                        GuildCard(
                            guild = guild,
                            showJoinButton = !guild.joined,
                            onJoin = { viewModel.joinGuild(guild) },
                            onOpenDetails = { viewModel.openGuildDetails(guild) }
                        )
                    }
                }
            }

        }
    }

    if (showCreateDialog) {
        CreateGuildDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, accent, banner ->
                viewModel.createGuild(name, description, accent, banner)
                showCreateDialog = false
            }
        )
    }

    if (showLeaveConfirm && selectedGuild != null) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Abandonar gremio") },
            text = { Text("¿Seguro que quieres abandonar ${selectedGuild!!.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        viewModel.leaveGuild(selectedGuild!!)
                    }
                ) {
                    Text("Abandonar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pendingTransferMember != null && selectedGuild != null) {
        AlertDialog(
            onDismissRequest = { pendingTransferMember = null },
            title = { Text("Transferir liderazgo") },
            text = {
                Text(
                    "¿Quieres convertir a ${
                        pendingTransferMember!!.displayName.ifBlank { pendingTransferMember!!.username }
                    } en el nuevo líder del gremio?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.transferLeadership(selectedGuild!!, pendingTransferMember!!.uid)
                        pendingTransferMember = null
                    }
                ) {
                    Text("Transferir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTransferMember = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuildDetailsFullscreen(
    guild: Guild,
    members: List<GuildMemberSummary>,
    guildChatMessages: List<GuildChatMessage>,
    isMembersLoading: Boolean,
    isGuildChatLoading: Boolean,
    isSendingGuildMessage: Boolean,
    canLeave: Boolean,
    isOwner: Boolean,
    currentUserUid: String,
    onDismiss: () -> Unit,
    onJoinGuild: () -> Unit,
    onLeaveGuild: () -> Unit,
    onTransferLeadership: (GuildMemberSummary) -> Unit,
    onOpenMemberChat: (GuildMemberSummary) -> Unit,
    onSendGuildMessage: (String) -> Unit
) {
    val banner = parseColor(guild.bannerColor)
    val accent = parseColor(guild.accentColor)
    var selectedTab by remember(guild.id) { mutableStateOf(GuildDetailsTab.RESUMEN) }
    var messageText by remember(guild.id) { mutableStateOf("") }
    val chatListState = rememberLazyListState()
    val chatScope = rememberCoroutineScope()

    LaunchedEffect(guildChatMessages.size, selectedTab) {
        if (selectedTab == GuildDetailsTab.CHAT && guildChatMessages.isNotEmpty()) {
            chatScope.launch {
                chatListState.animateScrollToItem(guildChatMessages.lastIndex)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                guild.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (guild.joined) "Página del gremio" else "Vista previa del gremio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) { Text("←") }
                    },
                    actions = {
                        when {
                            !guild.joined -> {
                                Button(onClick = onJoinGuild) {
                                    Text("Unirme")
                                }
                            }
                            canLeave -> {
                                TextButton(onClick = onLeaveGuild) {
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
                                    colors = listOf(
                                        banner,
                                        accent.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            guild.name,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            guild.description.ifBlank { "Sin descripción todavía." },
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniBadge(
                                text = "${guild.memberCount} miembros",
                                background = Color.White.copy(alpha = 0.18f),
                                content = Color.White
                            )
                            MiniBadge(
                                text = "Líder: ${guild.ownerDisplayName.ifBlank { "?" }}",
                                background = Color.Black.copy(alpha = 0.16f),
                                content = Color.White
                            )
                            MiniBadge(
                                text = if (guild.joined) "Mi gremio" else "Explorar",
                                background = Color.White.copy(alpha = 0.14f),
                                content = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!guild.joined) {
                                Button(
                                    onClick = onJoinGuild,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Unirme al gremio")
                                }
                            } else {
                                OutlinedButton(onClick = { selectedTab = GuildDetailsTab.CHAT }) {
                                    Text("Abrir chat")
                                }
                                OutlinedButton(onClick = { selectedTab = GuildDetailsTab.MIEMBROS }) {
                                    Text("Ver miembros")
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuildTabChip(
                        title = "Resumen",
                        selected = selectedTab == GuildDetailsTab.RESUMEN,
                        accent = accent,
                        onClick = { selectedTab = GuildDetailsTab.RESUMEN }
                    )
                    GuildTabChip(
                        title = "Chat",
                        selected = selectedTab == GuildDetailsTab.CHAT,
                        accent = accent,
                        onClick = { selectedTab = GuildDetailsTab.CHAT }
                    )
                    GuildTabChip(
                        title = "Miembros",
                        selected = selectedTab == GuildDetailsTab.MIEMBROS,
                        accent = accent,
                        onClick = { selectedTab = GuildDetailsTab.MIEMBROS }
                    )
                }

                when (selectedTab) {
                    GuildDetailsTab.RESUMEN -> {
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
                                    InfoPill(label = "Miembros", value = guild.memberCount.toString())
                                    InfoPill(label = "Líder", value = guild.ownerDisplayName.ifBlank { "?" })
                                }

                                Text(
                                    if (guild.joined) {
                                        "Desde aquí puedes hablar con el gremio, revisar sus miembros y abrir chats privados con ellos."
                                    } else {
                                        "Puedes revisar el gremio y sus integrantes. Si te unes, desbloquearás el chat del gremio y el acceso rápido a los chats privados."
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
                                    EmptyGuildBlock("No hay integrantes visibles todavía.")
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        members.take(4).forEach { member ->
                                            GuildMemberRow(
                                                member = member,
                                                accentColor = accent,
                                                canPromote = false,
                                                canOpenChat = guild.joined && member.uid != currentUserUid,
                                                onPromote = {},
                                                onOpenChat = { onOpenMemberChat(member) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GuildDetailsTab.CHAT -> {
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

                                if (!guild.joined) {
                                    EmptyGuildBlock("Debes unirte al gremio para leer y escribir en el chat.")
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
                                        EmptyGuildBlock("Todavía no hay mensajes. Rompe el hielo.")
                                    } else {
                                        LazyColumn(
                                            state = chatListState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 180.dp, max = 420.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(guildChatMessages, key = { it.id }) { msg ->
                                                GuildChatBubble(
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
                                                    onSendGuildMessage(text)
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

                    GuildDetailsTab.MIEMBROS -> {
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
                                    "Pulsa sobre un miembro para abrir chat privado con él. Si eres líder, también puedes transferir el liderazgo.",
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
                                    EmptyGuildBlock("No hay miembros para mostrar.")
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        members.forEach { member ->
                                            GuildMemberRow(
                                                member = member,
                                                accentColor = accent,
                                                canPromote = isOwner && member.uid != currentUserUid,
                                                canOpenChat = guild.joined && member.uid != currentUserUid,
                                                onPromote = { onTransferLeadership(member) },
                                                onOpenChat = { onOpenMemberChat(member) }
                                            )
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
}

@Composable
private fun GuildCard(
    guild: Guild,
    showJoinButton: Boolean,
    onJoin: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val accent = parseColor(guild.accentColor)
    val banner = parseColor(guild.bannerColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            banner.copy(alpha = 0.92f),
                            accent.copy(alpha = 0.78f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                guild.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                guild.description.ifBlank { "Sin descripción." },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBadge(
                        text = "${guild.memberCount} miembros",
                        background = Color.White.copy(alpha = 0.18f),
                        content = Color.White
                    )
                    if (guild.joined) {
                        MiniBadge(
                            text = "Tu gremio",
                            background = Color.Black.copy(alpha = 0.18f),
                            content = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenDetails) {
                        Text("Ver")
                    }
                    if (showJoinButton) {
                        Button(onClick = onJoin) {
                            Text("Unirme")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuildMemberRow(
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
            MemberAvatar(
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
                            "owner" -> "Líder"
                            else -> "Miembro"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun GuildChatBubble(
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 18.dp
            ),
            color = bubbleColor,
            tonalElevation = 1.dp,
            modifier = Modifier.width(280.dp).fillMaxWidth(0.88f)
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
                    formatTimestamp(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun GuildTabChip(
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
private fun MiniBadge(
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
private fun InfoPill(
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
private fun EmptyGuildBlock(
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
private fun MemberAvatar(
    photoUrl: String,
    displayName: String,
    size: androidx.compose.ui.unit.Dp,
    accent: Color
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = accent.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.38f))
    ) {
        if (photoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun CreateGuildDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, accent: String, banner: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accentSlider by remember { mutableStateOf(0.58f) }
    var bannerSlider by remember { mutableStateOf(0.22f) }

    val accentHex = hueToColor(accentSlider).toHexColor()
    val bannerHex = hueToColor(bannerSlider).copy(alpha = 1f).toHexColor()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear gremio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 32) name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 180) description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Text("Color de acento")
                Slider(
                    value = accentSlider,
                    onValueChange = { accentSlider = it }
                )
                ColorPreview(color = hueToColor(accentSlider))

                Text("Color de banner")
                Slider(
                    value = bannerSlider,
                    onValueChange = { bannerSlider = it }
                )
                ColorPreview(color = hueToColor(bannerSlider))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name.trim(),
                        description.trim(),
                        accentHex,
                        bannerHex
                    )
                },
                enabled = name.trim().length >= 3
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ColorPreview(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color)
            .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
    )
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}

private fun parseColor(value: String?): Color {
    return try {
        if (value.isNullOrBlank()) Color(0xFF6750A4) else Color(android.graphics.Color.parseColor(value))
    } catch (_: Exception) {
        Color(0xFF6750A4)
    }
}

private fun hueToColor(value: Float): Color {
    val hsv = floatArrayOf((value.coerceIn(0f, 1f) * 360f), 0.72f, 0.90f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun Color.toHexColor(): String {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return if (a >= 255) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", r, g, b, a)
    }
}