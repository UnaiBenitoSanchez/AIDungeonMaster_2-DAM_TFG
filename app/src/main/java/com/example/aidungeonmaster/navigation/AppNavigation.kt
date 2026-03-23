package com.example.aidungeonmaster.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aidungeonmaster.ui.game.CombatScreen
import com.example.aidungeonmaster.ui.game.GamePlayScreen
import com.example.aidungeonmaster.ui.game.GameSetupScreen
import com.example.aidungeonmaster.ui.game.InventoryScreen
import com.example.aidungeonmaster.ui.game.QRScannerScreen
import com.example.aidungeonmaster.ui.home.HomeScreen
import com.example.aidungeonmaster.ui.home.RankingScreen
import com.example.aidungeonmaster.viewmodel.RankingViewModel
import com.example.aidungeonmaster.ui.login.LoginScreen
import com.example.aidungeonmaster.ui.register.RegisterScreen
import com.example.aidungeonmaster.viewmodel.AuthViewModel
import com.example.aidungeonmaster.viewmodel.GameViewModel        // IMPORTANTE
import com.example.aidungeonmaster.viewmodel.InventoryViewModel   // IMPORTANTE


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")

    object Inventory : Screen("inventory/{userId}") {
        fun createRoute(userId: String) = "inventory/$userId"
    }
    object QRScanner : Screen("qr_scanner")

    object GameSetup : Screen("game_setup/{userId}/{characterName}") {
        fun createRoute(userId: String, characterName: String) = "game_setup/$userId/$characterName"
    }

    object Ranking : Screen("ranking")

    object GamePlay : Screen("game_play/{userId}/{characterName}/{theme}") {
        fun createRoute(userId: String, characterName: String, theme: String) =
            "game_play/$userId/$characterName/$theme"
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavigation(navController: NavHostController) {
    // INICIALIZACIÓN DE VIEWMODELS
    val authViewModel: AuthViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()           // <--- AÑADIDO
    val inventoryViewModel: InventoryViewModel = viewModel() // <--- AÑADIDO

    val startRoute = if (authViewModel.isUserLoggedIn()) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Ranking.route) {
            val rankingViewModel: RankingViewModel = viewModel()
            RankingScreen(
                onBack = { navController.popBackStack() },
                viewModel = rankingViewModel
            )
        }

        composable(Screen.GameSetup.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName") ?: ""
            GameSetupScreen(navController, userId, characterName)
        }

        composable(Screen.GamePlay.route) { backStackEntry ->
            val userId        = backStackEntry.arguments?.getString("userId")        ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName") ?: ""
            val theme         = backStackEntry.arguments?.getString("theme")         ?: ""
            GamePlayScreen(navController, userId, characterName, theme, gameViewModel, inventoryViewModel)
        }

        composable("qr_scanner/{gameId}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            QRScannerScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Inventory.route) { backStackEntry ->
            val idParaElInventario = backStackEntry.arguments?.getString("userId") ?: ""
            InventoryScreen(
                gameId = idParaElInventario,
                onBack = { navController.popBackStack() }
            )
        }

        // RUTA DE COMBATE — recibe el gameId para actualizar HP
        composable("combat/{gameId}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            CombatScreen(
                gameViewModel      = gameViewModel,
                inventoryViewModel = inventoryViewModel,
                gameId             = gameId,
                onCombatEnd        = { victory, xpGained ->
                    if (victory && xpGained > 0) {
                        gameViewModel.addPendingXp(xpGained)
                    }
                    gameViewModel.notifyCombatEnd(
                        victory,
                        gameViewModel.currentAdventureStep.value?.enemy?.name ?: "el enemigo"
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}