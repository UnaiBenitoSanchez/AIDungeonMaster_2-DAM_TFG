package com.example.aidungeonmaster.ui.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.aidungeonmaster.utils.ImageUtils

@Composable
fun SocialUserAvatar(
    photoUrl: String,
    displayName: String,
    size: Dp,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val profileBitmap = remember(photoUrl) {
        runCatching {
            if (photoUrl.startsWith("data:image")) {
                val base64Part = photoUrl.substringAfter("base64,", "")
                if (base64Part.isNotBlank()) ImageUtils.base64ToBitmap(base64Part) else null
            } else {
                null
            }
        }.getOrNull()
    }

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f))
    ) {
        when {
            profileBitmap != null -> {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            photoUrl.isNotBlank() -> {
                SubcomposeAsyncImage(
                    model = photoUrl,
                    contentDescription = displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        AvatarFallback(initial = initial, accent = accent)
                    }
                )
            }

            else -> {
                AvatarFallback(initial = initial, accent = accent)
            }
        }
    }
}

@Composable
private fun AvatarFallback(
    initial: String,
    accent: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}
