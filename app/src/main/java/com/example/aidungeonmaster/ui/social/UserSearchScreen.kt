package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.ui.accessibility.VoiceFormAction
import com.example.aidungeonmaster.ui.accessibility.VoiceFormField
import com.example.aidungeonmaster.ui.accessibility.VoiceFormRegistry
import com.example.aidungeonmaster.ui.accessibility.VoiceFormScreen
import com.example.aidungeonmaster.ui.accessibility.VoiceInputType
import com.example.aidungeonmaster.ui.accessibility.findBestVoiceOption
import com.example.aidungeonmaster.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(query, isSearching, results) {
        val registration = VoiceFormRegistry.register(
            VoiceFormScreen(
                screenName = "busqueda de usuarios",
                fields = listOf(
                    VoiceFormField(
                        label = "buscar usuario",
                        aliases = listOf(
                            "buscar usuario",
                            "buscar jugador",
                            "buscar aventurero",
                            "usuario",
                            "jugador",
                            "aventurero",
                            "busqueda",
                            "búsqueda"
                        ),
                        inputType = VoiceInputType.USERNAME,
                        onValue = { value ->
                            query = value

                            if (value.length >= 2) {
                                viewModel.searchUsers(value)
                            }
                        },
                        feedback = { value -> "Buscando usuarios por $value." }
                    ),
                    VoiceFormField(
                        label = "enviar solicitud",
                        aliases = listOf(
                            "enviar solicitud",
                            "mandar solicitud",
                            "solicitud a",
                            "agregar amigo",
                            "anadir amigo",
                            "añadir amigo"
                        ),
                        inputType = VoiceInputType.TEXT,
                        onValue = { value ->
                            val options = results.flatMap { user ->
                                listOf(user.displayName, user.username, "@${user.username}")
                            }.distinct()

                            val selected = findBestVoiceOption(value, options)
                            val target = results.firstOrNull { user ->
                                listOf(user.displayName, user.username, "@${user.username}")
                                    .any { it == selected }
                            }

                            if (target != null) {
                                viewModel.sendFriendRequest(target)
                            }
                        },
                        feedback = { value -> "Enviando solicitud de amistad a $value." }
                    )
                ),
                actions = listOf(
                    VoiceFormAction(
                        label = "enviar solicitud",
                        aliases = listOf(
                            "enviar solicitud",
                            "mandar solicitud",
                            "agregar amigo"
                        ),
                        enabled = { results.size == 1 },
                        disabledFeedback = "Di el nombre del usuario. Por ejemplo: enviar solicitud a Unai.",
                        onRun = {
                            results.firstOrNull()?.let { viewModel.sendFriendRequest(it) }
                        },
                        feedback = results.firstOrNull()?.let {
                            "Enviando solicitud a ${it.displayName.ifBlank { it.username }}."
                        } ?: "No hay usuarios para enviar solicitud."
                    )
                )
            )
        )

        onDispose { registration.dispose() }
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar aventureros") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("<-") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it

                    if (it.length >= 2) {
                        viewModel.searchUsers(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre o usuario") },
                placeholder = { Text("Ej: unai_gm") }
            )

            when {
                isSearching -> {
                    CircularProgressIndicator()
                }

                query.length < 2 -> {
                    Text(
                        "Escribe al menos 2 caracteres.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                results.isEmpty() -> {
                    Text(
                        "No se encontraron usuarios.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { it.uid }) { user ->
                            UserResultCard(
                                user = user,
                                onSendRequest = { viewModel.sendFriendRequest(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserResultCard(
    user: AppUser,
    onSendRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                user.displayName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "@${user.username}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Button(onClick = onSendRequest) {
                Text("Enviar solicitud")
            }
        }
    }
}
