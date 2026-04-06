package com.example.aidungeonmaster.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aidungeonmaster.utils.NotificationHelper
import com.example.aidungeonmaster.viewmodel.RankingCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Worker periódico que comprueba, para cada categoría del ranking, si algún
 * personaje del usuario autenticado ha perdido su puesto en el TOP 3 mundial.
 *
 * Algoritmo:
 *   1. Consulta el top 10 de cada categoría en Firestore.
 *   2. Busca documentos cuyo ID empiece por "{uid}_" (formato del proyecto).
 *   3. Compara la posición actual con la guardada en SharedPreferences.
 *   4. Si el personaje estaba en posición 0-2 y ya no está → notificación.
 *   5. Persiste la nueva posición para el próximo ciclo.
 *
 * Frecuencia de ejecución: cada 30 minutos (configurable en AIDungeonMasterApp).
 * Requiere conexión a internet (constraint NETWORK_TYPE_CONNECTED).
 */
class RankingCheckWorker(
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

        var hasError = false
        for (category in RankingCategory.entries) {
            try {
                checkCategory(userId, category)
            } catch (e: Exception) {
                Log.e(TAG, "Error comprobando ${category.name}: ${e.message}")
                hasError = true
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }

    // ── Lógica principal por categoría ────────────────────────────────────────

    private suspend fun checkCategory(userId: String, category: RankingCategory) {
        val snap = db.collection("ranking")
            .orderBy(category.field, Query.Direction.DESCENDING)
            .limit(FETCH_LIMIT)
            .get()
            .await()

        val docs = snap.documents
        // IDs de los 3 primeros para comparación rápida
        val top3Ids = docs.take(3).map { it.id }.toSet()

        // Procesar los personajes del usuario que aparecen en este top-10
        docs.forEachIndexed { index, doc ->
            val charId = doc.id
            if (!charId.startsWith("${userId}_")) return@forEachIndexed

            val charName = doc.getString("characterName")
                ?: charId.removePrefix("${userId}_")

            val prefKey       = prefKey(category, charId)
            val previousPos   = prefs.getInt(prefKey, POSITION_UNKNOWN)
            val wasInTop3     = previousPos in 0..2
            val isNowInTop3   = charId in top3Ids

            Log.d(TAG, "${category.name} | $charName | prev=$previousPos | now=$index")

            if (wasInTop3 && !isNowInTop3) {
                Log.i(TAG, "$charName PERDIÓ top 3 en ${category.label} (ahora #${index + 1})")
                NotificationHelper.showRankingLostNotification(
                    context          = applicationContext,
                    characterName    = charName,
                    categoryLabel    = category.label,
                    previousPosition = previousPos,
                    newPosition      = index,
                    notificationId   = notifId(userId, category, charId)
                )
            }

            // Actualizar posición guardada
            prefs.edit().putInt(prefKey, index).apply()
        }

        // Si el personaje del usuario no apareció en el top-10 pero antes estaba
        // en top 3 → notificar y marcar como "fuera del ranking visible"
        val userPrefKeys = prefs.all.keys.filter {
            it.startsWith("pos_${category.name}_${userId}_")
        }
        userPrefKeys.forEach { key ->
            val charId      = key.removePrefix("pos_${category.name}_")
            val appearsNow  = docs.any { it.id == charId }
            if (!appearsNow) {
                val prevPos = prefs.getInt(key, POSITION_UNKNOWN)
                if (prevPos in 0..2) {
                    val charName = charId.removePrefix("${userId}_")
                    Log.i(TAG, "$charName ya no aparece en top 10 de ${category.label}")
                    NotificationHelper.showRankingLostNotification(
                        context          = applicationContext,
                        characterName    = charName,
                        categoryLabel    = category.label,
                        previousPosition = prevPos,
                        newPosition      = POSITION_OUT_OF_TOP,
                        notificationId   = notifId(userId, category, charId)
                    )
                }
                // Resetear para no volver a notificar
                prefs.edit().putInt(key, POSITION_UNKNOWN).apply()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Clave en SharedPreferences para guardar la posición de un personaje en una categoría */
    private fun prefKey(category: RankingCategory, charId: String) =
        "pos_${category.name}_$charId"

    /** ID único de notificación combinando usuario, categoría y personaje */
    private fun notifId(userId: String, category: RankingCategory, charId: String): Int =
        ("$userId|${category.name}|$charId").hashCode()

    companion object {
        private const val TAG              = "RankingCheckWorker"
        private const val PREFS_NAME       = "ranking_positions_prefs"
        private const val FETCH_LIMIT      = 10L
        private const val POSITION_UNKNOWN = -1
        private const val POSITION_OUT_OF_TOP = 999
    }
}