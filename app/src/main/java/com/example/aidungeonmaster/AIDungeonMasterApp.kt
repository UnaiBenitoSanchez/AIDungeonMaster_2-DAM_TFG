package com.example.aidungeonmaster

import android.app.Application
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
import com.example.aidungeonmaster.data.repository.SocialRepository
import com.example.aidungeonmaster.utils.NotificationHelper
import com.example.aidungeonmaster.workers.InactivityWorker
import com.example.aidungeonmaster.workers.RankingCheckWorker
import com.example.aidungeonmaster.workers.SupermarketProximityWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AIDungeonMasterApp : Application(), DefaultLifecycleObserver {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var socialRepository: SocialRepository
    private lateinit var auth: FirebaseAuth

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null && ::socialRepository.isInitialized) {
            appScope.launch {
                runCatching { socialRepository.updatePresence(true) }
            }
        }
    }

    override fun onCreate() {
        super<Application>.onCreate()

        FirebaseApp.initializeApp(this)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        socialRepository = SocialRepository()
        auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(authListener)

        NotificationHelper.createChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val testRequest = OneTimeWorkRequestBuilder<RankingCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(testRequest)

        scheduleRankingCheck()
        scheduleInactivityReminder()
        scheduleProximityCheck()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (::socialRepository.isInitialized) {
            appScope.launch {
                runCatching { socialRepository.updatePresence(true) }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (::socialRepository.isInitialized) {
            appScope.launch {
                runCatching { socialRepository.updatePresence(false) }
            }
        }
    }

    override fun onTerminate() {
        if (::auth.isInitialized) {
            auth.removeAuthStateListener(authListener)
        }
        super.onTerminate()
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
        private const val WORK_RANKING_CHECK = "ranking_check"
        private const val WORK_INACTIVITY_REMINDER = "inactivity_reminder"
        private const val WORK_PROXIMITY_CHECK = "supermarket_proximity_check"
    }
}