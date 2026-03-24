package com.example.aidungeonmaster.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aidungeonmaster.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Worker periódico que comprueba si el usuario lleva más de [INACTIVITY_THRESHOLD_MS]
 * (12 horas por defecto) sin jugar ninguna partida.
 *
 * Lógica:
 *   1. Obtiene todos los documentos de "partidas" cuyo campo "userId" == uid del usuario.
 *   2. Encuentra el mayor valor de "lastPlayed" (timestamp de la última jugada).
 *   3. Si (ahora − lastPlayed) ≥ umbral Y no se ha enviado ya notificación en ese ciclo
 *      → envía la notificación de recordatorio.
 *   4. Guarda en SharedPreferences el momento en que se envió la última notificación
 *      para no spamear al usuario (mínimo 12 h entre avisos).
 *
 * Frecuencia de ejecución: cada hora (configurable en AIDungeonMasterApp).
 * Requiere conexión a internet (constraint NETWORK_TYPE_CONNECTED).
 */
class InactivityWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db    = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: run {
                Log.d(TAG, "Sin usuario autenticado, omitiendo comprobación.")
                return Result.success()
            }

        return try {
            checkInactivity(userId)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error comprobando inactividad: ${e.message}")
            Result.retry()
        }
    }

    // ── Lógica principal ──────────────────────────────────────────────────────

    private suspend fun checkInactivity(userId: String) {
        val now = System.currentTimeMillis()

        // Anti-spam: no notificar si ya se avisó hace menos de [INACTIVITY_THRESHOLD_MS]
        val lastNotified = prefs.getLong(prefKeyLastNotified(userId), 0L)
        if (now - lastNotified < INACTIVITY_THRESHOLD_MS) {
            Log.d(TAG, "Notificación reciente, omitiendo.")
            return
        }

        // Buscar partidas del usuario
        val snap = db.collection("partidas")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        if (snap.isEmpty) {
            Log.d(TAG, "Sin partidas para $userId")
            return
        }

        // Encontrar la partida más reciente
        var mostRecentLastPlayed = 0L
        var mostRecentCharName   = ""

        snap.documents.forEach { doc ->
            val lastPlayed = doc.getLong("lastPlayed") ?: 0L
            val charName   = doc.getString("characterName") ?: ""
            if (lastPlayed > mostRecentLastPlayed) {
                mostRecentLastPlayed = lastPlayed
                mostRecentCharName   = charName
            }
        }

        // Si nunca se ha jugado (lastPlayed == 0) no recordamos aún
        if (mostRecentLastPlayed == 0L) {
            Log.d(TAG, "Ninguna partida ha sido jugada todavía.")
            return
        }

        val elapsed = now - mostRecentLastPlayed
        Log.d(TAG, "Tiempo inactivo: ${elapsed / 3_600_000} h (umbral: ${INACTIVITY_THRESHOLD_MS / 3_600_000} h)")

        if (elapsed >= INACTIVITY_THRESHOLD_MS) {
            val hoursInactive = elapsed / 3_600_000L
            Log.i(TAG, "¡Inactividad detectada! $mostRecentCharName lleva $hoursInactive h.")

            NotificationHelper.showInactivityNotification(
                context        = applicationContext,
                characterName  = mostRecentCharName,
                hoursInactive  = hoursInactive,
                notificationId = userId.hashCode()
            )

            // Guardar el momento en que notificamos para el anti-spam
            prefs.edit().putLong(prefKeyLastNotified(userId), now).apply()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prefKeyLastNotified(userId: String) = "last_notified_$userId"

    companion object {
        private const val TAG = "InactivityWorker"
        private const val PREFS_NAME = "inactivity_prefs"

        /** Umbral de inactividad: 12 horas en milisegundos */
        const val INACTIVITY_THRESHOLD_MS = 12L * 60L * 60L * 1_000L
    }
}
