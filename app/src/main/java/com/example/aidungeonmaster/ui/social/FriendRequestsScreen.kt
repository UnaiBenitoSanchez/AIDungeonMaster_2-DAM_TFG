package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aidungeonmaster.data.model.FriendRequest
import com.example.aidungeonmaster.ui.accessibility.VoiceFormAction
import com.example.aidungeonmaster.ui.accessibility.VoiceFormField
import com.example.aidungeonmaster.ui.accessibility.VoiceFormRegistry
import com.example.aidungeonmaster.ui.accessibility.VoiceFormScreen
import com.example.aidungeonmaster.ui.accessibility.VoiceInputType
import com.example.aidungeonmaster.ui.accessibility.findBestVoiceOption
import com.example.aidungeonmaster.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de friend requests screen.
fun FriendRequestsScreen(
    onBack: () -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val requests by viewModel.incomingRequests.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.startIncomingRequestsListener()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopIncomingRequestsListener()
        }
    }

    DisposableEffect(requests) {
        val registration = VoiceFormRegistry.register(
            VoiceFormScreen(
                screenName = "solicitudes de amistad",
                fields = listOf(
                    VoiceFormField(
                        label = "aceptar solicitud",
                        aliases = listOf(
                            "aceptar solicitud",
                            "acepta solicitud",
                            "aceptar a",
                            "acepta a"
                        ),
                        inputType = VoiceInputType.TEXT,
                        onValue = { value ->
                            val options = requests.flatMap { req ->
                                listOf(
                                    req.fromDisplayName,
                                    req.fromUsername,
                                    "@${req.fromUsername}"
                                )
                            }.distinct()

                            val selected = findBestVoiceOption(value, options)
                            val target = requests.firstOrNull { req ->
                                listOf(req.fromDisplayName, req.fromUsername, "@${req.fromUsername}")
                                    .any { it == selected }
                            }

                            if (target != null) {
                                viewModel.acceptRequest(target)
                            }
                        },
                        feedback = { value -> "Aceptando solicitud de $value." }
                    )
                ),
                actions = listOf(
                    VoiceFormAction(
                        label = "aceptar solicitud",
                        aliases = listOf(
                            "aceptar solicitud",
                            "acepta solicitud",
                            "aceptar amistad"
                        ),
                        enabled = { requests.size == 1 },
                        disabledFeedback = "Di el nombre de la persona. Por ejemplo: aceptar solicitud de Luna.",
                        onRun = {
                            requests.firstOrNull()?.let { viewModel.acceptRequest(it) }
                        },
                        feedback = requests.firstOrNull()?.let {
                            "Aceptando solicitud de ${it.fromDisplayName.ifBlank { it.fromUsername }}."
                        } ?: "No hay solicitudes pendientes."
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
                title = { Text("Solicitudes de amistad") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (requests.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "No tienes solicitudes pendientes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    FriendRequestCard(
                        request = request,
                        onAccept = { viewModel.acceptRequest(request) },
                        onReject = { viewModel.rejectRequest(request) }
                    )
                }
            }
        }
    }
}

@Composable
// Ejecuta la lógica de friend request card.
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = request.fromDisplayName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "@${request.fromUsername}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text("Aceptar")
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text("Rechazar")
                }
            }
        }
    }
}
