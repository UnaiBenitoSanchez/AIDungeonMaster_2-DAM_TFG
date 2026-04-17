package com.example.aidungeonmaster.data.repository

import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GuildDetailsRepository {

    private val db = FirebaseFirestore.getInstance()

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
}