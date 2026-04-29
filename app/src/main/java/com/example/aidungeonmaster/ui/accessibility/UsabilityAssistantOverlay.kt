package com.example.aidungeonmaster.ui.accessibility

import com.example.aidungeonmaster.ui.i18n.Text

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.ui.settings.AppLanguage
import com.example.aidungeonmaster.ui.settings.AppLanguageManager
import com.example.aidungeonmaster.ui.theme.ColorBlindType
import com.example.aidungeonmaster.viewmodel.GameViewModel
import com.example.aidungeonmaster.utils.AdventureMusicEngine
import com.example.aidungeonmaster.utils.CombatMusicEngine

import com.example.aidungeonmaster.ui.settings.ColorBlindSettingsSheet

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun UsabilityAssistantOverlay(
    navController: NavHostController,
    currentRoute: String?,
    currentArguments: android.os.Bundle?,
    characters: List<Character>,
    myGuilds: List<Guild>,
    gameViewModel: GameViewModel,
    currentColorBlindType: ColorBlindType,
    onColorBlindChanged: (ColorBlindType) -> Unit,
    onRelaunchTutorial: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    openSheetRequest: Int = 0,
    showFloatingButton: Boolean = false
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(false) }
    var voiceState by remember { mutableStateOf(VoiceControlUiState()) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    var showColorBlindSheet by remember { mutableStateOf(false) }

    val commandHandlerState = rememberUpdatedState<(String) -> String>(
        newValue = { spokenCommand ->
            executeVoiceCommand(
                rawCommand = spokenCommand,
                navController = navController,
                currentRoute = currentRoute,
                currentArguments = currentArguments,
                characters = characters,
                myGuilds = myGuilds,
                gameViewModel = gameViewModel,
                currentColorBlindType = currentColorBlindType,
                onColorBlindChanged = onColorBlindChanged,
                onLanguageChanged = { language ->
                    AppLanguageManager.setLanguage(context, language)
                },
                onOpenUsabilityOptions = { showSheet = true },
                onStartVoiceControl = { voiceEnabled = true },
                onStopVoiceControl = { voiceEnabled = false },
                onRelaunchTutorial = onRelaunchTutorial,
                onLogout = onLogout
            )
        }
    )

    val voiceManager = remember(context) {
        VoiceControlManager(
            context = context,
            onUiStateChanged = { voiceState = it },
            onCommandRecognized = { command -> commandHandlerState.value(command) }
        )
    }

    LaunchedEffect(openSheetRequest) {
        if (openSheetRequest > 0) {
            showSheet = true
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDeniedMessage = if (granted) null else "Permiso de micrófono denegado. Actívalo para usar órdenes de voz."
        voiceEnabled = granted
    }

    LaunchedEffect(voiceEnabled) {
        AdventureMusicEngine.setVoiceControlDucking(voiceEnabled)
        CombatMusicEngine.setVoiceControlDucking(voiceEnabled)

        if (voiceEnabled) {
            voiceManager.start()
        } else {
            voiceManager.stop(announce = false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AdventureMusicEngine.setVoiceControlDucking(false)
            CombatMusicEngine.setVoiceControlDucking(false)
            voiceManager.destroy()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (voiceState.active) {
            VoiceStatusPill(
                state = voiceState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        if (showFloatingButton) {
            FloatingActionButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                containerColor = if (voiceState.active)
                    Color(0xFF1B5E20)
                else
                    MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (voiceState.active)
                    Color.White
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.AccessibilityNew,
                    contentDescription = "Opciones de usabilidad",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    if (showSheet) {
        AccessibilityOptionsSheet(
            currentColorBlindType = currentColorBlindType,
            voiceEnabled = voiceEnabled,
            voiceState = voiceState,
            permissionDeniedMessage = permissionDeniedMessage,
            onOpenColorBlind = {
                showSheet = false
                showColorBlindSheet = true
            },
            onVoiceEnabledChange = { enabled ->
                if (enabled) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        permissionDeniedMessage = null
                        voiceEnabled = true
                    } else {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                } else {
                    voiceEnabled = false
                }
            },
            onDismiss = { showSheet = false }
        )
    }

    if (showColorBlindSheet) {
        ColorBlindSettingsSheet(
            currentType = currentColorBlindType,
            onTypeSelected = { newType ->
                onColorBlindChanged(newType)
                showColorBlindSheet = false
                showSheet = true
            },
            onDismiss = {
                showColorBlindSheet = false
                showSheet = true
            }
        )
    }
}

@Composable
private fun VoiceStatusPill(
    state: VoiceControlUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (state.listening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = if (state.listening) Color(0xFF7CFC98) else Color(0xFFFFD700),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = state.status,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsabilitySettingsSheet(
    currentColorBlindType: ColorBlindType,
    voiceEnabled: Boolean,
    voiceState: VoiceControlUiState,
    permissionDeniedMessage: String?,
    onColorBlindChanged: (ColorBlindType) -> Unit,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingColorBlindType by remember(currentColorBlindType) { mutableStateOf(currentColorBlindType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opciones de usabilidad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configura ayudas visuales y manejo por voz de la aplicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            Surface(
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo daltónico", fontWeight = FontWeight.Bold)
                            Text(
                                "Aplica un filtro de color global sin modificar la lógica ni la navegación.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ColorBlindType.entries.forEach { type ->
                        ColorBlindChoiceRow(
                            type = type,
                            selected = pendingColorBlindType == type,
                            onClick = { pendingColorBlindType = type }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onColorBlindChanged(pendingColorBlindType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Aplicar filtro")
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (voiceEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Control por voz", fontWeight = FontWeight.Bold)
                            Text(
                                "Actívalo para navegar y dictar acciones sin tocar la pantalla.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = onVoiceEnabledChange
                        )
                    }

                    Text(
                        text = voiceState.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (voiceEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    voiceState.lastCommand?.takeIf { it.isNotBlank() }?.let { command ->
                        Text(
                            text = "Última orden: $command",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    permissionDeniedMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Ejemplos de órdenes", fontWeight = FontWeight.SemiBold)
                            Text("• Abre lista de amigos")
                            Text("• Abre la partida de Aria")
                            Text("• Abre inventario de Aria")
                            Text("• Vuelve atrás")
                            Text("• Dentro de una aventura: Ataco con mi espada")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun ColorBlindChoiceRow(
    type: ColorBlindType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = type.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibilityOptionsSheet(
    currentColorBlindType: ColorBlindType,
    voiceEnabled: Boolean,
    voiceState: VoiceControlUiState,
    permissionDeniedMessage: String?,
    onOpenColorBlind: () -> Unit,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedLanguage = AppLanguageManager.getSavedLanguage(context)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opciones de accesibilidad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configura ayudas visuales y control por voz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Idioma",
                        fontWeight = FontWeight.Bold
                    )

                    AppLanguage.entries.forEach { language ->
                        val selected = language == selectedLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (!selected) {
                                        AppLanguageManager.setLanguage(context, language)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(language.labelRes),
                                modifier = Modifier.weight(1f),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )

                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenColorBlind() },
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo daltónico",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentColorBlindType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (voiceEnabled)
                                Color(0xFF2E7D32)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Control por voz",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Permite navegar y dictar acciones mediante órdenes habladas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = onVoiceEnabledChange
                        )
                    }

                    Text(
                        text = voiceState.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (voiceEnabled)
                            Color(0xFF2E7D32)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    voiceState.lastCommand?.takeIf { it.isNotBlank() }?.let { command ->
                        Text(
                            text = "Última orden: $command",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    permissionDeniedMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Ejemplos de órdenes", fontWeight = FontWeight.SemiBold)
                            Text("• Abre lista de amigos")
                            Text("• Crear gremio")
                            Text("• Desactiva la voz")
                            Text("• Activa protanopía")
                            Text("• Colores normales")
                            Text("• Abre la partida de A")
                            Text("• Abre inventario de A")
                            Text("• Vuelve atrás")
                            Text("• Dentro de una aventura: Ataco con mi espada")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}
