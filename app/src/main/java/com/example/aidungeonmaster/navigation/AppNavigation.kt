package com.example.aidungeonmaster.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aidungeonmaster.ui.game.GamePlayScreen
import com.example.aidungeonmaster.ui.game.GameSetupScreen
import com.example.aidungeonmaster.ui.home.HomeScreen
import com.example.aidungeonmaster.ui.login.LoginScreen
import com.example.aidungeonmaster.ui.register.RegisterScreen
import com.example.aidungeonmaster.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")

    // Añadimos userId a la ruta
    object GameSetup : Screen("game_setup/{userId}/{characterName}") {
        fun createRoute(userId: String, characterName: String) = "game_setup/$userId/$characterName"
    }

    // Añadimos userId a la ruta de juego
    object GamePlay : Screen("game_play/{userId}/{characterName}/{theme}") {
        fun createRoute(userId: String, characterName: String, theme: String) =
            "game_play/$userId/$characterName/$theme"
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()

    // Si el usuario existe, empezamos en Home, si no, en Login
    val startRoute = if (authViewModel.isUserLoggedIn()) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startRoute // Destino dinámico
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }

        composable(Screen.GameSetup.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName") ?: ""
            GameSetupScreen(navController, userId, characterName)
        }

        composable(Screen.GamePlay.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val characterName = backStackEntry.arguments?.getString("characterName") ?: ""
            val theme = backStackEntry.arguments?.getString("theme") ?: ""

            // Ahora la GamePlayScreen recibe los 3 parámetros necesarios
            GamePlayScreen(navController, userId, characterName, theme)
        }
    }
}