package com.example.aidungeonmaster.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// SISTEMA DE REPUTACIÓN
// ══════════════════════════════════════════════════════════════════════════════

enum class ReputationTier(
    val label: String,
    val emoji: String,
    val minPoints: Int,
    val discountPct: Int,     // % de descuento aplicado al precio base
    val color: Color
) {
    DESCONOCIDO ("Desconocido",  "😶", 0,   0,  Color(0xFF808080)),
    CONOCIDO    ("Conocido",     "🙂", 25,  5,  Color(0xFF4CAF50)),
    APRECIADO   ("Apreciado",    "😊", 60,  10, Color(0xFF2196F3)),
    HONRADO     ("Honrado",      "😎", 120, 20, Color(0xFF9C27B0)),
    VENERADO    ("Venerado",     "🤩", 200, 30, Color(0xFFFFD700));

    companion object {
        fun fromPoints(points: Int) = entries.lastOrNull { points >= it.minPoints }
            ?: DESCONOCIDO
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CATÁLOGO POR CADENA
// ══════════════════════════════════════════════════════════════════════════════

data class ShopItem(
    val item: Item,
    val basePrice: Int,
    val emoji: String,
    val minTier: ReputationTier = ReputationTier.DESCONOCIDO  // tier mínimo para comprar
)

// Artículos básicos presentes en todas las tiendas
private val UNIVERSAL_CATALOG = listOf(
    ShopItem(
        item  = Item("shop_vendas", "Vendas de Campaña",
            "Vendas de lino con hierbas medicinales.", "pocion", "cura:3"),
        basePrice = 5, emoji = "🩹"
    ),
    ShopItem(
        item  = Item("shop_pan", "Pan Élfico",
            "Nutritivo pan que sana lentamente las heridas.", "consumible", "cura:2"),
        basePrice = 8, emoji = "🍞"
    ),
    ShopItem(
        item  = Item("shop_pocion_m", "Poción de Curación Menor",
            "Frasco de líquido rojizo que restaura la vitalidad.", "pocion", "cura:1d4+1"),
        basePrice = 12, emoji = "🧪"
    ),
    ShopItem(
        item  = Item("shop_pocion", "Poción de Curación",
            "Poción estándar de los aventureros.", "pocion", "cura:2d4+2"),
        basePrice = 25, emoji = "⚗️"
    )
)

// Catálogos especializados por cadena (se añaden a los universales)
private val SPECIALTY_CATALOGS: Map<String, List<ShopItem>> = mapOf(

    "MERCADONA" to listOf(
        ShopItem(
            item  = Item("shop_merca_antidoto", "Antídoto Casero",
                "Receta familiar de Mercadona. Cura venenos menores.", "pocion", "cura:1d4"),
            basePrice = 10, emoji = "💊"
        ),
        ShopItem(
            item  = Item("shop_merca_max", "Poción de Curación Mayor",
                "La más potente de la sección de salud de Mercadona.", "pocion", "cura:3d4+5"),
            basePrice = 45, emoji = "🔮", minTier = ReputationTier.APRECIADO
        ),
        ShopItem(
            item  = Item("shop_merca_regen", "Elixir de Regeneración",
                "Exclusivo Mercadona. Regenera 2 PV por turno durante 3 turnos.", "pocion", "cura:6"),
            basePrice = 60, emoji = "✨", minTier = ReputationTier.VENERADO
        )
    ),

    "LIDL" to listOf(
        ShopItem(
            item  = Item("shop_lidl_misterio", "Caja Misteriosa Lidl",
                "Nadie sabe qué hay dentro. Puede ser genial o terrible.", "consumible", "cura:2d6"),
            basePrice = 7, emoji = "🎲"
        ),
        ShopItem(
            item  = Item("shop_lidl_oferta", "Poción de Oferta Semanal",
                "Cada semana cambia. Hoy cura muchísimo.", "pocion", "cura:3d4"),
            basePrice = 15, emoji = "🏷️", minTier = ReputationTier.CONOCIDO
        ),
        ShopItem(
            item  = Item("shop_lidl_premio", "Premio del Club Lidl Plus",
                "Solo para clientes VIP. Cura y concede ventaja en el siguiente ataque.", "reliquia", "cura:2d4+3"),
            basePrice = 35, emoji = "🎁", minTier = ReputationTier.HONRADO
        )
    ),

    "CARREFOUR" to listOf(
        ShopItem(
            item  = Item("shop_carr_daga", "Daga de Campaña",
                "Sección de ferretería de Carrefour. Daño 1d4.", "arma", "daño:1d4"),
            basePrice = 20, emoji = "🗡️"
        ),
        ShopItem(
            item  = Item("shop_carr_escudo", "Escudo Ligero",
                "Sección deportes. +1 CA.", "armadura", "+1 CA"),
            basePrice = 30, emoji = "🛡️", minTier = ReputationTier.CONOCIDO
        ),
        ShopItem(
            item  = Item("shop_carr_espadón", "Espada Larga de Carrefour",
                "Calidad premium. Daño 1d8.", "arma", "daño:1d8"),
            basePrice = 60, emoji = "⚔️", minTier = ReputationTier.HONRADO
        ),
        ShopItem(
            item  = Item("shop_carr_pocion", "Poción de Curación Carrefour",
                "Marca blanca. Funciona igual de bien.", "pocion", "cura:2d4"),
            basePrice = 18, emoji = "🧪"
        )
    ),

    "ALDI" to listOf(
        ShopItem(
            item  = Item("shop_aldi_racion", "Ración de Emergencia",
                "Compacta y barata. Para cuando no hay nada más.", "consumible", "cura:4"),
            basePrice = 4, emoji = "🎒"
        ),
        ShopItem(
            item  = Item("shop_aldi_kit", "Kit de Primeros Auxilios Aldi",
                "Todo lo necesario. Sin lujos, pero funciona.", "pocion", "cura:1d6+2"),
            basePrice = 14, emoji = "🩺", minTier = ReputationTier.CONOCIDO
        )
    ),

    "DIA" to listOf(
        ShopItem(
            item  = Item("shop_dia_2x1", "Poción 2×1 de Dia",
                "¡Dos al precio de una! Dos dosis de curación.", "pocion", "cura:1d4+1"),
            basePrice = 8, emoji = "🏷️"
        ),
        ShopItem(
            item  = Item("shop_dia_fiel", "Recompensa Cliente Fiel",
                "Por acumular puntos en Dia. Solo para los mejores clientes.", "pocion", "cura:2d4+2"),
            basePrice = 5, emoji = "⭐", minTier = ReputationTier.APRECIADO
        )
    ),

    "EROSKI" to listOf(
        ShopItem(
            item  = Item("shop_eros_pergamino", "Pergamino de Curación",
                "Sección librería de Eroski. Lanzar para curar 2d6.", "pergamino", "cura:2d6"),
            basePrice = 22, emoji = "📜"
        ),
        ShopItem(
            item  = Item("shop_eros_tomo", "Tomo de Hechizos Eroski",
                "Contiene un hechizo de área. Daño 2d8 a todos los enemigos.", "pergamino", "daño:2d8"),
            basePrice = 40, emoji = "📚", minTier = ReputationTier.APRECIADO
        ),
        ShopItem(
            item  = Item("shop_eros_grimorio", "Grimorio del Cooperativista",
                "El grimorio más poderoso. Solo para socios honorarios.", "reliquia", "cura:3d6"),
            basePrice = 80, emoji = "🔯", minTier = ReputationTier.VENERADO
        )
    ),

    "LUPA" to listOf(
        ShopItem(
            item  = Item("shop_lupa_yerba", "Hierba de la Meseta",
                "Recogida en los campos de Castilla. Cura con sabor local.", "pocion", "cura:1d6+3"),
            basePrice = 10, emoji = "🌿"
        ),
        ShopItem(
            item  = Item("shop_lupa_regional", "Poción Regional de Lupa",
                "Exclusiva de la zona. No la encontrarás en ningún otro super.", "pocion", "cura:2d4+4"),
            basePrice = 28, emoji = "🏡", minTier = ReputationTier.APRECIADO
        )
    )
)

// Catálogo genérico para cadenas sin especialización propia
private val GENERIC_EXTRA = listOf(
    ShopItem(
        item  = Item("shop_gen_antidoto", "Antídoto Universal",
            "Contrarresta venenos y toxinas.", "pocion", "cura:1d4"),
        basePrice = 15, emoji = "💊"
    ),
    ShopItem(
        item  = Item("shop_gen_max", "Poción de Curación Mayor",
            "La más potente de la estantería.", "pocion", "cura:3d4+5"),
        basePrice = 50, emoji = "🔮", minTier = ReputationTier.APRECIADO
    )
)

fun catalogForShop(shopName: String): List<ShopItem> {
    val key = shopName.uppercase()
    val specialty = SPECIALTY_CATALOGS.entries
        .firstOrNull { key.contains(it.key) }?.value ?: GENERIC_EXTRA
    return UNIVERSAL_CATALOG + specialty
}

/** Devuelve la clave normalizada para guardar reputación en Firestore */
fun shopReputationKey(shopName: String): String =
    shopName.lowercase().replace(" ", "_")

/** Precio final aplicando descuento de reputación */
fun finalPrice(basePrice: Int, tier: ReputationTier): Int =
    (basePrice * (100 - tier.discountPct) / 100).coerceAtLeast(1)

// ══════════════════════════════════════════════════════════════════════════════
// COMPOSABLE PRINCIPAL DE LA TIENDA
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SupermarketShopOverlay(
    supermarketName: String,
    gameId: String,
    currentCoins: Int,
    inventoryViewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var coins      by remember { mutableIntStateOf(currentCoins) }
    var feedback   by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }

    // Reputación
    val repKey = shopReputationKey(supermarketName)
    var repPoints by remember { mutableIntStateOf(0) }
    var repTier   by remember { mutableStateOf(ReputationTier.DESCONOCIDO) }
    var repLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(gameId, supermarketName) {
        val rep = inventoryViewModel.loadShopReputation(gameId)
        repPoints = rep[repKey] ?: 0
        repTier   = ReputationTier.fromPoints(repPoints)
        repLoaded = true
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            kotlinx.coroutines.delay(2200)
            feedback = null
        }
    }

    val catalog = remember(supermarketName) { catalogForShop(supermarketName) }

    Box(
        modifier         = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A0A00), Color(0xFF2D1500), Color(0xFF1A0A00))
                        )
                    )
                    .border(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700))
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                // ── Cabecera ──────────────────────────────────────────────────
                ShopHeader(
                    supermarketName = supermarketName,
                    coins           = coins,
                    repTier         = repTier,
                    repPoints       = repPoints,
                    onClose         = onDismiss
                )

                HorizontalDivider(color = Color(0xFFFFD700).copy(alpha = 0.35f), thickness = 1.dp)

                // ── Banner de reputación ──────────────────────────────────────
                if (repLoaded) {
                    ReputationBanner(tier = repTier, points = repPoints, shopName = supermarketName)
                }

                // ── Feedback ──────────────────────────────────────────────────
                AnimatedVisibility(visible = feedback != null) {
                    feedback?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = if (msg.startsWith("✅")) Color(0xFF003300) else Color(0xFF330000)
                            )
                        ) {
                            Text(
                                text      = msg,
                                color     = if (msg.startsWith("✅")) Color(0xFF66FF66) else Color(0xFFFF6666),
                                modifier  = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center,
                                style     = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── Lista de artículos ────────────────────────────────────────
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(catalog) { shopItem ->
                        val price = finalPrice(shopItem.basePrice, repTier)
                        val locked = repTier.ordinal < shopItem.minTier.ordinal

                        ShopItemCard(
                            shopItem     = shopItem,
                            finalPrice   = price,
                            currentCoins = coins,
                            isLocked     = locked,
                            isProcessing = processing,
                            repTier      = repTier,
                            onBuy        = {
                                scope.launch {
                                    if (coins < price) {
                                        feedback = "❌ Necesitas $price 🪙 (tienes $coins)"
                                        return@launch
                                    }
                                    processing = true
                                    val ok = inventoryViewModel.spendCoins(gameId, price)
                                    if (ok) {
                                        val bought = shopItem.item.copy(
                                            id = "${shopItem.item.id}_${System.currentTimeMillis()}"
                                        )
                                        inventoryViewModel.addItemToInventory(gameId, bought)
                                        coins -= price
                                        // Añadir reputación
                                        repPoints = inventoryViewModel.addShopReputation(
                                            gameId, repKey, points = 10
                                        )
                                        repTier = ReputationTier.fromPoints(repPoints)
                                        feedback = "✅ ¡Compraste ${shopItem.emoji} ${shopItem.item.name}! (+10 rep)"
                                    } else {
                                        feedback = "❌ No tienes suficientes monedas"
                                    }
                                    processing = false
                                }
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚔️  Visita más veces para mejorar tu reputación y obtener descuentos  ⚔️",
                            color     = Color(0xFF6A5030),
                            style     = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── CABECERA ──────────────────────────────────────────────────────────────────

@Composable
private fun ShopHeader(
    supermarketName: String,
    coins: Int,
    repTier: ReputationTier,
    repPoints: Int,
    onClose: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFFFD700), modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "MERCADER DE $supermarketName",
                    color      = Color(0xFFFFD700),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${repTier.emoji} ${repTier.label}",
                        color = repTier.color,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (repTier.discountPct > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "−${repTier.discountPct}%",
                            color      = Color(0xFF66FF66),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A1A00))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("🪙 $coins", color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Cerrar", tint = Color(0xFFFFD700))
            }
        }
    }
}

// ── BANNER DE REPUTACIÓN ──────────────────────────────────────────────────────

@Composable
private fun ReputationBanner(tier: ReputationTier, points: Int, shopName: String) {
    val nextTier = ReputationTier.entries.firstOrNull { it.ordinal == tier.ordinal + 1 }
    val progress = if (nextTier != null) {
        val range = nextTier.minPoints - tier.minPoints
        val done  = points - tier.minPoints
        (done.toFloat() / range).coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0D0800)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "${tier.emoji} Reputación en $shopName: ${tier.label}",
                    color      = tier.color,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$points rep",
                    color = Color(0xFF9A8060),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (nextTier != null) {
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress          = { progress },
                    modifier          = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color             = tier.color,
                    trackColor        = Color(0xFF2A1A00)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Siguiente: ${nextTier.emoji} ${nextTier.label} (${nextTier.minPoints - points} rep más → −${nextTier.discountPct}% desc.)",
                    color = Color(0xFF7A6040),
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Text(
                    "¡Eres Venerado! Tienes el máximo descuento (−${tier.discountPct}%) y acceso a todos los artículos.",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ── TARJETA DE ARTÍCULO ───────────────────────────────────────────────────────

@Composable
private fun ShopItemCard(
    shopItem: ShopItem,
    finalPrice: Int,
    currentCoins: Int,
    isLocked: Boolean,
    isProcessing: Boolean,
    repTier: ReputationTier,
    onBuy: () -> Unit
) {
    val canAfford   = currentCoins >= finalPrice && !isLocked
    val borderColor = when {
        isLocked  -> Color(0xFF2A1A00)
        canAfford -> Color(0xFFFFD700)
        else      -> Color(0xFF4A3A20)
    }
    val bgColor = when {
        isLocked  -> Color(0xFF0D0800)
        canAfford -> Color(0xFF1E1200)
        else      -> Color(0xFF150D00)
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text   = if (isLocked) "🔒" else shopItem.emoji,
                    style  = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        shopItem.item.name,
                        color      = if (isLocked) Color(0xFF4A3A20) else if (canAfford) Color(0xFFFFD700) else Color(0xFF8A6A30),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isLocked) {
                        Text(
                            "Requiere reputación: ${shopItem.minTier.emoji} ${shopItem.minTier.label}",
                            color = Color(0xFF5A3A10),
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Text(
                            shopItem.item.description,
                            color    = Color(0xFF9A8060),
                            style    = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            formatEffect(shopItem.item.effect),
                            color = Color(0xFF66BB66),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (!isLocked) {
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Precio con descuento
                    if (repTier.discountPct > 0 && finalPrice < shopItem.basePrice) {
                        Text(
                            "🪙 ${shopItem.basePrice}",
                            color = Color(0xFF5A4A30),
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                    Text(
                        "🪙 $finalPrice",
                        color      = if (canAfford) Color(0xFFFFD700) else Color(0xFF6A5A30),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick        = onBuy,
                        enabled        = canAfford && !isProcessing,
                        modifier       = Modifier.width(90.dp),
                        colors         = ButtonDefaults.buttonColors(
                            containerColor         = if (canAfford) Color(0xFFB8860B) else Color(0xFF2A1A00),
                            disabledContainerColor = Color(0xFF2A1A00)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = Color(0xFFFFD700), strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (canAfford) "Comprar" else "Sin oro",
                                color      = if (canAfford) Color.White else Color(0xFF5A4A20),
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── HELPER ────────────────────────────────────────────────────────────────────

private fun formatEffect(effect: String): String {
    val e = effect.lowercase().trim()
    return when {
        e.startsWith("cura:")  -> "💚 Cura ${e.removePrefix("cura:").uppercase()} PV"
        e.startsWith("daño:")  -> "⚔️ Daño ${e.removePrefix("daño:").uppercase()}"
        e.startsWith("veneno:")-> "☠️ Veneno ${e.removePrefix("veneno:")}"
        e.startsWith("+")      -> "🛡️ $effect"
        e.isBlank()            -> "✨ Efecto especial"
        else                   -> "✨ $effect"
    }
}