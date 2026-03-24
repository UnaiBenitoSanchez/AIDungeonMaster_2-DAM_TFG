package com.example.aidungeonmaster.ui.achievements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidungeonmaster.data.model.Achievement
import com.example.aidungeonmaster.data.model.AchievementCategory
import com.example.aidungeonmaster.data.model.Quest
import com.example.aidungeonmaster.data.model.QuestStatus
import com.example.aidungeonmaster.viewmodel.AchievementViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Colores temáticos ─────────────────────────────────────────────────────────
private val Gold       = Color(0xFFFFD700)
private val DarkBrown  = Color(0xFF2C1A0E)
private val ParchmentBg = Color(0xFF1A120B)
private val CardLocked  = Color(0xFF2A2A2A)
private val CardUnlocked = Color(0xFF3D2B1A)
private val GreenSuccess = Color(0xFF4CAF50)
private val OrangeProgress = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementViewModel,
    onBack: () -> Unit
) {
    val achievements by viewModel.achievements.collectAsState()
    val quests       by viewModel.quests.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }

    val tabs = listOf("Logros", "Misiones")

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Salón de la Fama",
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBrown
                )
            )
        },
        containerColor = ParchmentBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header con estadísticas ───────────────────────────────────────
            AchievementStatsHeader(
                unlockedCount    = viewModel.unlockedCount,
                totalCount       = viewModel.totalCount,
                completedQuests  = viewModel.completedQuestCount,
                totalQuests      = quests.size
            )

            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = DarkBrown,
                contentColor     = Gold
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> AchievementsTab(
                    achievements     = achievements,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = if (selectedCategory == it) null else it }
                )
                1 -> QuestsTab(
                    quests    = quests,
                    onAccept  = { viewModel.acceptQuest(it) }
                )
            }
        }
    }
}

// ── Stats header ──────────────────────────────────────────────────────────────

@Composable
private fun AchievementStatsHeader(
    unlockedCount: Int,
    totalCount: Int,
    completedQuests: Int,
    totalQuests: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBrown)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatPill(emoji = "🏆", label = "Logros", value = "$unlockedCount/$totalCount")
        StatPill(emoji = "📜", label = "Misiones", value = "$completedQuests/$totalQuests")
        val pct = if (totalCount > 0) (unlockedCount * 100 / totalCount) else 0
        StatPill(emoji = "⭐", label = "Completado", value = "$pct%")
    }
}

@Composable
private fun StatPill(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

// ── Pestaña Logros ────────────────────────────────────────────────────────────

@Composable
private fun AchievementsTab(
    achievements: List<Achievement>,
    selectedCategory: AchievementCategory?,
    onCategorySelect: (AchievementCategory) -> Unit
) {
    val filtered = if (selectedCategory == null) achievements
    else achievements.filter { it.category == selectedCategory }

    Column {
        // Filtros de categoría
        LazyRow(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AchievementCategory.entries) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick  = { onCategorySelect(cat) },
                    label = { Text("${cat.emoji} ${cat.label}", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor     = Gold.copy(alpha = 0.3f),
                        selectedLabelColor         = Gold,
                        containerColor             = CardLocked,
                        labelColor                 = Color.LightGray
                    )
                )
            }
        }

        val unlocked = filtered.count { it.isUnlocked }
        Text(
            "$unlocked/${filtered.size} desbloqueados",
            color     = Color.Gray,
            fontSize  = 12.sp,
            modifier  = Modifier.padding(horizontal = 16.dp)
        )

        LazyColumn(
            modifier              = Modifier.fillMaxSize(),
            contentPadding        = PaddingValues(12.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            // Primero los desbloqueados
            val sortedAchievements = filtered.sortedByDescending { it.isUnlocked }
            items(sortedAchievements, key = { it.id }) { achievement ->
                AchievementCard(achievement)
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val bgColor by animateColorAsState(
        targetValue = if (achievement.isUnlocked) CardUnlocked else CardLocked,
        animationSpec = tween(400),
        label = "achiev_bg"
    )
    val borderColor = if (achievement.isUnlocked) Gold else Color.DarkGray
    val alpha = if (achievement.isUnlocked) 1f else 0.55f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (achievement.isUnlocked) Gold.copy(alpha = 0.2f)
                        else Color.DarkGray.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked || !achievement.isSecret) {
                    Text(achievement.emoji, fontSize = 26.sp, textAlign = TextAlign.Center)
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (achievement.isSecret && !achievement.isUnlocked) "???" else achievement.title,
                        color      = if (achievement.isUnlocked) Gold else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                    if (achievement.isUnlocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint     = GreenSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    if (achievement.isSecret && !achievement.isUnlocked) "Logro secreto — ¡Descúbrelo!"
                    else achievement.description,
                    color    = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (achievement.isUnlocked && achievement.unlockedAt > 0L) {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(achievement.unlockedAt))
                    Text("Desbloqueado el $date", color = Color.Gray, fontSize = 10.sp)
                }
            }

            // XP reward
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${achievement.xpReward}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("XP", color = Gold.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

// ── Pestaña Misiones ──────────────────────────────────────────────────────────

@Composable
private fun QuestsTab(quests: List<Quest>, onAccept: (String) -> Unit) {
    val inProgress = quests.filter { it.status == QuestStatus.IN_PROGRESS }
    val available  = quests.filter { it.status == QuestStatus.AVAILABLE }
    val completed  = quests.filter { it.status == QuestStatus.COMPLETED }

    LazyColumn(
        modifier              = Modifier.fillMaxSize(),
        contentPadding        = PaddingValues(12.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        if (inProgress.isNotEmpty()) {
            item {
                SectionHeader("⚡ En Progreso", OrangeProgress)
            }
            items(inProgress, key = { it.id }) { quest ->
                QuestCard(quest = quest, onAccept = onAccept)
            }
        }

        if (available.isNotEmpty()) {
            item { SectionHeader("📋 Disponibles", Color.LightGray) }
            items(available, key = { it.id }) { quest ->
                QuestCard(quest = quest, onAccept = onAccept)
            }
        }

        if (completed.isNotEmpty()) {
            item { SectionHeader("✅ Completadas", GreenSuccess) }
            items(completed, key = { it.id }) { quest ->
                QuestCard(quest = quest, onAccept = onAccept)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        title,
        color      = color,
        fontWeight = FontWeight.Bold,
        fontSize   = 14.sp,
        modifier   = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun QuestCard(quest: Quest, onAccept: (String) -> Unit) {
    val isCompleted  = quest.status == QuestStatus.COMPLETED
    val isInProgress = quest.status == QuestStatus.IN_PROGRESS

    val borderColor = when (quest.status) {
        QuestStatus.COMPLETED   -> GreenSuccess
        QuestStatus.IN_PROGRESS -> OrangeProgress
        else                    -> Color.DarkGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF1A2E1A) else CardLocked
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header de la misión
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(quest.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(quest.description, color = Color.LightGray, fontSize = 12.sp)
                }
                // XP reward
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+${quest.xpReward}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("XP", color = Gold.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Objetivos
            quest.objectives.forEach { obj ->
                ObjectiveRow(obj.description, obj.currentValue, obj.targetValue, obj.isCompleted)
                Spacer(Modifier.height(6.dp))
            }

            // Barra de progreso global
            if (isInProgress && quest.objectives.size > 1) {
                LinearProgressIndicator(
                    progress          = { quest.overallProgress },
                    modifier          = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color             = OrangeProgress,
                    trackColor        = Color.DarkGray,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(quest.overallProgress * 100).toInt()}% completado",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // Botón aceptar
            if (quest.status == QuestStatus.AVAILABLE) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onAccept(quest.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Gold.copy(alpha = 0.85f))
                ) {
                    Text("Aceptar Misión", color = DarkBrown, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ObjectiveRow(description: String, current: Int, target: Int, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (done) "✅" else "🔲",
            fontSize = 14.sp,
            modifier = Modifier.width(24.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(description, color = Color.LightGray, fontSize = 12.sp)
            LinearProgressIndicator(
                progress   = { (current.toFloat() / target).coerceIn(0f, 1f) },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color      = if (done) GreenSuccess else OrangeProgress,
                trackColor = Color.DarkGray,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$current/$target",
            color    = if (done) GreenSuccess else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}