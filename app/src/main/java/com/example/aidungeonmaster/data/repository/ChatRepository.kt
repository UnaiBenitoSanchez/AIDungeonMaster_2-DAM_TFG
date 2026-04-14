package com.example.aidungeonmaster.data.repository

import com.example.aidungeonmaster.data.model.ChatMessage
import com.example.aidungeonmaster.data.model.PrivateChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid

    fun buildFriendshipId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    fun buildChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "chat_${uid1}_${uid2}" else "chat_${uid2}_${uid1}"
    }

    suspend fun getOrCreatePrivateChat(friendUid: String): String {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(friendUid != myUid) { "No puedes crear un chat contigo mismo." }

        val friendshipId = buildFriendshipId(myUid, friendUid)
        val friendshipDoc = db.collection("friendships")
            .document(friendshipId)
            .get()
            .await()

        if (!friendshipDoc.exists()) {
            throw IllegalStateException("Solo puedes escribir a usuarios que sean tus amigos.")
        }

        val chatId = buildChatId(myUid, friendUid)
        val now = System.currentTimeMillis()

        val chat = PrivateChat(
            id = chatId,
            members = listOf(myUid, friendUid).sorted(),
            friendshipId = friendshipId,
            createdAt = now,
            lastMessage = "",
            lastMessageAt = 0L,
            lastSenderUid = ""
        )

        db.collection("private_chats")
            .document(chatId)
            .set(chat)
            .await()

        return chatId
    }

    suspend fun sendMessage(chatId: String, text: String) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val cleanText = text.trim()
        require(cleanText.isNotBlank()) { "El mensaje no puede estar vacío." }

        val chatRef = db.collection("private_chats").document(chatId)
        val now = System.currentTimeMillis()
        val messageRef = chatRef.collection("messages").document()

        val message = ChatMessage(
            id = messageRef.id,
            senderUid = myUid,
            text = cleanText,
            type = "text",
            createdAt = now,
            seenBy = listOf(myUid)
        )

        db.runBatch { batch ->
            batch.set(messageRef, message)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to cleanText,
                    "lastMessageAt" to now,
                    "lastSenderUid" to myUid
                )
            )
        }.await()
    }

    suspend fun markMessagesAsSeen(chatId: String, messages: List<ChatMessage>) {
        val myUid = currentUid() ?: return

        val unseenMessages = messages.filter { msg ->
            msg.senderUid != myUid && !msg.seenBy.contains(myUid)
        }

        if (unseenMessages.isEmpty()) return

        db.runBatch { batch ->
            unseenMessages.forEach { msg ->
                val newSeenBy = (msg.seenBy + myUid).distinct()
                val msgRef = db.collection("private_chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msg.id)

                batch.update(msgRef, "seenBy", newSeenBy)
            }
        }.await()
    }

    fun listenMessages(
        chatId: String,
        onChange: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection("private_chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error escuchando mensajes")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }

                onChange(messages)
            }
    }
}