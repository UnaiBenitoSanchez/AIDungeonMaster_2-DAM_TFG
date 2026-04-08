package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.JournalEntry
import com.example.aidungeonmaster.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    charId: String,
    onBack: () -> Unit,
    viewModel: JournalViewModel = viewModel()
) {
    LaunchedEffect(charId) {
        viewModel.loadJournal(charId)
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
                entry.title.lowercase().contains(q) ||
                        entry.summary.lowercase().contains(q) ||
                        entry.fullText.lowercase().contains(q) ||
                        entry.type.lowercase().contains(q) ||
                        entry.enemyName.lowercase().contains(q) ||
                        entry.locationName.lowercase().contains(q) ||
                        entry.tags.any { it.lowercase().contains(q) } ||
                        entry.itemNames.any { it.lowercase().contains(q) }
            }
        }
    }

    MedievalBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { MedievalTitle("DIARIO") },
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
                        JournalDetailView(
                            entry = selectedEntry!!,
                            onClose = { viewModel.selectEntry(null) }
                        )
                    }

                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            JournalSummaryCard(totalEntries = entries.size)

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Buscar en el diario") },
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
                                            entries.isEmpty() -> "Aún no hay entradas en el diario."
                                            query.isNotBlank() -> "No hay resultados para \"$query\"."
                                            else -> "No hay entradas en el diario."
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
                                        JournalEntryCard(
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
private fun JournalSummaryCard(totalEntries: Int) {
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
                Icons.Default.AutoStories,
                contentDescription = null,
                tint = Color(0xFFFFD700)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Crónica de la aventura",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Entradas registradas: $totalEntries",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = entry.title.ifBlank { "Entrada sin título" },
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (entry.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.summary,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                JournalMiniStat("Tipo", entry.type.ifBlank { "story" })
                JournalMiniStat("Fecha", formatJournalTimestampShort(entry.timestamp))
            }

            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
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
private fun JournalMiniStat(
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
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun JournalDetailView(
    entry: JournalEntry,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC111111)),
        border = BorderStroke(1.dp, Color(0x44FFD700))
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
                        text = entry.title.ifBlank { "Entrada sin título" },
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = formatJournalTimestamp(entry.timestamp),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (entry.summary.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = entry.summary,
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
                    JournalSectionCard("📖 Relato") {
                        Text(
                            text = entry.fullText.ifBlank { entry.summary.ifBlank { "Sin detalles." } },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item {
                    JournalSectionCard("📌 Detalles") {
                        DetailLine("Tipo", entry.type.ifBlank { "story" })
                        DetailLine("Capítulo", entry.chapter.ifBlank { "Sin capítulo" })
                        DetailLine("Ubicación", entry.locationName.ifBlank { "Sin datos" })
                        DetailLine("Enemigo", entry.enemyName.ifBlank { "Sin datos" })
                    }
                }

                item {
                    JournalSectionCard("🎒 Recompensas y cambios") {
                        DetailLine("Cambio de HP", formatSigned(entry.hpChange))
                        DetailLine("Cambio de monedas", formatSigned(entry.coinsChange))
                        DetailLine("XP ganada", if (entry.xpGained > 0) entry.xpGained.toString() else "0")
                        DetailListLine("Objetos", entry.itemNames)
                    }
                }

                item {
                    JournalSectionCard("🏷️ Etiquetas") {
                        DetailListLine("Tags", entry.tags)
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
private fun JournalSectionCard(
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
            values.distinct().forEach { value ->
                Text(
                    text = "• $value",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

private fun formatSigned(value: Int): String {
    return when {
        value > 0 -> "+$value"
        value < 0 -> value.toString()
        else -> "0"
    }
}

private fun formatJournalTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Desconocido"
    return try {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "Desconocido"
    }
}

private fun formatJournalTimestampShort(timestamp: Long): String {
    if (timestamp <= 0L) return "?"
    return try {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "?"
    }
}