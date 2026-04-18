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
import com.example.aidungeonmaster.viewmodel.ItemComparison
import kotlinx.coroutines.delay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

import com.example.aidungeonmaster.utils.AdventureMusicEngine

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

    var pendingRingItem by remember { mutableStateOf<Item?>(null) }

    LaunchedEffect(feedbackMsg) {
        if (feedbackMsg != null) {
            delay(2500)
            feedbackMsg = null
        }
    }

    DisposableEffect(Unit) {
        AdventureMusicEngine.setScreen(AdventureMusicEngine.MusicScreen.INVENTORY)
        onDispose {
            AdventureMusicEngine.releaseScreen(1200L)
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
                                        getComparison = { item -> viewModel.getItemComparison(item) },
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
                                            if (equippable.resolvedEquipSlot == "ring") {
                                                pendingRingItem = equippable
                                            } else {
                                                viewModel.equipItem(gameId, equippable) { msg ->
                                                    feedbackMsg = msg
                                                }
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

                pendingRingItem?.let { ringItem ->
                    RingSlotChooserDialog(
                        item = ringItem,
                        currentEquipment = character?.equipment,
                        onDismiss = { pendingRingItem = null },
                        onChoose = { chosenSlot ->
                            pendingRingItem = null
                            viewModel.equipItem(gameId, ringItem, targetSlot = chosenSlot) { msg ->
                                feedbackMsg = msg
                            }
                        }
                    )
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
    getComparison: (Item) -> ItemComparison?,
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

        val sortedInventory = remember(character.inventory) {
            character.inventory.sortedWith(
                compareBy<Item>(
                    { rarityOrder(it.rarity) },
                    { itemDisplayOrder(it) },
                    { it.name.lowercase() }
                )
            )
        }

        if (sortedInventory.isEmpty()) {
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
                itemsIndexed(sortedInventory) { _, item ->
                    InventoryItemRow(
                        item = item,
                        comparison = getComparison(item),
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
                "ring" to "Anillo 1",
                "ring2" to "Anillo 2",
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
    var statsExpanded by remember(character.id, character.lastPlayed, character.level) {
        mutableStateOf(false)
    }

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

            if (character.activeSetNames.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sets activos: ${character.activeSetNames.joinToString(" · ")}",
                    color = Color(0xFFB8A6FF),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { statsExpanded = !statsExpanded },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFFD700)
                ),
                border = BorderStroke(1.dp, Color(0x55FFD700)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (statsExpanded) "Ocultar stats finales" else "Ver stats finales",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (statsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = statsExpanded) {
                Column {
                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Stats finales",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = buildString {
                            append("FUE ${character.strTotal} (${formatMod(character.strMod)}) · ")
                            append("DES ${character.dexTotal} (${formatMod(character.dexMod)}) · ")
                            append("CON ${character.conTotal} (${formatMod(character.conMod)})")
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = buildString {
                            append("INT ${character.intTotal} (${formatMod(character.intMod)}) · ")
                            append("SAB ${character.wisTotal} (${formatMod(character.wisMod)}) · ")
                            append("CAR ${character.chaTotal} (${formatMod(character.chaMod)})")
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Ataque melee: +${character.meleeAttackBonus} · Iniciativa: ${formatMod(character.initiativeBonus)} · Daño: ${formatMod(character.weaponDamageBonus)}",
                        color = Color(0xFFBFE3FF),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
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
                    val rarityColor = when (item.rarity.lowercase()) {
                        "uncommon" -> Color(0xFF7CFF7C)
                        "rare" -> Color(0xFF6EC6FF)
                        "epic" -> Color(0xFFC58CFF)
                        "legendary" -> Color(0xFFFFC857)
                        else -> Color(0xFFCCCCCC)
                    }

                    Text(
                        text = item.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = friendlyRarityName(item.rarity),
                        color = rarityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
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
    comparison: ItemComparison? = null,
    onUse: ((Item) -> Unit)? = null,
    onEquip: ((Item) -> Unit)? = null
) {
    val rarityColor = when (item.rarity.lowercase()) {
        "uncommon" -> Color(0xFF7CFF7C)
        "rare" -> Color(0xFF6EC6FF)
        "epic" -> Color(0xFFC58CFF)
        "legendary" -> Color(0xFFFFC857)
        else -> Color(0xFFCCCCCC)
    }

    val isActuallyEquippable = item.resolvedEquipSlot.isNotBlank() || item.isWeapon || item.isArmor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, rarityColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = iconForItemType(item),
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = buildString {
                        append(friendlyRarityName(item.rarity))
                        if (item.setName.isNotBlank()) append(" · ${item.setName}")
                    },
                    color = rarityColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        color = Color(0xFFEAEAEA),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (item.effect.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.effect,
                        color = Color(0xFF00F5FF),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val detail = buildInventoryItemDetailText(item)
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail,
                        color = Color(0xFFBFE3FF),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (item.enchantments.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    item.enchantments.forEach { enchantment ->
                        Text(
                            text = "✧ ${enchantment.name}: ${enchantment.description.ifBlank { "encantamiento activo" }}",
                            color = Color(0xFFB8A6FF),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                comparison?.let { cmp ->
                    Spacer(Modifier.height(6.dp))

                    val weaponChanged = cmp.currentWeaponDamage != cmp.projectedWeaponDamage
                    if (weaponChanged) {
                        Text(
                            text = "Comparación arma: ${cmp.currentWeaponDamage} → ${cmp.projectedWeaponDamage}",
                            color = Color(0xFFFFD59A),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    cmp.replacedItemName?.let {
                        Text(
                            text = "Reemplaza: $it",
                            color = Color(0xFFCCCCCC),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    cmp.lines.forEach { line ->
                        val sign = if (line.delta > 0) "+" else ""
                        val color = when {
                            line.delta > 0 -> Color(0xFF7CFF7C)
                            line.delta < 0 -> Color(0xFFFF8A8A)
                            else -> Color.LightGray
                        }

                        Text(
                            text = "${line.label}: ${line.current} → ${line.projected} ($sign${line.delta})",
                            color = color,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            when {
                isActuallyEquippable && onEquip != null -> {
                    Button(
                        onClick = { onEquip(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Equipar")
                    }
                }

                !isActuallyEquippable && onUse != null -> {
                    Button(
                        onClick = { onUse(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Usar")
                    }
                }
            }
        }
    }
}

private fun buildInventoryItemDetailText(item: Item): String {
    val parts = mutableListOf<String>()

    if (item.resolvedEquipSlot.isNotBlank()) {
        parts += friendlySlotName(item.resolvedEquipSlot)
    }

    if (item.resolvedWeaponDamage.isNotBlank()) {
        parts += "daño=${item.resolvedWeaponDamage}"
    }

    item.armorBase?.let {
        parts += "CA base=$it"
    }

    if (item.totalArmorBonus != 0) {
        val sign = if (item.totalArmorBonus > 0) "+" else ""
        parts += "bonus CA=${sign}${item.totalArmorBonus}"
    }

    item.maxDexBonus?.let {
        parts += "máx DEX=$it"
    }

    if (item.handedness.equals("two_hand", ignoreCase = true)) {
        parts += "dos manos"
    }

    if (item.totalAttackBonus != 0) {
        val sign = if (item.totalAttackBonus > 0) "+" else ""
        parts += "ataque=${sign}${item.totalAttackBonus}"
    }

    if (item.totalWeaponDamageBonus != 0) {
        val sign = if (item.totalWeaponDamageBonus > 0) "+" else ""
        parts += "daño bonus=${sign}${item.totalWeaponDamageBonus}"
    }

    if (item.totalStatBonuses.isNotEmpty()) {
        val bonusText = item.totalStatBonuses.entries.joinToString(", ") { (stat, value) ->
            val sign = if (value > 0) "+" else ""
            "$stat $sign$value"
        }
        parts += bonusText
    }

    if (item.setName.isNotBlank()) {
        parts += "set=${item.setName}"
    }

    if (item.enchantments.isNotEmpty()) {
        parts += "encant=${item.enchantments.joinToString { it.name }}"
    }

    return parts.joinToString(" · ")
}

private fun buildEquipmentDetailText(item: Item): String {
    val parts = mutableListOf<String>()

    parts += friendlyRarityName(item.rarity)

    if (item.resolvedWeaponDamage.isNotBlank()) {
        parts += "Daño ${item.resolvedWeaponDamage}"
    }

    item.armorBase?.let {
        parts += "CA base $it"
    }

    if (item.totalArmorBonus != 0) {
        val sign = if (item.totalArmorBonus > 0) "+" else ""
        parts += "CA ${sign}${item.totalArmorBonus}"
    }

    item.maxDexBonus?.let {
        parts += "DEX máx $it"
    }

    if (item.totalAttackBonus != 0) {
        val sign = if (item.totalAttackBonus > 0) "+" else ""
        parts += "Ataque ${sign}${item.totalAttackBonus}"
    }

    if (item.totalWeaponDamageBonus != 0) {
        val sign = if (item.totalWeaponDamageBonus > 0) "+" else ""
        parts += "Daño ${sign}${item.totalWeaponDamageBonus}"
    }

    if (item.setName.isNotBlank()) {
        parts += item.setName
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

private fun friendlySlotName(slot: String): String = when (normalizeItemType(slot)) {
    "head", "cabeza" -> "cabeza"
    "chest", "pecho", "torso" -> "pecho"
    "legs", "piernas" -> "piernas"
    "feet", "pies" -> "pies"
    "hands", "manos" -> "manos"
    "main_hand", "mano principal", "mano_principal" -> "mano principal"
    "off_hand", "mano secundaria", "mano_secundaria" -> "mano secundaria"
    "ring", "anillo", "ring1", "anillo1" -> "anillo"
    "ring2", "anillo2" -> "anillo 2"
    "amulet", "amuleto" -> "amuleto"
    else -> slot
}

private fun slotEmoji(slot: String): String = when (normalizeItemType(slot)) {
    "head", "cabeza" -> "🪖"
    "chest", "pecho", "torso" -> "🦺"
    "legs", "piernas" -> "👖"
    "feet", "pies" -> "🥾"
    "hands", "manos" -> "🧤"
    "main_hand", "mano principal", "mano_principal" -> "⚔️"
    "off_hand", "mano secundaria", "mano_secundaria" -> "🛡️"
    "ring", "anillo", "ring1", "anillo1", "ring2", "anillo2" -> "💍"
    "amulet", "amuleto" -> "📿"
    else -> "📦"
}

private fun formatMod(value: Int): String = if (value >= 0) "+$value" else "$value"

private fun friendlyRarityName(rarity: String): String = when (rarity.lowercase()) {
    "common" -> "Común"
    "uncommon" -> "Poco común"
    "rare" -> "Raro"
    "epic" -> "Épico"
    "legendary" -> "Legendario"
    else -> rarity.replaceFirstChar { it.uppercase() }
}

private fun rarityOrder(rarity: String): Int = when (rarity.lowercase()) {
    "legendary" -> 0
    "epic" -> 1
    "rare" -> 2
    "uncommon" -> 3
    "common" -> 4
    else -> 5
}

private fun itemDisplayOrder(item: Item): Int = when {
    item.isWeapon -> 0
    item.isArmor -> 1
    item.resolvedEquipSlot == "ring" || item.resolvedEquipSlot == "ring2" -> 2
    item.resolvedEquipSlot == "amulet" -> 3
    else -> 4
}

private fun iconForItemType(item: Item): String = when {
    item.resolvedEquipSlot == "ring" || item.resolvedEquipSlot == "ring2" -> "💍"
    item.resolvedEquipSlot == "amulet" -> "📿"
    item.resolvedEquipSlot == "main_hand" -> "⚔️"
    item.resolvedEquipSlot == "off_hand" && item.name.contains("escudo", ignoreCase = true) -> "🛡️"
    item.resolvedEquipSlot == "head" -> "🪖"
    item.resolvedEquipSlot == "chest" -> "🦺"
    item.resolvedEquipSlot == "legs" -> "👖"
    item.resolvedEquipSlot == "feet" -> "🥾"
    item.resolvedEquipSlot == "hands" -> "🧤"
    item.isWeapon -> "⚔️"
    item.isArmor -> "🛡️"
    item.type.contains("pocion", ignoreCase = true) || item.name.contains("poción", ignoreCase = true) -> "🧪"
    item.type.contains("consum", ignoreCase = true) -> "🍖"
    else -> "🎒"
}

@Composable
private fun RingSlotChooserDialog(
    item: Item,
    currentEquipment: com.example.aidungeonmaster.data.model.EquippedItems?,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir slot de anillo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Dónde quieres equipar ${item.name}?")
                Text(
                    text = "Anillo 1: ${currentEquipment?.ring?.name ?: "vacío"}\n" +
                            "Anillo 2: ${currentEquipment?.ring2?.name ?: "vacío"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onChoose("ring") }) { Text("Anillo 1") }
                TextButton(onClick = { onChoose("ring2") }) { Text("Anillo 2") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}