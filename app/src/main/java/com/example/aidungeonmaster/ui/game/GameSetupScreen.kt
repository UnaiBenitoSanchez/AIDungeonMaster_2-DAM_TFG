package com.example.aidungeonmaster.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.aidungeonmaster.navigation.Screen

@Composable
fun GameSetupScreen(
    navController: NavHostController,
    userId: String,
    characterName: String
) {
    val themes = listOf("Fantasía Épica", "Terror Gótico", "Cyberpunk", "Misterio")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(24.dp)
    ) {
        Text(
            text = "Preparando aventura para $characterName",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("Selecciona la temática de tu partida:", color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        themes.forEach { theme ->
            ThemeButtonCustom(
                theme = theme,
                onClick = {
                    navController.navigate(Screen.GamePlay.createRoute(userId, characterName, theme))
                }
            )
        }
    }
}

@Composable
fun ThemeButtonCustom(theme: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        when (theme) {
            "Cyberpunk" -> CyberpunkLayout(theme)
            "Terror Gótico" -> GothicLayout(theme)
            "Fantasía Épica" -> EpicLayout(theme)
            "Misterio" -> MysteryLayout(theme)
        }
    }
}

@Composable
fun CyberpunkLayout(title: String) {
    // Animación de parpadeo (flicker)
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 500
                1f at 0
                0.2f at 100
                1f at 150
                0.5f at 300
                1f at 500
            },
            repeatMode = RepeatMode.Reverse
        ), label = "flicker"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val neonColor = Color(0xFF00FBFF).copy(alpha = alpha)
            val glowColor = Color(0xFFBC00FF).copy(alpha = alpha * 0.5f)

            // Reflejo neón
            drawRect(Brush.verticalGradient(listOf(glowColor, Color.Transparent)))

            // Líneas de neón con "brillo"
            drawLine(neonColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 6f)
            drawLine(Color(0xFFBC00FF).copy(alpha = alpha), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 6f)
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF00FBFF).copy(alpha = alpha),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
        )
    }
}

@Composable
fun GothicLayout(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0000))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Dibujar enredaderas en las esquinas
            val vineColor = Color(0xFF2E3B23)
            val path = Path().apply {
                moveTo(0f, 0f)
                quadraticBezierTo(50f, 100f, 150f, 20f)
                moveTo(size.width, size.height)
                quadraticBezierTo(size.width - 50f, size.height - 100f, size.width - 150f, size.height - 20f)
            }
            drawPath(path, vineColor, style = Stroke(width = 4f))

            // Murciélagos
            drawbat(50f, 40f)
            drawbat(size.width - 100f, 70f)
        }
        // Degradado de sangre
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF4A0000).copy(alpha = 0.5f)))))

        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = androidx.compose.ui.text.TextStyle(
                color = Color(0xFFD1D1D1),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// Función auxiliar para dibujar un murciélago simple
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawbat(x: Float, y: Float) {
    val batPath = Path().apply {
        moveTo(x, y)
        lineTo(x - 20f, y - 10f)
        lineTo(x - 10f, y + 5f)
        lineTo(x, y)
        lineTo(x + 10f, y + 5f)
        lineTo(x + 20f, y - 10f)
        close()
    }
    drawPath(batPath, Color.Black)
}

@Composable
fun EpicLayout(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(
        Brush.linearGradient(listOf(Color(0xFF53350A), Color(0xFFB8860B)))
    )) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Escamas de dragón
            val scaleColor = Color.Black.copy(alpha = 0.15f)
            for (x in 0..size.width.toInt() step 40) {
                for (y in 0..size.height.toInt() step 30) {
                    drawCircle(scaleColor, radius = 35f, center = Offset(x.toFloat(), y.toFloat()), style = Stroke(2f))
                }
            }
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, shadow = Shadow(Color.Black, Offset(4f, 4f), 8f))
        )
    }
}

@Composable
fun MysteryLayout(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Interrogaciones de fondo aleatorias
            val paint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.WHITE
                alpha = 20
                textSize = 80f
            }
            drawContext.canvas.nativeCanvas.drawText("?", 100f, 150f, paint)
            drawContext.canvas.nativeCanvas.drawText("?", size.width - 200f, 80f, paint)
            drawContext.canvas.nativeCanvas.drawText("?", size.width / 2, size.height - 20f, paint)

            // Niebla central
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF303F9F).copy(alpha = 0.2f), Color.Transparent),
                    center = center,
                    radius = size.width / 2
                )
            )
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = Color(0xFFE0E0E0),
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 8.sp)
        )
    }
}