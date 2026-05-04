package com.example.aidungeonmaster.ui.achievements

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.aidungeonmaster.data.model.Achievement
import com.example.aidungeonmaster.data.model.Quest
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private val Gold = Color(0xFFFFD700)
private val SoftGold = Color(0xFFFFE9A6)
private val DarkBrown = Color(0xFF221108)
private val Brown = Color(0xFF3A1D0C)
private val RedGem = Color(0xFF8B1E16)
private val GreenSuccess = Color(0xFF4CAF50)
private val DarkGreen = Color(0xFF102814)

@Composable
// Ejecuta la lógica de achievement toast.
fun AchievementToast(
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var animationKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(achievement?.id, achievement?.unlockedAt) {
        if (achievement != null) {
            animationKey++
            visible = true
            delay(4300)
            visible = false
            delay(450)
            onDismiss()
        }
    }

    AchievementUnlockedOverlay(
        visible = visible,
        animationKey = animationKey,
        achievement = achievement
    )
}

@Composable
// Ejecuta la lógica de achievement unlocked overlay.
private fun AchievementUnlockedOverlay(
    visible: Boolean,
    animationKey: Int,
    achievement: Achievement?
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(
            durationMillis = 520,
            easing = EaseOutBack
        ),
        label = "achievementScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "achievementInfinite")

    val glow by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "achievementGlow"
    )

    val shimmer by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1850, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "achievementShimmer"
    )

    val trophyRotation by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "achievementTrophyRotation"
    )

    AnimatedVisibility(
        visible = visible && achievement != null,
        enter = slideInVertically(
            animationSpec = tween(420, easing = EaseOutCubic),
            initialOffsetY = { -it }
        ) + fadeIn(tween(300)) + scaleIn(
            animationSpec = tween(420, easing = EaseOutBack),
            initialScale = 0.88f
        ),
        exit = slideOutVertically(
            animationSpec = tween(320, easing = EaseOutCubic),
            targetOffsetY = { -it }
        ) + fadeOut(tween(240)) + scaleOut(
            animationSpec = tween(260),
            targetScale = 0.94f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .zIndex(1000f)
    ) {
        if (achievement != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                ConfettiLayer(
                    key = animationKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    shadowElevation = 14.dp,
                    border = BorderStroke(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Gold.copy(alpha = 0.85f),
                                SoftGold,
                                Gold.copy(alpha = 0.85f)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        DarkBrown,
                                        Brown,
                                        Color(0xFF180B05)
                                    )
                                )
                            )
                    ) {
                        ShineBand(
                            progress = shimmer,
                            modifier = Modifier.matchParentSize()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AchievementIcon(
                                emoji = achievement.emoji,
                                glow = glow,
                                rotation = trophyRotation
                            )

                            Spacer(Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "🏆 LOGRO DESBLOQUEADO",
                                    color = SoftGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                )

                                Text(
                                    text = achievement.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = achievement.description,
                                    color = Color(0xFFEADFC8),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            RewardPill(
                                xpReward = achievement.xpReward
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
// Ejecuta la lógica de achievement icon.
private fun AchievementIcon(
    emoji: String,
    glow: Float,
    rotation: Float
) {
    Box(
        modifier = Modifier.size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((48 + glow * 10).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Gold.copy(alpha = 0.36f + glow * 0.20f),
                            Gold.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color(0xFF4A260E),
            border = BorderStroke(1.4.dp, Gold.copy(alpha = 0.9f)),
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emoji,
                    fontSize = 30.sp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotation
                    }
                )
            }
        }
    }
}

@Composable
// Ejecuta la lógica de reward pill.
private fun RewardPill(
    xpReward: Int
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFFE082),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "+$xpReward",
                color = Color(0xFF2A1606),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Text(
                text = "XP",
                color = Color(0xFF5A3610),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
// Ejecuta la lógica de shine band.
private fun ShineBand(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bandWidth = size.width * 0.20f
        val x = size.width * progress

        rotate(degrees = -18f, pivot = Offset(x, size.height / 2f)) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(x - bandWidth / 2f, -size.height),
                size = Size(bandWidth, size.height * 3f)
            )
        }
    }
}

@Composable
// Ejecuta la lógica de confetti layer.
private fun ConfettiLayer(
    key: Int,
    modifier: Modifier = Modifier
) {
    val pieces = remember(key) {
        val random = Random(key + 91)
        List(22) {
            ConfettiPiece(
                x = random.nextFloat(),
                y = random.nextFloat() * 0.55f,
                size = 4f + random.nextFloat() * 5f,
                rotation = random.nextFloat() * 360f,
                color = listOf(
                    Gold,
                    SoftGold,
                    Color(0xFFFF7043),
                    Color(0xFF81C784),
                    Color(0xFF64B5F6)
                ).random(random)
            )
        }
    }

    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = keyframes {
            durationMillis = 1600
            0f at 0
            1f at 1600
        },
        label = "confettiProgress$key"
    )

    BoxWithConstraints(modifier = modifier.size(width = 1.dp, height = 130.dp)) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .size(height = 130.dp, width = 1.dp)
        ) {
            pieces.forEachIndexed { index, piece ->
                val fall = progress * (60f + index % 5 * 14f)
                val drift = kotlin.math.sin((progress * 4f + index) * 1.7f) * 18f
                val alpha = (1f - progress).coerceIn(0f, 1f)

                rotate(
                    degrees = piece.rotation + progress * 240f,
                    pivot = Offset(piece.x * widthPx + drift, piece.y * size.height + fall)
                ) {
                    drawRoundRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(
                            x = piece.x * widthPx + drift,
                            y = piece.y * size.height + fall
                        ),
                        size = Size(piece.size * 1.8f, piece.size),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

// Clase que encapsula la lógica de confetti piece.
private data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val size: Float,
    val rotation: Float,
    val color: Color
)

/**
 * Toast similar para misiones completadas, con estilo mejorado.
 */
@Composable
// Ejecuta la lógica de quest completed toast.
fun QuestCompletedToast(
    quest: Quest?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(quest?.id, quest?.completedAt) {
        if (quest != null) {
            visible = true
            delay(3900)
            visible = false
            delay(400)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && quest != null,
        enter = slideInVertically(
            animationSpec = tween(400, easing = EaseOutCubic),
            initialOffsetY = { -it }
        ) + fadeIn(tween(300)) + scaleIn(
            animationSpec = tween(400, easing = EaseOutBack),
            initialScale = 0.9f
        ),
        exit = slideOutVertically(
            animationSpec = tween(300, easing = EaseOutCubic),
            targetOffsetY = { -it }
        ) + fadeOut(tween(240)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .zIndex(1000f)
    ) {
        if (quest != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                shadowElevation = 12.dp,
                border = BorderStroke(1.8.dp, GreenSuccess.copy(alpha = 0.95f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    DarkGreen,
                                    Color(0xFF1C3D1D),
                                    Color(0xFF0B1D0D)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = Color(0xFF1E4A22),
                        border = BorderStroke(1.2.dp, GreenSuccess),
                        shadowElevation = 7.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = quest.emoji,
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✅ MISIÓN COMPLETADA",
                            color = Color(0xFFA5D6A7),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp
                        )

                        Text(
                            text = quest.title,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Recompensa conseguida",
                            color = Color(0xFFD5EAD7),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFC8E6C9),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "+${quest.xpReward}",
                                color = Color(0xFF0F2A10),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "XP",
                                color = Color(0xFF255B28),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
