package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMenuSheet(
    onDismiss: () -> Unit,
    onMyProfile: () -> Unit,
    onSearchUsers: () -> Unit,
    onFriendRequests: () -> Unit,
    onFriendsList: () -> Unit,
    onGuilds: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "Social",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SocialActionCard(
                title = "Mi perfil",
                subtitle = "Personaliza colores, biografía y presencia",
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = onMyProfile
            )

            SocialActionCard(
                title = "Buscar aventureros",
                subtitle = "Busca usuarios y envía solicitudes",
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                onClick = onSearchUsers
            )

            SocialActionCard(
                title = "Solicitudes de amistad",
                subtitle = "Acepta o rechaza solicitudes recibidas",
                icon = { Icon(Icons.Default.Mail, contentDescription = null) },
                onClick = onFriendRequests
            )

            SocialActionCard(
                title = "Mis amigos",
                subtitle = "Abre perfiles y chatea por separado",
                icon = { Icon(Icons.Default.People, contentDescription = null) },
                onClick = onFriendsList
            )

            SocialActionCard(
                title = "Gremios",
                subtitle = "Crea un gremio o únete a uno existente",
                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                onClick = onGuilds
            )
        }
    }
}

@Composable
private fun SocialActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() }
    ) {
        ListItem(
            leadingContent = icon,
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) }
        )
    }
}
