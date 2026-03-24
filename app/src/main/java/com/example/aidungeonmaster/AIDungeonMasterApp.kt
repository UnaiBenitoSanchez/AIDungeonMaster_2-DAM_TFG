package com.example.aidungeonmaster

import android.app.Application
import androidx.work.*
import com.example.aidungeonmaster.utils.NotificationHelper
import com.example.aidungeonmaster.workers.InactivityWorker
import com.example.aidungeonmaster.workers.RankingCheckWorker
import java.util.concurrent.TimeUnit

class AIDungeonMasterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)

        // --- AÑADE ESTO SOLO PARA PROBAR ---
        val testRequest = OneTimeWorkRequestBuilder<RankingCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(testRequest)
        // -----------------------------------

        scheduleRankingCheck()
        scheduleInactivityReminder()
    }

    /**
     * Comprueba cada 30 minutos si algún personaje del usuario perdió su
     * puesto en el top 3 de cualquier categoría del ranking mundial.
     *
     * Se usa KEEP para no reiniciar el temporizador si la app se reinicia
     * (evita duplicados y respeta la cadencia mínima real de WorkManager: ~15 min).
     */
    private fun scheduleRankingCheck() {
        val request = PeriodicWorkRequestBuilder<RankingCheckWorker>(
            15, TimeUnit.MINUTES // Pon 15 minutos, es el mínimo real
        )
            // COMENTA O QUITA LAS CONSTRAINTS TEMPORALMENTE
            /* .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ) */
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_RANKING_CHECK,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Comprueba cada hora si el usuario lleva más de 12 horas sin jugar.
     * El propio worker tiene lógica anti-spam (no repite la notificación
     * hasta que pasen otras 12 horas desde el último aviso).
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

    companion object {
        private const val WORK_RANKING_CHECK       = "ranking_check"
        private const val WORK_INACTIVITY_REMINDER = "inactivity_reminder"
    }
}
