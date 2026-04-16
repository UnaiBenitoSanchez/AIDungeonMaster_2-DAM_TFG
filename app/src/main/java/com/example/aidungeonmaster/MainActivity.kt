package com.example.aidungeonmaster

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.aidungeonmaster.navigation.AppNavigation
import com.example.aidungeonmaster.ui.theme.AIDungeonMasterTheme

class MainActivity : ComponentActivity() {

    private val pendingPermissions = ArrayDeque<String>()
    private val showPermissionRationale = mutableStateOf(false)
    private var currentPermission by mutableStateOf<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        requestNextPermissionInQueue()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            AIDungeonMasterTheme {
                androidx.compose.material3.Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    val showRationale by showPermissionRationale
                    if (showRationale) {
                        PermissionRationaleDialog(
                            permission = currentPermission,
                            onConfirm = {
                                showPermissionRationale.value = false
                                currentPermission?.let { permissionLauncher.launch(it) }
                            },
                            onDismiss = {
                                showPermissionRationale.value = false
                                requestNextPermissionInQueue()
                            }
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                    ) { paddingValues ->
                        val navController = rememberNavController()
                        Box(modifier = Modifier.padding(paddingValues)) {
                            AppNavigation(navController = navController)
                        }
                    }
                }
            }
        }

        launchInitialPermissionFlowIfNeeded()
    }

    private fun launchInitialPermissionFlowIfNeeded() {
        val prefs = getSharedPreferences("first_run_permissions", Context.MODE_PRIVATE)
        if (prefs.getBoolean("done", false)) return

        buildInitialPermissionQueue()
        requestNextPermissionInQueue()
        prefs.edit().putBoolean("done", true).apply()
    }

    private fun buildInitialPermissionQueue() {
        pendingPermissions.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            pendingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasPermission(Manifest.permission.CAMERA)) {
            pendingPermissions.add(Manifest.permission.CAMERA)
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            pendingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            pendingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            pendingPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun requestNextPermissionInQueue() {
        while (pendingPermissions.isNotEmpty()) {
            val nextPermission = pendingPermissions.removeFirst()
            if (hasPermission(nextPermission)) continue

            currentPermission = nextPermission
            if (shouldShowRequestPermissionRationale(nextPermission)) {
                showPermissionRationale.value = true
            } else {
                permissionLauncher.launch(nextPermission)
            }
            return
        }
        currentPermission = null
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun PermissionRationaleDialog(
    permission: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body) = when (permission) {
        Manifest.permission.POST_NOTIFICATIONS -> "Permiso de notificaciones" to "Se usa para avisos de inactividad, ranking y eventos del juego."
        Manifest.permission.CAMERA -> "Permiso de cámara" to "Se usa para escanear QR, reconocer texto y activar funciones contextuales."
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION -> "Permiso de ubicación" to "Se usa para funciones contextuales, detección de lugares cercanos y exploración del mundo."
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Ubicación en segundo plano" to "Permite mantener activas las mecánicas contextuales y los recordatorios geolocalizados aunque no tengas la app abierta."
        else -> "Permiso" to "La aplicación necesita este permiso para funcionar correctamente."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { Button(onClick = onConfirm) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Saltar") } }
    )
}
