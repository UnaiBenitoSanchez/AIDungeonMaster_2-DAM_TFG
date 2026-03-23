package com.example.aidungeonmaster.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.navigation.Screen
import com.example.aidungeonmaster.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = viewModel()) {
    val characters by viewModel.characters.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var characterToDelete by remember { mutableStateOf<Character?>(null) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Refrescar el HP desde partidas cada vez que HomeScreen vuelve a ser visible
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tus Personajes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("ranking") }) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Ranking Mundial", tint = Color(0xFFFFD700))
                    }
                    IconButton(onClick = {
                        viewModel.logout {
                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, "Cerrar Sesión", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
            if (characters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay aventureros todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(characters) { character ->
                        CharacterCard(
                            character = character,
                            onClick = {
                                if (character.gameTheme.isNullOrEmpty()) {
                                    navController.navigate(Screen.GameSetup.createRoute(userId, character.name))
                                } else {
                                    navController.navigate(Screen.GamePlay.createRoute(userId, character.name, character.gameTheme))
                                }
                            },
                            onDelete = { characterToDelete = character }
                        )
                    }
                }
            }
        }
    }

    // ── DIÁLOGO CREACIÓN ─────────────────────────────────────────────────────
    if (showDialog) {
        CreateCharacterDialog(
            onDismiss = { showDialog = false },
            onCreate  = { name, race, clazz, stats, traits, portraitUrl ->
                viewModel.saveCharacter(name, race, clazz, stats, traits, portraitUrl)
                showDialog = false
            }
        )
    }

    // ── DIÁLOGO CONFIRMACIÓN BORRADO ─────────────────────────────────────────
    characterToDelete?.let { character ->
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            title   = { Text("¿Eliminar personaje?") },
            text    = { Text("¿Estás seguro de que quieres borrar a ${character.name}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteCharacter(character.id, character.name); characterToDelete = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { characterToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun CharacterCard(character: Character, onClick: () -> Unit, onDelete: () -> Unit) {

    // Cambia la lógica del imageModel por esta más robusta:
    val imageModel = remember(character.portraitUrl) {
        val url = character.portraitUrl
        if (url.isNullOrEmpty()) {
            null
        } else if (url.startsWith("iVBOR") || url.length > 100) {
            // Si empieza por iVBOR o es muy largo, asumimos que es Base64 puro
            try {
                android.util.Base64.decode(url, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        } else {
            url // Es una URL de internet normal
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // ── RETRATO ──
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (character.portraitUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel) // <── Usamos el modelo procesado aquí
                            .crossfade(true)
                            .build(),
                        contentDescription = "Retrato de ${character.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                val subtitle = if (!character.gameTheme.isNullOrEmpty()) {
                    "${character.race} • ${character.characterClass} | ${character.gameTheme}"
                } else {
                    "${character.race} • ${character.characterClass} (Sin partida)"
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)

                // HP si tiene partida
                if (character.hpMax > 0) {
                    Text(
                        "❤️ ${character.hpCurrent} / ${character.hpMax}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            character.hpCurrent <= character.hpMax * 0.25f -> Color.Red
                            character.hpCurrent <= character.hpMax * 0.5f  -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }

                // Nivel + barra de XP
                if (character.level > 0) {
                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge de nivel
                        Surface(
                            color = Color(0xFF3A2A00),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFD700))
                        ) {
                            Text(
                                text = "Nv.${character.level}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Barra de progreso de XP
                        val xpProgress = if (character.level * 100 > 0)
                            character.xp.toFloat() / (character.level * 100) else 0f

                        LinearProgressIndicator(
                            progress = { xpProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color      = Color(0xFFFFD700),
                            trackColor = Color(0xFF2A2A2A)
                        )

                        Text(
                            text = "${character.xp}/${character.level * 100} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Última sesión (He unido tus dos bloques de tiempo en uno solo más limpio)
                if (character.lastPlayed > 0L) {
                    Text(
                        "🕐 ${formatLastPlayed(character.lastPlayed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ── FORMATO DE FECHA ──────────────────────────────────────────────────────────
private fun formatLastPlayed(timestamp: Long): String {
    val now     = System.currentTimeMillis()
    val diff    = now - timestamp
    val days    = TimeUnit.MILLISECONDS.toDays(diff)
    val hours   = TimeUnit.MILLISECONDS.toHours(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1  -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        hours   < 24 -> "Hace $hours h"
        days    == 1L -> "Ayer"
        days    < 7  -> "Hace $days días"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}