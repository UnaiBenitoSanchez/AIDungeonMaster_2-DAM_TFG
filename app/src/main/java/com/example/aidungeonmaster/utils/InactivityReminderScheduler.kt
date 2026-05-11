package com.example.aidungeonmaster.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aidungeonmaster.workers.InactivityWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Programa un recordatorio exacto de inactividad a partir del último lastPlayed.
 *
 * Mantiene el worker periódico existente como red de seguridad cuando la app no
 * vuelve a abrirse, pero añade una versión puntual mucho más precisa.
 */
object InactivityReminderScheduler {

    suspend fun rescheduleForCurrentUser(context: Context) {
        val appContext = context.applicationContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            cancel(appContext)
            return
        }

        val snap = FirebaseFirestore.getInstance()
            .collection("partidas")
            .whereEqualTo("userId", uid)
            .get()
            .await()

        if (snap.isEmpty) {
            cancel(appContext)
            return
        }

        var mostRecentLastPlayed = 0L
        snap.documents.forEach { doc ->
            val lastPlayed = doc.getLong("lastPlayed") ?: 0L
            if (lastPlayed > mostRecentLastPlayed) {
                mostRecentLastPlayed = lastPlayed
            }
        }

        if (mostRecentLastPlayed <= 0L) {
            cancel(appContext)
            return
        }

        val elapsed = System.currentTimeMillis() - mostRecentLastPlayed
        val delayMs = (InactivityWorker.INACTIVITY_THRESHOLD_MS - elapsed).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<InactivityWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(TAG_EXACT_INACTIVITY)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(TAG_EXACT_INACTIVITY)
    }

    private const val UNIQUE_WORK_NAME = "exact_inactivity_reminder"
    private const val TAG_EXACT_INACTIVITY = "tag_exact_inactivity_reminder"
}