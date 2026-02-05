package com.example.aidungeonmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aidungeonmaster.ui.login.LoginScreen
import com.example.aidungeonmaster.ui.register.RegisterScreen
import com.example.aidungeonmaster.ui.theme.AIDungeonMasterTheme
import androidx.core.view.WindowCompat
import com.example.aidungeonmaster.navigation.AppNavigation
import com.example.aidungeonmaster.ui.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            AIDungeonMasterTheme {
                androidx.compose.material3.Surface(
                    color = Color.Black,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) { paddingValues ->
                        val navController = rememberNavController()

                        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(paddingValues)) {
                            AppNavigation(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AIDungeonMasterTheme {
        Greeting("Android")
    }
}