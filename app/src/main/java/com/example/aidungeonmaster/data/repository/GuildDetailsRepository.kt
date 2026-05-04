package com.example.aidungeonmaster.data.repository

import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildChatMessage
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

// Repositorio que centraliza el acceso a datos de guild details.
class GuildDetailsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Ejecuta la lógica de current uid.
    fun currentUid(): String? = auth.currentUser?.uid

    // Obtiene guild members.
    suspend fun getGuildMembers(guild: Guild): List<GuildMemberSummary> {
        val membersSnapshot = db.collection("guilds")
            .document(guild.id)
            .collection("members")
            .get()
            .await()

        val membersList = mutableListOf<GuildMemberSummary>()

        for (memberDoc in membersSnapshot.documents) {
            val uid = memberDoc.getString("uid").orEmpty()

            val profileDoc = if (uid.isNotBlank()) {
                db.collection("users")
                    .document(uid)
                    .get()
                    .await()
            } else {
                null
            }

            val member = GuildMemberSummary(
                uid = uid,
                displayName = profileDoc?.getString("displayName")
                    ?.takeIf { it.isNotBlank() }
                    ?: memberDoc.getString("displayName").orEmpty(),
                username = profileDoc?.getString("username")
                    ?.takeIf { it.isNotBlank() }
                    ?: memberDoc.getString("username").orEmpty(),
                photoUrl = profileDoc?.getString("photoUrl").orEmpty(),
                role = memberDoc.getString("role").orEmpty().ifBlank { "member" },
                joinedAt = memberDoc.getLong("joinedAt") ?: 0L,
                characterCount = (profileDoc?.getLong("characterCount") ?: 0L).toInt(),
                isOwner = uid == guild.ownerUid
            )

            membersList.add(member)
        }

        return membersList.sortedWith(
            compareByDescending<GuildMemberSummary> { it.isOwner }
                .thenBy { it.displayName.lowercase() }
        )
    }

    // Escucha guild chat.
    fun listenGuildChat(
        guildId: String,
        onChange: (List<GuildChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection("guilds")
            .document(guildId)
            .collection("chat")
            .orderBy("createdAt")
            .limitToLast(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error escuchando el chat del gremio")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(GuildChatMessage::class.java)?.copy(id = doc.id)
                }

                onChange(messages)
            }
    }

    // Envía guild chat message.
    suspend fun sendGuildChatMessage(guildId: String, text: String) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val cleanText = text.trim()

        require(cleanText.isNotBlank()) { "El mensaje no puede estar vacío." }
        require(cleanText.length <= 2000) { "El mensaje no puede superar los 2000 caracteres." }

        val profile = db.collection("users").document(myUid).get().await()
        val senderDisplayName = profile.getString("displayName")
            ?.takeIf { it.isNotBlank() }
            ?: profile.getString("username")
                ?.takeIf { it.isNotBlank() }
            ?: "Aventurero"

        val senderPhotoUrl = profile.getString("photoUrl").orEmpty()
        val now = System.currentTimeMillis()
        val msgRef = db.collection("guilds")
            .document(guildId)
            .collection("chat")
            .document()

        val message = GuildChatMessage(
            id = msgRef.id,
            senderUid = myUid,
            senderDisplayName = senderDisplayName,
            senderPhotoUrl = senderPhotoUrl,
            text = cleanText,
            type = "text",
            createdAt = now
        )

        msgRef.set(message).await()
    }
}
