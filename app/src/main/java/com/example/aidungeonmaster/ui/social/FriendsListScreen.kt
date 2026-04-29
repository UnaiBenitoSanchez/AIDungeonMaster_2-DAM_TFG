package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.FriendWithProfile
import com.example.aidungeonmaster.viewmodel.SocialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenChat: (String, String) -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()

    LaunchedEffect(Unit) { viewModel.startFriendsListener() }
    DisposableEffect(Unit) { onDispose { viewModel.stopFriendsListener() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis amigos") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        if (friends.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                Text("Todavía no tienes amigos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends, key = { it.uid }) { friend ->
                    FriendCard(
                        friend = friend,
                        onOpenProfile = { onOpenProfile(friend.uid) },
                        onOpenChat = { onOpenChat(friend.uid, friend.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendWithProfile,
    onOpenProfile: () -> Unit,
    onOpenChat: () -> Unit
) {
    val accent = friendsParseColor(friend.accentColor)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialUserAvatar(
                    photoUrl = friend.photoUrl,
                    displayName = friend.displayName,
                    size = 56.dp,
                    accent = accent
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("@${friend.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PresenceIndicator(
                        isOnline = friend.isOnline,
                        lastSeen = friend.lastSeen
                    )
                }

                Button(onClick = onOpenChat) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Chat")

                        if (friend.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFE53935),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            if (friend.bio.isNotBlank()) {
                Text(friend.bio, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0L) return "sin datos"
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun friendsParseColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFFD4AF37))
