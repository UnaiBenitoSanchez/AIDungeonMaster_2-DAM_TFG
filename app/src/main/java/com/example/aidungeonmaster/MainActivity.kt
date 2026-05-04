package com.example.aidungeonmaster

import com.example.aidungeonmaster.ui.i18n.Text

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.aidungeonmaster.navigation.AppNavigation
import com.example.aidungeonmaster.ui.settings.AppLanguageManager
import com.example.aidungeonmaster.ui.theme.AIDungeonMasterTheme
import com.example.aidungeonmaster.ui.theme.ColorBlindType
import com.example.aidungeonmaster.ui.theme.LocalColorBlindType
import com.example.aidungeonmaster.ui.theme.colorMatrixForType
import com.example.aidungeonmaster.utils.ColorBlindPreferencesManager

// Clase que encapsula la lógica de main activity.
class MainActivity : ComponentActivity() {

    private val pendingPermissions = ArrayDeque<String>()
    private val showPermissionRationale = mutableStateOf(false)
    private var currentPermission by mutableStateOf<String?>(null)

    // Estado global del modo daltónico, inicializado desde SharedPreferences
    // antes del primer frame para evitar parpadeos de color al arrancar.
    private var colorBlindType by mutableStateOf(ColorBlindType.NONE)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        requestNextPermissionInQueue()
    }

    // Ejecuta la lógica de attach base context.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    // Gestiona el evento de create.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageManager.applySavedLanguage(this)

        // Leer la preferencia guardada antes de componer la UI
        colorBlindType = ColorBlindPreferencesManager.load(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            // Proveemos el tipo activo a toda la jerarquía Composable
            CompositionLocalProvider(LocalColorBlindType provides colorBlindType) {
                AIDungeonMasterTheme {
                    androidx.compose.material3.Surface(
                        color = Color.Black,
                        modifier = Modifier.fillMaxSize()
                    ) {
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

                        // ── Filtro daltónico aplicado a todo el contenido ──────
                        // graphicsLayer intercepta el renderizado del árbol
                        // completo y le aplica la matriz de color correspondiente
                        // sin afectar a la lógica, las animaciones ni el layout.
                        val matrix = remember(colorBlindType) {
                            colorMatrixForType(colorBlindType)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (matrix != null) {
                                        Modifier.graphicsLayer {
                                            colorFilter = ColorFilter.colorMatrix(matrix)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Scaffold(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                            ) { paddingValues ->
                                val navController = rememberNavController()
                                Box(modifier = Modifier.padding(paddingValues)) {
                                    AppNavigation(
                                        navController      = navController,
                                        // Pasamos el tipo y un callback para que
                                        // HomeScreen pueda abrir el diálogo y
                                        // persistir el cambio desde allí.
                                        onColorBlindChanged = { newType ->
                                            colorBlindType = newType
                                            ColorBlindPreferencesManager.save(
                                                this@MainActivity,
                                                newType
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        launchInitialPermissionFlowIfNeeded()
    }

    // ─── Permisos (sin cambios) ─────────────────────────────────────────────

    private fun launchInitialPermissionFlowIfNeeded() {
        val prefs = getSharedPreferences("first_run_permissions", Context.MODE_PRIVATE)
        if (prefs.getBoolean("done", false)) return

        buildInitialPermissionQueue()
        requestNextPermissionInQueue()
        prefs.edit().putBoolean("done", true).apply()
    }

    // Construye initial permission queue.
    private fun buildInitialPermissionQueue() {
        pendingPermissions.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            pendingPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // Ejecuta la lógica de request next permission in queue.
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

    // Comprueba si permission.
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}

@Composable
// Ejecuta la lógica de permission rationale dialog.
private fun PermissionRationaleDialog(
    permission: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body) = when (permission) {
        Manifest.permission.POST_NOTIFICATIONS ->
            "Permiso de notificaciones" to
                    "Se usa para avisos de inactividad, ranking y eventos del juego."
        Manifest.permission.CAMERA ->
            "Permiso de cámara" to
                    "Se usa para escanear QR, reconocer texto y activar funciones contextuales."
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION ->
            "Permiso de ubicación" to
                    "Se usa para funciones contextuales, detección de lugares cercanos y exploración del mundo."
        Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
            "Ubicación en segundo plano" to
                    "Permite mantener activas las mecánicas contextuales y los recordatorios " +
                    "geolocalizados aunque no tengas la app abierta."
        else ->
            "Permiso" to
                    "La aplicación necesita este permiso para funcionar correctamente."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title) },
        text             = { Text(body) },
        confirmButton    = { Button(onClick = onConfirm) { Text("Continuar") } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Saltar") } }
    )
}
