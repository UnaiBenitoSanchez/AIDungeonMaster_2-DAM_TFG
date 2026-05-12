package com.example.aidungeonmaster.data.repository

import android.util.Log
import com.example.aidungeonmaster.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

// Repositorio que centraliza el acceso a datos de chat.
class ChatRepository {

    private val pushGatewayRepository = PushGatewayRepository()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Devuelve el UID del usuario autenticado actual.
    fun currentUid(): String? = auth.currentUser?.uid

    // Construye un identificador estable para dos usuarios.
    fun buildFriendshipId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // El chat privado reutiliza el mismo identificador estable entre ambos usuarios.
    fun buildChatId(uid1: String, uid2: String): String {
        return buildFriendshipId(uid1, uid2)
    }

    // Obtiene o crea un chat privado entre amigos o miembros del mismo gremio actual.
    suspend fun getOrCreatePrivateChat(friendUid: String, guildId: String? = null): String {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(friendUid != myUid) { "No puedes crear un chat contigo mismo." }

        val access = resolvePrivateChatAccess(
            myUid = myUid,
            otherUid = friendUid,
            explicitGuildId = guildId
        )

        if (!access.allowed) {
            throw IllegalStateException("Solo puedes escribir a amigos o miembros de tu gremio actual.")
        }

        val chatId = buildChatId(myUid, friendUid)
        val chatRef = db.collection("private_chats").document(chatId)

        val existingChat = runCatching { chatRef.get().await() }.getOrNull()
        if (existingChat?.exists() == true) {
            return chatId
        }

        val now = System.currentTimeMillis()
        val chat = mutableMapOf<String, Any>(
            "id" to chatId,
            "members" to listOf(myUid, friendUid).sorted(),
            "createdAt" to now,
            "lastMessage" to "",
            "lastMessageAt" to now,
            "lastSenderUid" to myUid
        )

        access.friendshipId?.takeIf { it.isNotBlank() }?.let { chat["friendshipId"] = it }
        access.guildId?.takeIf { it.isNotBlank() }?.let { chat["guildId"] = it }

        chatRef.set(chat).await()
        return chatId
    }

    // Resuelve si el usuario puede abrir un chat privado con otro usuario.
    private suspend fun resolvePrivateChatAccess(
        myUid: String,
        otherUid: String,
        explicitGuildId: String?
    ): PrivateChatAccess {
        val friendshipMirror = db.collection("users")
            .document(myUid)
            .collection("friends")
            .document(otherUid)
            .get()
            .await()

        if (friendshipMirror.exists()) {
            return PrivateChatAccess(
                allowed = true,
                friendshipId = friendshipMirror.getString("friendshipId")
                    .orEmpty()
                    .ifBlank { buildFriendshipId(myUid, otherUid) }
            )
        }

        val normalizedGuildId = explicitGuildId.orEmpty().trim()
        if (normalizedGuildId.isNotBlank() && areUsersMembersOfGuild(normalizedGuildId, myUid, otherUid)) {
            return PrivateChatAccess(
                allowed = true,
                guildId = normalizedGuildId
            )
        }

        val sharedCurrentGuildId = sharedCurrentGuildId(myUid, otherUid)
        if (sharedCurrentGuildId.isNotBlank()) {
            return PrivateChatAccess(
                allowed = true,
                guildId = sharedCurrentGuildId
            )
        }

        return PrivateChatAccess(allowed = false)
    }

    // Comprueba si ambos usuarios tienen el mismo gremio activo en su perfil público.
    private suspend fun sharedCurrentGuildId(myUid: String, otherUid: String): String {
        val myUser = db.collection("users").document(myUid).get().await()
        val otherUser = db.collection("users").document(otherUid).get().await()

        val myCurrentGuildId = myUser.getString("currentGuildId").orEmpty()
        val otherCurrentGuildId = otherUser.getString("currentGuildId").orEmpty()

        return if (myCurrentGuildId.isNotBlank() && myCurrentGuildId == otherCurrentGuildId) {
            myCurrentGuildId
        } else {
            ""
        }
    }

    // Comprueba si ambos usuarios figuran como miembros del gremio indicado.
    private suspend fun areUsersMembersOfGuild(guildId: String, myUid: String, otherUid: String): Boolean {
        val guildMembers = db.collection("guilds").document(guildId).collection("members")
        val myMember = guildMembers.document(myUid).get().await()
        val otherMember = guildMembers.document(otherUid).get().await()
        return myMember.exists() && otherMember.exists()
    }

    // Envía un mensaje al chat y actualiza la vista previa del último mensaje.
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

        val preview = cleanText.take(300)

        db.runBatch { batch ->
            batch.set(messageRef, message)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to preview,
                    "lastMessageAt" to now,
                    "lastSenderUid" to myUid
                )
            )
        }.await()
    }

    // Marca como vistos los mensajes entrantes pendientes del usuario actual.
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

    // Escucha en tiempo real los mensajes del chat privado.
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

    companion object {
        private const val TAG = "ChatRepository"
    }
}

// Resultado de la validación de acceso al chat privado.
private data class PrivateChatAccess(
    val allowed: Boolean,
    val friendshipId: String? = null,
    val guildId: String? = null
)