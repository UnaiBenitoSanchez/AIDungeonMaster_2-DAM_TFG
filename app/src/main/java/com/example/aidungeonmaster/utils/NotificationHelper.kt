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
 *  - CHANNEL_RANKING    → Alta prioridad. Perdida de puesto en TOP 3.
 *  - CHANNEL_INACTIVITY → Prioridad normal. Recordatorio de inactividad.
 *  - CHANNEL_PROXIMITY  → Alta prioridad. Supermercado cerca (tienda de pociones).
 */
object NotificationHelper {

    const val CHANNEL_RANKING    = "ranking_channel"
    const val CHANNEL_INACTIVITY = "inactivity_channel"
    const val CHANNEL_PROXIMITY  = "proximity_channel"
    const val CHANNEL_SOCIAL     = "social_channel"
    const val CHANNEL_GUILD_RAID = "guild_raid_channel"

    // ── Extras de navegación (leídos en MainActivity) ─────────────────────────
    const val EXTRA_NAV_TARGET      = "nav_target"
    const val NAV_TARGET_CHAT       = "private_chat"
    const val NAV_TARGET_FRIEND_REQ = "friend_requests"
    const val NAV_TARGET_FRIENDS    = "friends_list"
    const val NAV_TARGET_GUILD_BOSS = "guild_boss_battle"
    const val EXTRA_NAV_SENDER_UID  = "nav_sender_uid"
    const val EXTRA_NAV_SENDER_NAME = "nav_sender_name"
    const val EXTRA_NAV_GUILD_ID    = "nav_guild_id"

    // ── Emojis de posición ────────────────────────────────────────────────────

    private fun positionEmoji(pos: Int) = when (pos) {
        0    -> "🥇"
        1    -> "🥈"
        2    -> "🥉"
        else -> "🏅"
    }

    // Ejecuta la lógica de position name.
    private fun positionName(pos: Int) = when (pos) {
        0    -> "primer puesto"
        1    -> "segundo puesto"
        2    -> "tercer puesto"
        else -> "puesto #${pos + 1}"
    }

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

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROXIMITY,
                    "Tienda Cercana",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        "Aviso cuando hay un supermercado cerca donde comprar pociones y suministros"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SOCIAL,
                    "Eventos sociales",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Mensajes privados, solicitudes de amistad y aceptaciones"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GUILD_RAID,
                    "Jefe final del gremio",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos de actividad en la sala de espera del jefe final"
                    enableVibration(true)
                }
            )
        }
    }

    fun showFriendRequestNotification(
        context: Context,
        fromDisplayName: String,
        fromUsername: String,
        notificationId: Int = 4000
    ) {
        val sender = fromDisplayName.ifBlank { fromUsername }.ifBlank { "Un aventurero" }
        val pending = pendingIntent(
            context,
            requestCode = 400 + notificationId,
            intent = launchIntent(context).apply {
                putExtra(EXTRA_NAV_TARGET, NAV_TARGET_FRIEND_REQ)
            }
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🤝 Nueva solicitud de amistad")
            .setContentText("$sender quiere formar grupo contigo.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$sender te ha enviado una solicitud de amistad. " +
                            "Entra en el menú social para aceptarla o rechazarla."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    fun showFriendAcceptedNotification(
        context: Context,
        friendDisplayName: String,
        friendUsername: String,
        notificationId: Int = 4100
    ) {
        val friend = friendDisplayName.ifBlank { friendUsername }.ifBlank { "Tu compañero" }
        val pending = pendingIntent(
            context,
            requestCode = 410 + notificationId,
            intent = launchIntent(context).apply {
                putExtra(EXTRA_NAV_TARGET, NAV_TARGET_FRIENDS)
            }
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ Solicitud aceptada")
            .setContentText("$friend ya forma parte de tus amistades.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$friend ha aceptado tu solicitud de amistad. " +
                            "Ya podéis visitar salas personales y abrir chat privado."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    fun showPrivateMessageNotification(
        context: Context,
        senderName: String,
        messagePreview: String,
        senderUid: String = "",
        notificationId: Int = 4200
    ) {
        val pending = pendingIntent(
            context,
            requestCode = 420 + notificationId,
            intent = launchIntent(context).apply {
                putExtra(EXTRA_NAV_TARGET, NAV_TARGET_CHAT)
                putExtra(EXTRA_NAV_SENDER_UID, senderUid)
                putExtra(EXTRA_NAV_SENDER_NAME, senderName)
            }
        )

        val preview = messagePreview.take(120)
        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💬 Nuevo mensaje de $senderName")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messagePreview))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    fun showGuildBossWaitingRoomNotification(
        context: Context,
        guildName: String,
        playerName: String,
        guildId: String = "",
        notificationId: Int = 4300
    ) {
        val pending = pendingIntent(
            context,
            requestCode = 430 + notificationId,
            intent = launchIntent(context).apply {
                putExtra(EXTRA_NAV_TARGET, NAV_TARGET_GUILD_BOSS)
                putExtra(EXTRA_NAV_GUILD_ID, guildId)
            }
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GUILD_RAID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🐉 Movimiento en la sala del jefe final")
            .setContentText("$playerName se ha unido a la sala de espera de $guildName.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$playerName se ha unido a la sala de espera del jefe final de $guildName. " +
                            "Quizá sea un buen momento para preparar tu personaje y entrar."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    // ── Notificación: personaje desplazado del top 3 ─────────────────────────

    /**
     * @param characterName  Nombre del personaje afectado.
     * @param categoryLabel  Nombre de la categoría (p. ej. "Stats Totales").
     * @param previousPosition  Posición que tenía ANTES (0 = 1er puesto, 1 = 2º, 2 = 3º).
     * @param newPosition    Nueva posición en el ranking (base 0). Usa 999 si sale del top 10.
     * @param notificationId ID único para poder cancelarla o actualizarla después.
     */
    fun showRankingLostNotification(
        context: Context,
        characterName: String,
        categoryLabel: String,
        newPosition: Int,
        previousPosition: Int = -1,   // -1 = desconocido
        notificationId: Int = 1000
    ) {
        val oldPosText = if (previousPosition in 0..2)
            positionName(previousPosition)
        else
            "el Top 3"

        val oldEmoji = if (previousPosition in 0..2)
            positionEmoji(previousPosition)
        else
            "🏆"

        val newPosText = when {
            newPosition >= 100 -> "fuera del top 10"
            else               -> "el puesto #${newPosition + 1}"
        }

        val title   = "$oldEmoji ¡$characterName ha caído del Top 3!"
        val summary = "$characterName ha perdido $oldPosText en $categoryLabel. " +
                "Ahora ocupa $newPosText."
        val bigText = "Tu héroe $characterName ocupaba $oldEmoji $oldPosText en " +
                "la categoría «$categoryLabel» del ranking mundial.\n\n" +
                "Otro aventurero le ha desbancado. Ahora está en $newPosText.\n\n" +
                "¡Vuelve a la batalla y recupera tu gloria!"

        val intent  = launchIntent(context)
        val pending = pendingIntent(context, requestCode = 100 + notificationId, intent)

        val notification = NotificationCompat.Builder(context, CHANNEL_RANKING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    // ── Notificación: inactividad prolongada ──────────────────────────────────

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

    // ── Notificación: supermercado cercano ────────────────────────────────────

    /**
     * @param supermarketName  Nombre del supermercado detectado cercano.
     * @param distanceMeters   Distancia aproximada en metros.
     * @param specialty        Especialidad de esa cadena (pociones, armas, etc.).
     * @param notificationId   ID para no duplicar notificaciones.
     */
    fun showProximityNotification(
        context: Context,
        supermarketName: String,
        distanceMeters: Int,
        specialty: String,
        notificationId: Int = 3000
    ) {
        val distText = when {
            distanceMeters < 100 -> "a menos de 100 m"
            distanceMeters < 250 -> "a unos ${(distanceMeters / 50) * 50} m"
            else                 -> "a unos ${(distanceMeters / 100) * 100} m"
        }

        val title   = "🏪 ¡Mercader cercano detectado!"
        val summary = "$supermarketName está $distText. Especialidad: $specialty"
        val bigText = "Un comerciante de $supermarketName ha instalado su puesto $distText.\n\n" +
                "Especialidad de hoy: $specialty\n\n" +
                "Abre el escáner en la app y apunta al cartel del super para " +
                "acceder a la tienda de aventureros. ¡Las pociones no duran para siempre!"

        val intent  = launchIntent(context)
        val pending = pendingIntent(context, requestCode = 300 + notificationId, intent)

        val notification = NotificationCompat.Builder(context, CHANNEL_PROXIMITY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private fun launchIntent(context: Context) =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    // Ejecuta la lógica de pending intent.
    private fun pendingIntent(context: Context, requestCode: Int, intent: Intent) =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}