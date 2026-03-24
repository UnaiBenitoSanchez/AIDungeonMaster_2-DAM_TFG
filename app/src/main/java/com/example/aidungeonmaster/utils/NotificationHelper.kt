package com.example.aidungeonmaster.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.aidungeonmaster.MainActivity
import com.example.aidungeonmaster.R

/**
 * Centraliza la creación de canales y el envío de notificaciones locales.
 *
 * Canales disponibles:
 *  - CHANNEL_RANKING    → Alta prioridad. Se activa cuando un personaje pierde el top 3.
 *  - CHANNEL_INACTIVITY → Prioridad normal. Recordatorio si llevas +12 h sin jugar.
 */
object NotificationHelper {

    const val CHANNEL_RANKING    = "ranking_channel"
    const val CHANNEL_INACTIVITY = "inactivity_channel"

    // ── Creación de canales (llamar una sola vez, en Application.onCreate) ────

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RANKING,
                    "Alertas de Ranking",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        "Aviso cuando un personaje tuyo pierde su puesto en el top 3 mundial"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_INACTIVITY,
                    "Recordatorios de Aventura",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Recordatorio para retomar tus aventuras activas"
                }
            )
        }
    }

    // ── Notificación: personaje desplazado del top 3 ─────────────────────────

    /**
     * @param characterName  Nombre del personaje afectado.
     * @param categoryLabel  Nombre de la categoría (p. ej. "Stats Totales").
     * @param newPosition    Nueva posición en el ranking (base 0). Usa 999 si sale del top 10.
     * @param notificationId ID único para poder cancelarla o actualizarla después.
     */
    fun showRankingLostNotification(
        context: Context,
        characterName: String,
        categoryLabel: String,
        newPosition: Int,
        notificationId: Int = 1000
    ) {
        val posText = if (newPosition >= 10) "fuera del top 10" else "#${newPosition + 1}"
        val intent  = launchIntent(context)
        val pending = pendingIntent(context, requestCode = 100 + notificationId, intent)

        val notification = NotificationCompat.Builder(context, CHANNEL_RANKING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚔️ ¡Alerta de Ranking!")
            .setContentText(
                "$characterName ha perdido el podio en $categoryLabel. Ahora es $posText."
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Tu héroe $characterName ha sido desbancado del podio de $categoryLabel. " +
                            "Ahora ocupa la posición $posText. " +
                            "¡Vuelve a la batalla para recuperar tu gloria!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    // ── Notificación: inactividad prolongada ──────────────────────────────────

    /**
     * @param characterName  El personaje más reciente del usuario.
     * @param hoursInactive  Horas sin jugar (para construir el texto).
     * @param notificationId ID único para evitar duplicados por usuario.
     */
    fun showInactivityNotification(
        context: Context,
        characterName: String,
        hoursInactive: Long,
        notificationId: Int = 2000
    ) {
        val timeText = when {
            hoursInactive >= 48 -> "${hoursInactive / 24} días"
            hoursInactive >= 24 -> "1 día"
            else                -> "$hoursInactive horas"
        }
        val intent  = launchIntent(context)
        val pending = pendingIntent(context, requestCode = 200 + notificationId, intent)

        val notification = NotificationCompat.Builder(context, CHANNEL_INACTIVITY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🗡️ ¡Tu aventura te espera!")
            .setContentText(
                "$characterName lleva $timeText sin aventurarse. ¡El reino te necesita!"
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Han pasado $timeText desde que $characterName pisó tierras desconocidas. " +
                            "Las mazmorras esperan, los tesoros siguen sin reclamar... " +
                            "¡Continúa tu aventura!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private fun launchIntent(context: Context) =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

    private fun pendingIntent(context: Context, requestCode: Int, intent: Intent) =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
