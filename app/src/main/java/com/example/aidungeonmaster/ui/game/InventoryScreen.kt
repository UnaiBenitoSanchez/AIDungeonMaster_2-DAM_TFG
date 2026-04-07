package com.example.aidungeonmaster.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    LaunchedEffect(gameId) {
        viewModel.loadInventory(gameId)
    }

    val character by viewModel.character.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(feedbackMsg) {
        if (feedbackMsg != null) {
            delay(2500)
            feedbackMsg = null
        }
    }

    MedievalBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { MedievalTitle("MOCHILA") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    isLoading -> {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD700))
                        }
                    }

                    character == null -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val name = gameId.split("_").getOrElse(1) { "Héroe" }
                            Text(
                                "Error al cargar el equipo de $name",
                                color = Color.White
                            )
                        }
                    }

                    else -> {
                        val currentCharacter = character!!

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            CharacterHeaderCard(currentCharacter)

                            InventoryPagerTabs(
                                selectedPage = pagerState.currentPage
                            )

                            Spacer(Modifier.height(8.dp))

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> InventoryPage(
                                        character = currentCharacter,
                                        onUse = { usedItem ->
                                            val msg = viewModel.useItem(
                                                gameId,
                                                usedItem,
                                                currentCharacter.hpCurrent,
                                                currentCharacter.hpMax
                                            )
                                            feedbackMsg = msg
                                        },
                                        onEquip = { equippable ->
                                            viewModel.equipItem(gameId, equippable) { msg ->
                                                feedbackMsg = msg
                                            }
                                        }
                                    )

                                    1 -> EquipmentPage(
                                        character = currentCharacter,
                                        onUnequip = { slot ->
                                            viewModel.unequipItem(gameId, slot) { msg ->
                                                feedbackMsg = msg
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                feedbackMsg?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Text(
                            msg,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            color = Color(0xFFFFD700),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryPagerTabs(selectedPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color(0x44FFD700),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabChip(
            title = "🎒 Inventario",
            selected = selectedPage == 0,
            modifier = Modifier.weight(1f)
        )
        TabChip(
            title = "🧍 Equipo",
            selected = selectedPage == 1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabChip(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (selected) Color(0x66FFD700) else Color(0x22000000),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFFFFD700) else Color(0x33FFFFFF)
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (selected) Color(0xFFFFF2B2) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun InventoryPage(
    character: Character,
    onUse: (Item) -> Unit,
    onEquip: (Item) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(4.dp))

        Text(
            "Desliza para cambiar entre inventario y equipo",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(10.dp))

        if (character.inventory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Mochila vacía. ¡Busca botín!",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(character.inventory) { _, item ->
                    InventoryItemRow(
                        item = item,
                        onUse = onUse,
                        onEquip = onEquip
                    )
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun EquipmentPage(
    character: Character,
    onUnequip: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(4.dp))

        Text(
            "Desliza para volver al inventario",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val slots = listOf(
                "head" to "Cabeza",
                "chest" to "Pecho",
                "legs" to "Piernas",
                "feet" to "Pies",
                "hands" to "Manos",
                "main_hand" to "Mano principal",
                "off_hand" to "Mano secundaria",
                "ring" to "Anillo",
                "amulet" to "Amuleto"
            )

            itemsIndexed(slots) { _, (slotKey, slotLabel) ->
                EquipmentSlotCard(
                    slotKey = slotKey,
                    slotLabel = slotLabel,
                    item = character.equipment.itemInSlot(slotKey),
                    onUnequip = onUnequip
                )
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun CharacterHeaderCard(character: Character) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x44000000)),
        border = BorderStroke(1.dp, Color(0x55FFD700))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Healing,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "HP: ${character.hpCurrent} / ${character.hpMax}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                val ratio = if (character.hpMax > 0) {
                    character.hpCurrent.toFloat() / character.hpMax
                } else {
                    0f
                }

                val barColor = when {
                    ratio > 0.5f -> Color(0xFF22CC55)
                    ratio > 0.25f -> Color(0xFFFFAA00)
                    else -> Color(0xFFCC2222)
                }

                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .width(100.dp)
                        .height(8.dp),
                    color = barColor,
                    trackColor = Color(0xFF333333)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${character.coins} monedas de oro",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Clase de armadura: ${character.armorClass}",
                    color = Color(0xFFBFE3FF),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            character.equipment.mainHand?.let { weapon ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Arma equipada: ${weapon.name}" +
                                if (weapon.resolvedWeaponDamage.isNotBlank()) {
                                    " (${weapon.resolvedWeaponDamage})"
                                } else {
                                    ""
                                },
                        color = Color(0xFFFFD59A),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentSlotCard(
    slotKey: String,
    slotLabel: String,
    item: Item?,
    onUnequip: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x22000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slotEmoji(slotKey),
                fontSize = 24.sp
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotLabel,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (item == null) {
                    Text(
                        text = "Vacío",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val details = buildEquipmentDetailText(item)
                    if (details.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = details,
                            color = Color(0xFFBFE3FF),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (item != null) {
                OutlinedButton(
                    onClick = { onUnequip(slotKey) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFD700)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        "Quitar",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(
    item: Item,
    onUse: ((Item) -> Unit)? = null,
    onEquip: ((Item) -> Unit)? = null
) {
    val normalizedType = normalizeItemType(item.type)

    val isConsumable =
        normalizedType == "pocion" ||
                normalizedType == "consumible" ||
                normalizedType == "comida" ||
                normalizedType == "pergamino" ||
                normalizedType == "veneno" ||
                normalizedType == "explosivo" ||
                normalizedType.contains("pocion") ||
                normalizedType.contains("consumible") ||
                normalizedType.contains("pergamino")

    val isEquippable = item.isEquippable

    val typeEmoji = when {
        normalizedType == "pocion" || normalizedType.contains("pocion") -> "🧪"
        normalizedType == "arma" || normalizedType.contains("arma") -> "⚔️"
        normalizedType == "armadura" || normalizedType.contains("armadura") -> "🛡️"
        normalizedType == "pergamino" || normalizedType.contains("pergamino") -> "📜"
        normalizedType == "veneno" || normalizedType.contains("veneno") -> "☠️"
        normalizedType == "explosivo" || normalizedType.contains("explosivo") -> "💣"
        normalizedType == "reliquia" || normalizedType.contains("reliquia") -> "✨"
        else -> "🎒"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(typeEmoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (item.description.isNotEmpty()) {
                    Text(
                        item.description,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (item.effect.isNotEmpty()) {
                    Text(
                        "✦ ${item.effect}",
                        color = Color.Cyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val detail = buildInventoryItemDetailText(item)
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        detail,
                        color = Color(0xFFBFE3FF),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onUse != null && isConsumable) {
                    Button(
                        onClick = { onUse(item) },
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF004400)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF22CC55)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Usar",
                            color = Color(0xFF22CC55),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (onEquip != null && isEquippable) {
                    OutlinedButton(
                        onClick = { onEquip(item) },
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFD700)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFFD700)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Equipar",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun buildInventoryItemDetailText(item: Item): String {
    val parts = mutableListOf<String>()

    if (item.resolvedEquipSlot.isNotBlank()) {
        parts += "slot=${friendlySlotName(item.resolvedEquipSlot)}"
    }

    if (item.resolvedWeaponDamage.isNotBlank()) {
        parts += "daño=${item.resolvedWeaponDamage}"
    }

    item.armorBase?.let {
        parts += "CA base=$it"
    }

    if (item.armorBonus != 0) {
        val sign = if (item.armorBonus > 0) "+" else ""
        parts += "bonus CA=${sign}${item.armorBonus}"
    }

    item.maxDexBonus?.let {
        parts += "máx DEX=$it"
    }

    if (item.handedness.equals("two_hand", ignoreCase = true)) {
        parts += "dos manos"
    }

    if (item.statBonuses.isNotEmpty()) {
        val bonusText = item.statBonuses.entries.joinToString(", ") { (stat, value) ->
            val sign = if (value > 0) "+" else ""
            "$stat $sign$value"
        }
        parts += bonusText
    }

    return parts.joinToString(" · ")
}

private fun buildEquipmentDetailText(item: Item): String {
    val parts = mutableListOf<String>()

    if (item.resolvedWeaponDamage.isNotBlank()) {
        parts += "Daño ${item.resolvedWeaponDamage}"
    }

    item.armorBase?.let {
        parts += "CA base $it"
    }

    if (item.armorBonus != 0) {
        val sign = if (item.armorBonus > 0) "+" else ""
        parts += "CA ${sign}${item.armorBonus}"
    }

    item.maxDexBonus?.let {
        parts += "DEX máx $it"
    }

    return parts.joinToString(" · ")
}

private fun normalizeItemType(raw: String): String {
    return raw
        .trim()
        .lowercase()
        .replace("ó", "o")
        .replace("í", "i")
        .replace("á", "a")
        .replace("é", "e")
        .replace("ú", "u")
}

private fun friendlySlotName(slot: String): String = when (slot.lowercase()) {
    "head" -> "cabeza"
    "chest" -> "pecho"
    "legs" -> "piernas"
    "feet" -> "pies"
    "hands" -> "manos"
    "main_hand" -> "mano principal"
    "off_hand" -> "mano secundaria"
    "ring" -> "anillo"
    "amulet" -> "amuleto"
    else -> slot
}

private fun slotEmoji(slot: String): String = when (slot.lowercase()) {
    "head" -> "🪖"
    "chest" -> "🦺"
    "legs" -> "👖"
    "feet" -> "🥾"
    "hands" -> "🧤"
    "main_hand" -> "⚔️"
    "off_hand" -> "🛡️"
    "ring" -> "💍"
    "amulet" -> "📿"
    else -> "📦"
}