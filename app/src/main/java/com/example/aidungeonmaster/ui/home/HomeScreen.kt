package com.example.aidungeonmaster.ui.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.aidungeonmaster.R
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.navigation.Screen
import com.example.aidungeonmaster.ui.social.SocialMenuSheet
import com.example.aidungeonmaster.ui.tutorial.DragonTutorialOverlay
import com.example.aidungeonmaster.ui.tutorial.TutorialStep
import com.example.aidungeonmaster.ui.tutorial.tutorialAnchor
import com.example.aidungeonmaster.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val tutorialPrefs = remember {
        context.getSharedPreferences("dragon_tutorial_prefs", Context.MODE_PRIVATE)
    }

    var showTutorial by rememberSaveable {
        mutableStateOf(!tutorialPrefs.getBoolean("home_tutorial_completed", false))
    }

    var tutorialStepIndex by rememberSaveable { mutableIntStateOf(0) }

    val tutorialTargets = remember {
        mutableStateMapOf<String, Rect>()
    }

    val characters by viewModel.characters.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var characterToDelete by remember { mutableStateOf<Character?>(null) }
    var showSocialSheet by remember { mutableStateOf(false) }
    var showTutorialSocialPanel by remember { mutableStateOf(false) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val listState = rememberLazyListState()

    val tutorialSteps = remember(characters.isNotEmpty()) {
        buildList {
            add(
                TutorialStep(
                    targetKey = "welcome",
                    title = "¡Bienvenido a AI Dungeon Master!",
                    description = "Soy Enzo, tu guía de aventura. Te enseñaré rápidamente las zonas principales de la app para que puedas empezar sin perderte.",
                    mascotRes = R.drawable.dragon_idle
                )
            )

            add(
                TutorialStep(
                    targetKey = "btn_characters_title",
                    title = "Tus personajes",
                    description = "Esta es la pantalla principal. Aquí aparecen todos tus héroes creados y puedes continuar sus aventuras.",
                    mascotRes = R.drawable.dragon_waving
                )
            )
            add(
                TutorialStep(
                    targetKey = "btn_ranking",
                    title = "Ranking mundial",
                    description = "Aquí puedes ver la clasificación global y comparar tu progreso con otros jugadores.",
                    mascotRes = R.drawable.dragon_pointing
                )
            )
            add(
                TutorialStep(
                    targetKey = "btn_achievements",
                    title = "Logros",
                    description = "Este botón abre tus logros y recompensas especiales desbloqueadas.",
                    mascotRes = R.drawable.dragon_idle
                )
            )
            add(
                TutorialStep(
                    targetKey = "btn_logout",
                    title = "Cerrar sesión",
                    description = "Usa este botón para salir de tu cuenta actual de forma segura.",
                    mascotRes = R.drawable.dragon_waving
                )
            )

            if (characters.isNotEmpty()) {
                add(
                    TutorialStep(
                        targetKey = "first_character_card",
                        title = "Tarjeta del personaje",
                        description = "Pulsa en una tarjeta para entrar en la aventura de ese personaje o continuar su partida.",
                        mascotRes = R.drawable.dragon_pointing
                    )
                )
                add(
                    TutorialStep(
                        targetKey = "first_character_room",
                        title = "Sala personal",
                        description = "Este botón abre la fortaleza o sala personal del personaje.",
                        mascotRes = R.drawable.dragon_idle
                    )
                )
                add(
                    TutorialStep(
                        targetKey = "first_character_delete",
                        title = "Eliminar personaje",
                        description = "Este botón borra el personaje. La app te pedirá confirmación antes de eliminarlo.",
                        mascotRes = R.drawable.dragon_pointing
                    )
                )
            }

            add(
                TutorialStep(
                    targetKey = "btn_social",
                    title = "Social",
                    description = "Este botón abre la zona social de la app.",
                    mascotRes = R.drawable.dragon_waving
                )
            )
            add(
                TutorialStep(
                    targetKey = "social_my_profile",
                    title = "Mi perfil",
                    description = "Desde aquí puedes ver y editar tu perfil de jugador.",
                    mascotRes = R.drawable.dragon_idle
                )
            )
            add(
                TutorialStep(
                    targetKey = "social_search_users",
                    title = "Buscar usuarios",
                    description = "Te permite buscar otros jugadores dentro de la aplicación.",
                    mascotRes = R.drawable.dragon_pointing
                )
            )
            add(
                TutorialStep(
                    targetKey = "social_friend_requests",
                    title = "Solicitudes de amistad",
                    description = "Aquí puedes revisar las solicitudes de amistad recibidas.",
                    mascotRes = R.drawable.dragon_waving
                )
            )
            add(
                TutorialStep(
                    targetKey = "social_friends_list",
                    title = "Lista de amigos",
                    description = "Abre tu lista de amigos para ver tus contactos y acceder a opciones sociales.",
                    mascotRes = R.drawable.dragon_idle
                )
            )
            add(
                TutorialStep(
                    targetKey = "social_guilds",
                    title = "Gremios",
                    description = "Aquí puedes entrar en gremios, unirte a otros jugadores y participar en contenido cooperativo.",
                    mascotRes = R.drawable.dragon_pointing
                )
            )
            add(
                TutorialStep(
                    targetKey = "btn_create_character",
                    title = "Crear personaje",
                    description = "Pulsa aquí para crear un nuevo héroe y comenzar una nueva aventura.",
                    mascotRes = R.drawable.dragon_waving
                )
            )
        }
    }

    val currentStep = tutorialSteps.getOrNull(tutorialStepIndex)
    val currentTargetKey = currentStep?.targetKey.orEmpty()

    fun finishTutorial() {
        tutorialPrefs.edit()
            .putBoolean("home_tutorial_completed", true)
            .apply()

        showTutorial = false
        tutorialStepIndex = 0
        showSocialSheet = false
        showTutorialSocialPanel = false
    }

    LaunchedEffect(showTutorial, tutorialStepIndex, characters.size) {
        if (!showTutorial || tutorialSteps.isEmpty()) return@LaunchedEffect

        when (currentTargetKey) {
            "first_character_card",
            "first_character_room",
            "first_character_delete" -> {
                if (characters.isNotEmpty()) {
                    listState.animateScrollToItem(0)
                }
            }
        }

        showTutorialSocialPanel = currentTargetKey.startsWith("social_")

        if (showTutorialSocialPanel) {
            showSocialSheet = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Tus Personajes",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.tutorialAnchor("btn_characters_title", tutorialTargets)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { navController.navigate("ranking") },
                            modifier = Modifier.tutorialAnchor("btn_ranking", tutorialTargets)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Ranking Mundial",
                                tint = Color(0xFFFFD700)
                            )
                        }

                        IconButton(
                            onClick = { navController.navigate(Screen.Achievements.route) },
                            modifier = Modifier.tutorialAnchor("btn_achievements", tutorialTargets)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Logros",
                                tint = Color(0xFFFFD700)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.tutorialAnchor("btn_logout", tutorialTargets)
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Cerrar Sesión",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (characters.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay aventureros todavía.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(state = listState) {
                            itemsIndexed(
                                items = characters,
                                key = { _, character -> character.id.ifBlank { character.name } }
                            ) { index, character ->
                                CharacterCard(
                                    character = character,
                                    modifier = if (index == 0) {
                                        Modifier.tutorialAnchor("first_character_card", tutorialTargets)
                                    } else {
                                        Modifier
                                    },
                                    roomButtonModifier = if (index == 0) {
                                        Modifier.tutorialAnchor("first_character_room", tutorialTargets)
                                    } else {
                                        Modifier
                                    },
                                    deleteButtonModifier = if (index == 0) {
                                        Modifier.tutorialAnchor("first_character_delete", tutorialTargets)
                                    } else {
                                        Modifier
                                    },
                                    onClick = {
                                        if (character.gameTheme.isNullOrEmpty()) {
                                            navController.navigate(
                                                Screen.GameSetup.createRoute(userId, character.name)
                                            )
                                        } else {
                                            navController.navigate(
                                                Screen.GamePlay.createRoute(
                                                    userId,
                                                    character.name,
                                                    character.gameTheme
                                                )
                                            )
                                        }
                                    },
                                    onOpenRoom = {
                                        val partidaId = "${userId}_${character.name}"
                                        navController.navigate(
                                            Screen.PersonalRoom.createRoute(partidaId, character.name)
                                        )
                                    },
                                    onDelete = { characterToDelete = character }
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showSocialSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .tutorialAnchor("btn_social", tutorialTargets)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Social"
                    )
                }

                FloatingActionButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .tutorialAnchor("btn_create_character", tutorialTargets)
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        if (showTutorialSocialPanel) {
            TutorialSocialPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                tutorialTargets = tutorialTargets
            )
        }

        DragonTutorialOverlay(
            visible = showTutorial,
            steps = tutorialSteps,
            currentStepIndex = tutorialStepIndex,
            targets = tutorialTargets,
            onNext = {
                if (tutorialStepIndex < tutorialSteps.lastIndex) {
                    tutorialStepIndex++
                } else {
                    finishTutorial()
                }
            },
            onBack = {
                if (tutorialStepIndex > 0) {
                    tutorialStepIndex--
                }
            },
            onFinish = { finishTutorial() },
            onSkip = { finishTutorial() }
        )
    }

    if (showSocialSheet && !showTutorialSocialPanel) {
        SocialMenuSheet(
            onDismiss = {
                if (!showTutorial || !currentTargetKey.startsWith("social_")) {
                    showSocialSheet = false
                }
            },
            onMyProfile = {
                showSocialSheet = false
                navController.navigate(Screen.MyProfile.route)
            },
            onSearchUsers = {
                showSocialSheet = false
                navController.navigate(Screen.UserSearch.route)
            },
            onFriendRequests = {
                showSocialSheet = false
                navController.navigate(Screen.FriendRequests.route)
            },
            onFriendsList = {
                showSocialSheet = false
                navController.navigate(Screen.FriendsList.route)
            },
            onGuilds = {
                showSocialSheet = false
                navController.navigate(Screen.Guilds.route)
            },
            tutorialTargets = tutorialTargets,
            lockForTutorial = showTutorial && currentTargetKey.startsWith("social_")
        )
    }

    if (showDialog) {
        CreateCharacterDialog(
            onDismiss = { showDialog = false },
            onCreate = { name, race, clazz, stats, traits, portraitUrl ->
                viewModel.saveCharacter(name, race, clazz, stats, traits, portraitUrl)
                showDialog = false
            }
        )
    }

    characterToDelete?.let { character ->
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            title = { Text("¿Eliminar personaje?") },
            text = {
                Text("¿Estás seguro de que quieres borrar a ${character.name}? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCharacter(character.id, character.name)
                        characterToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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
fun CharacterCard(
    character: Character,
    modifier: Modifier = Modifier,
    roomButtonModifier: Modifier = Modifier,
    deleteButtonModifier: Modifier = Modifier,
    onClick: () -> Unit,
    onOpenRoom: () -> Unit,
    onDelete: () -> Unit
) {
    val imageModel = remember(character.portraitUrl) {
        val url = character.portraitUrl
        if (url.isNullOrEmpty()) {
            null
        } else if (url.startsWith("iVBOR") || url.length > 100) {
            try {
                android.util.Base64.decode(url, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        } else {
            url
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (character.portraitUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Retrato de ${character.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    character.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val subtitle = if (!character.gameTheme.isNullOrEmpty()) {
                    "${character.race} • ${character.characterClass} | ${character.gameTheme}"
                } else {
                    "${character.race} • ${character.characterClass} (Sin partida)"
                }

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (character.hpMax > 0) {
                    Text(
                        "❤️ ${character.hpCurrent} / ${character.hpMax}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            character.hpCurrent <= character.hpMax * 0.25f -> Color.Red
                            character.hpCurrent <= character.hpMax * 0.5f -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }

                if (character.level > 0) {
                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF3A2A00),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFD700))
                        ) {
                            Text(
                                text = "Nv.${character.level}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val xpProgress = if (character.level * 100 > 0) {
                            character.xp.toFloat() / (character.level * 100)
                        } else {
                            0f
                        }

                        LinearProgressIndicator(
                            progress = { xpProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFFD700),
                            trackColor = Color(0xFF2A2A2A)
                        )

                        Text(
                            text = "${character.xp}/${character.level * 100} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                if (character.lastPlayed > 0L) {
                    Text(
                        "🕐 ${formatLastPlayed(character.lastPlayed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenRoom,
                    modifier = roomButtonModifier
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Abrir fortaleza",
                        tint = Color(0xFFFFD700)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = deleteButtonModifier
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialSocialPanel(
    modifier: Modifier = Modifier,
    tutorialTargets: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Rect>
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFF1E1E22),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.LightGray.copy(alpha = 0.85f))
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Zona social",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF1C2)
            )

            TutorialSocialOption(
                text = "Mi perfil",
                modifier = Modifier.tutorialAnchor("social_my_profile", tutorialTargets)
            )

            TutorialSocialOption(
                text = "Buscar usuarios",
                modifier = Modifier.tutorialAnchor("social_search_users", tutorialTargets)
            )

            TutorialSocialOption(
                text = "Solicitudes de amistad",
                modifier = Modifier.tutorialAnchor("social_friend_requests", tutorialTargets)
            )

            TutorialSocialOption(
                text = "Lista de amigos",
                modifier = Modifier.tutorialAnchor("social_friends_list", tutorialTargets)
            )

            TutorialSocialOption(
                text = "Gremios",
                modifier = Modifier.tutorialAnchor("social_guilds", tutorialTargets)
            )
        }
    }
}

@Composable
private fun TutorialSocialOption(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2D281B),
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFFF1C2),
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatLastPlayed(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

    return when {
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days == 1L -> "Ayer"
        days < 7 -> "Hace $days días"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}