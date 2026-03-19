package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.viewmodel.GameViewModel
import com.example.aidungeonmaster.viewmodel.InventoryViewModel // Importante añadir esto
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    navController: androidx.navigation.NavHostController,
    userId: String,
    characterName: String,
    theme: String,
    viewModel: GameViewModel = viewModel(),
    inventoryViewModel: InventoryViewModel = viewModel() // Inyectamos el ViewModel del inventario/vida
) {
    val listState = rememberLazyListState()
    val messages: List<Pair<String, String>> by viewModel.messages.collectAsState()
    val options: List<String> by viewModel.currentOptions.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()

    // Observamos el personaje para obtener su vida
    val characterState by inventoryViewModel.character.collectAsState()
    val hpCurrent = characterState?.hpCurrent ?: 20
    val hpMax = characterState?.hpMax ?: 20

    var customAction by remember { mutableStateOf("") }
    // gameId incluye el tema → identifica la partida/historia concreta
    val gameId = "${userId}_${characterName}_${theme}".replace(" ", "_")
    // charId sin tema → identifica al personaje (inventario/HP persisten entre aventuras)
    val charId = "${userId}_${characterName}"

    // Observar el paso de aventura para detectar inicio de combate
    val currentStep by viewModel.currentAdventureStep.collectAsState()
    var showPixelTransition by remember { mutableStateOf(false) }

    // Cargar inventario/vida del personaje (charId, sin tema)
    LaunchedEffect(charId) {
        inventoryViewModel.loadInventory(charId)
        viewModel.startStory(userId, characterName, theme)
    }

    // Detectar si el DM ha iniciado un combate
    LaunchedEffect(currentStep?.combatStarted) {
        if (currentStep?.combatStarted == true && currentStep?.enemy != null) {
            showPixelTransition = true
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    MedievalBackground {
        // Box para poder superponer el overlay de transición sobre el Scaffold
        Box(modifier = Modifier.fillMaxSize()) {

            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f)) // Un fondo elegante para la cabecera
                    ) {
                        // Fila para el Título y la Mochila
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp), // Padding superior para evitar la cámara/notch
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Título con espacio de sobra
                            Box(modifier = Modifier.weight(1f)) {
                                MedievalTitle(
                                    text = "Aventura: $theme",
                                    modifier = Modifier.padding(vertical = 4.dp) // Espacio extra para que no se corte
                                )
                            }

                            // Botón de Inventario
                            IconButton(onClick = {
                                navController.navigate("inventory/$charId")
                            }) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = "Mochila",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Indicador de Vida justo debajo
                        PlayerStatsHeader(hpCurrent = hpCurrent, hpMax = hpMax)
                    }
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
                        color = Color.Black.copy(alpha = 0.9f) // Un poco de transparencia para el estilo
                    ) {
                        Column {
                            if (options.isNotEmpty() && !isLoading) {
                                LazyRow(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(options) { opcion ->
                                        AssistChip(
                                            onClick = { viewModel.sendPlayerAction(opcion) },
                                            label = { Text(opcion, color = Color.Cyan) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                labelColor = Color.Cyan,
                                                containerColor = Color(0xFF1E1E1E)
                                            )
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    navController.navigate("qr_scanner/$charId") // charId para QR → inventario
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = Color.Yellow)
                                }

                                TextField(
                                    value = customAction,
                                    onValueChange = { customAction = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("¿Qué quieres hacer?", color = Color.Gray) },
                                    maxLines = 2,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E1E1E),
                                        unfocusedContainerColor = Color(0xFF1E1E1E),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.Cyan
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        if (customAction.isNotBlank()) {
                                            viewModel.sendCustomAction(customAction)
                                            customAction = ""
                                        }
                                    },
                                    enabled = !isLoading && customAction.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Enviar",
                                        tint = if (customAction.isNotBlank()) Color.Cyan else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = Color.Transparent // Para ver el MedievalBackground
            ) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(messages) { message ->
                        GameMessageBubble(author = message.first, text = message.second)
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFFFFD700) // Color Oro
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // ── OVERLAY: Transición de píxeles al entrar en combate ──
            if (showPixelTransition) {
                PixelTransitionOverlay {
                    showPixelTransition = false
                    navController.navigate("combat/$charId")
                }
            }

        } // fin Box
    } // fin MedievalBackground
}

@Composable
fun GameMessageBubble(author: String, text: String) {
    val isAI = author == "DM"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isAI) Alignment.Start else Alignment.End
    ) {
        Text(
            text = author,
            style = MaterialTheme.typography.labelSmall,
            color = if (isAI) Color(0xFFBB86FC) else Color(0xFF03DAC5),
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = if (isAI) Color(0xFF1E1E1E) else Color(0xFF2C2C2C),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isAI) 0.dp else 12.dp,
                bottomEnd = if (isAI) 12.dp else 0.dp
            ),
            tonalElevation = 2.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun PlayerStatsHeader(hpCurrent: Int, hpMax: Int) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f), // Fondo semitransparente para legibilidad
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HP", color = Color.Red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { if (hpMax > 0) hpCurrent.toFloat() / hpMax.toFloat() else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp) // Un poco más fina queda más elegante
                    .clip(RoundedCornerShape(5.dp)),
                color = Color.Green,
                trackColor = Color.DarkGray
            )
        }
    }
}