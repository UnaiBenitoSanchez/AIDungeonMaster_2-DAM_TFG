package com.example.aidungeonmaster.services

import android.util.Log
import com.example.aidungeonmaster.data.repository.PushTokenRepository
import com.example.aidungeonmaster.utils.NotificationHelper
import com.example.aidungeonmaster.utils.PushConstants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AIDungeonFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        NotificationHelper.createChannels(applicationContext)
        Log.d(TAG, "Firebase devolvió un nuevo token FCM")

        serviceScope.launch {
            runCatching {
                PushTokenRepository(applicationContext).syncProvidedToken(token)
            }.onFailure {
                Log.e(TAG, "No se pudo sincronizar el nuevo token FCM", it)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        NotificationHelper.createChannels(applicationContext)

        Log.d(TAG, "Push recibida. data=${remoteMessage.data}")

        val data = remoteMessage.data
        when (data[PushConstants.EXTRA_EVENT_TYPE].orEmpty()) {
            PushConstants.EVENT_TYPE_CHAT_MESSAGE -> {
                val senderName = data[PushConstants.EXTRA_SENDER_NAME].orEmpty()
                    .ifBlank { "Aventurero" }

                val messagePreview = data[PushConstants.EXTRA_MESSAGE_PREVIEW].orEmpty()
                    .ifBlank {
                        data["body"].orEmpty().ifBlank { "Te han enviado un nuevo mensaje." }
                    }

                NotificationHelper.showPrivateMessageNotification(
                    context = applicationContext,
                    senderName = senderName,
                    messagePreview = messagePreview,
                    senderUid = data[PushConstants.EXTRA_SENDER_UID].orEmpty(),
                    notificationId = buildChatNotificationId(data)
                )
            }

            PushConstants.EVENT_TYPE_FRIEND_REQUEST -> {
                val senderName = data[PushConstants.EXTRA_SENDER_NAME].orEmpty()

                NotificationHelper.showFriendRequestNotification(
                    context = applicationContext,
                    fromDisplayName = senderName,
                    fromUsername = senderName,
                    notificationId = (
                            "push_friend_request|${data[PushConstants.EXTRA_REQUEST_ID].orEmpty()}"
                            ).hashCode()
                )
            }

            PushConstants.EVENT_TYPE_FRIEND_ACCEPTED -> {
                val senderName = data[PushConstants.EXTRA_SENDER_NAME].orEmpty()

                NotificationHelper.showFriendAcceptedNotification(
                    context = applicationContext,
                    friendDisplayName = senderName,
                    friendUsername = senderName,
                    notificationId = (
                            "push_friend_accepted|${data[PushConstants.EXTRA_REQUEST_ID].orEmpty()}"
                            ).hashCode()
                )
            }

            PushConstants.EVENT_TYPE_GUILD_WAITING_ROOM -> {
                NotificationHelper.showGuildBossWaitingRoomNotification(
                    context = applicationContext,
                    guildName = data[PushConstants.EXTRA_GUILD_NAME].orEmpty(),
                    playerName = data[PushConstants.EXTRA_PLAYER_NAME].orEmpty(),
                    guildId = data[PushConstants.EXTRA_GUILD_ID].orEmpty(),
                    notificationId = (
                            "push_guild_waiting|${data[PushConstants.EXTRA_GUILD_ID].orEmpty()}|${
                                data[PushConstants.EXTRA_SENDER_UID].orEmpty()
                            }"
                            ).hashCode()
                )
            }

            else -> {
                val title = data["title"].orEmpty().ifBlank { remoteMessage.notification?.title.orEmpty() }
                val body = data["body"].orEmpty().ifBlank { remoteMessage.notification?.body.orEmpty() }
                Log.d(TAG, "Push recibida sin eventType reconocido. title=$title body=$body")
            }
        }
    }

    private fun buildChatNotificationId(data: Map<String, String>): Int {
        val eventId = data[PushConstants.EXTRA_EVENT_ID].orEmpty()
        if (eventId.isNotBlank()) {
            return "push_chat_event|$eventId".hashCode()
        }

        val sentAt = data[PushConstants.EXTRA_SENT_AT].orEmpty()
        if (sentAt.isNotBlank()) {
            return "push_chat_sent_at|$sentAt".hashCode()
        }

        val chatId = data[PushConstants.EXTRA_CHAT_ID].orEmpty()
        val senderUid = data[PushConstants.EXTRA_SENDER_UID].orEmpty()
        val messagePreview = data[PushConstants.EXTRA_MESSAGE_PREVIEW].orEmpty()

        return "push_chat|$chatId|$senderUid|$messagePreview".hashCode()
    }

    companion object {
        private const val TAG = "AIDungeonFCMService"
    }
}