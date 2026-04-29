package com.example.aidungeonmaster.ui.home

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.viewmodel.RankingCategory
import com.example.aidungeonmaster.viewmodel.RankingEntry
import com.example.aidungeonmaster.viewmodel.RankingViewModel

// ── Paleta medieval ────────────────────────────────────────────────────────
private val DarkBg     = Color(0xFF0D0A1E)
private val CardBg     = Color(0xFF1A1530)
private val Gold       = Color(0xFFFFD700)
private val Silver     = Color(0xFFC0C0C0)
private val Bronze     = Color(0xFFCD7F32)
private val BorderColor= Color(0xFF4A3060)
private val TabActive  = Color(0xFF7B2FBE)
private val TextPrimary= Color(0xFFF0E6FF)
private val TextSecond = Color(0xFFAA99CC)

// ── Medallas ────────────────────────────────────────────────────────────────
private fun medalColor(pos: Int) = when (pos) {
    0 -> Gold
    1 -> Silver
    2 -> Bronze
    else -> TextSecond
}
private fun medalEmoji(pos: Int) = when (pos) {
    0 -> "👑"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${pos + 1}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onBack: () -> Unit,
    viewModel: RankingViewModel = viewModel()
) {
    val rankings by viewModel.rankings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val categories = RankingCategory.entries

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D0A1E), Color(0xFF1A0D30), Color(0xFF0D0A1E)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        "⚔️ Ranking Mundial",
                        color = Gold,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllRankings() }) {
                        Icon(Icons.Default.Refresh, null, tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF110D25))
            )

            // ── Subtitle ─────────────────────────────────────────────────
            Text(
                "Los mejores héroes de todos los reinos",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
                color = TextSecond,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif
            )

            // ── Tabs ──────────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF110D25),
                contentColor = Gold,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .background(Gold, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                "${category.icon} ${category.label}",
                                color = if (selectedTab == index) Gold else TextSecond,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // ── Contenido ─────────────────────────────────────────────────
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Gold)
                        Spacer(Modifier.height(12.dp))
                        Text("Convocando a los héroes...", color = TextSecond, fontFamily = FontFamily.Serif)
                    }
                }
            } else {
                val currentCategory = categories[selectedTab]
                val list = rankings[currentCategory] ?: emptyList()

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏚️", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Nadie ha reclamado el trono aún.\n¡Sé el primero en la historia!",
                                color = TextSecond,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    // Top 3 podio
                    TopPodium(list = list.take(3), category = currentCategory)

                    // Resto del ranking
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(list) { index, entry ->
                            if (index >= 3) {
                                RankingRow(position = index, entry = entry, category = currentCategory)
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── Podio top 3 ─────────────────────────────────────────────────────────────
@Composable
private fun TopPodium(list: List<RankingEntry>, category: RankingCategory) {
    if (list.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2A1A50), Color(0xFF1A1030))
                )
            )
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                "✨ Hall of Fame",
                color = Gold,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            list.forEachIndexed { index, entry ->
                PodiumCard(position = index, entry = entry, category = category)
                if (index < list.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PodiumCard(position: Int, entry: RankingEntry, category: RankingCategory) {
    val medal = medalColor(position)
    val value = categoryValue(entry, category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(medal.copy(alpha = 0.08f))
            .border(1.dp, medal.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medal
        Text(
            medalEmoji(position),
            fontSize = if (position == 0) 22.sp else 18.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.characterName,
                color = medal,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entry.race} • ${entry.characterClass}",
                color = TextSecond,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Valor
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$value",
                color = medal,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                category.displayName,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                color = medal.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Fila normal ──────────────────────────────────────────────────────────────
@Composable
private fun RankingRow(position: Int, entry: RankingEntry, category: RankingCategory) {
    val value = categoryValue(entry, category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#${position + 1}",
            color = TextSecond,
            fontSize = 13.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.characterName,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entry.race} • ${entry.characterClass}",
                color = TextSecond,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$value",
                color = Gold.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                category.displayName,
                color = TextSecond,
                fontSize = 10.sp
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────
private fun categoryValue(entry: RankingEntry, category: RankingCategory): Int = when (category) {
    RankingCategory.TOTAL_STATS   -> entry.totalStats
    RankingCategory.HP_MAX        -> entry.hpMax
    RankingCategory.STRENGTH      -> entry.fuerza.takeIf { it > 0 } ?: entry.stats["Fuerza"]       ?: 0
    RankingCategory.DEXTERITY     -> entry.destreza.takeIf { it > 0 } ?: entry.stats["Destreza"]    ?: 0
    RankingCategory.INTELLIGENCE  -> entry.inteligencia.takeIf { it > 0 } ?: entry.stats["Inteligencia"] ?: 0
    RankingCategory.WISDOM        -> entry.sabiduria.takeIf { it > 0 } ?: entry.stats["Sabiduría"]  ?: 0
    RankingCategory.CONSTITUTION  -> entry.constitucion.takeIf { it > 0 } ?: entry.stats["Constitución"] ?: 0
    RankingCategory.CHARISMA      -> entry.carisma.takeIf { it > 0 } ?: entry.stats["Carisma"]     ?: 0
}