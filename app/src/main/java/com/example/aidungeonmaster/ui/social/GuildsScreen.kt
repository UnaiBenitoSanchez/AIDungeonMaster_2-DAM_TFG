package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuildsScreen(
    onBack: () -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val myGuilds by viewModel.myGuilds.collectAsState()
    val searchResults by viewModel.guildSearchResults.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mis gremios", style = MaterialTheme.typography.titleLarge)
            if (myGuilds.isEmpty()) {
                Text("Todavía no perteneces a ningún gremio.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(0.45f, fill = false)) {
                    items(myGuilds, key = { it.id }) { guild -> GuildCard(guild = guild, showJoinButton = false, onJoin = {}) }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) viewModel.searchGuilds(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar gremios") }
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults, key = { it.id }) { guild ->
                    GuildCard(
                        guild = guild,
                        showJoinButton = !guild.joined,
                        onJoin = { viewModel.joinGuild(guild) }
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
}

@Composable
private fun GuildCard(
    guild: Guild,
    showJoinButton: Boolean,
    onJoin: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(parseColor(guild.bannerColor))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(guild.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (guild.description.isNotBlank()) {
                Text(guild.description, color = Color.White)
            }
            Text("Miembros: ${guild.memberCount}", color = parseColor(guild.accentColor))
            if (showJoinButton) {
                Button(onClick = onJoin) {
                    Text("Unirme")
                }
            }
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
            Button(onClick = { onCreate(name, description, accentColor, bannerColor) }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
    val a = (color.alpha * 255).toInt()
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
            Text(colorToHex(selectedColor), color = Color.White)
        }

        Text("R", color = Color.LightGray)
        Slider(
            value = red,
            onValueChange = { red = it },
            valueRange = 0f..255f
        )

        Text("G", color = Color.LightGray)
        Slider(
            value = green,
            onValueChange = { green = it },
            valueRange = 0f..255f
        )

        Text("B", color = Color.LightGray)
        Slider(
            value = blue,
            onValueChange = { blue = it },
            valueRange = 0f..255f
        )
    }
}

private fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
    .getOrElse { Color(0xFF1F1235) }
