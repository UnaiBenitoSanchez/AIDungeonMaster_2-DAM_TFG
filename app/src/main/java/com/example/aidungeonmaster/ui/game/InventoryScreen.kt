package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadInventory(gameId) }

    val character by viewModel.character.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Mensaje de feedback al usar un objeto
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(feedbackMsg) {
        if (feedbackMsg != null) {
            kotlinx.coroutines.delay(2500)
            feedbackMsg = null
        }
    }

    MedievalBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { MedievalTitle("MOCHILA") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                } else if (character == null) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        val name = gameId.split("_").getOrElse(1) { "Héroe" }
                        Text("Error al cargar el equipo de $name", color = Color.White)
                    }
                } else {
                    Column(modifier = Modifier.padding(padding).padding(16.dp)) {

                        // ── CABECERA DE VIDA ─────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors   = CardDefaults.cardColors(containerColor = Color(0x44000000)),
                            border   = BorderStroke(1.dp, Color(0x55FFD700))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Fila HP
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Healing, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "HP: ${character!!.hpCurrent} / ${character!!.hpMax}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Barra de vida mini
                                    val ratio = if (character!!.hpMax > 0)
                                        character!!.hpCurrent.toFloat() / character!!.hpMax else 0f
                                    val barColor = when {
                                        ratio > 0.5f  -> Color(0xFF22CC55)
                                        ratio > 0.25f -> Color(0xFFFFAA00)
                                        else          -> Color(0xFFCC2222)
                                    }
                                    LinearProgressIndicator(
                                        progress       = { ratio },
                                        modifier       = Modifier.width(100.dp).height(8.dp),
                                        color          = barColor,
                                        trackColor     = Color(0xFF333333)
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                // Fila Monedas
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "🪙",
                                        fontSize = 20.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text       = "${character!!.coins} monedas de oro",
                                        color      = Color(0xFFFFD700),
                                        style      = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // ── LISTA DE OBJETOS ─────────────────────────────────
                        if (character!!.inventory.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Mochila vacía. ¡Busca botín!", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(
                                    items = character!!.inventory
                                ) { index, item ->
                                    InventoryItemRow(
                                        item  = item,
                                        onUse = { usedItem ->
                                            val msg = viewModel.useItem(gameId, usedItem, character!!.hpCurrent, character!!.hpMax)
                                            feedbackMsg = msg
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── TOAST DE FEEDBACK ────────────────────────────────────
                feedbackMsg?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        color  = Color(0xFF1A1A1A),
                        shape  = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Text(
                            msg,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            color    = Color(0xFFFFD700),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── FILA DE ITEM CON BOTÓN USAR ───────────────────────────────────────────────

@Composable
fun InventoryItemRow(
    item: Item,
    onUse: ((Item) -> Unit)? = null   // null = solo lectura (en BotinEncontradoDialog)
) {
    val isConsumable = item.type in listOf("pocion", "consumible", "comida", "pergamino", "veneno", "explosivo")

    val (icon, iconColor) = when (item.type) {
        "arma"      -> Icons.Default.Hiking   to Color(0xFFFF4500)
        "armadura"  -> Icons.Default.Shield   to Color(0xFFC0C0C0)
        "pocion",
        "consumible" -> Icons.Default.Healing to Color(0xFF32CD32)
        "pergamino" -> Icons.Default.Hiking   to Color(0xFFBB88FF)
        "veneno"    -> Icons.Default.Hiking   to Color(0xFF88FF44)
        "explosivo" -> Icons.Default.Hiking   to Color(0xFFFF8800)
        "reliquia"  -> Icons.Default.Hiking   to Color(0xFFFFD700)
        else        -> Icons.Default.Hiking   to Color.Gray
    }

    val typeEmoji = when (item.type) {
        "pocion"    -> "🧪"
        "arma"      -> "⚔️"
        "armadura"  -> "🛡️"
        "pergamino" -> "📜"
        "veneno"    -> "☠️"
        "explosivo" -> "💣"
        "reliquia"  -> "✨"
        else        -> "🎒"
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
        colors   = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del tipo
            Text(typeEmoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))

            // Info del objeto
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (item.description.isNotEmpty())
                    Text(item.description, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                if (item.effect.isNotEmpty())
                    Text("✦ ${item.effect}", color = Color.Cyan, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            }

            // Botón USAR (solo para consumibles y si se pasó callback)
            if (onUse != null && isConsumable) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onUse(item) },
                    modifier = Modifier.height(36.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF004400)),
                    border   = BorderStroke(1.dp, Color(0xFF22CC55)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("Usar", color = Color(0xFF22CC55), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}