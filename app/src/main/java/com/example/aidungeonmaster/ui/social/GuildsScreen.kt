package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuildsScreen(
    onBack: () -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val myGuilds by viewModel.myGuilds.collectAsState()
    val searchResults by viewModel.guildSearchResults.collectAsState()
    val message by viewModel.message.collectAsState()
    val selectedGuild by viewModel.selectedGuild.collectAsState()
    val selectedGuildMembers by viewModel.selectedGuildMembers.collectAsState()
    val isGuildMembersLoading by viewModel.isGuildMembersLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gremios") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                actions = { TextButton(onClick = { showCreateDialog = true }) { Text("Crear") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                            onOpenDetails = { viewModel.openGuildDetails(guild) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) viewModel.searchGuilds(it)
                    if (it.isBlank()) viewModel.searchGuilds("")
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar gremios") },
                supportingText = { Text("Pulsa sobre un gremio para ver sus integrantes") }
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

    if (showCreateDialog) {
        CreateGuildDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, accent, banner ->
                viewModel.createGuild(name, description, accent, banner)
                showCreateDialog = false
            }
        )
    }

    if (selectedGuild != null) {
        GuildMembersDialog(
            guild = selectedGuild!!,
            members = selectedGuildMembers,
            isLoading = isGuildMembersLoading,
            canLeave = viewModel.canLeaveGuild(selectedGuild!!),
            isOwner = viewModel.isGuildOwner(selectedGuild!!),
            onDismiss = { viewModel.closeGuildDetails() },
            onLeaveGuild = { showLeaveConfirm = true }
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
}

@Composable
private fun EmptyGuildBlock(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
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
    val banner = parseColor(guild.bannerColor)
    val accent = parseColor(guild.accentColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(banner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        guild.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (guild.description.isNotBlank()) {
                        Text(
                            guild.description,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                MiniBadge(
                    text = if (guild.joined) "Mi gremio" else "Explorar",
                    background = accent.copy(alpha = 0.18f),
                    content = accent
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniBadge(
                    text = "${guild.memberCount} miembros",
                    background = Color.White.copy(alpha = 0.12f),
                    content = Color.White
                )

                MiniBadge(
                    text = "Líder: ${guild.ownerDisplayName.ifBlank { "Desconocido" }}",
                    background = accent.copy(alpha = 0.20f),
                    content = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pulsa para ver integrantes",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )

                if (showJoinButton) {
                    Button(onClick = onJoin) {
                        Text("Unirme")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuildMembersDialog(
    guild: Guild,
    members: List<GuildMemberSummary>,
    isLoading: Boolean,
    canLeave: Boolean,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onLeaveGuild: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    guild.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    guild.description.ifBlank { "Sin descripción" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(label = "Miembros", value = guild.memberCount.toString())
                    InfoPill(label = "Líder", value = guild.ownerDisplayName.ifBlank { "?" })
                }

                if (isOwner) {
                    Text(
                        "Eres el líder de este gremio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = parseColor(guild.accentColor)
                    )
                }

                HorizontalDivider()

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (members.isEmpty()) {
                    Text("No hay integrantes visibles en este gremio.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(members, key = { it.uid }) { member ->
                            GuildMemberCard(
                                member = member,
                                accentColor = parseColor(guild.accentColor)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            if (canLeave) {
                Button(onClick = onLeaveGuild) {
                    Text("Abandonar gremio")
                }
            }
        }
    )
}

@Composable
private fun GuildMemberCard(
    member: GuildMemberSummary,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
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
                displayName = member.displayName
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    member.displayName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "@${member.username.ifBlank { "usuario" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBadge(
                        text = if (member.isOwner) "Líder" else member.role.replaceFirstChar { it.uppercase() },
                        background = accentColor.copy(alpha = 0.15f),
                        content = accentColor
                    )

                    MiniBadge(
                        text = "${member.characterCount} personajes",
                        background = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (member.joinedAt > 0L) {
                    Text(
                        "Entró el ${formatDate(member.joinedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberAvatar(
    photoUrl: String,
    displayName: String
) {
    if (photoUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = photoUrl,
            contentDescription = "Avatar de $displayName",
            loading = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2B2B)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            },
            error = {
                InitialAvatar(displayName)
            },
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
        )
    } else {
        InitialAvatar(displayName)
    }
}

@Composable
private fun InitialAvatar(displayName: String) {
    val initials = displayName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color(0xFF3A2F4F))
            .border(2.dp, Color(0xFFD4AF37), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CreateGuildDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accentColor by remember { mutableStateOf("#8E24AA") }
    var bannerColor by remember { mutableStateOf("#1F1235") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear gremio") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                SimpleColorPicker(
                    title = "Color acento",
                    initialHex = accentColor,
                    onColorChanged = { accentColor = it }
                )

                SimpleColorPicker(
                    title = "Color fondo",
                    initialHex = bannerColor,
                    onColorChanged = { bannerColor = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, description, accentColor, bannerColor) }) {
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
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
    )
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun hexToColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { Color(0xFFD4AF37) }
}

@Composable
private fun SimpleColorPicker(
    title: String,
    initialHex: String,
    onColorChanged: (String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(hexToColor(initialHex)) }

    var red by remember { mutableStateOf(selectedColor.red * 255f) }
    var green by remember { mutableStateOf(selectedColor.green * 255f) }
    var blue by remember { mutableStateOf(selectedColor.blue * 255f) }

    LaunchedEffect(red, green, blue) {
        selectedColor = Color(
            red = red / 255f,
            green = green / 255f,
            blue = blue / 255f,
            alpha = 1f
        )
        onColorChanged(colorToHex(selectedColor))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color(0xFFD4AF37))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ColorPreview(selectedColor)
            Text(colorToHex(selectedColor))
        }

        Text("R")
        Slider(
            value = red,
            onValueChange = { red = it },
            valueRange = 0f..255f
        )

        Text("G")
        Slider(
            value = green,
            onValueChange = { green = it },
            valueRange = 0f..255f
        )

        Text("B")
        Slider(
            value = blue,
            onValueChange = { blue = it },
            valueRange = 0f..255f
        )
    }
}

private fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { Color(0xFF1F1235) }

private fun formatDate(timestamp: Long): String {
    return runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }.getOrElse { "-" }
}