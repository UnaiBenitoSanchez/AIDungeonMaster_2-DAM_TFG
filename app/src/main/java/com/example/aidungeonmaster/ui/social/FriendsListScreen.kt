package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aidungeonmaster.data.model.FriendWithProfile
import com.example.aidungeonmaster.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    viewModel: SocialViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startFriendsListener()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopFriendsListener()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis amigos") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        if (friends.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "Todavía no tienes amigos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends, key = { it.uid }) { friend ->
                    FriendCard(
                        friend = friend,
                        onClick = { onOpenChat(friend.uid, friend.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendWithProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
            Text("@${friend.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}