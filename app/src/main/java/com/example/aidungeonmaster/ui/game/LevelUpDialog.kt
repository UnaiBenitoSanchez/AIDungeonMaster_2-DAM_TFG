package com.example.aidungeonmaster.ui.game

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val GoldAccent  = Color(0xFFFFD700)
private val LevelBg     = Color(0xFF0A0A0A)
private val LevelBorder = Color(0xFFFFD700)

@Composable
// Ejecuta la lógica de level up dialog.
fun LevelUpDialog(
    newLevel: Int,
    characterClass: String,
    onDismiss: () -> Unit
) {
    // Animación de pulso en la estrella
    val infiniteTransition = rememberInfiniteTransition(label = "lu_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue  = 1.1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "lu_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(LevelBg, RoundedCornerShape(16.dp))
                .border(2.dp, LevelBorder, RoundedCornerShape(16.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Estrella animada
                Text(
                    text = "⭐",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(scale)
                )

                // Título
                Text(
                    text = "¡NIVEL $newLevel!",
                    color = GoldAccent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )

                Text(
                    text = "Tu $characterClass ha crecido en poder",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif
                )

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = GoldAccent.copy(alpha = 0.3f))
                Spacer(Modifier.height(4.dp))

                // Bonificaciones obtenidas
                val bonuses = levelBonuses(newLevel, characterClass)
                bonuses.forEach { bonus ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✦", color = GoldAccent, fontSize = 12.sp)
                        Text(
                            text = bonus,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bonus de competencia si sube
                val newProfBonus = profBonusForLevel(newLevel)
                val oldProfBonus = profBonusForLevel(newLevel - 1)
                if (newProfBonus > oldProfBonus) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✦", color = GoldAccent, fontSize = 12.sp)
                        Text(
                            text = "Bonus de competencia: +$newProfBonus",
                            color = Color(0xFF88AAFF),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A2A00)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "¡Continuar la aventura!",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// Descripción de los beneficios según nivel
private fun levelBonuses(level: Int, charClass: String): List<String> {
    val base = mutableListOf<String>()
    base.add("HP máximo aumentado")
    when (level) {
        2  -> base.add("Nueva habilidad de clase disponible")
        3  -> base.add("Subclase especializada desbloqueada")
        4  -> base.add("Mejora de característica (+2 a un stat)")
        5  -> base.add("Ataques extra: ahora atacas dos veces")
        6  -> base.add("Rasgo de subclase mejorado")
        7  -> base.add("Habilidad de clase avanzada")
        8  -> base.add("Mejora de característica (+2 a un stat)")
        9  -> base.add("Tercer nivel de hechizos disponible")
        10 -> base.add("Característica de clase épica")
    }
    return base
}

// Ejecuta la lógica de prof bonus for level.
private fun profBonusForLevel(level: Int): Int = when {
    level >= 17 -> 6
    level >= 13 -> 5
    level >= 9  -> 4
    level >= 5  -> 3
    else        -> 2
}
