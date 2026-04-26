package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_GUILD_MEMBERS = 15

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

    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val shouldShowSearchResults = query.trim().length >= 2

    LaunchedEffect(Unit) {
        query = ""
        viewModel.clearGuildSearch()
        viewModel.startGuildsListener()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearGuildSearch()
            viewModel.stopGuildsListener()
        }
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
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                },
                actions = {
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text("Crear")
                    }
                }
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
                            onOpenDetails = { onOpenGuildDetails(guild.id) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery

                    if (newQuery.trim().length >= 2) {
                        viewModel.searchGuilds(newQuery)
                    } else {
                        viewModel.clearGuildSearch()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar gremios") },
                supportingText = { Text("Solo puedes pertenecer a un gremio a la vez") }
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (shouldShowSearchResults) {
                    items(searchResults, key = { it.id }) { guild ->
                        GuildCard(
                            guild = guild,
                            showJoinButton = !guild.joined,
                            onJoin = { viewModel.joinGuild(guild) },
                            onOpenDetails = {
                                query = ""
                                viewModel.clearGuildSearch()
                                onOpenGuildDetails(guild.id)
                            }
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
    val isFull = guild.memberCount >= MAX_GUILD_MEMBERS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        )
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
                        text = "${guild.memberCount}/$MAX_GUILD_MEMBERS miembros",
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

                    if (isFull && !guild.joined) {
                        MiniBadge(
                            text = "Completo",
                            background = Color.Black.copy(alpha = 0.24f),
                            content = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenDetails) {
                        Text("Ver")
                    }
                    if (showJoinButton) {
                        Button(
                            onClick = onJoin,
                            enabled = !isFull
                        ) {
                            Text(if (isFull) "Completo" else "Unirme")
                        }
                    }
                }
            }
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
    size: Dp,
    accent: Color
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f))
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
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
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
        if (value.isNullOrBlank()) Color(0xFF6750A4)
        else Color(android.graphics.Color.parseColor(value))
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