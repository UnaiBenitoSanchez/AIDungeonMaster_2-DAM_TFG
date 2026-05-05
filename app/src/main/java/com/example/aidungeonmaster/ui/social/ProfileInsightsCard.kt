package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aidungeonmaster.data.model.Character
import kotlin.math.roundToInt

private data class AdventurerInsights(
    val totalCharacters: Int,
    val highestLevel: Int,
    val averageLevel: Int,
    val totalCoins: Int,
    val favoriteClass: String,
    val strongestCharacterName: String,
    val strongestCharacterLevel: Int,
    val lastPlayedLabel: String,
    val activeRaces: List<String>
)

@Composable
fun AdventurerInsightsCard(
    characters: List<Character>,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val insights = buildAdventurerInsights(characters) ?: return

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.92f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Resumen del aventurero",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = insights.lastPlayedLabel,
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${insights.totalCharacters} personajes",
                        color = accentColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InsightStatBubble(
                    title = "Nivel máx.",
                    value = insights.highestLevel.toString(),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                InsightStatBubble(
                    title = "Nivel medio",
                    value = insights.averageLevel.toString(),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                InsightStatBubble(
                    title = "Monedas",
                    value = insights.totalCoins.toString(),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InsightLine(
                    label = "Clase dominante",
                    value = insights.favoriteClass,
                    accentColor = accentColor
                )
                InsightLine(
                    label = "Personaje más fuerte",
                    value = "${insights.strongestCharacterName} · Nivel ${insights.strongestCharacterLevel}",
                    accentColor = accentColor
                )
            }

            if (insights.activeRaces.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Razas activas",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        insights.activeRaces.forEach { race ->
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = race,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStatBubble(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun InsightLine(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label:",
            color = accentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun buildAdventurerInsights(characters: List<Character>): AdventurerInsights? {
    if (characters.isEmpty()) return null

    val strongest = characters.maxByOrNull { characterStrengthScore(it) } ?: return null
    val favoriteClass = characters
        .groupingBy { it.characterClass.ifBlank { "Sin clase" } }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        ?.key
        ?: "Sin clase"

    val recentCharacter = characters.maxByOrNull { it.lastPlayed }
    val activeRaces = characters
        .map { it.race.ifBlank { "Desconocida" } }
        .distinct()
        .sorted()
        .take(4)

    return AdventurerInsights(
        totalCharacters = characters.size,
        highestLevel = characters.maxOf { it.level },
        averageLevel = characters.map { it.level }.average().roundToInt(),
        totalCoins = characters.sumOf { it.coins },
        favoriteClass = favoriteClass,
        strongestCharacterName = strongest.name.ifBlank { "Aventurero" },
        strongestCharacterLevel = strongest.level,
        lastPlayedLabel = buildLastPlayedLabel(recentCharacter?.lastPlayed ?: 0L),
        activeRaces = activeRaces
    )
}

private fun characterStrengthScore(character: Character): Int {
    val statScore = character.finalStats.values.sum()
    val equipmentScore = character.equipment.allEquipped().size * 5
    val inventoryScore = character.inventory.size
    return (character.level * 100) + statScore + equipmentScore + inventoryScore
}

private fun buildLastPlayedLabel(lastPlayed: Long): String {
    if (lastPlayed <= 0L) return "Todavía no hay actividad reciente"

    val elapsed = System.currentTimeMillis() - lastPlayed
    return when {
        elapsed < 60_000L -> "Activo hace un momento"
        elapsed < 3_600_000L -> "Activo hace ${elapsed / 60_000L} min"
        elapsed < 86_400_000L -> "Activo hace ${elapsed / 3_600_000L} h"
        else -> "Activo hace ${elapsed / 86_400_000L} d"
    }
}