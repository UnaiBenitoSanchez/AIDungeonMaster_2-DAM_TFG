package com.example.aidungeonmaster.data.model

data class FriendWithProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val friendshipId: String = "",
    val bio: String = "",
    val accentColor: String = "#D4AF37",
    val profileBackgroundColor: String = "#1E1E1E",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
