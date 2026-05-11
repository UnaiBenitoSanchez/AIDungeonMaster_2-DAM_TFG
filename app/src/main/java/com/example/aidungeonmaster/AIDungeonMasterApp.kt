package com.example.aidungeonmaster

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.aidungeonmaster.data.repository.GuildRaidRepository
import com.example.aidungeonmaster.data.repository.PushTokenRepository
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.example.aidungeonmaster.ui.settings.AppLanguageManager
import com.example.aidungeonmaster.utils.InactivityReminderScheduler
import com.example.aidungeonmaster.utils.NotificationHelper
import com.example.aidungeonmaster.utils.RealtimeNotificationManager
import com.example.aidungeonmaster.utils.SupermarketProximityManager
import com.example.aidungeonmaster.workers.InactivityWorker
import com.example.aidungeonmaster.workers.RankingCheckWorker
import com.example.aidungeonmaster.workers.SupermarketProximityWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AIDungeonMasterApp : Application(), DefaultLifecycleObserver {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var socialRepository: SocialRepository
    private lateinit var guildRaidRepository: GuildRaidRepository
    private lateinit var realtimeNotificationManager: RealtimeNotificationManager
    private lateinit var pushTokenRepository: PushTokenRepository
    private lateinit var auth: FirebaseAuth
    private var presenceHeartbeatJob: Job? = null
    private var pushSyncJob: Job? = null
    private var supermarketProximityManager: SupermarketProximityManager? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null && ::socialRepository.isInitialized) {
            startPresenceHeartbeat()
            realtimeNotificationManager.start()

            appScope.launch {
                runCatching { socialRepository.updatePresence(true) }
                    .onFailure { Log.e(TAG, "No se pudo actualizar presencia a online", it) }

                syncPushTokenWithRetry("auth_listener")

                try {
                    InactivityReminderScheduler.rescheduleForCurrentUser(this@AIDungeonMasterApp)
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo reprogramar la inactividad", e)
                }
            }
        } else {
            stopPresenceHeartbeat()
            stopRealtimeProximity()

            if (::realtimeNotificationManager.isInitialized) {
                realtimeNotificationManager.stop()
            }

            pushSyncJob?.cancel()
            InactivityReminderScheduler.cancel(this)
        }
    }

    override fun onCreate() {
        super<Application>.onCreate()

        instance = this
        FirebaseApp.initializeApp(this)
        AppLanguageManager.applySavedLanguage(this)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        socialRepository = SocialRepository()
        guildRaidRepository = GuildRaidRepository()
        realtimeNotificationManager = RealtimeNotificationManager(this)
        pushTokenRepository = PushTokenRepository(this)
        auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(authListener)

        runCatching {
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
        }.onFailure { Log.e(TAG, "No se pudo activar el auto-init de Firebase Messaging", it) }

        NotificationHelper.createChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val testRequest = OneTimeWorkRequestBuilder<RankingCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(testRequest)

        scheduleRankingCheck()
        scheduleInactivityReminder()
        scheduleProximityCheck()

        if (auth.currentUser != null) {
            syncPushTokenWithRetry("application_on_create")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        startPresenceHeartbeat()
        startRealtimeProximityIfPossible()

        if (::socialRepository.isInitialized) {
            appScope.launch {
                runCatching { socialRepository.updatePresence(true) }
                    .onFailure { Log.e(TAG, "No se pudo actualizar presencia en onStart", it) }

                syncPushTokenWithRetry("process_on_start")

                try {
                    InactivityReminderScheduler.rescheduleForCurrentUser(this@AIDungeonMasterApp)
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo reprogramar la inactividad en onStart", e)
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        stopRealtimeProximity()
        markAppInactiveAndCleanup()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            markAppInactiveAndCleanup()
        }
    }

    override fun onTerminate() {
        stopPresenceHeartbeat()
        stopRealtimeProximity()
        pushSyncJob?.cancel()

        if (::realtimeNotificationManager.isInitialized) {
            realtimeNotificationManager.destroy()
        }
        if (::auth.isInitialized) {
            auth.removeAuthStateListener(authListener)
        }

        super.onTerminate()
    }

    private fun syncPushTokenWithRetry(reason: String) {
        if (!::pushTokenRepository.isInitialized) return
        if (auth.currentUser == null) return

        pushSyncJob?.cancel()
        pushSyncJob = appScope.launch {
            val delaysMs = listOf(0L, 2_500L, 8_000L)

            for ((index, delayMs) in delaysMs.withIndex()) {
                if (delayMs > 0L) delay(delayMs)
                if (auth.currentUser == null) return@launch

                val result = runCatching {
                    FirebaseMessaging.getInstance().isAutoInitEnabled = true
                    pushTokenRepository.syncCurrentToken()
                }

                if (result.isSuccess) {
                    Log.d(TAG, "Registro push completado. reason=$reason intento=${index + 1}")
                    return@launch
                } else {
                    Log.e(
                        TAG,
                        "Fallo registrando token push. reason=$reason intento=${index + 1}",
                        result.exceptionOrNull()
                    )
                }
            }
        }
    }

    private fun startRealtimeProximityIfPossible() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) return
        if (supermarketProximityManager != null) return

        supermarketProximityManager = SupermarketProximityManager(this).also { manager ->
            runCatching { manager.start() }
                .onFailure { Log.e(TAG, "No se pudo iniciar la proximidad en tiempo real", it) }
        }
    }

    private fun stopRealtimeProximity() {
        supermarketProximityManager?.stop()
        supermarketProximityManager = null
    }

    private fun startPresenceHeartbeat() {
        if (!::socialRepository.isInitialized || presenceHeartbeatJob?.isActive == true) return

        presenceHeartbeatJob = appScope.launch {
            while (isActive) {
                runCatching { socialRepository.updatePresence(true) }
                    .onFailure { Log.e(TAG, "Error en heartbeat de presencia", it) }
                delay(PRESENCE_HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun stopPresenceHeartbeat() {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
    }

    private fun markAppInactiveAndCleanup() {
        stopPresenceHeartbeat()

        if (!::socialRepository.isInitialized) return

        appScope.launch {
            runCatching { guildRaidRepository.leaveAllWaitingBossRoomsIfPresent() }
                .onFailure { Log.e(TAG, "No se pudo salir de las salas de espera al cerrar", it) }

            runCatching { socialRepository.updatePresence(false) }
                .onFailure { Log.e(TAG, "No se pudo actualizar presencia a offline", it) }
        }
    }

    private fun scheduleRankingCheck() {
        val request = PeriodicWorkRequestBuilder<RankingCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_RANKING_CHECK,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleInactivityReminder() {
        val request = PeriodicWorkRequestBuilder<InactivityWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_INACTIVITY_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleProximityCheck() {
        val request = PeriodicWorkRequestBuilder<SupermarketProximityWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_PROXIMITY_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val TAG = "AIDungeonMasterApp"

        lateinit var instance: AIDungeonMasterApp
            private set

        private const val WORK_RANKING_CHECK = "ranking_check"
        private const val WORK_INACTIVITY_REMINDER = "inactivity_reminder"
        private const val WORK_PROXIMITY_CHECK = "supermarket_proximity_check"
        private const val PRESENCE_HEARTBEAT_INTERVAL_MS = 45_000L
    }
}