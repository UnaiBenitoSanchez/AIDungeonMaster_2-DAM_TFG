package com.example.aidungeonmaster.ui.game

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.JournalEntry
import com.example.aidungeonmaster.python.PythonJournalBridge
import com.example.aidungeonmaster.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.DisposableEffect
import com.example.aidungeonmaster.utils.AdventureMusicEngine
import com.example.aidungeonmaster.ui.accessibility.VoiceFormAction
import com.example.aidungeonmaster.ui.accessibility.VoiceFormRegistry
import com.example.aidungeonmaster.ui.accessibility.VoiceFormScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    charId: String,
    onBack: () -> Unit,
    viewModel: JournalViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(charId) {
        viewModel.loadJournal(charId)
    }

    DisposableEffect(Unit) {
        AdventureMusicEngine.setScreen(AdventureMusicEngine.MusicScreen.JOURNAL)
        onDispose {
            AdventureMusicEngine.releaseScreen(1200L)
        }
    }

    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var summaryExpanded by rememberSaveable { mutableStateOf(false) }

    val filteredEntries = remember(entries, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                entry.title.lowercase().contains(q) ||
                        entry.summary.lowercase().contains(q) ||
                        entry.fullText.lowercase().contains(q) ||
                        entry.epicText.lowercase().contains(q) ||
                        entry.type.lowercase().contains(q) ||
                        entry.enemyName.lowercase().contains(q) ||
                        entry.locationName.lowercase().contains(q) ||
                        entry.chapter.lowercase().contains(q) ||
                        entry.tags.any { it.lowercase().contains(q) } ||
                        entry.itemNames.any { it.lowercase().contains(q) }
            }
        }
    }

    val pythonInput = remember(entries) {
        entries.take(8).map { entry ->
            mapOf(
                "title" to entry.title,
                "summary" to entry.summary,
                "fullText" to entry.fullText,
                "type" to entry.type,
                "enemyName" to entry.enemyName,
                "locationName" to entry.locationName,
                "tags" to entry.tags,
                "itemNames" to entry.itemNames,
                "timestamp" to entry.timestamp,
                "chapter" to entry.chapter
            )
        }
    }

    val chapterTitle = remember(pythonInput) {
        PythonJournalBridge.makeChapterTitle(pythonInput)
    }

    val adventureSummary = remember(pythonInput) {
        PythonJournalBridge.summarizeEntries(pythonInput)
    }

    DisposableEffect(summaryExpanded, selectedEntry, entries.size) {
        val registration = VoiceFormRegistry.register(
            VoiceFormScreen(
                screenName = "diario",
                actions = listOf(
                    VoiceFormAction(
                        label = "desplegar resumen",
                        aliases = listOf("desplegar resumen", "abrir resumen", "mostrar resumen"),
                        enabled = { selectedEntry == null && !summaryExpanded },
                        disabledFeedback = "El resumen ya está desplegado o estás dentro de una entrada.",
                        onRun = { summaryExpanded = true },
                        feedback = "Desplegando resumen del diario."
                    ),
                    VoiceFormAction(
                        label = "plegar resumen",
                        aliases = listOf("plegar resumen", "ocultar resumen", "cerrar resumen"),
                        enabled = { selectedEntry == null && summaryExpanded },
                        disabledFeedback = "El resumen ya está plegado o estás dentro de una entrada.",
                        onRun = { summaryExpanded = false },
                        feedback = "Plegando resumen del diario."
                    ),
                    VoiceFormAction(
                        label = "compartir diario",
                        aliases = listOf("compartir diario", "exportar diario", "enviar diario"),
                        enabled = { selectedEntry == null && entries.isNotEmpty() },
                        disabledFeedback = "No hay entradas para compartir o estás dentro de una entrada.",
                        onRun = {
                            val exportText = buildJournalExportText(entries)
                            shareJournal(context, exportText)
                        },
                        feedback = "Abriendo compartir diario."
                    )
                )
            )
        )
        onDispose { registration.dispose() }
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
                    actions = {
                        IconButton(
                            onClick = {
                                val exportText = buildJournalExportText(entries)
                                shareJournal(context, exportText)
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Exportar diario",
                                tint = Color(0xFFFFD700)
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
                            charId = charId,
                            entry = selectedEntry!!,
                            viewModel = viewModel,
                            onClose = { viewModel.selectEntry(null) }
                        )
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            JournalSummaryCard(totalEntries = entries.size)

                            Spacer(Modifier.height(12.dp))

                            CollapsibleAdventureSummaryCard(
                                chapterTitle = chapterTitle,
                                summary = adventureSummary,
                                expanded = summaryExpanded,
                                onToggle = { summaryExpanded = !summaryExpanded }
                            )

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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
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
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    filteredEntries.forEach { entry ->
                                        JournalEntryCard(
                                            entry = entry,
                                            onClick = { viewModel.selectEntry(entry) }
                                        )
                                    }

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
private fun CollapsibleAdventureSummaryCard(
    chapterTitle: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = Color(0x55220000)),
        border = BorderStroke(1.dp, Color(0x66FFD700))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🪶 $chapterTitle",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (expanded) "Tocar para plegar resumen" else "Tocar para desplegar resumen",
                        color = Color(0xFFFFD59A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Plegar resumen" else "Desplegar resumen",
                    tint = Color(0xFFFFD700)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = summary,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
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

            if (entry.repeatCount > 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Se repitió ${entry.repeatCount} veces",
                    color = Color(0xFFFFD59A),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

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

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                JournalMiniStat("Capítulo", entry.chapter.ifBlank { "Sin capítulo" })
                JournalMiniStat("Tono", entry.toneVersion.ifBlank { "normal" })
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
    charId: String,
    entry: JournalEntry,
    viewModel: JournalViewModel,
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
                            text = entry.epicText.ifBlank {
                                entry.fullText.ifBlank { entry.summary.ifBlank { "Sin detalles." } }
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item {
                    JournalSectionCard("📌 Detalles") {
                        DetailLine("Tipo", entry.type.ifBlank { "story" })
                        DetailLine("Capítulo", entry.chapter.ifBlank { "Sin capítulo" })
                        DetailLine("Índice de capítulo", entry.chapterIndex.toString())
                        DetailLine("Escena", entry.sceneIndex.toString())
                        DetailLine("Ubicación", entry.locationName.ifBlank { "Sin datos" })
                        DetailLine("Enemigo", entry.enemyName.ifBlank { "Sin datos" })
                        DetailLine("Veces repetida", entry.repeatCount.toString())
                        DetailLine("Tono", entry.toneVersion.ifBlank { "normal" })
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
                    JournalActionButton(
                        text = "Reescribir en tono épico",
                        onClick = { viewModel.rewriteEntryEpic(charId, entry) }
                    )
                }

                item {
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
private fun JournalActionButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color(0x33222222),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x55FFD700))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color(0xFFFFE9A8),
                fontWeight = FontWeight.Bold
            )
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

private fun buildJournalExportText(entries: List<JournalEntry>): String {
    return buildString {
        appendLine("DIARIO DE AVENTURA")
        appendLine("==================")
        appendLine()

        entries.sortedBy { it.timestamp }.forEach { entry ->
            appendLine(entry.chapter.ifBlank { "Sin capítulo" })
            appendLine(entry.title.ifBlank { "Entrada sin título" })
            appendLine(formatJournalTimestamp(entry.timestamp))
            appendLine()

            val text = entry.epicText.ifBlank {
                entry.fullText.ifBlank { entry.summary }
            }

            appendLine(text.ifBlank { "Sin contenido." })

            if (entry.repeatCount > 1) {
                appendLine()
                appendLine("Repetida: ${entry.repeatCount} veces")
            }

            if (entry.tags.isNotEmpty()) {
                appendLine()
                appendLine("Etiquetas: ${entry.tags.joinToString()}")
            }

            appendLine()
            appendLine("----------------------------------------")
            appendLine()
        }
    }
}

private fun shareJournal(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Diario de aventura")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar diario"))
}
