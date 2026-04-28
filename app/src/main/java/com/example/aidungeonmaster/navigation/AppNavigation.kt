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

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")

    object Home : Screen("home") {
        const val openCreateRoute = "home?openCreateCharacter={openCreateCharacter}"

        fun createRoute(openCreateCharacter: Boolean = false): String {
            return if (openCreateCharacter) {
                "home?openCreateCharacter=true"
            } else {
                route
            }
        }
    }

    object MyProfile : Screen("my_profile")
    object Guilds : Screen("guilds?openCreate={openCreate}") {
        fun createRoute(openCreate: Boolean = false): String {
            return "guilds?openCreate=$openCreate"
        }
    }

    object Inventory : Screen("inventory/{userId}") {
        fun createRoute(userId: String) = "inventory/${Uri.encode(userId)}"
    }

    object CharacterSheet : Screen("character_sheet/{userId}/{characterName}") {
        fun createRoute(userId: String, characterName: String): String {
            val encodedName = Uri.encode(characterName)
            return "character_sheet/$userId/$encodedName"
        }
    }

    object QRScanner : Screen("qr_scanner")

    object GameSetup : Screen("game_setup/{userId}/{characterName}") {
        fun createRoute(userId: String, characterName: String) =
            "game_setup/${Uri.encode(userId)}/${Uri.encode(characterName)}"
    }

    object Ranking : Screen("ranking")
    object Achievements : Screen("achievements")

    object GamePlay : Screen("game_play/{userId}/{characterName}/{theme}") {
        fun createRoute(userId: String, characterName: String, theme: String) =
            "game_play/${Uri.encode(userId)}/${Uri.encode(characterName)}/${Uri.encode(theme)}"
    }

    object ARMap : Screen("ar_map/{charId}") {
        fun createRoute(charId: String) = "ar_map/${Uri.encode(charId)}"
    }

    object LocationsGallery : Screen("locations_gallery/{charId}/{characterName}") {
        fun createRoute(charId: String, characterName: String) =
            "locations_gallery/${Uri.encode(charId)}/${Uri.encode(characterName)}"
    }

    object DeathSummary : Screen("death_summary/{xpGained}/{coinsGained}") {
        fun createRoute(xpGained: Int, coinsGained: Int) =
            "death_summary/$xpGained/$coinsGained"
    }

    object Journal : Screen("journal/{charId}") {
        fun createRoute(charId: String) = "journal/${Uri.encode(charId)}"
    }

    object PersonalRoom : Screen("personal_room/{charId}/{characterName}") {
        fun createRoute(charId: String, characterName: String) =
            "personal_room/${Uri.encode(charId)}/${Uri.encode(characterName)}"
    }

    object UserSearch : Screen("user_search")
    object FriendRequests : Screen("friend_requests")
    object FriendsList : Screen("friends_list")

    object FriendProfile : Screen("friend_profile/{friendUid}") {
        fun createRoute(friendUid: String): String =
            "friend_profile/${Uri.encode(friendUid)}"
    }

    object FriendPersonalRoom : Screen("friend_personal_room/{friendUid}/{characterId}/{characterName}") {
        fun createRoute(friendUid: String, characterId: String, characterName: String): String {
            return "friend_personal_room/${Uri.encode(friendUid)}/${Uri.encode(characterId)}/${Uri.encode(characterName)}"
        }
    }

    object PrivateChat : Screen("private_chat/{friendUid}/{friendName}") {
        fun createRoute(friendUid: String, friendName: String): String {
            val encodedUid = Uri.encode(friendUid)
            val encodedName = Uri.encode(friendName)
            return "private_chat/$encodedUid/$encodedName"
        }
    }

    object GuildBossBattle : Screen("guild_boss_battle/{guildId}") {
        fun createRoute(guildId: String): String =
            "guild_boss_battle/${Uri.encode(guildId)}"
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
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

    var accessibilityOpenRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentRoute) {
        homeViewModel.fetchCharacters()

        if (!currentRoute.isAdventureRoute()) {
            AdventureMusicEngine.stopNow()
            CombatMusicEngine.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        val homeContent: @Composable (Boolean) -> Unit = { autoOpenCreateCharacter ->
            HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                onColorBlindChanged = onColorBlindChanged,
                onOpenAccessibilityOptions = {
                    accessibilityOpenRequest++
                },
                autoOpenCreateCharacter = autoOpenCreateCharacter
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
                    onOpenGuildDetails = { guildId ->
                        navController.navigate("guild_details/${Uri.encode(guildId)}")
                    },
                    autoOpenCreate = openCreate,
                    viewModel = socialViewModel
                )
            }

            composable("guild_details/{guildId}") { backStackEntryArg ->
                val guildId = Uri.decode(
                    backStackEntryArg.arguments?.getString("guildId").orEmpty()
                )

                GuildDetailsScreen(
                    guildId = guildId,
                    onBack = { navController.popBackStack() },
                    onOpenMemberChat = { memberUid, memberName, _ ->
                        navController.navigate(Screen.PrivateChat.createRoute(memberUid, memberName))
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
                    navArgument("friendName") { type = NavType.StringType }
                )
            ) { backStackEntryArg ->
                val friendUid = Uri.decode(
                    backStackEntryArg.arguments?.getString("friendUid").orEmpty()
                )
                val friendName = Uri.decode(
                    backStackEntryArg.arguments?.getString("friendName").orEmpty()
                )

                PrivateChatScreen(
                    friendUid = friendUid,
                    friendName = friendName,
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
            gameViewModel = gameViewModel,
            currentColorBlindType = LocalColorBlindType.current,
            onColorBlindChanged = onColorBlindChanged,
            openSheetRequest = accessibilityOpenRequest,
            showFloatingButton = false
        )
    }

}

private fun String?.isAdventureRoute(): Boolean {
    val route = this?.lowercase().orEmpty()

    return route.startsWith("game_play") ||
            route.startsWith("gameplay") ||
            route.startsWith("gamelayout") ||
            route.startsWith("inventory") ||
            route.startsWith("journal") ||
            route.startsWith("bestiary") ||
            route.startsWith("ar_map") ||
            route.startsWith("armap") ||
            route.startsWith("locations_gallery") ||
            route.startsWith("locationsgallery") ||
            route.startsWith("combat")
}