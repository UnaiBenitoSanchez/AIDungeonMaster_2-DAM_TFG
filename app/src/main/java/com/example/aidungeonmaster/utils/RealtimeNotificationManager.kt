package com.example.aidungeonmaster.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.aidungeonmaster.data.model.FriendRequest
//import com.example.aidungeonmaster.viewmodel.RankingCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
//import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Orquesta listeners globales de Firestore para convertir cambios en tiempo real
 * en notificaciones locales sin añadir servicios de pago.
 *
 * Coberturas:
 *  - solicitudes de amistad entrantes
 *  - solicitudes enviadas que han sido aceptadas
 *  - mensajes privados entrantes
 *  - nuevas incorporaciones a la sala de espera del jefe final del gremio
 *  - pérdida de Top 3 del ranking en tiempo real mientras la app sigue viva
 */
class RealtimeNotificationManager(
    private val context: Context
) {

    private val appContext = context.applicationContext
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_REALTIME, Context.MODE_PRIVATE)
//    private val rankingPrefs: SharedPreferences =
//        appContext.getSharedPreferences(PREFS_RANKING, Context.MODE_PRIVATE)

    private var sessionUid: String? = null
    private var incomingRequestsListener: ListenerRegistration? = null
    private var acceptedRequestsListener: ListenerRegistration? = null
    private var privateChatsListener: ListenerRegistration? = null
    private var userGuildsListener: ListenerRegistration? = null

    private val participantListeners = linkedMapOf<String, ListenerRegistration>()
    //    private val rankingListeners = linkedMapOf<RankingCategory, ListenerRegistration>()
//    private val rankingInitialized = linkedMapOf<RankingCategory, Boolean>()
    private val userDisplayNameCache = linkedMapOf<String, String>()
    private val guildNameCache = linkedMapOf<String, String>()

    fun start() {
        val uid = auth.currentUser?.uid ?: run {
            stop()
            return
        }

        if (sessionUid == uid && isRunning()) return

        stop()
        sessionUid = uid

//        startIncomingFriendRequestsListener(uid)
//        startAcceptedRequestsListener(uid)
//        startPrivateChatsListener(uid)
//        startGuildWaitingRoomsListener(uid)
//        startRankingListeners(uid)
    }

    fun stop() {
        incomingRequestsListener?.remove()
        acceptedRequestsListener?.remove()
        privateChatsListener?.remove()
        userGuildsListener?.remove()

        incomingRequestsListener = null
        acceptedRequestsListener = null
        privateChatsListener = null
        userGuildsListener = null

        participantListeners.values.forEach { it.remove() }
        participantListeners.clear()

//        rankingListeners.values.forEach { it.remove() }
//        rankingListeners.clear()
//        rankingInitialized.clear()

        userDisplayNameCache.clear()
        guildNameCache.clear()
        sessionUid = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun isRunning(): Boolean {
        return incomingRequestsListener != null ||
                acceptedRequestsListener != null ||
                privateChatsListener != null ||
                userGuildsListener != null ||
                participantListeners.isNotEmpty()
//                ||
//                rankingListeners.isNotEmpty()
    }

    private fun startIncomingFriendRequestsListener(myUid: String) {
        var initialized = false

        incomingRequestsListener = db.collection("friend_requests")
            .whereEqualTo("toUid", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val requests = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toObject(FriendRequest::class.java)?.copy(id = it.id) }

                if (!initialized) {
                    initialized = true
                    return@addSnapshotListener
                }

                snapshot?.documentChanges.orEmpty().forEach { change ->
                    if (change.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) return@forEach

                    val request = change.document
                        .toObject(FriendRequest::class.java)
                        .copy(id = change.document.id)

                    NotificationHelper.showFriendRequestNotification(
                        context = appContext,
                        fromDisplayName = request.fromDisplayName.ifBlank { request.fromUsername },
                        fromUsername = request.fromUsername,
                        notificationId = ("friend_request|${request.id}").hashCode()
                    )
                }

                if (requests.isEmpty()) {
                    prefs.edit().remove(KEY_LAST_INCOMING_REQUEST_BOOTSTRAP).apply()
                }
            }
    }

    private fun startAcceptedRequestsListener(myUid: String) {
        var initialized = false

        acceptedRequestsListener = db.collection("friend_requests")
            .whereEqualTo("fromUid", myUid)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, _ ->
                if (!initialized) {
                    initialized = true
                    return@addSnapshotListener
                }

                snapshot?.documentChanges.orEmpty().forEach { change ->
                    if (change.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) return@forEach

                    val request = change.document
                        .toObject(FriendRequest::class.java)
                        .copy(id = change.document.id)

                    NotificationHelper.showFriendAcceptedNotification(
                        context = appContext,
                        friendDisplayName = request.toDisplayName.ifBlank { request.toUsername },
                        friendUsername = request.toUsername,
                        notificationId = ("friend_accepted|${request.id}").hashCode()
                    )
                }
            }
    }

    private fun startPrivateChatsListener(myUid: String) {
        var initialized = false

        privateChatsListener = db.collection("private_chats")
            .whereArrayContains("members", myUid)
            .addSnapshotListener { snapshot, _ ->
                val changes = snapshot?.documentChanges.orEmpty()

                if (!initialized) {
                    snapshot?.documents.orEmpty().forEach { chatDoc ->
                        prefs.edit()
                            .putLong(
                                prefKeyChatTimestamp(chatDoc.id),
                                chatDoc.getLong("lastMessageAt") ?: 0L
                            )
                            .apply()
                    }
                    initialized = true
                    return@addSnapshotListener
                }

                changes.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        return@forEach
                    }

                    val chatDoc = change.document
                    val chatId = chatDoc.id
                    val lastMessageAt = chatDoc.getLong("lastMessageAt") ?: 0L
                    val lastSenderUid = chatDoc.getString("lastSenderUid").orEmpty()
                    val lastMessage = chatDoc.getString("lastMessage").orEmpty().trim()
                    val knownTimestamp = prefs.getLong(prefKeyChatTimestamp(chatId), 0L)

                    if (lastMessageAt <= knownTimestamp) {
                        prefs.edit().putLong(prefKeyChatTimestamp(chatId), knownTimestamp).apply()
                        return@forEach
                    }

                    prefs.edit().putLong(prefKeyChatTimestamp(chatId), lastMessageAt).apply()

                    if (lastSenderUid.isBlank() || lastSenderUid == myUid || lastMessage.isBlank()) {
                        return@forEach
                    }

                    scope.launch {
                        val senderName = resolveUserDisplayName(lastSenderUid)
                        NotificationHelper.showPrivateMessageNotification(
                            context = appContext,
                            senderName = senderName,
                            messagePreview = lastMessage,
                            senderUid = lastSenderUid,
                            notificationId = ("private_chat|$chatId|$lastMessageAt").hashCode()
                        )
                    }
                }
            }
    }

    private fun startGuildWaitingRoomsListener(myUid: String) {
        userGuildsListener = db.collection("users")
            .document(myUid)
            .collection("guilds")
            .addSnapshotListener { snapshot, _ ->
                val guildDocs = snapshot?.documents.orEmpty()
                val liveGuildIds = guildDocs.mapNotNull { doc ->
                    val guildId = doc.getString("guildId").orEmpty().ifBlank { doc.id }
                    if (guildId.isBlank()) null else guildId
                }.toSet()

                val toRemove = participantListeners.keys.filter { it !in liveGuildIds }
                toRemove.forEach { guildId ->
                    participantListeners.remove(guildId)?.remove()
                    guildNameCache.remove(guildId)
                }

                guildDocs.forEach { userGuildDoc ->
                    val guildId = userGuildDoc.getString("guildId").orEmpty().ifBlank { userGuildDoc.id }
                    if (guildId.isBlank() || participantListeners.containsKey(guildId)) return@forEach

                    guildNameCache[guildId] = userGuildDoc.getString("name")
                        .orEmpty()
                        .ifBlank { "tu gremio" }

//                    attachParticipantListener(guildId, myUid)
                }
            }
    }

    private fun attachParticipantListener(guildId: String, myUid: String) {
        var initialized = false

        participantListeners[guildId] = db.collection("guilds")
            .document(guildId)
            .collection("boss_rooms")
            .document(com.example.aidungeonmaster.data.repository.GuildRaidRepository.FINAL_BOSS_ROOM_ID)
            .collection("participants")
            .orderBy("joinedAt")
            .addSnapshotListener { snapshot, _ ->
                if (!initialized) {
                    initialized = true
                    return@addSnapshotListener
                }

                snapshot?.documentChanges.orEmpty().forEach { change ->
                    if (change.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) return@forEach

                    val joinedUid = change.document.getString("uid").orEmpty()
                    if (joinedUid.isBlank() || joinedUid == myUid) return@forEach

                    val playerName = change.document.getString("displayName")
                        .orEmpty()
                        .ifBlank { change.document.getString("username").orEmpty() }
                        .ifBlank { "Un aventurero" }

                    NotificationHelper.showGuildBossWaitingRoomNotification(
                        context = appContext,
                        guildName = guildNameCache[guildId].orEmpty().ifBlank { "tu gremio" },
                        playerName = playerName,
                        guildId = guildId,
                        notificationId = (
                                "boss_waiting_room|$guildId|$joinedUid|${
                                    change.document.getLong("joinedAt") ?: 0L
                                }"
                                ).hashCode()
                    )
                }
            }
    }

//    private fun startRankingListeners(userId: String) {
//        RankingCategory.entries.forEach { category ->
//            rankingInitialized[category] = false
//
//            rankingListeners[category] = db.collection("ranking")
//                .orderBy(category.field, Query.Direction.DESCENDING)
//                .limit(10)
//                .addSnapshotListener { snapshot, _ ->
//                    val docs = snapshot?.documents.orEmpty()
//                    val initialized = rankingInitialized[category] == true
//
//                    if (!initialized) {
//                        persistCurrentRankingState(category, userId, docs)
//                        rankingInitialized[category] = true
//                        return@addSnapshotListener
//                    }
//
//                    handleRankingUpdate(category, userId, docs)
//                }
//        }
//    }

//    private fun handleRankingUpdate(
//        category: RankingCategory,
//        userId: String,
//        docs: List<DocumentSnapshot>
//    ) {
//        val top3Ids = docs.take(3).map { it.id }.toSet()
//        val editor = rankingPrefs.edit()
//
//        docs.forEachIndexed { index, doc ->
//            val charId = doc.id
//            if (!charId.startsWith("${userId}_")) return@forEachIndexed
//
//            val charName = doc.getString("characterName")
//                ?: charId.removePrefix("${userId}_")
//
//            val prefKey = prefKeyRankingPosition(category, charId)
//            val previousPos = rankingPrefs.getInt(prefKey, POSITION_UNKNOWN)
//            val wasInTop3 = previousPos in 0..2
//            val isNowInTop3 = charId in top3Ids
//
//            if (wasInTop3 && !isNowInTop3) {
//                NotificationHelper.showRankingLostNotification(
//                    context = appContext,
//                    characterName = charName,
//                    categoryLabel = category.label,
//                    previousPosition = previousPos,
//                    newPosition = index,
//                    notificationId = ("ranking|${category.name}|$charId").hashCode()
//                )
//            }
//
//            editor.putInt(prefKey, index)
//        }
//
//        val existingKeys = rankingPrefs.all.keys.filter {
//            it.startsWith("pos_${category.name}_${userId}_")
//        }
//
//        existingKeys.forEach { key ->
//            val charId = key.removePrefix("pos_${category.name}_")
//            val appearsNow = docs.any { it.id == charId }
//            if (!appearsNow) {
//                val prevPos = rankingPrefs.getInt(key, POSITION_UNKNOWN)
//                if (prevPos in 0..2) {
//                    val charName = charId.removePrefix("${userId}_")
//                    NotificationHelper.showRankingLostNotification(
//                        context = appContext,
//                        characterName = charName,
//                        categoryLabel = category.label,
//                        previousPosition = prevPos,
//                        newPosition = POSITION_OUT_OF_TOP,
//                        notificationId = ("ranking|${category.name}|$charId").hashCode()
//                    )
//                }
//                editor.putInt(key, POSITION_UNKNOWN)
//            }
//        }
//
//        editor.apply()
//    }

//    private fun persistCurrentRankingState(
//        category: RankingCategory,
//        userId: String,
//        docs: List<DocumentSnapshot>
//    ) {
//        val editor = rankingPrefs.edit()
//
//        docs.forEachIndexed { index, doc ->
//            if (!doc.id.startsWith("${userId}_")) return@forEachIndexed
//            editor.putInt(prefKeyRankingPosition(category, doc.id), index)
//        }
//
//        editor.apply()
//    }

    private suspend fun resolveUserDisplayName(uid: String): String {
        userDisplayNameCache[uid]?.let { return it }

        val snap = db.collection("users").document(uid).get().await()
        val name = snap.getString("displayName")
            .orEmpty()
            .ifBlank { snap.getString("username").orEmpty() }
            .ifBlank { "Aventurero" }

        userDisplayNameCache[uid] = name
        return name
    }

    private fun prefKeyChatTimestamp(chatId: String) = "chat_last_message_$chatId"

//    private fun prefKeyRankingPosition(category: RankingCategory, charId: String) =
//        "pos_${category.name}_$charId"

    companion object {
        private const val PREFS_REALTIME = "realtime_notification_prefs"
        //        private const val PREFS_RANKING = "ranking_positions_prefs"
//        private const val POSITION_UNKNOWN = -1
//        private const val POSITION_OUT_OF_TOP = 999
        private const val KEY_LAST_INCOMING_REQUEST_BOOTSTRAP = "incoming_request_bootstrap"
    }
}