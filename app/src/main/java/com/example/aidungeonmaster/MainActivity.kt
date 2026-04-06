package com.example.aidungeonmaster

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.aidungeonmaster.navigation.AppNavigation
import com.example.aidungeonmaster.ui.theme.AIDungeonMasterTheme
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    // ── Estado compartido con el Composable para mostrar el diálogo ──────────
    private val showLocationRationale = mutableStateOf(false)

    // ── Launcher para solicitar el permiso POST_NOTIFICATIONS (Android 13+) ──
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* El usuario puede activarlo luego desde Ajustes si rechaza. */ }

    // ── Launcher para solicitar ubicación precisa + aproximada juntas ─────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted   = grants[Manifest.permission.ACCESS_FINE_LOCATION]   == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!fineGranted && !coarseGranted) {
            // El usuario denegó ambos: el worker de supermercados no funcionará,
            // pero la app sigue operativa. Podríamos informarle si quisiéramos.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 1. Permiso de notificaciones (Android 13+) ────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // ── 2. Permiso de ubicación ───────────────────────────────────────────
        requestLocationPermissionIfNeeded()

        // ── Configuración de barras del sistema ───────────────────────────────
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            AIDungeonMasterTheme {
                androidx.compose.material3.Surface(
                    color    = Color.Black,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Diálogo de justificación de ubicación ─────────────────
                    val showRationale by showLocationRationale

                    if (showRationale) {
                        LocationPermissionRationaleDialog(
                            onConfirm = {
                                showLocationRationale.value = false
                                launchLocationPermission()
                            },
                            onDismiss = {
                                showLocationRationale.value = false
                            }
                        )
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) { paddingValues ->
                        val navController = rememberNavController()
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            AppNavigation(navController = navController)
                        }
                    }
                }
            }
        }
    }

    // ── Lógica de solicitud de ubicación ─────────────────────────────────────

    private fun requestLocationPermissionIfNeeded() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) return  // Ya tenemos permiso, nada que hacer

        // Si Android recomienda mostrar una justificación previa, la mostramos
        // mediante el diálogo Compose (se activa en el setContent de arriba)
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationRationale.value = true
        } else {
            // Primera vez o "No volver a preguntar" → pedimos directamente
            launchLocationPermission()
        }
    }

    private fun launchLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

// ── Diálogo de justificación ─────────────────────────────────────────────────

/**
 * Explica al usuario POR QUÉ la app necesita la ubicación antes de lanzar
 * el diálogo del sistema. Es una buena práctica recomendada por Google cuando
 * [shouldShowRequestPermissionRationale] devuelve true.
 */
@Composable
private fun LocationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📍 Permiso de ubicación") },
        text  = {
            Text(
                "AI Dungeon Master necesita conocer tu ubicación para detectar " +
                        "supermercados cercanos y convertirlos en tiendas de aventuras.\n\n" +
                        "Tu posición nunca se almacena ni se comparte; solo se usa para " +
                        "buscar comercios en un radio de 500 m."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Conceder permiso")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ahora no")
            }
        }
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AIDungeonMasterTheme {
        Greeting("Android")
    }
}