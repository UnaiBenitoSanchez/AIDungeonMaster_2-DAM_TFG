package com.example.aidungeonmaster.data.repository

import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.data.model.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class SocialRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid

    suspend fun searchUsers(query: String): List<AppUser> {
        val myUid = currentUid() ?: return emptyList()
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val usernameResults = db.collection("users")
            .orderBy("usernameLower")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .limit(20)
            .get()
            .await()
            .documents

        val displayResults = db.collection("users")
            .orderBy("displayNameLower")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .limit(20)
            .get()
            .await()
            .documents

        return (usernameResults + displayResults)
            .distinctBy { it.id }
            .mapNotNull { doc ->
                doc.toObject(AppUser::class.java)?.copy(uid = doc.id)
            }
            .filter { it.uid != myUid }
            .sortedBy { it.usernameLower }
    }

    suspend fun sendFriendRequest(targetUser: AppUser) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(targetUser.uid != myUid) { "No puedes enviarte una solicitud a ti mismo." }

        val myDoc = db.collection("users").document(myUid).get().await()
        val me = myDoc.toObject(AppUser::class.java)?.copy(uid = myDoc.id)
            ?: throw IllegalStateException("No se encontró tu perfil público.")

        val existingPendingSent = db.collection("friend_requests")
            .whereEqualTo("fromUid", myUid)
            .whereEqualTo("toUid", targetUser.uid)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        if (!existingPendingSent.isEmpty) {
            throw IllegalStateException("Ya has enviado una solicitud pendiente a este usuario.")
        }

        val existingPendingReceived = db.collection("friend_requests")
            .whereEqualTo("fromUid", targetUser.uid)
            .whereEqualTo("toUid", myUid)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        if (!existingPendingReceived.isEmpty) {
            throw IllegalStateException("Ese usuario ya te ha enviado una solicitud pendiente.")
        }

        val now = System.currentTimeMillis()

        val payload = FriendRequest(
            fromUid = myUid,
            toUid = targetUser.uid,
            fromDisplayName = me.displayName,
            fromUsername = me.username,
            toDisplayName = targetUser.displayName,
            toUsername = targetUser.username,
            status = "pending",
            createdAt = now,
            updatedAt = now
        )

        db.collection("friend_requests")
            .add(payload)
            .await()
    }

    fun listenIncomingRequests(
        onChange: (List<FriendRequest>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val myUid = currentUid() ?: return null

        return db.collection("friend_requests")
            .whereEqualTo("toUid", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error escuchando solicitudes")
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    }
                    .sortedByDescending { it.createdAt }

                onChange(requests)
            }
    }

    suspend fun acceptFriendRequest(request: FriendRequest) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(request.toUid == myUid) { "No puedes aceptar una solicitud que no es tuya." }

        val now = System.currentTimeMillis()

        db.runBatch { batch ->
            val requestRef = db.collection("friend_requests").document(request.id)
            batch.update(
                requestRef,
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to now
                )
            )

            val friendshipId = buildFriendshipId(request.fromUid, request.toUid)
            val friendshipRef = db.collection("friendships").document(friendshipId)
            batch.set(
                friendshipRef,
                mapOf(
                    "userA" to minOf(request.fromUid, request.toUid),
                    "userB" to maxOf(request.fromUid, request.toUid),
                    "createdAt" to now,
                    "createdBy" to myUid
                )
            )
        }.await()
    }

    suspend fun rejectFriendRequest(request: FriendRequest) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(request.toUid == myUid) { "No puedes rechazar una solicitud que no es tuya." }

        db.collection("friend_requests")
            .document(request.id)
            .update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    companion object {
        fun buildFriendshipId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }
}