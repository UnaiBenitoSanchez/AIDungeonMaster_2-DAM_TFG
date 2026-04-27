package com.example.aidungeonmaster.ui.home

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aidungeonmaster.data.model.Character
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.viewmodel.AchievementViewModel

private val SheetBg = Color(0xFF120C07)
private val ParchmentTop = Color(0xFFF8EAC5)
private val ParchmentBottom = Color(0xFFE4C98E)
private val Ink = Color(0xFF2D1608)
private val InkSoft = Color(0xFF6B3F1D)
private val Border = Color(0xFF4A270E)
private val Accent = Color(0xFF7A461D)
private val AccentDark = Color(0xFF3A1F0B)
private val BloodRed = Color(0xFF8B1E16)
private val CardCream = Color(0xFFFFF0C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    userId: String,
    characterName: String,
    onBack: () -> Unit,
    onOpenRoom: (String, String) -> Unit,
    onContinueAdventure: (String, String, String) -> Unit,
    achievementViewModel: AchievementViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var character by remember { mutableStateOf<Character?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val current = character ?: return@rememberLauncherForActivityResult

        if (uri != null) {
            CharacterSheetPdfExporter.exportCharacterSheet(
                context = context,
                uri = uri,
                character = current
            ).onSuccess {
                achievementViewModel.onCharacterSheetExported()
                errorMessage = "Ficha PDF guardada correctamente."
            }.onFailure {
                errorMessage = it.message ?: "No se pudo guardar el PDF."
            }
        }
    }

    LaunchedEffect(userId, characterName) {
        isLoading = true
        errorMessage = null

        runCatching {
            loadCharacterSheet(userId, characterName)
        }.onSuccess {
            character = it
        }.onFailure {
            errorMessage = it.message ?: "No se pudo cargar la ficha."
        }

        isLoading = false
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage!!)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha RPG") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            character == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se pudo cargar la ficha del personaje.")
                }
            }

            else -> {
                val current = character!!
                val xpToNext = current.xpToNextLevel.coerceAtLeast(1)
                val xpProgress = (current.xp.toFloat() / xpToNext.toFloat()).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(SheetBg)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.cardColors(containerColor = ParchmentTop),
                        border = BorderStroke(2.dp, Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ParchmentTop,
                                            Color(0xFFF2DEAD),
                                            ParchmentBottom
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SheetHeader(current.name)

                            IdentitySection(current)

                            SectionTitle("Progreso")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MainStatBox("Nivel", current.level.toString(), Modifier.weight(1f))
                                MainStatBox("XP", "${current.xp}/$xpToNext", Modifier.weight(1f))
                                MainStatBox("Vida", "${current.hpCurrent}/${current.hpMax}", Modifier.weight(1f))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = { xpProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = BloodRed,
                                    trackColor = Color(0xFFCFAE64)
                                )

                                Text(
                                    text = "Progreso de experiencia",
                                    color = InkSoft,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }

                            SectionTitle("Atributos")

                            AttributeGrid(current)

                            SectionTitle("Combate y partida")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MainStatBox("CA", current.armorClass.toString(), Modifier.weight(1f))
                                MainStatBox("Competencia", "+${current.profBonus}", Modifier.weight(1f))
                                MainStatBox("Iniciativa", signed(current.initiativeBonus), Modifier.weight(1f))
                            }

                            DetailPanel(
                                title = "Última partida",
                                value = formatLastPlayed(current.lastPlayed)
                            )

                            if (current.physicalTraits.isNotBlank()) {
                                SectionTitle("Rasgos físicos")

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFFFFF3CE).copy(alpha = 0.78f),
                                    border = BorderStroke(1.dp, Border.copy(alpha = 0.65f))
                                ) {
                                    Text(
                                        text = current.physicalTraits,
                                        color = Ink,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Serif,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val theme = current.gameTheme.orEmpty()
                                if (theme.isBlank()) {
                                    errorMessage = "Este personaje todavía no tiene partida configurada."
                                } else {
                                    onContinueAdventure(userId, current.name, theme)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Continuar")
                        }

                        FilledTonalButton(
                            onClick = {
                                val partidaId = "${userId}_${current.name}"
                                onOpenRoom(partidaId, current.name)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Sala")
                        }
                    }

                    Button(
                        onClick = {
                            val safeName = current.name
                                .replace(" ", "_")
                                .replace("/", "_")
                                .replace("\\", "_")

                            pdfLauncher.launch("Ficha_$safeName.pdf")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloodRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Descargar ficha en PDF")
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private suspend fun loadCharacterSheet(
    userId: String,
    characterName: String
): Character {
    val db = FirebaseFirestore.getInstance()

    val characterDocs = db.collection("users")
        .document(userId)
        .collection("characters")
        .get()
        .await()

    val characterDoc = characterDocs.documents.firstOrNull { doc ->
        val name = doc.getString("name").orEmpty()
        name.equals(characterName, ignoreCase = true)
    } ?: error("No se encontró el personaje.")

    var character = characterDoc.toObject(Character::class.java)
        ?.copy(id = characterDoc.id)
        ?: error("No se pudo leer el personaje.")

    val partidaId = "${userId}_${character.name}"
    val partidaSnap = db.collection("partidas")
        .document(partidaId)
        .get()
        .await()

    if (partidaSnap.exists()) {
        character = character.copy(
            hpMax = partidaSnap.getLong("hpMax")?.toInt() ?: character.hpMax,
            hpCurrent = partidaSnap.getLong("hpCurrent")?.toInt() ?: character.hpCurrent,
            lastPlayed = partidaSnap.getLong("lastPlayed") ?: character.lastPlayed,
            xp = partidaSnap.getLong("xp")?.toInt() ?: character.xp,
            level = partidaSnap.getLong("level")?.toInt() ?: character.level,
            coins = partidaSnap.getLong("coins")?.toInt() ?: character.coins
        )
    }

    return character
}

@Composable
private fun SheetHeader(characterName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "FICHA DE AVENTURERO",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold,
            color = AccentDark
        )

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = AccentDark.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AccentDark.copy(alpha = 0.45f))
        ) {
            Text(
                text = characterName,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                color = AccentDark,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun IdentitySection(character: Character) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFF1C7).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            CharacterPortrait(
                portraitUrl = character.portraitUrl,
                fallback = character.name.take(1).uppercase(),
                modifier = Modifier.size(108.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SheetLine("Nombre", character.name)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SheetLine("Raza", character.race, Modifier.weight(1f))
                    SheetLine("Clase", character.characterClass, Modifier.weight(1f))
                }
                SheetLine("Tema / mundo", character.gameTheme.orEmpty().ifBlank { "Sin mundo asignado" })
            }
        }
    }
}

@Composable
private fun CharacterPortrait(
    portraitUrl: String,
    fallback: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(portraitUrl) {
        runCatching {
            val rawBase64 = when {
                portraitUrl.startsWith("data:image") -> portraitUrl.substringAfter("base64,", "")
                portraitUrl.startsWith("iVBOR") || portraitUrl.length > 100 -> portraitUrl
                else -> ""
            }

            if (rawBase64.isBlank()) null
            else {
                val bytes = Base64.decode(rawBase64, Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE2C47A),
        border = BorderStroke(2.dp, Border)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Retrato",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    text = fallback.ifBlank { "?" },
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SheetLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = InkSoft,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = value.ifBlank { "—" },
            color = Ink,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Surface(
        color = Accent,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 2.dp
    ) {
        Text(
            text = text.uppercase(),
            color = Color(0xFFFFE7A3),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MainStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CardCream.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                color = InkSoft,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AttributeGrid(character: Character) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttributeBox("Fuerza", character.strTotal, abilityModifier(character.strTotal), Modifier.weight(1f))
            AttributeBox("Destreza", character.dexTotal, abilityModifier(character.dexTotal), Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttributeBox("Constitución", character.conTotal, abilityModifier(character.conTotal), Modifier.weight(1f))
            AttributeBox("Inteligencia", character.intTotal, abilityModifier(character.intTotal), Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttributeBox("Sabiduría", character.wisTotal, abilityModifier(character.wisTotal), Modifier.weight(1f))
            AttributeBox("Carisma", character.chaTotal, abilityModifier(character.chaTotal), Modifier.weight(1f))
        }
    }
}

@Composable
private fun AttributeBox(
    label: String,
    value: Int,
    statModifier: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardCream.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = InkSoft,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value.toString(),
                color = Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BloodRed.copy(alpha = 0.10f)
            ) {
                Text(
                    text = signed(statModifier),
                    color = BloodRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DetailPanel(
    title: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF1C7).copy(alpha = 0.70f),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.65f))
    ) {
        SheetLine(
            label = title,
            value = value,
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun abilityModifier(score: Int): Int {
    return floor((score - 10) / 2.0).toInt()
}

private fun signed(value: Int): String {
    return if (value >= 0) "+$value" else value.toString()
}

private fun formatLastPlayed(timestamp: Long): String {
    if (timestamp <= 0L) return "Sin partidas todavía"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}