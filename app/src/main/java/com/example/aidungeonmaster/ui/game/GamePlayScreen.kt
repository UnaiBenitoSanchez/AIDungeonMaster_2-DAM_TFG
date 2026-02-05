package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.viewmodel.GameViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun GamePlayScreen(
    navController: androidx.navigation.NavHostController,
    userId: String, // <--- NUEVO: Añadimos el ID del usuario
    characterName: String,
    theme: String,
    viewModel: GameViewModel = viewModel()
) {
    val listState = rememberLazyListState()

    val messages: List<Pair<String, String>> by viewModel.messages.collectAsState()
    val options: List<String> by viewModel.currentOptions.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()

    // Sincronizamos con los 3 parámetros que espera el ViewModel
    LaunchedEffect(Unit) {
        viewModel.startStory(userId, characterName, theme)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Cabecera
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(
                text = "Aventura: $theme - Héroe: $characterName",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Chat
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                GameMessageBubble(author = message.first, text = message.second)
            }

            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLoading) "El DM está pensando..." else "¿Qué quieres hacer?",
            color = Color.Gray,
            style = MaterialTheme.typography.labelLarge
        )

        // Botones de opciones
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayOptions = if (options.isEmpty() && !isLoading) listOf("Continuar") else options

            displayOptions.forEach { accion ->
                Button(
                    onClick = { viewModel.sendPlayerAction(accion) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    Text(text = accion, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
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