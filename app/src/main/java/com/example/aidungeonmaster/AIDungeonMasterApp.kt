package com.example.aidungeonmaster

import android.app.Application
import androidx.work.*
import com.example.aidungeonmaster.utils.NotificationHelper
import com.example.aidungeonmaster.workers.InactivityWorker
import com.example.aidungeonmaster.workers.RankingCheckWorker
import com.example.aidungeonmaster.workers.SupermarketProximityWorker
import java.util.concurrent.TimeUnit

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
class AIDungeonMasterApp : Application() {

    override fun onCreate() {

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        super.onCreate()
        NotificationHelper.createChannels(this)

        // --- SOLO PARA PRUEBAS: ejecutar el RankingCheck una vez al arrancar ---
        val testRequest = OneTimeWorkRequestBuilder<RankingCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(testRequest)
        // -----------------------------------------------------------------------

        scheduleRankingCheck()
        scheduleInactivityReminder()
        scheduleProximityCheck()
    }

    /**
     * Comprueba cada 15 minutos si algún personaje del usuario perdió su
     * puesto en el top 3 de cualquier categoría del ranking mundial.
     */
    private fun scheduleRankingCheck() {
        val request = PeriodicWorkRequestBuilder<RankingCheckWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_RANKING_CHECK,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Comprueba cada hora si el usuario lleva más de 12 horas sin jugar.
     */
    private fun scheduleInactivityReminder() {
        val request = PeriodicWorkRequestBuilder<InactivityWorker>(
            1, TimeUnit.HOURS
        )
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

    /**
     * Comprueba cada 30 minutos si hay un supermercado cercano (radio 500 m).
     * Si lo encuentra, muestra una notificación invitando a abrir la tienda.
     *
     * El worker tiene su propio cooldown interno de 1 hora entre avisos
     * para no saturar al usuario.
     *
     * IMPORTANTE: El permiso ACCESS_FINE_LOCATION debe estar concedido en runtime.
     * Si el usuario lo deniega, el worker simplemente devuelve Result.success()
     * sin notificar.
     */
    private fun scheduleProximityCheck() {
        val request = PeriodicWorkRequestBuilder<SupermarketProximityWorker>(
            30, TimeUnit.MINUTES
        )
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
        private const val WORK_RANKING_CHECK       = "ranking_check"
        private const val WORK_INACTIVITY_REMINDER = "inactivity_reminder"
        private const val WORK_PROXIMITY_CHECK     = "supermarket_proximity_check"
    }
}