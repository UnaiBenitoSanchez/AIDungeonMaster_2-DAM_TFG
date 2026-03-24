package com.example.aidungeonmaster.ui.achievements

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.aidungeonmaster.data.model.Achievement
import com.example.aidungeonmaster.data.model.Quest
import kotlinx.coroutines.delay

private val Gold = Color(0xFFFFD700)
private val DarkBrown = Color(0xFF2C1A0E)
private val GreenSuccess = Color(0xFF4CAF50)

/**
 * Toast flotante que aparece en la parte superior de la pantalla
 * cuando se desbloquea un logro.
 *
 * Uso en cualquier pantalla:
 *
 * ```kotlin
 * val achievementVM: AchievementViewModel = viewModel()
 * var toastAchievement by remember { mutableStateOf<Achievement?>(null) }
 *
 * LaunchedEffect(Unit) {
 *     achievementVM.newAchievement.collect { toastAchievement = it }
 * }
 *
 * Box(Modifier.fillMaxSize()) {
 *     // tu contenido...
 *     AchievementToast(achievement = toastAchievement) { toastAchievement = null }
 * }
 * ```
 */
@Composable
fun AchievementToast(
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(achievement) {
        if (achievement != null) {
            visible = true
            delay(3500)
            visible = false
            delay(400)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible  = visible,
        enter    = slideInVertically(tween(350)) { -it } + fadeIn(tween(350)),
        exit     = slideOutVertically(tween(350)) { -it } + fadeOut(tween(350)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .zIndex(100f)
    ) {
        if (achievement != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkBrown)
                    .border(1.5.dp, Gold, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(achievement.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "¡Logro Desbloqueado!",
                        color      = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp
                    )
                    Text(
                        achievement.title,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                    Text(achievement.description, color = Color.LightGray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+${achievement.xpReward}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("XP", color = Gold.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }
        }
    }
}

/**
 * Toast similar para misiones completadas.
 */
@Composable
fun QuestCompletedToast(
    quest: Quest?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(quest) {
        if (quest != null) {
            visible = true
            delay(4000)
            visible = false
            delay(400)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible  = visible,
        enter    = slideInVertically(tween(350)) { -it } + fadeIn(tween(350)),
        exit     = slideOutVertically(tween(350)) { -it } + fadeOut(tween(350)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .zIndex(100f)
    ) {
        if (quest != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A2E1A))
                    .border(1.5.dp, GreenSuccess, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(quest.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "¡Misión Completada!",
                        color      = GreenSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp
                    )
                    Text(
                        quest.title,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+${quest.xpReward}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("XP", color = Gold.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }
        }
    }
}