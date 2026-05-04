package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.utils.ImageUtils
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Clase que encapsula la lógica de profile palette.
private data class ProfilePalette(val accent: String, val background: String)

private val profilePalettes = listOf(
    ProfilePalette("#D4AF37", "#1E1E1E"),
    ProfilePalette("#4FC3F7", "#102A43"),
    ProfilePalette("#FF8A65", "#3E2723"),
    ProfilePalette("#81C784", "#1B4332"),
    ProfilePalette("#BA68C8", "#2D1B69")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de social profile screen.
fun SocialProfileScreen(
    userUid: String,
    isMe: Boolean,
    onBack: () -> Unit,
    onOpenChat: ((String, String) -> Unit)? = null,
    onOpenPersonalRoom: ((String, String, String) -> Unit)? = null,
    viewModel: SocialViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val profileCharacters by viewModel.profileCharacters.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var accentColor by remember { mutableStateOf("#D4AF37") }
    var backgroundColor by remember { mutableStateOf("#1E1E1E") }

    val context = LocalContext.current

    LaunchedEffect(userUid) {
        viewModel.loadProfile(userUid)
        viewModel.loadProfileCharacters(userUid)
    }

    LaunchedEffect(profile?.uid) {
        profile?.let {
            displayName = it.displayName
            bio = it.bio
            accentColor = it.accentColor
            backgroundColor = it.profileBackgroundColor
        }
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadMyProfilePhoto(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMe) "Mi perfil" else "Perfil del amigo") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val currentProfile = profile

        if (currentProfile == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Cargando perfil...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(parseColor(backgroundColor))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val profileBitmap = remember(currentProfile.photoUrl) {
                            runCatching {
                                if (currentProfile.photoUrl.startsWith("data:image")) {
                                    val base64Part =
                                        currentProfile.photoUrl.substringAfter("base64,", "")
                                    if (base64Part.isNotBlank()) {
                                        ImageUtils.base64ToBitmap(base64Part)
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }.getOrNull()
                        }

                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(parseColor(accentColor), CircleShape),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentProfile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.Black
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = currentProfile.displayName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = "@${currentProfile.username}",
                                color = parseColor(accentColor)
                            )

                            PresenceIndicator(
                                isOnline = currentProfile.isOnline,
                                lastSeen = currentProfile.lastSeen
                            )
                        }
                    }

                    Text(
                        text = if (bio.isBlank()) "Sin biografía todavía." else bio,
                        color = Color.White
                    )
                }
            }

            if (isMe) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre visible") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Elegir foto")
                }

                Text("Colores del perfil", style = MaterialTheme.typography.titleMedium)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profilePalettes.forEach { palette ->
                        ProfilePaletteChip(
                            palette = palette,
                            selected = palette.accent == accentColor &&
                                    palette.background == backgroundColor,
                            onClick = {
                                accentColor = palette.accent
                                backgroundColor = palette.background
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveMyProfile(displayName, bio, accentColor, backgroundColor)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar perfil")
                }
            } else {
                Button(
                    onClick = {
                        onOpenChat?.invoke(currentProfile.uid, currentProfile.displayName)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir chat privado")
                }

                Text(
                    text = "Salas de personajes",
                    style = MaterialTheme.typography.titleMedium
                )

                if (profileCharacters.isEmpty()) {
                    Text(
                        text = "Este amigo todavía no tiene personajes visibles.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profileCharacters.forEach { character ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = character.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "${character.race} · ${character.characterClass} · Nivel ${character.level}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = {
                                            onOpenPersonalRoom?.invoke(
                                                currentProfile.uid,
                                                character.id,
                                                character.name
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Visitar sala")
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
// Ejecuta la lógica de profile palette chip.
private fun ProfilePaletteChip(
    palette: ProfilePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .size(18.dp)
                    .background(parseColor(palette.accent), CircleShape)
            ) {}

            Column(
                modifier = Modifier
                    .size(18.dp)
                    .background(parseColor(palette.background), CircleShape)
            ) {}

            Text(if (selected) "Seleccionado" else "Aplicar")
        }
    }
}

@Composable
// Ejecuta la lógica de presence indicator.
fun PresenceIndicator(
    isOnline: Boolean,
    lastSeen: Long?
) {
    val text = if (isOnline) {
        "En línea"
    } else {
        "Última vez: " + when {
            lastSeen == null || lastSeen <= 0L -> "sin datos"
            System.currentTimeMillis() - lastSeen < 60_000 -> "hace un momento"
            System.currentTimeMillis() - lastSeen < 3_600_000 ->
                "hace ${(System.currentTimeMillis() - lastSeen) / 60_000} min"
            System.currentTimeMillis() - lastSeen < 86_400_000 ->
                "hace ${(System.currentTimeMillis() - lastSeen) / 3_600_000} h"
            else ->
                "hace ${(System.currentTimeMillis() - lastSeen) / 86_400_000} d"
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            color = if (isOnline) Color(0xFF7CFC00) else Color.LightGray
        )
    }
}

// Analiza color.
private fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { Color(0xFF1E1E1E) }

// Formatea last seen.
private fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0L) return "sin datos"
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}
