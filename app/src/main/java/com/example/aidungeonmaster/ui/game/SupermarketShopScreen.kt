package com.example.aidungeonmaster.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

// ── CATÁLOGO DE ARTÍCULOS CURATIVOS DE LA TIENDA ────────────────────────────

data class ShopItem(
    val item: Item,
    val price: Int,
    val emoji: String,
    val stock: Int = 3
)

private val SHOP_CATALOG = listOf(
    ShopItem(
        item  = Item(
            id          = "shop_vendas",
            name        = "Vendas de Campaña",
            description = "Vendas básicas de lino impregnadas con hierbas medicinales.",
            type        = "pocion",
            effect      = "cura:3"
        ),
        price = 5,
        emoji = "🩹",
        stock = 5
    ),
    ShopItem(
        item  = Item(
            id          = "shop_pocion_menor",
            name        = "Poción de Curación Menor",
            description = "Un frasco de líquido rojizo que restaura la vitalidad.",
            type        = "pocion",
            effect      = "cura:1d4+1"
        ),
        price = 12,
        emoji = "🧪"
    ),
    ShopItem(
        item  = Item(
            id          = "shop_pan_elvico",
            name        = "Pan Élfico",
            description = "Nutritivo pan mágico que sana lentamente las heridas.",
            type        = "consumible",
            effect      = "cura:2"
        ),
        price = 8,
        emoji = "🍞"
    ),
    ShopItem(
        item  = Item(
            id          = "shop_pocion",
            name        = "Poción de Curación",
            description = "Poción estándar de los aventureros. Sana heridas considerables.",
            type        = "pocion",
            effect      = "cura:2d4+2"
        ),
        price = 25,
        emoji = "⚗️"
    ),
    ShopItem(
        item  = Item(
            id          = "shop_antidoto",
            name        = "Antídoto Universal",
            description = "Contrarresta venenos y toxinas. Huele a pino y azufre.",
            type        = "pocion",
            effect      = "cura:1d4"
        ),
        price = 15,
        emoji = "💊"
    ),
    ShopItem(
        item  = Item(
            id          = "shop_pocion_mayor",
            name        = "Poción de Curación Mayor",
            description = "La poción más potente del mercado. Reservada para héroes.",
            type        = "pocion",
            effect      = "cura:3d4+5"
        ),
        price = 50,
        emoji = "🔮"
    )
)

// ── COMPOSABLE PRINCIPAL DE LA TIENDA ────────────────────────────────────────

@Composable
fun SupermarketShopOverlay(
    supermarketName: String,
    gameId: String,
    currentCoins: Int,
    inventoryViewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var coins by remember { mutableIntStateOf(currentCoins) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Limpiar mensaje de feedback tras 2 segundos
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(2000)
            feedbackMessage = null
        }
    }

    // Fondo oscuro semitransparente
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Panel principal de la tienda
        AnimatedVisibility(
            visible = true,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A0A00), Color(0xFF2D1500), Color(0xFF1A0A00))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700))
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                // ── CABECERA ─────────────────────────────────────────────────
                ShopHeader(
                    supermarketName = supermarketName,
                    coins           = coins,
                    onClose         = onDismiss
                )

                HorizontalDivider(color = Color(0xFFFFD700).copy(alpha = 0.4f), thickness = 1.dp)

                // ── MENSAJE DE BIENVENIDA ─────────────────────────────────────
                Text(
                    text      = "\"¡Bienvenido, aventurero! ¿Qué necesitas para tu viaje?\"",
                    color     = Color(0xFFD4B896),
                    style     = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light,
                    textAlign  = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // ── FEEDBACK DE COMPRA ────────────────────────────────────────
                AnimatedVisibility(visible = feedbackMessage != null) {
                    feedbackMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = if (msg.startsWith("✅"))
                                    Color(0xFF003300) else Color(0xFF330000)
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

                // ── LISTA DE ARTÍCULOS ────────────────────────────────────────
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(SHOP_CATALOG) { shopItem ->
                        ShopItemCard(
                            shopItem     = shopItem,
                            currentCoins = coins,
                            isProcessing = isProcessing,
                            onBuy        = {
                                scope.launch {
                                    if (coins < shopItem.price) {
                                        feedbackMessage = "❌ No tienes suficientes monedas (${shopItem.price} 🪙 necesarias)"
                                        return@launch
                                    }
                                    isProcessing = true
                                    val success = inventoryViewModel.spendCoins(gameId, shopItem.price)
                                    if (success) {
                                        val boughtItem = shopItem.item.copy(
                                            id = "${shopItem.item.id}_${System.currentTimeMillis()}"
                                        )
                                        inventoryViewModel.addItemToInventory(gameId, boughtItem)
                                        coins -= shopItem.price
                                        feedbackMessage = "✅ ¡Compraste ${shopItem.emoji} ${shopItem.item.name}!"
                                    } else {
                                        feedbackMessage = "❌ No tienes suficientes monedas"
                                    }
                                    isProcessing = false
                                }
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = "⚔️ La aventura te espera. ¡Viaja con seguridad! ⚔️",
                            color     = Color(0xFF8A6A40),
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

// ── CABECERA DE LA TIENDA ─────────────────────────────────────────────────────

@Composable
private fun ShopHeader(
    supermarketName: String,
    coins: Int,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Icono + título
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint     = Color(0xFFFFD700),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text  = "MERCADER DE $supermarketName",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text  = "Tienda de pociones y suministros",
                    color = Color(0xFFD4B896),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Monedas + botón cerrar
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Monedero
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A1A00))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text  = "🪙 $coins",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar tienda",
                    tint = Color(0xFFFFD700)
                )
            }
        }
    }
}

// ── TARJETA DE ARTÍCULO ───────────────────────────────────────────────────────

@Composable
private fun ShopItemCard(
    shopItem: ShopItem,
    currentCoins: Int,
    isProcessing: Boolean,
    onBuy: () -> Unit
) {
    val canAfford = currentCoins >= shopItem.price
    val borderColor = if (canAfford) Color(0xFFFFD700) else Color(0xFF4A3A20)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) Color(0xFF1E1200) else Color(0xFF150D00)
        )
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Emoji + info
            Row(
                modifier          = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = shopItem.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text  = shopItem.item.name,
                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFF8A6A30),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text  = shopItem.item.description,
                        color = Color(0xFF9A8060),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    // Efecto en formato legible
                    val effectText = formatEffect(shopItem.item.effect)
                    Text(
                        text  = effectText,
                        color = Color(0xFF66BB66),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Precio + botón comprar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = "🪙 ${shopItem.price}",
                    color = if (canAfford) Color(0xFFFFD700) else Color(0xFF6A5A30),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick  = onBuy,
                    enabled  = canAfford && !isProcessing,
                    modifier = Modifier.width(90.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (canAfford) Color(0xFFB8860B) else Color(0xFF2A1A00),
                        disabledContainerColor = Color(0xFF2A1A00)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color    = Color(0xFFFFD700),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text  = if (canAfford) "Comprar" else "Sin oro",
                            color = if (canAfford) Color.White else Color(0xFF5A4A20),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── HELPER: FORMATO DE EFECTO LEGIBLE ────────────────────────────────────────

private fun formatEffect(effect: String): String {
    val e = effect.lowercase().trim()
    return when {
        e.startsWith("cura:")  -> "💚 Cura ${e.removePrefix("cura:").uppercase()} PV"
        e.startsWith("daño:")  -> "⚔️ Daño ${e.removePrefix("daño:").uppercase()}"
        e.startsWith("veneno:")-> "☠️ Veneno ${e.removePrefix("veneno:")}"
        e.isBlank()            -> "✨ Efecto especial"
        else                   -> "✨ $effect"
    }
}