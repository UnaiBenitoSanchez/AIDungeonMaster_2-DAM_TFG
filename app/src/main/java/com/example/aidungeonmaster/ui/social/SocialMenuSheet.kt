package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aidungeonmaster.ui.tutorial.tutorialAnchor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMenuSheet(
    onDismiss: () -> Unit,
    onMyProfile: () -> Unit,
    onSearchUsers: () -> Unit,
    onFriendRequests: () -> Unit,
    onFriendsList: () -> Unit,
    onGuilds: () -> Unit,
    tutorialTargets: SnapshotStateMap<String, Rect>? = null,
    lockForTutorial: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!lockForTutorial) {
                onDismiss()
            }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Zona social",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SocialOptionItem(
                text = "Mi perfil",
                modifier = anchorOrPlain("social_my_profile", tutorialTargets),
                onClick = onMyProfile
            )

            SocialOptionItem(
                text = "Buscar usuarios",
                modifier = anchorOrPlain("social_search_users", tutorialTargets),
                onClick = onSearchUsers
            )

            SocialOptionItem(
                text = "Solicitudes de amistad",
                modifier = anchorOrPlain("social_friend_requests", tutorialTargets),
                onClick = onFriendRequests
            )

            SocialOptionItem(
                text = "Lista de amigos",
                modifier = anchorOrPlain("social_friends_list", tutorialTargets),
                onClick = onFriendsList
            )

            SocialOptionItem(
                text = "Gremios",
                modifier = anchorOrPlain("social_guilds", tutorialTargets),
                onClick = onGuilds
            )
        }
    }
}

@Composable
private fun SocialOptionItem(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        )
    }
}

private fun anchorOrPlain(
    key: String,
    tutorialTargets: SnapshotStateMap<String, Rect>?
): Modifier {
    return if (tutorialTargets != null) {
        Modifier.tutorialAnchor(key, tutorialTargets)
    } else {
        Modifier
    }
}