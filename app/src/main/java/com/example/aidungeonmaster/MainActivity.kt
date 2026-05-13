package com.example.aidungeonmaster

import com.example.aidungeonmaster.ui.i18n.Text
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.aidungeonmaster.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private val pendingPermissions = ArrayDeque<String>()
    private val showPermissionRationale = mutableStateOf(false)
    private var currentPermission by mutableStateOf<String?>(null)

    private var colorBlindType by mutableStateOf(ColorBlindType.NONE)

    /** Ruta de navegación pendiente procedente de una notificación. */
    private var pendingNavRoute by mutableStateOf<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        requestNextPermissionInQueue()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageManager.applySavedLanguage(this)

        colorBlindType = ColorBlindPreferencesManager.load(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
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
                                        navController = navController,
                                        onColorBlindChanged = { newType ->
                                            colorBlindType = newType
                                            ColorBlindPreferencesManager.save(
                                                this@MainActivity,
                                                newType
                                            )
                                        },
                                        pendingRoute = pendingNavRoute,
                                        onRouteConsumed = { pendingNavRoute = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        launchPermissionFlowIfNeeded()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * Lee los extras de navegación que inserta [NotificationHelper] en el Intent
     * y los convierte en la ruta de Compose Navigation correspondiente.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        val target = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_TARGET) ?: return
        intent.removeExtra(NotificationHelper.EXTRA_NAV_TARGET)

        pendingNavRoute = when (target) {
            NotificationHelper.NAV_TARGET_CHAT -> {
                val uid  = intent.getStringExtra(NotificationHelper.EXTRA_NAV_SENDER_UID).orEmpty()
                val name = intent.getStringExtra(NotificationHelper.EXTRA_NAV_SENDER_NAME).orEmpty()
                if (uid.isNotBlank()) {
                    "private_chat/${Uri.encode(uid)}/${Uri.encode(name.ifBlank { "Aventurero" })}"
                } else null
            }
            NotificationHelper.NAV_TARGET_FRIEND_REQ -> "friend_requests"
            NotificationHelper.NAV_TARGET_FRIENDS    -> "friends_list"
            NotificationHelper.NAV_TARGET_GUILD_BOSS -> {
                val guildId = intent.getStringExtra(NotificationHelper.EXTRA_NAV_GUILD_ID).orEmpty()
                if (guildId.isNotBlank()) {
                    "guild_details/${Uri.encode(guildId)}?tab=${Uri.encode("jefe_final")}"
                } else null
            }
            else -> null
        }
    }

    override fun onStart() {
        super.onStart()
        launchPermissionFlowIfNeeded()
    }

    private fun launchPermissionFlowIfNeeded() {
        if (currentPermission != null || showPermissionRationale.value) return

        buildPermissionQueue()
        requestNextPermissionInQueue()
        requestIgnoreBatteryOptimizationsIfNeeded()
    }

    private fun requestIgnoreBatteryOptimizationsIfNeeded() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun buildPermissionQueue() {
        pendingPermissions.clear()

        // IMPORTANTE:
        // Las notificaciones NO deben quedar atadas al "primer arranque",
        // porque si el usuario ya tenía la app instalada o denegó el permiso
        // una vez, de otro modo no se volverían a pedir nunca.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            pendingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences("first_run_permissions", Context.MODE_PRIVATE)
        val firstRunDone = prefs.getBoolean("done", false)

        if (!firstRunDone) {
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

            prefs.edit().putBoolean("done", true).apply()
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
        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun PermissionRationaleDialog(
    permission: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body) = when (permission) {
        Manifest.permission.POST_NOTIFICATIONS ->
            "Permiso de notificaciones" to
                    "Se usa para avisos de mensajes directos, solicitudes de amistad, " +
                    "aceptaciones, ranking, inactividad y eventos del juego."
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
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { Button(onClick = onConfirm) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Saltar") } }
    )
}