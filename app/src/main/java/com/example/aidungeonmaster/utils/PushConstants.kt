package com.example.aidungeonmaster.utils

object PushConstants {
    const val EVENT_TYPE_CHAT_MESSAGE = "chat_message"
    const val EVENT_TYPE_FRIEND_REQUEST = "friend_request"
    const val EVENT_TYPE_FRIEND_ACCEPTED = "friend_accepted"
    const val EVENT_TYPE_GUILD_WAITING_ROOM = "guild_waiting_room"

    const val EXTRA_EVENT_TYPE = "eventType"
    const val EXTRA_CHAT_ID = "chatId"
    const val EXTRA_REQUEST_ID = "requestId"
    const val EXTRA_GUILD_ID = "guildId"
    const val EXTRA_SENDER_UID = "senderUid"
    const val EXTRA_SENDER_NAME = "senderName"
    const val EXTRA_MESSAGE_PREVIEW = "messagePreview"
    const val EXTRA_GUILD_NAME = "guildName"
    const val EXTRA_PLAYER_NAME = "playerName"

    const val EXTRA_TARGET_UID = "targetUid"

    const val EXTRA_EVENT_ID = "eventId"
    const val EXTRA_SENT_AT = "sentAt"
}