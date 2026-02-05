package com.example.aidungeonmaster.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete // Icono de borrar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.navigation.Screen
import com.example.aidungeonmaster.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = viewModel()) {
    val characters by viewModel.characters.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Estado para el diálogo de confirmación de borrado
    var characterToDelete by remember { mutableStateOf<Character?>(null) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tus Personajes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
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
                                navController.navigate(Screen.GameSetup.createRoute(userId, character.name))
                            },
                            onDelete = {
                                // Guardamos qué personaje queremos borrar para mostrar el aviso
                                characterToDelete = character
                            }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGO DE CREACIÓN
    if (showDialog) {
        CreateCharacterDialog(
            onDismiss = { showDialog = false },
            onCreate = { name, race, clazz, stats, traits ->
                viewModel.saveCharacter(name, race, clazz, stats, traits)
                showDialog = false
            }
        )
    }

    // DIÁLOGO DE CONFIRMACIÓN DE BORRADO
    characterToDelete?.let { character ->
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            title = { Text("¿Eliminar personaje?") },
            text = { Text("¿Estás seguro de que quieres borrar a ${character.name}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCharacter(character.id)
                        characterToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { characterToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CharacterCard(character: Character, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${character.race} • ${character.characterClass}", style = MaterialTheme.typography.bodyMedium)
            }

            // BOTÓN DE BORRAR
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}