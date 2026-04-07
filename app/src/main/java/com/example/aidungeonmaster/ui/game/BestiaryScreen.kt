package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.BestiaryEntry
import com.example.aidungeonmaster.viewmodel.BestiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: BestiaryViewModel = viewModel()
) {
    LaunchedEffect(gameId) {
        android.util.Log.d("BESTIARY_DEBUG", "BestiaryScreen gameId recibido = $gameId")
        viewModel.loadBestiary(gameId)
    }

    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()

    var query by remember { mutableStateOf("") }

    val filteredEntries = remember(entries, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                entry.name.lowercase().contains(q) ||
                        entry.description.lowercase().contains(q) ||
                        entry.tags.any { it.lowercase().contains(q) } ||
                        entry.locationsSeen.any { it.lowercase().contains(q) }
            }
        }
    }

    MedievalBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { MedievalTitle("BESTIARIO") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.selectEntry(null)
                            onBack()
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD700))
                        }
                    }

                    selectedEntry != null -> {
                        BestiaryDetailView(
                            entry = selectedEntry!!,
                            onClose = { viewModel.selectEntry(null) }
                        )
                    }

                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            BestiarySummaryCard(totalEntries = entries.size)

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Buscar monstruo, zona o etiqueta")
                                },
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            if (filteredEntries.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when {
                                            entries.isEmpty() -> "Aún no has descubierto monstruos."
                                            query.isNotBlank() -> "No hay resultados para \"$query\"."
                                            else -> "No hay entradas en el bestiario."
                                        },
                                        color = Color.LightGray,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredEntries) { entry ->
                                        BestiaryEntryCard(
                                            entry = entry,
                                            onClick = { viewModel.selectEntry(entry) }
                                        )
                                    }

                                    item {
                                        Spacer(Modifier.height(80.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestiarySummaryCard(totalEntries: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x44000000)),
        border = BorderStroke(1.dp, Color(0x55FFD700))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = Color(0xFFFFD700)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Enciclopedia de criaturas",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Monstruos descubiertos: $totalEntries",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BestiaryEntryCard(
    entry: BestiaryEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x33000000))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = entry.name.ifBlank { "Criatura desconocida" },
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (entry.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.description,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BestiaryMiniStat("Encuentros", entry.timesEncountered.toString())
                BestiaryMiniStat("Derrotas", entry.timesDefeated.toString())
                BestiaryMiniStat(
                    "HP visto",
                    entry.lastObservedStats.hpMaxObserved.takeIf { it > 0 }?.toString() ?: "?"
                )
                BestiaryMiniStat(
                    "CA vista",
                    entry.lastObservedStats.armorClassObserved?.toString() ?: "?"
                )
            }

            if (entry.locationsSeen.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "📍 ${entry.locationsSeen.joinToString()}",
                    color = Color(0xFFBFE3FF),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🏷️ ${entry.tags.joinToString()}",
                    color = Color(0xFFFFD59A),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun BestiaryMiniStat(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.LightGray,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BestiaryDetailView(
    entry: BestiaryEntry,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color(0x44FFD700), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC111111))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Surface(
                color = Color(0x33FFD700),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0x55FFD700))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = entry.name.ifBlank { "Criatura desconocida" },
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                    if (entry.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = entry.description,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BestiarySectionCard(
                        title = "📊 Estadísticas observadas"
                    ) {
                        DetailLine("HP máximo visto", entry.lastObservedStats.hpMaxObserved.takeIf { it > 0 }?.toString() ?: "Desconocido")
                        DetailLine("CA observada", entry.lastObservedStats.armorClassObserved?.toString() ?: "Desconocida")
                        DetailLine("Veces encontrado", entry.timesEncountered.toString())
                        DetailLine("Veces derrotado", entry.timesDefeated.toString())
                    }
                }

                item {
                    BestiarySectionCard(
                        title = "📍 Avistamientos"
                    ) {
                        DetailLine(
                            "Zonas",
                            entry.locationsSeen.takeIf { it.isNotEmpty() }?.joinToString()
                                ?: "Sin datos"
                        )
                        DetailLine("Primera vez visto", formatTimestamp(entry.firstSeenAt))
                        DetailLine("Última vez visto", formatTimestamp(entry.lastSeenAt))
                    }
                }

                item {
                    BestiarySectionCard(
                        title = "✨ Habilidades y daño observado"
                    ) {
                        DetailListLine(
                            "Daño observado",
                            entry.lastObservedStats.damageNotes
                        )
                        DetailListLine(
                            "Habilidades vistas",
                            entry.lastObservedStats.abilitiesSeen
                        )
                    }
                }

                item {
                    BestiarySectionCard(
                        title = "🎁 Botín conocido"
                    ) {
                        DetailListLine("Loot", entry.knownLoot)
                    }
                }

                item {
                    BestiarySectionCard(
                        title = "🏷️ Etiquetas"
                    ) {
                        DetailListLine("Tags", entry.tags)
                    }
                }

                if (entry.notes.isNotBlank()) {
                    item {
                        BestiarySectionCard(
                            title = "📝 Notas"
                        ) {
                            Text(
                                text = entry.notes,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClose() },
                        color = Color(0x44FFD700),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Volver al listado",
                                color = Color(0xFFFFF2B2),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun BestiarySectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            color = Color.LightGray,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DetailListLine(
    label: String,
    values: List<String>
) {
    Column {
        Text(
            text = label,
            color = Color.LightGray,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(4.dp))

        if (values.isEmpty()) {
            Text(
                text = "Sin datos",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            values.distinct().forEachIndexed { index, value ->
                Text(
                    text = "• $value",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (index != values.lastIndex) {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Desconocido"
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        "Desconocido"
    }
}