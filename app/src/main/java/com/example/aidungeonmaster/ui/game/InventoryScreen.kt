package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val character by viewModel.character.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInventory(gameId)
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
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                }
            } else if (character == null) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    // Extraemos el nombre: asume que el formato es userId_Nombre_Tema...
                    val partes = gameId.split("_")
                    val nombrePersonaje = if (partes.size > 1) partes[1] else "Héroe"

                    Text("Error al cargar el equipo de $nombrePersonaje", color = Color.White)
                }
            } else {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    // INFO DEL PERSONAJE (Header)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                        border = BorderStroke(1.dp, Color(0x55FFD700))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Icono de vida
                            Icon(Icons.Default.Healing, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "HP: ${character!!.hpCurrent} / ${character!!.hpMax}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // LISTA DE OBJETOS
                    if (character!!.inventory.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Mochila vacía. ¡Busca botín!", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(character!!.inventory) { item ->
                                InventoryItemRow(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(item: Item) {
    // Decidir icono según el tipo
    val (icon, color) = when (item.type) {
        "arma" -> Icons.Default.Hiking to Color(0xFFFF4500) // Naranja Rojizo
        "armadura" -> Icons.Default.Shield to Color(0xFFC0C0C0) // Plateado
        "consumible" -> Icons.Default.Healing to Color(0xFF32CD32) // Verde Lima
        else -> Icons.Default.Hiking to Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (item.description.isNotEmpty()) {
                    Text(item.description, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                if (item.effect.isNotEmpty()) {
                    Text("Efecto: ${item.effect}", color = Color.Cyan, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}