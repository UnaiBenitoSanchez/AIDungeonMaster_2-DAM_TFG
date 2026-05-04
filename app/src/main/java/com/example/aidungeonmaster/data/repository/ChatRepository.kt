package com.example.aidungeonmaster.data.repository

import com.example.aidungeonmaster.data.model.ChatMessage
import com.example.aidungeonmaster.data.model.PrivateChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

// Repositorio que centraliza el acceso a datos de chat.
class ChatRepository {

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

        val isFriend = db.collection("users")
            .document(myUid)
            .collection("friends")
            .document(friendUid)
            .get()
            .await()
            .exists()

        val allowedByGuild = areUsersInSameCurrentGuild(myUid, friendUid)

        if (!isFriend && !allowedByGuild) {
            throw IllegalStateException("Solo puedes escribir a amigos o miembros de tu gremio actual.")
        }

        val chatId = buildChatId(myUid, friendUid)
        val chatRef = db.collection("private_chats").document(chatId)

        // Si el chat ya existe, no lo tocamos.
        // Tras la primera creación ya eres miembro, así que esta lectura sí está permitida.
        val existingChat = chatRef.get().await()
        if (existingChat.exists()) {
            return chatId
        }

        val now = System.currentTimeMillis()

        val chat = PrivateChat(
            id = chatId,
            members = listOf(myUid, friendUid).sorted(),
            friendshipId = chatId,
            createdAt = now,
            lastMessage = "",
            lastMessageAt = now,
            lastSenderUid = myUid
        )

        // Solo se crea cuando no existe.
        chatRef.set(chat).await()

        return chatId
    }

    // Comprueba si ambos usuarios tienen el mismo gremio activo en su perfil público.
    private suspend fun areUsersInSameCurrentGuild(myUid: String, otherUid: String): Boolean {
        val myUser = db.collection("users").document(myUid).get().await()
        val otherUser = db.collection("users").document(otherUid).get().await()

        val myCurrentGuildId = myUser.getString("currentGuildId").orEmpty()
        val otherCurrentGuildId = otherUser.getString("currentGuildId").orEmpty()

        return myCurrentGuildId.isNotBlank() && myCurrentGuildId == otherCurrentGuildId
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
}