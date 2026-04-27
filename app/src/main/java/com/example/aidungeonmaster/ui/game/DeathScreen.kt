package com.example.aidungeonmaster.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Item

// ── Colores ───────────────────────────────────────────────────────────────────
private val DSRed        = Color(0xFFCC2200)
private val DSRedDark    = Color(0xFF880000)
private val DSGold       = Color(0xFFD4AF37)
private val DSGoldDark   = Color(0xFF8B7536)
private val DSBackground = Color(0xFF0A0005)
private val DSPanel      = Color(0xFF1A0A10)
private val DSPanelBorder= Color(0xFF4A1A2A)
private val DSTextPri    = Color(0xFFE8D5C0)
private val DSTextSec    = Color(0xFF9A8070)
private val RarCommon    = Color(0xFFB0B0B0)
private val RarUncommon  = Color(0xFF4CAF50)
private val RarRare      = Color(0xFF2196F3)
private val RarEpic      = Color(0xFF9C27B0)
private val RarLegendary = Color(0xFFFF9800)

@Composable
fun DeathSummaryScreen(
    character: Character?,
    xpGained: Int,
    coinsGained: Int,
    itemsFound: List<Item>,
    onGoHome: () -> Unit
) {
    // ── Animaciones ───────────────────────────────────────────────────────────
    val overlayAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = EaseIn),
        label = "overlay"
    )
    val skullScale by animateFloatAsState(
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skull"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title"
    )

    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(overlayAlpha)
            .background(
                Brush.verticalGradient(
                    colors = listOf(DSBackground, Color(0xFF150010), DSBackground)
                )
            )
    ) {
        // Viñeta roja
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, DSRedDark.copy(alpha = 0.35f)),
                        radius = 1400f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Cráneo ───────────────────────────────────────────────────────
            Text(
                text = "💀",
                fontSize = (70 * skullScale).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // ── Título ───────────────────────────────────────────────────────
            Text(
                text = "HAS MUERTO",
                color = DSRed.copy(alpha = 0.6f + 0.4f * titleAlpha),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            character?.let {
                Text(
                    text = "La historia de ${it.name} ha llegado a su fin",
                    color = DSTextSec,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
            DSDivider()
            Spacer(Modifier.height(20.dp))

            // ── Resumen del héroe ─────────────────────────────────────────────
            character?.let { char ->
                DSPanel(title = "⚔️  Resumen del Héroe") {
                    DSRow("Nombre",  char.name)
                    DSRow("Clase",   "${char.characterClass} · ${char.race}")
                    DSRow("Nivel",   "${char.level}")
                    DSRow("XP total","${char.xp} / ${char.xpToNextLevel}")
                    DSRow("Monedas acumuladas", "${char.coins}")
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Stats del combate/sesión ──────────────────────────────────────
            DSPanel(title = "📜  Estadísticas Finales") {
                DSRow("⚗️  XP ganada",        "$xpGained XP")
                DSRow("💰  Monedas obtenidas", "$coinsGained monedas")
                DSRow("🎒  Objetos encontrados","${itemsFound.size}")
            }

            // ── Objetos encontrados ───────────────────────────────────────────
            if (itemsFound.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                DSPanel(title = "🎒  Botín de la Aventura (${itemsFound.size})") {
                    itemsFound.forEach { item -> DSItemRow(item) }
                }
            }

            // ── Inventario final ──────────────────────────────────────────────
            character?.let { char ->
                if (char.inventory.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    DSPanel(title = "📦  Inventario Final (${char.inventory.size} objetos)") {
                        char.inventory.takeLast(8).forEach { item -> DSItemRow(item) }
                        if (char.inventory.size > 8) {
                            Text(
                                text = "… y ${char.inventory.size - 8} objetos más",
                                color = DSTextSec,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            DSDivider()
            Spacer(Modifier.height(20.dp))

            // ── Cita ──────────────────────────────────────────────────────────
            Text(
                text = "\"Que tu historia inspire a los bardos\ny tu leyenda perdure en el tiempo.\"",
                color = DSGold.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── Botón Volver al menú ──────────────────────────────────────────
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DSRed,
                    contentColor   = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(52.dp)
            ) {
                Text(
                    text = "⚰️  Volver al menú",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun DSPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = DSPanel, shape = RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(DSRedDark, DSPanelBorder, DSRedDark)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = DSGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun DSRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = DSTextSec, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = DSTextPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun DSItemRow(item: Item) {
    val rarityColor = when (item.rarity.lowercase()) {
        "uncommon"  -> RarUncommon
        "rare"      -> RarRare
        "epic"      -> RarEpic
        "legendary" -> RarLegendary
        else        -> RarCommon
    }
    val rarityEmoji = when (item.rarity.lowercase()) {
        "uncommon"  -> "🟢"
        "rare"      -> "🔵"
        "epic"      -> "🟣"
        "legendary" -> "🟠"
        else        -> "⚪"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = rarityEmoji, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp, end = 6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, color = rarityColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description.take(60) + if (item.description.length > 60) "…" else "",
                    color = DSTextSec,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 15.sp
                )
            }
        }
        Text(
            text = item.type.replaceFirstChar { it.uppercase() },
            color = DSTextSec,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun DSDivider() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(colors = listOf(Color.Transparent, DSRedDark))
            )
        )
        Text(text = "  ✦  ", color = DSGoldDark, fontSize = 14.sp)
        Box(
            modifier = Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(colors = listOf(DSRedDark, Color.Transparent))
            )
        )
    }
}