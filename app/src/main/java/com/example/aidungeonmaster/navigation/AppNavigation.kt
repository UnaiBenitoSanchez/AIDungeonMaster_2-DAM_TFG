package com.example.aidungeonmaster.navigation

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.aidungeonmaster.data.repository.CharacterDeletionRepository
import com.example.aidungeonmaster.ui.accessibility.UsabilityAssistantOverlay
import com.example.aidungeonmaster.ui.achievements.AchievementsScreen
import com.example.aidungeonmaster.ui.game.ARMapScreen
import com.example.aidungeonmaster.ui.game.BestiaryScreen
import com.example.aidungeonmaster.ui.game.CombatScreen
import com.example.aidungeonmaster.ui.game.DeathSummaryScreen
import com.example.aidungeonmaster.ui.game.GamePlayScreen
import com.example.aidungeonmaster.ui.game.GameSetupScreen
import com.example.aidungeonmaster.ui.game.InventoryScreen
import com.example.aidungeonmaster.ui.game.JournalScreen
import com.example.aidungeonmaster.ui.game.LocationsGalleryScreen
import com.example.aidungeonmaster.ui.game.WorldMapDialog
import com.example.aidungeonmaster.ui.game.PersonalRoomScreen
import com.example.aidungeonmaster.ui.game.QRScannerScreen
import com.example.aidungeonmaster.ui.home.HomeScreen
import com.example.aidungeonmaster.ui.home.RankingScreen
import com.example.aidungeonmaster.ui.login.LoginScreen
import com.example.aidungeonmaster.ui.register.RegisterScreen
import com.example.aidungeonmaster.ui.social.FriendRequestsScreen
import com.example.aidungeonmaster.ui.social.FriendsListScreen
import com.example.aidungeonmaster.ui.social.GuildBossBattleScreen
import com.example.aidungeonmaster.ui.social.GuildDetailsScreen
import com.example.aidungeonmaster.ui.social.GuildsScreen
import com.example.aidungeonmaster.ui.social.PrivateChatScreen
import com.example.aidungeonmaster.ui.social.SocialProfileScreen
import com.example.aidungeonmaster.ui.social.UserSearchScreen
import com.example.aidungeonmaster.utils.AdventureMusicEngine
import com.example.aidungeonmaster.utils.CombatMusicEngine
import com.example.aidungeonmaster.viewmodel.AchievementViewModel
import com.example.aidungeonmaster.viewmodel.AuthViewModel
import com.example.aidungeonmaster.viewmodel.GameViewModel
import com.example.aidungeonmaster.viewmodel.HomeViewModel
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.example.aidungeonmaster.viewmodel.PersonalRoomViewModel
import com.example.aidungeonmaster.viewmodel.RankingViewModel
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import com.example.aidungeonmaster.viewmodel.WorldMapViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.aidungeonmaster.ui.tutorial.DragonTutorialOverlay

import com.example.aidungeonmaster.ui.home.CharacterSheetScreen
import com.example.aidungeonmaster.viewmodel.CombatPhase
import com.example.aidungeonmaster.ui.theme.ColorBlindType
import com.example.aidungeonmaster.ui.theme.LocalColorBlindType

// Pantalla que representa screen.
sealed class Screen(val route: String) {
    // Clase que encapsula la lógica de login.
    object Login : Screen("login")
    // Clase que encapsula la lógica de register.
    object Register : Screen("register")

    // Clase que encapsula la lógica de home.
    object Home : Screen("home") {
        const val openCreateRoute = "home?openCreateCharacter={openCreateCharacter}"

        // Crea route.
        fun createRoute(openCreateCharacter: Boolean = false): String {
            return if (openCreateCharacter) {
                "home?openCreateCharacter=true"
            } else {
                route
            }
        }
    }

    // Modelo de datos que representa my profile.
    object MyProfile : Screen("my_profile")
    // Clase que encapsula la lógica de guilds.
    object Guilds : Screen("guilds?openCreate={openCreate}") {
        // Crea route.
        fun createRoute(openCreate: Boolean = false): String {
            return "guilds?openCreate=$openCreate"
        }
    }

    // Clase que encapsula la lógica de guild details.
    object GuildDetails : Screen("guild_details/{guildId}?tab={tab}") {
        // Crea route.
        fun createRoute(guildId: String, tab: String? = null): String {
            val encodedId = Uri.encode(guildId)
            return if (tab.isNullOrBlank()) {
                "guild_details/$encodedId"
            } else {
                "guild_details/$encodedId?tab=${Uri.encode(tab)}"
            }
        }
    }

    // Clase que encapsula la lógica de inventory.
    object Inventory : Screen("inventory/{userId}") {
        // Crea route.
        fun createRoute(userId: String) = "inventory/${Uri.encode(userId)}"
    }

    // Clase que encapsula la lógica de character sheet.
    object CharacterSheet : Screen("character_sheet/{userId}/{characterName}") {
        // Crea route.
        fun createRoute(userId: String, characterName: String): String {
            val encodedName = Uri.encode(characterName)
            return "character_sheet/$userId/$encodedName"
        }
    }

    // Clase que encapsula la lógica de qrscanner.
    object QRScanner : Screen("qr_scanner")

    // Clase que encapsula la lógica de game setup.
    object GameSetup : Screen("game_setup/{userId}/{characterName}") {
        // Crea route.
        fun createRoute(userId: String, characterName: String) =
            "game_setup/${Uri.encode(userId)}/${Uri.encode(characterName)}"
    }

    // Clase que encapsula la lógica de ranking.
    object Ranking : Screen("ranking")
    // Clase que encapsula la lógica de achievements.
    object Achievements : Screen("achievements")

    // Clase que encapsula la lógica de game play.
    object GamePlay : Screen("game_play/{userId}/{characterName}/{theme}") {
        // Crea route.
        fun createRoute(userId: String, characterName: String, theme: String) =
            "game_play/${Uri.encode(userId)}/${Uri.encode(characterName)}/${Uri.encode(theme)}"
    }

    // Clase que encapsula la lógica de world map.
    object WorldMap : Screen("world_map/{charId}") {
        // Crea route.
        fun createRoute(charId: String) = "world_map/${Uri.encode(charId)}"
    }

    // Clase que encapsula la lógica de armap.
    object ARMap : Screen("ar_map/{charId}") {
        // Crea route.
        fun createRoute(charId: String) = "ar_map/${Uri.encode(charId)}"
    }

    // Clase que encapsula la lógica de locations gallery.
    object LocationsGallery : Screen("locations_gallery/{charId}/{characterName}") {
        // Crea route.
        fun createRoute(charId: String, characterName: String) =
            "locations_gallery/${Uri.encode(charId)}/${Uri.encode(characterName)}"
    }

    // Modelo de datos que representa death summary.
    object DeathSummary : Screen("death_summary/{xpGained}/{coinsGained}") {
        // Crea route.
        fun createRoute(xpGained: Int, coinsGained: Int) =
            "death_summary/$xpGained/$coinsGained"
    }

    // Clase que encapsula la lógica de journal.
    object Journal : Screen("journal/{charId}") {
        // Crea route.
        fun createRoute(charId: String) = "journal/${Uri.encode(charId)}"
    }

    // Modelo de datos que representa personal room.
    object PersonalRoom : Screen("personal_room/{charId}/{characterName}") {
        // Crea route.
        fun createRoute(charId: String, characterName: String) =
            "personal_room/${Uri.encode(charId)}/${Uri.encode(characterName)}"
    }

    // Clase que encapsula la lógica de user search.
    object UserSearch : Screen("user_search")
    // Clase que encapsula la lógica de friend requests.
    object FriendRequests : Screen("friend_requests")
    // Clase que encapsula la lógica de friends list.
    object FriendsList : Screen("friends_list")

    // Modelo de datos que representa friend profile.
    object FriendProfile : Screen("friend_profile/{friendUid}") {
        // Crea route.
        fun createRoute(friendUid: String): String =
            "friend_profile/${Uri.encode(friendUid)}"
    }

    // Modelo de datos que representa friend personal room.
    object FriendPersonalRoom : Screen("friend_personal_room/{friendUid}/{characterId}/{characterName}") {
        // Crea route.
        fun createRoute(friendUid: String, characterId: String, characterName: String): String {
            return "friend_personal_room/${Uri.encode(friendUid)}/${Uri.encode(characterId)}/${Uri.encode(characterName)}"
        }
    }

    // Clase que encapsula la lógica de private chat.
    object PrivateChat : Screen("private_chat/{friendUid}/{friendName}?guildId={guildId}") {
        // Crea route.
        fun createRoute(friendUid: String, friendName: String, guildId: String? = null): String {
            val encodedUid = Uri.encode(friendUid)
            val encodedName = Uri.encode(friendName)
            return if (guildId.isNullOrBlank()) {
                "private_chat/$encodedUid/$encodedName"
            } else {
                "private_chat/$encodedUid/$encodedName?guildId=${Uri.encode(guildId)}"
            }
        }
    }

    // Clase que encapsula la lógica de guild boss battle.
    object GuildBossBattle : Screen("guild_boss_battle/{guildId}") {
        // Crea route.
        fun createRoute(guildId: String): String =
            "guild_boss_battle/${Uri.encode(guildId)}"
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
// Ejecuta la lógica de app navigation.
fun AppNavigation(
    navController: NavHostController,
    onColorBlindChanged: (ColorBlindType) -> Unit = {}
) {
    val authViewModel: AuthViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val inventoryViewModel: InventoryViewModel = viewModel()
    val worldMapViewModel: WorldMapViewModel = viewModel()
    val achievementViewModel: AchievementViewModel = viewModel()
    val socialViewModel: SocialViewModel = viewModel()
    val personalRoomViewModel: PersonalRoomViewModel = viewModel()

    val deletionRepository = CharacterDeletionRepository()
    val scope = rememberCoroutineScope()

    val startRoute = if (authViewModel.isUserLoggedIn()) Screen.Home.createRoute() else Screen.Login.route

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val characters by homeViewModel.characters.collectAsState()
    val myGuilds by socialViewModel.myGuilds.collectAsState()

    var accessibilityOpenRequest by remember { mutableIntStateOf(0) }
    var tutorialOpenRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentRoute) {
        homeViewModel.fetchCharacters()

        if (!currentRoute.isAdventureRoute()) {
            AdventureMusicEngine.stopNow()
            CombatMusicEngine.stop()
        }
    }
    LaunchedEffect(Unit) {
        socialViewModel.startGuildsListener()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        val homeContent: @Composable (Boolean) -> Unit = { autoOpenCreateCharacter ->
            HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                socialViewModel = socialViewModel,
                onColorBlindChanged = onColorBlindChanged,
                onOpenAccessibilityOptions = {
                    accessibilityOpenRequest++
                },
                autoOpenCreateCharacter = autoOpenCreateCharacter,
                restartTutorialRequest = tutorialOpenRequest
            )
        }

        NavHost(navController = navController, startDestination = startRoute) {
            composable(Screen.Login.route) {
                LoginScreen(
                    navController = navController,
                    onOpenAccessibilityOptions = {
                        accessibilityOpenRequest++
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(navController)
            }

            composable(Screen.Home.route) {
                homeContent(false)
            }

            composable(
                route = Screen.Home.openCreateRoute,
                arguments = listOf(
                    navArgument("openCreateCharacter") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntryArg ->
                val openCreateCharacter =
                    backStackEntryArg.arguments?.getBoolean("openCreateCharacter") ?: false

                homeContent(openCreateCharacter)
            }

            composable(Screen.MyProfile.route) {
                val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

                SocialProfileScreen(
                    userUid = myUid,
                    isMe = true,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Ranking.route) {
                val rankingViewModel: RankingViewModel = viewModel()

                RankingScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = rankingViewModel
                )
            }

            composable(Screen.Achievements.route) {
                AchievementsScreen(
                    viewModel = achievementViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.GameSetup.route) { backStackEntryArg ->
                val userId = Uri.decode(
                    backStackEntryArg.arguments?.getString("userId").orEmpty()
                )
                val characterName = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterName").orEmpty()
                )

                GameSetupScreen(navController, userId, characterName)
            }

            composable(Screen.GamePlay.route) { backStackEntryArg ->
                val userId = Uri.decode(
                    backStackEntryArg.arguments?.getString("userId").orEmpty()
                )
                val characterName = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterName").orEmpty()
                )
                val theme = Uri.decode(
                    backStackEntryArg.arguments?.getString("theme").orEmpty()
                )

                GamePlayScreen(
                    navController = navController,
                    userId = userId,
                    characterName = characterName,
                    theme = theme,
                    viewModel = gameViewModel,
                    inventoryViewModel = inventoryViewModel,
                    mapViewModel = worldMapViewModel,
                    achievementViewModel = achievementViewModel
                )
            }

            composable("qr_scanner/{gameId}") { backStackEntryArg ->
                val gameId = Uri.decode(
                    backStackEntryArg.arguments?.getString("gameId").orEmpty()
                )

                QRScannerScreen(
                    gameId = gameId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CharacterSheet.route) { backStackEntryArg ->
                val userId = backStackEntryArg.arguments?.getString("userId").orEmpty()
                val characterName = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterName").orEmpty()
                )

                CharacterSheetScreen(
                    userId = userId,
                    characterName = characterName,
                    onBack = { navController.popBackStack() },
                    onOpenRoom = { charId, name ->
                        navController.navigate(Screen.PersonalRoom.createRoute(charId, name))
                    },
                    onContinueAdventure = { uid, name, theme ->
                        navController.navigate(Screen.GamePlay.createRoute(uid, name, theme))
                    }
                )
            }

            composable(Screen.Inventory.route) { backStackEntryArg ->
                val idParaElInventario = Uri.decode(
                    backStackEntryArg.arguments?.getString("userId").orEmpty()
                )

                InventoryScreen(
                    gameId = idParaElInventario,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("combat/{gameId}") { backStackEntryArg ->
                val gameId = Uri.decode(
                    backStackEntryArg.arguments?.getString("gameId").orEmpty()
                )

                CombatScreen(
                    gameViewModel = gameViewModel,
                    inventoryViewModel = inventoryViewModel,
                    gameId = gameId,
                    achievementViewModel = achievementViewModel,
                    onCombatEnd = { result, xpGained ->
                        val enemyName =
                            gameViewModel.currentAdventureStep.value?.enemy?.name ?: "el enemigo"

                        when (result) {
                            CombatPhase.VICTORY -> {
                                if (xpGained > 0) {
                                    gameViewModel.addPendingXp(xpGained)
                                }

                                gameViewModel.notifyCombatEnd(true, enemyName)
                                navController.popBackStack()
                            }

                            CombatPhase.FLED -> {
                                gameViewModel.notifyCombatFled(enemyName)
                                navController.popBackStack()
                            }

                            CombatPhase.DEFEAT -> {
                                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                                val characterName = gameId
                                    .removePrefix("${currentUserId}_")
                                    .substringBefore("_")

                                // Borrar personaje en background y navegar al resumen de muerte
                                scope.launch {
                                    try {
                                        deletionRepository.deleteEverywhere(
                                            userId = currentUserId,
                                            characterName = characterName
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                            "APP_NAV",
                                            "Error borrando personaje al morir: ${e.message}",
                                            e
                                        )
                                    }
                                }

                                AdventureMusicEngine.stopNow()

                                navController.navigate(
                                    Screen.DeathSummary.createRoute(
                                        xpGained   = xpGained,
                                        coinsGained = 0
                                    )
                                ) {
                                    // Limpiar el back stack de la aventura completa
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }

                            else -> Unit
                        }
                    }
                )
            }

            // ── PANTALLA DE RESUMEN DE MUERTE ─────────────────────────────
            composable(
                route = Screen.DeathSummary.route,
                arguments = listOf(
                    navArgument("xpGained")    { type = NavType.IntType; defaultValue = 0 },
                    navArgument("coinsGained") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { backStackEntry ->
                val xpGained    = backStackEntry.arguments?.getInt("xpGained")    ?: 0
                val coinsGained = backStackEntry.arguments?.getInt("coinsGained") ?: 0

                val character by inventoryViewModel.character.collectAsState()

                DeathSummaryScreen(
                    character    = character,
                    xpGained     = xpGained,
                    coinsGained  = coinsGained,
                    itemsFound   = emptyList(), // los ítems de sesión no viajan por ruta; se muestran los del inventario final
                    onGoHome     = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.WorldMap.route) { backStackEntryArg ->
                val charId = Uri.decode(
                    backStackEntryArg.arguments?.getString("charId").orEmpty()
                )

                LaunchedEffect(charId) {
                    if (charId.isNotBlank()) {
                        worldMapViewModel.loadMap(charId)
                    }
                }

                val mapState by worldMapViewModel.worldMapState.collectAsState()

                WorldMapDialog(
                    mapState = mapState,
                    onDismiss = { navController.popBackStack() },
                    onOpenAR = {
                        navController.navigate(Screen.ARMap.createRoute(charId)) {
                            popUpTo(Screen.WorldMap.createRoute(charId)) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.ARMap.route) { backStackEntryArg ->
                val charId = Uri.decode(
                    backStackEntryArg.arguments?.getString("charId").orEmpty()
                )

                LaunchedEffect(charId) {
                    if (charId.isNotBlank()) {
                        worldMapViewModel.loadMap(charId)
                    }
                }

                val mapState by worldMapViewModel.worldMapState.collectAsState()
                val characterName = charId.substringAfter("_")

                ARMapScreen(
                    mapState = mapState,
                    onBack = { navController.popBackStack() },
                    onOpen3DGallery = {
                        navController.navigate(
                            Screen.LocationsGallery.createRoute(charId, characterName)
                        )
                    }
                )
            }

            composable(Screen.LocationsGallery.route) { backStackEntryArg ->
                val charId = Uri.decode(
                    backStackEntryArg.arguments?.getString("charId").orEmpty()
                )
                val characterName = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterName").orEmpty()
                )

                LaunchedEffect(charId) {
                    if (charId.isNotBlank()) {
                        worldMapViewModel.loadMap(charId)
                    }
                }

                val mapState by worldMapViewModel.worldMapState.collectAsState()

                LocationsGalleryScreen(
                    mapState = mapState,
                    charId = charId,
                    characterName = characterName,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PersonalRoom.route) { backStackEntryArg ->
                val charId = Uri.decode(
                    backStackEntryArg.arguments?.getString("charId").orEmpty()
                )
                val characterName = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterName").orEmpty()
                )

                PersonalRoomScreen(
                    charId = charId,
                    characterName = characterName,
                    onBack = { navController.popBackStack() },
                    readOnly = false,
                    roomViewModel = personalRoomViewModel,
                    inventoryViewModel = inventoryViewModel
                )
            }

            composable("bestiary/{charId}") { backStackEntryArg ->
                val charId = Uri.decode(
                    backStackEntryArg.arguments?.getString("charId").orEmpty()
                )

                BestiaryScreen(
                    gameId = charId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Journal.route) { backStackEntryArg ->
                val charId = Uri.decode(
                    backStackEntryArg.arguments?.getString("charId").orEmpty()
                )

                JournalScreen(
                    charId = charId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.UserSearch.route) {
                UserSearchScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FriendRequests.route) {
                FriendRequestsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FriendsList.route) {
                FriendsListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { friendUid ->
                        navController.navigate(Screen.FriendProfile.createRoute(friendUid))
                    },
                    onOpenChat = { friendUid, friendName ->
                        navController.navigate(Screen.PrivateChat.createRoute(friendUid, friendName))
                    }
                )
            }

            composable(Screen.FriendProfile.route) { backStackEntryArg ->
                val friendUid = Uri.decode(
                    backStackEntryArg.arguments?.getString("friendUid").orEmpty()
                )

                SocialProfileScreen(
                    userUid = friendUid,
                    isMe = false,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { uid, name ->
                        navController.navigate(Screen.PrivateChat.createRoute(uid, name))
                    },
                    onOpenPersonalRoom = { friendUidForRoom, characterId, characterName ->
                        navController.navigate(
                            Screen.FriendPersonalRoom.createRoute(
                                friendUid = friendUidForRoom,
                                characterId = characterId,
                                characterName = characterName
                            )
                        )
                    }
                )
            }

            composable(
                route = Screen.FriendPersonalRoom.route,
                arguments = listOf(
                    navArgument("friendUid") { type = NavType.StringType },
                    navArgument("characterId") { type = NavType.StringType },
                    navArgument("characterName") { type = NavType.StringType }
                )
            ) { backStackEntryArg ->
                val friendUid = Uri.decode(
                    backStackEntryArg.arguments?.getString("friendUid").orEmpty()
                )

                val characterId = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterId").orEmpty()
                )

                val characterName = Uri.decode(
                    backStackEntryArg.arguments?.getString("characterName").orEmpty()
                )

                val charId = when {
                    characterId.isNotBlank() && characterId.contains("_") -> characterId
                    characterName.isNotBlank() -> "${friendUid}_${characterName}"
                    else -> characterId
                }

                PersonalRoomScreen(
                    charId = charId,
                    characterName = characterName,
                    onBack = { navController.popBackStack() },
                    readOnly = true,
                    roomViewModel = personalRoomViewModel,
                    inventoryViewModel = inventoryViewModel
                )
            }

            composable(
                route = Screen.Guilds.route,
                arguments = listOf(
                    navArgument("openCreate") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntryArg ->
                val openCreate = backStackEntryArg.arguments?.getBoolean("openCreate") ?: false

                GuildsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenGuildDetails = { guildId, tab ->
                        navController.navigate(Screen.GuildDetails.createRoute(guildId, tab))
                    },
                    autoOpenCreate = openCreate,
                    viewModel = socialViewModel
                )
            }

            composable(
                route = Screen.GuildDetails.route,
                arguments = listOf(
                    navArgument("guildId") { type = NavType.StringType },
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = "resumen"
                    }
                )
            ) { backStackEntryArg ->
                val guildId = Uri.decode(
                    backStackEntryArg.arguments?.getString("guildId").orEmpty()
                )
                val initialTab = Uri.decode(
                    backStackEntryArg.arguments?.getString("tab").orEmpty()
                )

                GuildDetailsScreen(
                    guildId = guildId,
                    initialTab = initialTab,
                    onBack = { navController.popBackStack() },
                    onOpenMemberChat = { memberUid, memberName, guildIdForChat ->
                        navController.navigate(
                            Screen.PrivateChat.createRoute(
                                friendUid = memberUid,
                                friendName = memberName,
                                guildId = guildIdForChat
                            )
                        )
                    },
                    onOpenBossBattle = { battleGuildId ->
                        navController.navigate(Screen.GuildBossBattle.createRoute(battleGuildId)) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = socialViewModel
                )
            }

            composable(
                route = Screen.PrivateChat.route,
                arguments = listOf(
                    navArgument("friendUid") { type = NavType.StringType },
                    navArgument("friendName") { type = NavType.StringType },
                    navArgument("guildId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntryArg ->
                val friendUid = Uri.decode(
                    backStackEntryArg.arguments?.getString("friendUid").orEmpty()
                )
                val friendName = Uri.decode(
                    backStackEntryArg.arguments?.getString("friendName").orEmpty()
                )
                val guildId = backStackEntryArg.arguments?.getString("guildId")?.let(Uri::decode)

                PrivateChatScreen(
                    friendUid = friendUid,
                    friendName = friendName,
                    guildId = guildId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.GuildBossBattle.route) { backStackEntryArg ->
                val guildId = Uri.decode(
                    backStackEntryArg.arguments?.getString("guildId").orEmpty()
                )

                GuildBossBattleScreen(
                    guildId = guildId,
                    onBack = { navController.popBackStack() },
                    viewModel = socialViewModel
                )
            }
        }

        UsabilityAssistantOverlay(
            navController = navController,
            currentRoute = currentRoute,
            currentArguments = backStackEntry?.arguments,
            characters = characters,
            myGuilds = myGuilds,
            gameViewModel = gameViewModel,
            currentColorBlindType = LocalColorBlindType.current,
            onColorBlindChanged = onColorBlindChanged,
            onRelaunchTutorial = {
                tutorialOpenRequest++
                navController.navigate(Screen.Home.route) {
                    launchSingleTop = true
                }
            },
            onLogout = {
                socialViewModel.resetSessionState()
                homeViewModel.logout {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            openSheetRequest = accessibilityOpenRequest,
            showFloatingButton = false
        )
    }

}

// Ejecuta la lógica de string.
private fun String?.isAdventureRoute(): Boolean {
    val route = this?.lowercase().orEmpty()

    return route.startsWith("game_play") ||
            route.startsWith("gameplay") ||
            route.startsWith("gamelayout") ||
            route.startsWith("inventory") ||
            route.startsWith("journal") ||
            route.startsWith("bestiary") ||
            route.startsWith("world_map") ||
            route.startsWith("worldmap") ||
            route.startsWith("ar_map") ||
            route.startsWith("armap") ||
            route.startsWith("locations_gallery") ||
            route.startsWith("locationsgallery") ||
            route.startsWith("combat")
}
