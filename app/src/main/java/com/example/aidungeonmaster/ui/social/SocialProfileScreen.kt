package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.viewmodel.AuthViewModel
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable

// Clase que encapsula la lógica de profile palette.
private data class ProfilePalette(val accent: String, val background: String)

private val profilePalettes = listOf(
    ProfilePalette("#D4AF37", "#1E1E1E"),
    ProfilePalette("#4FC3F7", "#102A43"),
    ProfilePalette("#FF8A65", "#3E2723"),
    ProfilePalette("#81C784", "#1B4332"),
    ProfilePalette("#BA68C8", "#2D1B69")
)

private const val PRESENCE_STALE_THRESHOLD_MS = 90_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de social profile screen.
fun SocialProfileScreen(
    userUid: String,
    isMe: Boolean,
    onBack: () -> Unit,
    onOpenChat: ((String, String) -> Unit)? = null,
    onOpenPersonalRoom: ((String, String, String) -> Unit)? = null,
    viewModel: SocialViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val profileCharacters by viewModel.profileCharacters.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var accentColor by remember { mutableStateOf("#D4AF37") }
    var backgroundColor by remember { mutableStateOf("#1E1E1E") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }

    var pendingProfileBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showProfilePhotoPreview by remember { mutableStateOf(false) }

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

    LaunchedEffect(authViewModel.errorMessage) {
        authViewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearError()
        }
    }

    LaunchedEffect(passwordSuccessMessage) {
        if (!passwordSuccessMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(passwordSuccessMessage!!)
            passwordSuccessMessage = null
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingProfileBitmap = decodeBitmapFromUri(context, uri)

            if (pendingProfileBitmap == null) {
                viewModel.showMessage("No se pudo leer la imagen seleccionada")
            }
        }
    }

    val canChangePassword = authViewModel.canCurrentUserChangePassword()
    val isGoogleOnlyAccount = authViewModel.isCurrentUserGoogleOnly()

    if (showProfilePhotoPreview) {
        ProfilePhotoPreviewDialog(
            photoUrl = profile?.photoUrl.orEmpty(),
            displayName = profile?.displayName.orEmpty().ifBlank { "Perfil" },
            accentColor = parseColor(accentColor),
            backgroundColor = parseColor(backgroundColor),
            onDismiss = { showProfilePhotoPreview = false }
        )
    }

    pendingProfileBitmap?.let { bitmap ->
        ProfileImageCropperDialog(
            sourceBitmap = bitmap,
            onCancel = { pendingProfileBitmap = null },
            onCropConfirmed = { dataUrl ->
                pendingProfileBitmap = null
                viewModel.uploadMyProfilePhotoDataUrl(dataUrl)
            }
        )
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
                        SocialUserAvatar(
                            photoUrl = currentProfile.photoUrl,
                            displayName = currentProfile.displayName,
                            size = 64.dp,
                            accent = parseColor(accentColor),
                            modifier = Modifier.clickable {
                                showProfilePhotoPreview = true
                            }
                        )

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

            AdventurerInsightsCard(
                characters = profileCharacters,
                accentColor = parseColor(accentColor),
                backgroundColor = parseColor(backgroundColor)
            )

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

                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Seguridad de la cuenta",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = if (canChangePassword) {
                                "Puedes cambiar tu contraseña desde aquí. Te pediremos la contraseña actual para reautenticarte."
                            } else if (isGoogleOnlyAccount) {
                                "Esta cuenta ha iniciado sesión solo con Google. El cambio de contraseña queda desactivado en la app."
                            } else {
                                "El cambio de contraseña no está disponible para esta cuenta."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Contraseña actual") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = canChangePassword && !authViewModel.isChangingPassword,
                            visualTransformation = if (currentPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { currentPasswordVisible = !currentPasswordVisible },
                                    enabled = canChangePassword
                                ) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (currentPasswordVisible) "Ocultar contraseña actual" else "Mostrar contraseña actual"
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Nueva contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = canChangePassword && !authViewModel.isChangingPassword,
                            visualTransformation = if (newPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { newPasswordVisible = !newPasswordVisible },
                                    enabled = canChangePassword
                                ) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (newPasswordVisible) "Ocultar nueva contraseña" else "Mostrar nueva contraseña"
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Confirmar nueva contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = canChangePassword && !authViewModel.isChangingPassword,
                            visualTransformation = if (confirmNewPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible },
                                    enabled = canChangePassword
                                ) {
                                    Icon(
                                        imageVector = if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmNewPasswordVisible) "Ocultar confirmación de contraseña" else "Mostrar confirmación de contraseña"
                                    )
                                }
                            }
                        )

                        Button(
                            onClick = {
                                when {
                                    newPassword != confirmNewPassword -> {
                                        authViewModel.errorMessage = "Las contraseñas nuevas no coinciden."
                                    }
                                    newPassword == currentPassword -> {
                                        authViewModel.errorMessage = "La nueva contraseña debe ser distinta de la actual."
                                    }
                                    else -> {
                                        authViewModel.changeCurrentUserPassword(
                                            currentPassword = currentPassword,
                                            newPassword = newPassword
                                        ) {
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmNewPassword = ""
                                            currentPasswordVisible = false
                                            newPasswordVisible = false
                                            confirmNewPasswordVisible = false
                                            passwordSuccessMessage = "Contraseña actualizada correctamente."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canChangePassword && !authViewModel.isChangingPassword
                        ) {
                            if (authViewModel.isChangingPassword) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Cambiar contraseña")
                            }
                        }
                    }
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
    val effectiveOnline = isOnline &&
            lastSeen != null &&
            lastSeen > 0L &&
            (System.currentTimeMillis() - lastSeen) <= PRESENCE_STALE_THRESHOLD_MS

    val text = if (effectiveOnline) {
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
                .background(if (effectiveOnline) Color(0xFF4CAF50) else Color.Gray)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            color = if (effectiveOnline) Color(0xFF7CFC00) else Color.LightGray
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
