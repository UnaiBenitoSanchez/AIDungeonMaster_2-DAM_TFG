package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.aidungeonmaster.data.model.BestiaryEntry
import com.example.aidungeonmaster.data.model.BestiaryLoot
import com.example.aidungeonmaster.viewmodel.BestiaryEditableListField
import com.example.aidungeonmaster.viewmodel.BestiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.aidungeonmaster.utils.ImageUtils

import androidx.compose.runtime.DisposableEffect
import com.example.aidungeonmaster.utils.AdventureMusicEngine

private enum class BestiaryFilter(val label: String) {
    ALL("Todos"),
    DEFEATED("Derrotados"),
    WITH_NOTES("Con notas"),
    WITH_LOOT("Con loot"),
    WITH_WEAKNESS("Con debilidad"),
    WITH_RESISTANCE("Con resistencia")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: BestiaryViewModel = viewModel()
) {
    LaunchedEffect(gameId) {
        viewModel.loadBestiary(gameId)
    }

    DisposableEffect(Unit) {
        AdventureMusicEngine.setScreen(AdventureMusicEngine.MusicScreen.BESTIARY)
        onDispose {
            AdventureMusicEngine.releaseScreen(1200L)
        }
    }

    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()
    val imageGenerationMonsterId by viewModel.imageGenerationMonsterId.collectAsState()

    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(BestiaryFilter.ALL) }

    val filteredEntries = remember(entries, query, activeFilter) {
        val q = query.trim().lowercase()
        entries.filter { entry ->
            val matchesQuery = q.isBlank() ||
                    entry.name.lowercase().contains(q) ||
                    entry.description.lowercase().contains(q) ||
                    entry.tags.any { it.lowercase().contains(q) } ||
                    entry.locationsSeen.any { it.lowercase().contains(q) } ||
                    entry.observedWeaknesses.any { it.lowercase().contains(q) } ||
                    entry.observedResistances.any { it.lowercase().contains(q) } ||
                    entry.detailedKnownLoot.any {
                        it.name.lowercase().contains(q) ||
                                it.category.lowercase().contains(q) ||
                                it.details.lowercase().contains(q)
                    }

            val matchesFilter = when (activeFilter) {
                BestiaryFilter.ALL -> true
                BestiaryFilter.DEFEATED -> entry.timesDefeated > 0
                BestiaryFilter.WITH_NOTES -> entry.notes.isNotBlank()
                BestiaryFilter.WITH_LOOT -> entry.knownLoot.isNotEmpty() || entry.detailedKnownLoot.isNotEmpty()
                BestiaryFilter.WITH_WEAKNESS -> entry.observedWeaknesses.isNotEmpty()
                BestiaryFilter.WITH_RESISTANCE -> entry.observedResistances.isNotEmpty()
            }

            matchesQuery && matchesFilter
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
                            gameId = gameId,
                            entry = selectedEntry!!,
                            isGeneratingImage = imageGenerationMonsterId == selectedEntry!!.monsterId,
                            onClose = { viewModel.selectEntry(null) },
                            onGenerateImage = { viewModel.regenerateMonsterImage(gameId, selectedEntry!!) },
                            onSaveNotes = { notes ->
                                viewModel.saveMonsterNotes(gameId, selectedEntry!!.monsterId, notes)
                            },
                            onSaveListField = { field, value ->
                                viewModel.saveMonsterFieldList(gameId, selectedEntry!!.monsterId, field, value)
                            }
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
                                    Text("Buscar monstruo, zona, etiqueta, loot o afinidad")
                                },
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BestiaryFilter.values().forEach { filter ->
                                    FilterPill(
                                        text = filter.label,
                                        selected = activeFilter == filter,
                                        onClick = { activeFilter = filter }
                                    )
                                }
                            }

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
                                            else -> "No hay entradas que coincidan con el filtro."
                                        },
                                        color = Color.LightGray,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
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
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Color(0x66FFD700) else Color(0x33000000),
        border = BorderStroke(1.dp, if (selected) Color(0xFFFFD700) else Color(0x44FFFFFF))
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFFFFF2B2) else Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
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
        Row(modifier = Modifier.padding(14.dp)) {
            MonsterImage(
                imageUrl = entry.imageUrl,
                name = entry.name,
                modifier = Modifier.size(86.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
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
                        "Debilidades",
                        entry.observedWeaknesses.size.toString()
                    )
                    BestiaryMiniStat(
                        "Loot",
                        (entry.detailedKnownLoot.ifEmpty { entry.knownLoot }).size.toString()
                    )
                }

                if (entry.observedWeaknesses.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "⚔️ Débil ante: ${entry.observedWeaknesses.joinToString()}",
                        color = Color(0xFFFFD59A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (entry.observedResistances.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🛡️ Resiste: ${entry.observedResistances.joinToString()}",
                        color = Color(0xFFBFE3FF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MonsterImage(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier
) {
    val emoji = monsterEmoji(name)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x55FFD700), RoundedCornerShape(12.dp))
            .background(Color(0x22000000)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = remember(imageUrl) {
            try {
                when {
                    imageUrl.startsWith("data:image", ignoreCase = true) -> {
                        val base64Part = imageUrl.substringAfter("base64,", "")
                        if (base64Part.isNotBlank()) {
                            ImageUtils.base64ToBitmap(base64Part)
                        } else null
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = emoji, fontSize = 28.sp)
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
    gameId: String,
    entry: BestiaryEntry,
    isGeneratingImage: Boolean,
    onClose: () -> Unit,
    onGenerateImage: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onSaveListField: (BestiaryEditableListField, String) -> Unit
) {
    var notesDraft by remember(entry.monsterId, entry.notes) { mutableStateOf(entry.notes) }
    var weaknessesDraft by remember(entry.monsterId, entry.observedWeaknesses) {
        mutableStateOf(entry.observedWeaknesses.joinToString(", "))
    }
    var resistancesDraft by remember(entry.monsterId, entry.observedResistances) {
        mutableStateOf(entry.observedResistances.joinToString(", "))
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = Color(0x33FFD700),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0x55FFD700))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonsterImage(
                        imageUrl = entry.imageUrl,
                        name = entry.name,
                        modifier = Modifier.size(112.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name.ifBlank { "Criatura desconocida" },
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (entry.description.isBlank()) "Sin descripción registrada." else entry.description,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "ID interno: ${entry.monsterId}",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            BestiarySectionCard(title = "🖼️ Ilustración del monstruo") {
                TextButton(onClick = onGenerateImage, enabled = !isGeneratingImage) {
                    if (isGeneratingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generando ilustración...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (entry.imageUrl.isBlank()) "Generar ilustración" else "Regenerar ilustración")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "📊 Estadísticas observadas") {
                DetailLine("HP máximo visto", entry.lastObservedStats.hpMaxObserved.takeIf { it > 0 }?.toString() ?: "Desconocido")
                DetailLine("CA observada", entry.lastObservedStats.armorClassObserved?.toString() ?: "Desconocida")
                DetailLine("Veces encontrado", entry.timesEncountered.toString())
                DetailLine("Veces derrotado", entry.timesDefeated.toString())
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "📍 Avistamientos") {
                DetailLine(
                    "Zonas",
                    entry.locationsSeen.takeIf { it.isNotEmpty() }?.joinToString() ?: "Sin datos"
                )
                DetailLine("Primera vez visto", formatTimestamp(entry.firstSeenAt))
                DetailLine("Última vez visto", formatTimestamp(entry.lastSeenAt))
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "✨ Habilidades y daño observado") {
                DetailListLine("Daño observado", entry.lastObservedStats.damageNotes)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))
                Spacer(Modifier.height(8.dp))
                DetailListLine("Habilidades vistas", entry.lastObservedStats.abilitiesSeen)
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "⚔️ Debilidades observadas") {
                EditableListField(
                    value = weaknessesDraft,
                    label = "Debilidades separadas por coma",
                    helper = "Ejemplo: fuego, sagrado, daño perforante",
                    onValueChange = { weaknessesDraft = it },
                    onSave = { onSaveListField(BestiaryEditableListField.WEAKNESSES, weaknessesDraft) }
                )
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "🛡️ Resistencias observadas") {
                EditableListField(
                    value = resistancesDraft,
                    label = "Resistencias separadas por coma",
                    helper = "Ejemplo: veneno, frío, daño físico",
                    onValueChange = { resistancesDraft = it },
                    onSave = { onSaveListField(BestiaryEditableListField.RESISTANCES, resistancesDraft) }
                )
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "🎁 Loot conocido más detallado") {
                DetailedLootSection(entry.detailedKnownLoot, entry.knownLoot)
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "🏷️ Etiquetas") {
                DetailListLine("Tags", entry.tags)
            }

            Spacer(Modifier.height(10.dp))

            BestiarySectionCard(title = "📝 Notas editables") {
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { notesDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    label = { Text("Anotaciones del aventurero") }
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSaveNotes(notesDraft) }) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar notas")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun EditableListField(
    value: String,
    label: String,
    helper: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        label = { Text(label) }
    )
    Spacer(Modifier.height(6.dp))
    Text(text = helper, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onSave) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar")
        }
    }
}

@Composable
private fun DetailedLootSection(
    detailedLoot: List<BestiaryLoot>,
    fallbackLoot: List<String>
) {
    when {
        detailedLoot.isNotEmpty() -> {
            detailedLoot.forEachIndexed { index, loot ->
                Column {
                    Text(
                        text = loot.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Categoría: ${loot.category} · Cantidad máxima observada: ${loot.quantityObserved} · Veces visto: ${loot.timesDropped}",
                        color = Color(0xFFFFD59A),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (loot.details.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = loot.details,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (index != detailedLoot.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        fallbackLoot.isNotEmpty() -> {
            DetailListLine("Loot registrado", fallbackLoot)
        }
        else -> {
            Text(
                text = "Todavía no hay botín confirmado para esta criatura.",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
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

private fun monsterEmoji(name: String): String = when {
    name.contains("dragón", true) || name.contains("dragon", true) -> "🐉"
    name.contains("esqueleto", true) || name.contains("skeleton", true) -> "💀"
    name.contains("orco", true) || name.contains("orc", true) -> "👹"
    name.contains("lobo", true) || name.contains("wolf", true) -> "🐺"
    name.contains("araña", true) || name.contains("spider", true) -> "🕷️"
    name.contains("troll", true) -> "🧌"
    name.contains("goblin", true) -> "👺"
    name.contains("slime", true) || name.contains("babosa", true) -> "🟢"
    else -> "👾"
}