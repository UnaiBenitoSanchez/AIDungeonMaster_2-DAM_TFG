package com.example.aidungeonmaster.data.repository

import android.content.Context
import android.net.Uri
import com.example.aidungeonmaster.data.model.AppUser
import com.example.aidungeonmaster.data.model.FriendRequest
import com.example.aidungeonmaster.data.model.FriendWithProfile
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.data.model.GuildMemberSummary
import com.example.aidungeonmaster.data.model.GuildMembership
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class SocialRepository {

    companion object {
        private const val MAX_GUILD_MEMBERS = 15

        fun buildFriendshipId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val friendProfileListeners = mutableListOf<ListenerRegistration>()

    fun currentUid(): String? = auth.currentUser?.uid

    suspend fun searchUsers(query: String): List<AppUser> {
        val myUid = currentUid() ?: return emptyList()
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val usernameResults = db.collection("users")
            .orderBy("usernameLower")
            .startAt(q)
            .endAt(q + "")
            .limit(20)
            .get()
            .await()
            .documents

        val displayResults = db.collection("users")
            .orderBy("displayNameLower")
            .startAt(q)
            .endAt(q + "")
            .limit(20)
            .get()
            .await()
            .documents

        return (usernameResults + displayResults)
            .distinctBy { it.id }
            .mapNotNull { doc -> doc.toAppUser() }
            .filter { it.uid != myUid }
            .sortedBy { it.usernameLower }
    }

    suspend fun sendFriendRequest(targetUser: AppUser) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(targetUser.uid != myUid) { "No puedes enviarte una solicitud a ti mismo." }

        val myDoc = db.collection("users").document(myUid).get().await()
        val me = myDoc.toAppUser()
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

        db.collection("friend_requests").add(payload).await()
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
                    .mapNotNull { doc -> doc.toObject(FriendRequest::class.java)?.copy(id = doc.id) }
                    .sortedByDescending { it.createdAt }

                onChange(requests)
            }
    }

    suspend fun acceptFriendRequest(request: FriendRequest) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(request.toUid == myUid) { "Solo el receptor puede aceptar la solicitud." }

        val friendshipId = buildFriendshipId(request.fromUid, request.toUid)
        val now = System.currentTimeMillis()

        val friendship = mapOf(
            "userA" to minOf(request.fromUid, request.toUid),
            "userB" to maxOf(request.fromUid, request.toUid),
            "members" to listOf(request.fromUid, request.toUid),
            "createdAt" to now,
            "createdBy" to myUid
        )

        val requesterProfile = db.collection("users").document(request.fromUid).get().await().toAppUser()
            ?: throw IllegalStateException("No se encontró el perfil del remitente.")

        val myProfile = db.collection("users").document(myUid).get().await().toAppUser()
            ?: throw IllegalStateException("No se encontró tu perfil.")

        val myFriendMirror = mapOf(
            "uid" to requesterProfile.uid,
            "displayName" to requesterProfile.displayName,
            "username" to requesterProfile.username,
            "photoUrl" to requesterProfile.photoUrl,
            "friendshipId" to friendshipId,
            "createdAt" to now
        )

        val otherFriendMirror = mapOf(
            "uid" to myProfile.uid,
            "displayName" to myProfile.displayName,
            "username" to myProfile.username,
            "photoUrl" to myProfile.photoUrl,
            "friendshipId" to friendshipId,
            "createdAt" to now
        )

        val chatId = friendshipId
        val chat = mapOf(
            "members" to listOf(request.fromUid, request.toUid),
            "friendshipId" to friendshipId,
            "createdAt" to now,
            "lastMessage" to "",
            "lastMessageAt" to now,
            "lastSenderUid" to myUid
        )

        db.runBatch { batch ->
            batch.update(
                db.collection("friend_requests").document(request.id),
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to now
                )
            )

            batch.set(db.collection("friendships").document(friendshipId), friendship)

            batch.set(
                db.collection("users").document(myUid)
                    .collection("friends").document(request.fromUid),
                myFriendMirror
            )

            batch.set(
                db.collection("users").document(request.fromUid)
                    .collection("friends").document(myUid),
                otherFriendMirror
            )

            batch.set(db.collection("private_chats").document(chatId), chat)
        }.await()
    }

    suspend fun rejectFriendRequest(request: FriendRequest) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        require(request.toUid == myUid) { "Solo el receptor puede rechazar la solicitud." }

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

    fun listenFriends(
        onChange: (List<FriendWithProfile>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val myUid = currentUid() ?: return null

        return db.collection("users")
            .document(myUid)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error escuchando amigos")
                    return@addSnapshotListener
                }

                clearFriendProfileListeners()

                val docs = snapshot?.documents.orEmpty()
                if (docs.isEmpty()) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }

                val liveFriends = linkedMapOf<String, FriendWithProfile>()

                docs.forEach { friendDoc ->
                    val friendUid = friendDoc.getString("uid").orEmpty()
                    if (friendUid.isBlank()) return@forEach

                    val friendshipId = friendDoc.getString("friendshipId").orEmpty()

                    val reg = db.collection("users")
                        .document(friendUid)
                        .addSnapshotListener { userSnapshot, userError ->
                            if (userError != null) {
                                onError(userError.message ?: "Error escuchando perfil del amigo")
                                return@addSnapshotListener
                            }

                            val user = userSnapshot?.toAppUser()
                            if (user == null) {
                                liveFriends.remove(friendUid)
                            } else {
                                liveFriends[friendUid] = FriendWithProfile(
                                    uid = user.uid,
                                    displayName = user.displayName,
                                    username = user.username,
                                    photoUrl = user.photoUrl,
                                    friendshipId = friendshipId,
                                    bio = user.bio,
                                    accentColor = user.accentColor,
                                    profileBackgroundColor = user.profileBackgroundColor,
                                    isOnline = user.isOnline,
                                    lastSeen = user.lastSeen
                                )
                            }

                            onChange(liveFriends.values.sortedBy { it.username.lowercase() })
                        }

                    friendProfileListeners += reg
                }
            }
    }

    suspend fun getUserProfile(userUid: String): AppUser {
        val snap = db.collection("users").document(userUid).get().await()
        return snap.toAppUser()
            ?: throw IllegalStateException("No se pudo cargar el perfil.")
    }

    suspend fun updateMyProfile(
        displayName: String,
        bio: String,
        accentColor: String,
        profileBackgroundColor: String
    ) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val now = System.currentTimeMillis()

        db.collection("users")
            .document(myUid)
            .update(
                mapOf(
                    "displayName" to displayName.trim(),
                    "displayNameLower" to displayName.trim().lowercase(),
                    "bio" to bio.trim(),
                    "accentColor" to accentColor,
                    "profileBackgroundColor" to profileBackgroundColor,
                    "profileAccentColor" to accentColor,
                    "profilePrimaryColor" to profileBackgroundColor,
                    "profileSecondaryColor" to profileBackgroundColor,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to now,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun updateMyProfilePhoto(context: Context, uri: Uri) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer la imagen seleccionada")

        val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("No se pudo decodificar la imagen")

        val maxSize = 256
        val ratio = minOf(
            maxSize.toFloat() / originalBitmap.width.toFloat(),
            maxSize.toFloat() / originalBitmap.height.toFloat(),
            1f
        )

        val targetWidth = (originalBitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (originalBitmap.height * ratio).toInt().coerceAtLeast(1)

        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
            originalBitmap,
            targetWidth,
            targetHeight,
            true
        )

        var quality = 75
        var finalBytes: ByteArray

        do {
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            finalBytes = outputStream.toByteArray()
            quality -= 10
        } while (finalBytes.size > 350_000 && quality >= 25)

        val base64 = android.util.Base64.encodeToString(finalBytes, android.util.Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64"

        db.collection("users")
            .document(myUid)
            .update(
                mapOf(
                    "photoUrl" to dataUrl,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    suspend fun createGuild(name: String, description: String, accentColor: String, bannerColor: String) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")
        val me = getUserProfile(myUid)

        if (me.currentGuildId.isNotBlank()) {
            throw IllegalStateException("Ya perteneces a un gremio. Debes abandonarlo antes de crear otro.")
        }

        val guildName = name.trim()
        require(guildName.length >= 3) { "El nombre del gremio debe tener al menos 3 caracteres." }

        val now = System.currentTimeMillis()
        val guildRef = db.collection("guilds").document()

        val guild = Guild(
            id = guildRef.id,
            name = guildName,
            nameLower = guildName.lowercase(),
            description = description.trim(),
            ownerUid = myUid,
            ownerDisplayName = me.displayName,
            accentColor = accentColor,
            bannerColor = bannerColor,
            memberCount = 1,
            createdAt = now,
            updatedAt = now,
            joined = true
        )

        val membership = GuildMembership(
            guildId = guildRef.id,
            uid = myUid,
            role = "owner",
            joinedAt = now,
            displayName = me.displayName,
            username = me.username
        )

        db.runBatch { batch ->
            batch.set(guildRef, guild)
            batch.set(guildRef.collection("members").document(myUid), membership)
            batch.set(
                db.collection("users").document(myUid).collection("guilds").document(guildRef.id),
                mapOf(
                    "guildId" to guildRef.id,
                    "name" to guildName,
                    "accentColor" to accentColor,
                    "bannerColor" to bannerColor,
                    "joinedAt" to now,
                    "role" to "owner"
                )
            )
            batch.update(
                db.collection("users").document(myUid),
                mapOf(
                    "currentGuildId" to guildRef.id,
                    "updatedAt" to now
                )
            )
        }.await()
    }

    suspend fun searchGuilds(query: String): List<Guild> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val myUid = currentUid().orEmpty()

        val memberGuildIds = db.collection("users")
            .document(myUid)
            .collection("guilds")
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        return db.collection("guilds")
            .orderBy("nameLower")
            .startAt(q)
            .endAt(q + "")
            .limit(20)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Guild::class.java)?.copy(
                    id = doc.id,
                    joined = memberGuildIds.contains(doc.id)
                )
            }
            .sortedBy { it.nameLower }
    }

    suspend fun getGuildById(guildId: String): Guild? {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val guildSnap = db.collection("guilds").document(guildId).get().await()
        if (!guildSnap.exists()) return null

        val guild = guildSnap.toObject(Guild::class.java)?.copy(id = guildSnap.id) ?: return null

        val joined = if (currentUid.isBlank()) {
            false
        } else {
            db.collection("users")
                .document(currentUid)
                .collection("guilds")
                .document(guildId)
                .get()
                .await()
                .exists()
        }

        return guild.copy(joined = joined)
    }

    suspend fun joinGuild(guild: Guild) {
        val myUid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val me = getUserProfile(myUid)

        if (me.currentGuildId.isNotBlank()) {
            throw IllegalStateException("Solo puedes pertenecer a un gremio a la vez.")
        }

        val now = System.currentTimeMillis()
        val guildRef = db.collection("guilds").document(guild.id)
        val memberRef = guildRef.collection("members").document(myUid)
        val userGuildRef = db.collection("users").document(myUid).collection("guilds").document(guild.id)
        val userRef = db.collection("users").document(myUid)

        db.runTransaction { tx ->
            val guildSnap = tx.get(guildRef)
            if (!guildSnap.exists()) throw IllegalStateException("El gremio ya no existe.")

            val memberSnap = tx.get(memberRef)
            val userGuildSnap = tx.get(userGuildRef)
            val userSnap = tx.get(userRef)

            if ((userSnap.getString("currentGuildId") ?: "").isNotBlank()) {
                throw IllegalStateException("Solo puedes pertenecer a un gremio a la vez.")
            }
            if (memberSnap.exists() || userGuildSnap.exists()) {
                throw IllegalStateException("Ya formas parte de este gremio.")
            }

            val currentCount = (guildSnap.getLong("memberCount") ?: 0L).toInt()

            if (currentCount >= MAX_GUILD_MEMBERS) {
                throw IllegalStateException("Este gremio ya está completo (15 miembros).")
            }

            // BUG FIX: El orden de operaciones en la transacción es crítico para las reglas de Firestore.
            // La regla validGuildMemberCountUpdate usa getAfter() para verificar que el miembro
            // y el userGuild se crean en la misma transacción. Por tanto el orden correcto es:
            // 1) Actualizar el gremio (memberCount)
            // 2) Crear memberRef
            // 3) Crear userGuildRef
            // 4) Actualizar userRef (currentGuildId)
            tx.update(guildRef, mapOf(
                "memberCount" to currentCount + 1,
                "updatedAt" to now
            ))

            tx.set(memberRef, mapOf(
                "guildId" to guild.id,
                "uid" to myUid,
                "role" to "member",
                "joinedAt" to now,
                "displayName" to me.displayName,
                "username" to me.username
            ))

            tx.set(userGuildRef, mapOf(
                "guildId" to guild.id,
                "name" to guild.name,
                "accentColor" to guild.accentColor,
                "bannerColor" to guild.bannerColor,
                "joinedAt" to now,
                "role" to "member"
            ))

            tx.update(userRef, mapOf(
                "currentGuildId" to guild.id,
                "updatedAt" to now
            ))
        }.await()
    }

    suspend fun leaveGuild(guild: Guild) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")

        if (guild.ownerUid == myUid) {
            throw IllegalStateException("El líder no puede abandonar su propio gremio desde aquí.")
        }

        val guildRef = db.collection("guilds").document(guild.id)
        val memberRef = guildRef.collection("members").document(myUid)
        val userGuildRef = db.collection("users").document(myUid).collection("guilds").document(guild.id)
        val userRef = db.collection("users").document(myUid)

        db.runTransaction { tx ->
            val guildSnap = tx.get(guildRef)
            val memberSnap = tx.get(memberRef)
            val userGuildSnap = tx.get(userGuildRef)

            if (!guildSnap.exists()) throw IllegalStateException("El gremio ya no existe.")
            if (!memberSnap.exists() || !userGuildSnap.exists()) throw IllegalStateException("No formas parte de este gremio.")

            val now = System.currentTimeMillis()
            val currentCount = (guildSnap.getLong("memberCount") ?: 1L).toInt()

            // ✅ Primero actualizar el gremio (mientras memberRef aún "existe")
            tx.update(guildRef, mapOf(
                "memberCount" to maxOf(0, currentCount - 1),
                "updatedAt" to now
            ))

            // ✅ Luego actualizar el usuario
            tx.update(userRef, mapOf(
                "currentGuildId" to "",
                "updatedAt" to now
            ))

            // ✅ Los deletes al final
            tx.delete(memberRef)
            tx.delete(userGuildRef)
        }.await()
    }

    suspend fun transferGuildLeadership(guild: Guild, newLeaderUid: String) {
        val myUid = currentUid() ?: throw IllegalStateException("Usuario no autenticado")

        if (guild.ownerUid != myUid) {
            throw IllegalStateException("Solo el líder actual puede transferir el liderazgo.")
        }

        if (newLeaderUid == myUid) {
            throw IllegalStateException("Ya eres el líder de este gremio.")
        }

        val guildRef = db.collection("guilds").document(guild.id)
        val oldOwnerMemberRef = guildRef.collection("members").document(myUid)
        val newOwnerMemberRef = guildRef.collection("members").document(newLeaderUid)

        val oldOwnerUserGuildRef = db.collection("users")
            .document(myUid)
            .collection("guilds")
            .document(guild.id)

        val newOwnerUserGuildRef = db.collection("users")
            .document(newLeaderUid)
            .collection("guilds")
            .document(guild.id)

        val newOwnerProfile = db.collection("users").document(newLeaderUid).get().await()
        val newOwnerDisplayName = newOwnerProfile.getString("displayName").orEmpty()

        if (newOwnerDisplayName.isBlank()) {
            throw IllegalStateException("No se pudo obtener el perfil del nuevo líder.")
        }

        val newOwnerMemberSnap = newOwnerMemberRef.get().await()
        if (!newOwnerMemberSnap.exists()) {
            throw IllegalStateException("El usuario seleccionado no pertenece al gremio.")
        }

        val now = System.currentTimeMillis()

        db.runBatch { batch ->
            batch.update(
                guildRef,
                mapOf(
                    "ownerUid" to newLeaderUid,
                    "ownerDisplayName" to newOwnerDisplayName,
                    "updatedAt" to now
                )
            )

            batch.update(newOwnerMemberRef, "role", "owner")
            batch.update(oldOwnerMemberRef, "role", "member")

            batch.update(newOwnerUserGuildRef, "role", "owner")
            batch.update(oldOwnerUserGuildRef, "role", "member")
        }.await()
    }

    fun listenMyGuilds(
        onChange: (List<Guild>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val myUid = currentUid() ?: return null

        val guildDocListeners = mutableListOf<ListenerRegistration>()
        val liveGuilds = linkedMapOf<String, Guild>()

        return db.collection("users")
            .document(myUid)
            .collection("guilds")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error escuchando gremios")
                    return@addSnapshotListener
                }

                guildDocListeners.forEach { it.remove() }
                guildDocListeners.clear()
                liveGuilds.clear()

                val docs = snapshot?.documents.orEmpty()
                if (docs.isEmpty()) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }

                docs.forEach { userGuildDoc ->
                    val guildId = userGuildDoc.getString("guildId").orEmpty()
                    if (guildId.isBlank()) return@forEach

                    val reg = db.collection("guilds")
                        .document(guildId)
                        .addSnapshotListener { guildSnapshot, guildError ->
                            if (guildError != null) {
                                onError(guildError.message ?: "Error escuchando datos del gremio")
                                return@addSnapshotListener
                            }

                            if (guildSnapshot == null || !guildSnapshot.exists()) {
                                liveGuilds.remove(guildId)
                            } else {
                                liveGuilds[guildId] = Guild(
                                    id = guildSnapshot.id,
                                    name = guildSnapshot.getString("name").orEmpty(),
                                    nameLower = guildSnapshot.getString("nameLower").orEmpty(),
                                    description = guildSnapshot.getString("description").orEmpty(),
                                    ownerUid = guildSnapshot.getString("ownerUid").orEmpty(),
                                    ownerDisplayName = guildSnapshot.getString("ownerDisplayName").orEmpty(),
                                    accentColor = guildSnapshot.getString("accentColor") ?: "#8E24AA",
                                    bannerColor = guildSnapshot.getString("bannerColor") ?: "#1F1235",
                                    memberCount = (guildSnapshot.getLong("memberCount") ?: 0L).toInt(),
                                    createdAt = guildSnapshot.getLong("createdAt") ?: 0L,
                                    updatedAt = guildSnapshot.getLong("updatedAt") ?: 0L,
                                    joined = true
                                )
                            }

                            onChange(liveGuilds.values.sortedBy { it.name.lowercase() })
                        }

                    guildDocListeners += reg
                }
            }
    }

    private fun clearFriendProfileListeners() {
        friendProfileListeners.forEach { it.remove() }
        friendProfileListeners.clear()
    }

    private fun DocumentSnapshot.toAppUser(): AppUser? {
        if (!exists()) return null

        val accent = getString("profileAccentColor")
            ?.takeIf { it.isNotBlank() }
            ?: getString("accentColor")
            ?: "#D4AF37"

        val background = getString("profilePrimaryColor")
            ?.takeIf { it.isNotBlank() }
            ?: getString("profileBackgroundColor")
            ?: "#1E1E1E"

        return AppUser(
            uid = id,
            email = getString("email").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            displayNameLower = getString("displayNameLower").orEmpty(),
            username = getString("username").orEmpty(),
            usernameLower = getString("usernameLower").orEmpty(),
            photoUrl = getString("photoUrl").orEmpty(),
            bio = getString("bio").orEmpty(),
            accentColor = accent,
            profileBackgroundColor = background,
            isOnline = getBoolean("isOnline") ?: false,
            lastSeen = getLong("lastSeen") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L,
            characterCount = (getLong("characterCount") ?: 0L).toInt(),
            currentGuildId = getString("currentGuildId").orEmpty()
        )
    }

}