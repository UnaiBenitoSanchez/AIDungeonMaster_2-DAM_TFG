package com.example.aidungeonmaster.ui.home

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de dropdown.
fun Dropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

/**
 * Fila de atributo.
 * [diceRolls] lista de los 4 dados tirados (vacía si modo manual).
 * Cuando hay dados muestra los resultados en pequeño con el menor tachado.
 */
@Composable
// Ejecuta la lógica de stat row.
fun StatRow(
    name: String,
    value: Int,
    isPlus2: Boolean,
    isPlus1: Boolean,
    onStatChange: (Int) -> Unit,
    onTogglePlus2: () -> Unit,
    onTogglePlus1: () -> Unit,
    diceRolls: List<Int> = emptyList(),
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name.take(3).uppercase(),
                Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp
            )
            IconButton(onClick = { if (value > 3) onStatChange(value - 1) }, enabled = enabled, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Disminuir", modifier = Modifier.size(16.dp))
            }
            Text(
                "$value",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(26.dp),
                fontSize = 14.sp
            )
            IconButton(onClick = { if (value < 18) onStatChange(value + 1) }, enabled = enabled, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.weight(1f))
            FilterChip(selected = isPlus2, onClick = onTogglePlus2, label = { Text("+2", fontSize = 11.sp) }, enabled = enabled)
            Spacer(Modifier.width(4.dp))
            FilterChip(selected = isPlus1, onClick = onTogglePlus1, label = { Text("+1", fontSize = 11.sp) }, enabled = enabled)
        }

        // Mostrar dados tirados debajo del stat
        if (diceRolls.isNotEmpty()) {
            val minRoll = diceRolls.min()
            var minUsed = false   // para tachar solo una vez el menor
            Row(Modifier.padding(start = 40.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                diceRolls.forEach { roll ->
                    val isMin = roll == minRoll && !minUsed
                    if (isMin) minUsed = true
                    Text(
                        text = "[$roll]",
                        fontSize = 10.sp,
                        color = if (isMin) Color.Red.copy(alpha = 0.6f) else Color(0xFF81C784)
                    )
                }
                Text("= $value", fontSize = 10.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            }
        }
    }
}
