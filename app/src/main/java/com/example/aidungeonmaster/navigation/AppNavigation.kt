package com.example.aidungeonmaster.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aidungeonmaster.data.repository.CharacterDeletionRepository
import com.example.aidungeonmaster.ui.achievements.AchievementsScreen
import com.example.aidungeonmaster.ui.game.ARMapScreen
import com.example.aidungeonmaster.ui.game.BestiaryScreen
import com.example.aidungeonmaster.ui.game.CombatScreen
import com.example.aidungeonmaster.ui.game.GamePlayScreen
import com.example.aidungeonmaster.ui.game.GameSetupScreen
import com.example.aidungeonmaster.ui.game.InventoryScreen
import com.example.aidungeonmaster.ui.game.JournalScreen
import com.example.aidungeonmaster.ui.game.LocationsGalleryScreen
import com.example.aidungeonmaster.ui.game.QRScannerScreen
import com.example.aidungeonmaster.ui.home.HomeScreen
import com.example.aidungeonmaster.ui.home.RankingScreen
import com.example.aidungeonmaster.ui.login.LoginScreen
import com.example.aidungeonmaster.ui.register.RegisterScreen
import com.example.aidungeonmaster.ui.social.FriendRequestsScreen
import com.example.aidungeonmaster.ui.social.FriendsListScreen
import com.example.aidungeonmaster.ui.social.GuildsScreen
import com.example.aidungeonmaster.ui.social.PrivateChatScreen
import com.example.aidungeonmaster.ui.social.SocialProfileScreen
import com.example.aidungeonmaster.ui.social.UserSearchScreen
import com.example.aidungeonmaster.viewmodel.AchievementViewModel
import com.example.aidungeonmaster.viewmodel.AuthViewModel
import com.example.aidungeonmaster.viewmodel.GameViewModel
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.example.aidungeonmaster.viewmodel.RankingViewModel
import com.example.aidungeonmaster.viewmodel.WorldMapViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.aidungeonmaster.utils.AdventureMusicEngine
import com.example.aidungeonmaster.utils.CombatMusicEngine

import android.net.Uri
import com.example.aidungeonmaster.ui.social.GuildDetailsScreen

import com.example.aidungeonmaster.ui.social.GuildBossBattleScreen
import com.example.aidungeonmaster.viewmodel.SocialViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object MyProfile : Screen("my_profile")
    object Guilds : Screen("guilds")

    object Inventory : Screen("inventory/{userId}") {
        fun createRoute(userId: String) = "inventory/$userId"
    }

    object QRScanner : Screen("qr_scanner")

    object GameSetup : Screen("game_setup/{userId}/{characterName}") {
        fun createRoute(userId: String, characterName: String) = "game_setup/$userId/$characterName"
    }

    object Ranking : Screen("ranking")
    object Achievements : Screen("achievements")

    object GamePlay : Screen("game_play/{userId}/{characterName}/{theme}") {
        fun createRoute(userId: String, characterName: String, theme: String) =
            "game_play/$userId/$characterName/$theme"
    }

    object ARMap : Screen("ar_map/{charId}") {
        fun createRoute(charId: String) = "ar_map/$charId"
    }

    object LocationsGallery : Screen("locations_gallery/{charId}/{characterName}") {
        fun createRoute(charId: String, characterName: String) =
            "locations_gallery/$charId/${characterName.replace(" ", "_")}"
    }

    object Journal : Screen("journal/{charId}") {
        fun createRoute(charId: String) = "journal/$charId"
    }

    object UserSearch : Screen("user_search")
    object FriendRequests : Screen("friend_requests")
    object FriendsList : Screen("friends_list")
    object FriendProfile : Screen("friend_profile/{friendUid}") {
        fun createRoute(friendUid: String): String = "friend_profile/$friendUid"
    }
    object PrivateChat : Screen("private_chat/{friendUid}/{friendName}") {
        fun createRoute(friendUid: String, friendName: String): String {
            val encodedName = Uri.encode(friendName)
            return "private_chat/$friendUid/$encodedName"
        }
    }

    object GuildBossBattle : Screen("guild_boss_battle/{guildId}") {
        fun createRoute(guildId: String): String = "guild_boss_battle/$guildId"
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavigation(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()
    val inventoryViewModel: InventoryViewModel = viewModel()
    val worldMapViewModel: WorldMapViewModel = viewModel()
    val achievementViewModel: AchievementViewModel = viewModel()
    val socialViewModel: SocialViewModel = viewModel()

    val deletionRepository = CharacterDeletionRepository()
    val scope = rememberCoroutineScope()

    val startRoute = if (authViewModel.isUserLoggedIn()) Screen.Home.route else Screen.Login.route

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        if (!currentRoute.isAdventureRoute()) {
            AdventureMusicEngine.stopNow()
            CombatMusicEngine.stop()
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.MyProfile.route) {
            val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            SocialProfileScreen(userUid = myUid, isMe = true, onBack = { navController.popBackStack() })
        }

        composable(Screen.Ranking.route) {
            val rankingViewModel: RankingViewModel = viewModel()
            RankingScreen(onBack = { navController.popBackStack() }, viewModel = rankingViewModel)
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(viewModel = achievementViewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.GameSetup.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName") ?: ""
            GameSetupScreen(navController, userId, characterName)
        }

        composable(Screen.GamePlay.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName") ?: ""
            val theme = backStackEntry.arguments?.getString("theme") ?: ""

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

        composable("qr_scanner/{gameId}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            QRScannerScreen(gameId = gameId, onBack = { navController.popBackStack() })
        }

        composable(Screen.Inventory.route) { backStackEntry ->
            val idParaElInventario = backStackEntry.arguments?.getString("userId") ?: ""
            InventoryScreen(gameId = idParaElInventario, onBack = { navController.popBackStack() })
        }

        composable("combat/{gameId}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            CombatScreen(
                gameViewModel = gameViewModel,
                inventoryViewModel = inventoryViewModel,
                gameId = gameId,
                achievementViewModel = achievementViewModel,
                onCombatEnd = { victory, xpGained ->
                    if (victory && xpGained > 0) {
                        gameViewModel.addPendingXp(xpGained)
                    }

                    val enemyName = gameViewModel.currentAdventureStep.value?.enemy?.name ?: "el enemigo"

                    if (victory) {
                        gameViewModel.notifyCombatEnd(true, enemyName)
                        navController.popBackStack()
                    } else {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        val characterName = gameId.removePrefix("${currentUserId}_")

                        scope.launch {
                            try {
                                deletionRepository.deleteEverywhere(userId = currentUserId, characterName = characterName)
                            } catch (e: Exception) {
                                Log.e("APP_NAV", "Error borrando personaje al morir: ${e.message}", e)
                            } finally {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.ARMap.route) { backStackEntry ->
            val charId = backStackEntry.arguments?.getString("charId") ?: ""
            LaunchedEffect(charId) {
                if (charId.isNotBlank()) worldMapViewModel.loadMap(charId)
            }
            val mapState by worldMapViewModel.worldMapState.collectAsState()
            val characterName = charId.substringAfter("_")

            ARMapScreen(
                mapState = mapState,
                onBack = { navController.popBackStack() },
                onOpen3DGallery = { navController.navigate(Screen.LocationsGallery.createRoute(charId, characterName)) }
            )
        }

        composable(Screen.LocationsGallery.route) { backStackEntry ->
            val charId = backStackEntry.arguments?.getString("charId") ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName")?.replace("_", " ") ?: ""

            LaunchedEffect(charId) {
                if (charId.isNotBlank()) worldMapViewModel.loadMap(charId)
            }
            val mapState by worldMapViewModel.worldMapState.collectAsState()

            LocationsGalleryScreen(mapState = mapState, characterName = characterName, onBack = { navController.popBackStack() })
        }

        composable("bestiary/{charId}") { backStackEntry ->
            val charId = backStackEntry.arguments?.getString("charId").orEmpty()
            BestiaryScreen(gameId = charId, onBack = { navController.popBackStack() })
        }

        composable(Screen.Journal.route) { backStackEntry ->
            val charId = backStackEntry.arguments?.getString("charId") ?: ""
            JournalScreen(charId = charId, onBack = { navController.popBackStack() })
        }

        composable(Screen.UserSearch.route) {
            UserSearchScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.FriendRequests.route) {
            FriendRequestsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.FriendsList.route) {
            FriendsListScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { friendUid -> navController.navigate(Screen.FriendProfile.createRoute(friendUid)) },
                onOpenChat = { friendUid, friendName -> navController.navigate(Screen.PrivateChat.createRoute(friendUid, friendName)) }
            )
        }

        composable(Screen.FriendProfile.route) { backStackEntry ->
            val friendUid = backStackEntry.arguments?.getString("friendUid").orEmpty()
            SocialProfileScreen(
                userUid = friendUid,
                isMe = false,
                onBack = { navController.popBackStack() },
                onOpenChat = { uid, name -> navController.navigate(Screen.PrivateChat.createRoute(uid, name)) }
            )
        }

        composable("guilds") {
            GuildsScreen(
                onBack = { navController.popBackStack() },
                onOpenGuildDetails = { guildId ->
                    navController.navigate("guild_details/$guildId")
                },
                viewModel = socialViewModel
            )
        }

        composable("guild_details/{guildId}") { backStackEntry ->
            val guildId = backStackEntry.arguments?.getString("guildId").orEmpty()

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
        ) { backStackEntry ->
            val friendUid = backStackEntry.arguments?.getString("friendUid").orEmpty()
            val friendNameEncoded = backStackEntry.arguments?.getString("friendName").orEmpty()
            val friendName = Uri.decode(friendNameEncoded)

            PrivateChatScreen(
                friendUid = friendUid,
                friendName = friendName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GuildBossBattle.route) { backStackEntry ->
            val guildId = backStackEntry.arguments?.getString("guildId").orEmpty()

            GuildBossBattleScreen(
                guildId = guildId,
                onBack = { navController.popBackStack() },
                viewModel = socialViewModel
            )
        }
    }
}

private fun String?.isAdventureRoute(): Boolean {
    val route = this?.lowercase().orEmpty()

    return route.startsWith("gameplay/") ||
            route.startsWith("gamelayout/") ||
            route.startsWith("inventory/") ||
            route.startsWith("journal/") ||
            route.startsWith("bestiary/") ||
            route.startsWith("armap/") ||
            route.startsWith("locationsgallery/") ||
            route.startsWith("combat/")
}