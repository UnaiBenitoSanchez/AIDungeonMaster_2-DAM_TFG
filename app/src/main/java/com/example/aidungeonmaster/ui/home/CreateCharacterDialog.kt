package com.example.aidungeonmaster.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun CreateCharacterDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Map<String, Int>, String) -> Unit,
    isGenerating: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var race by remember { mutableStateOf("Humano") }
    var clazz by remember { mutableStateOf("Guerrero") }
    var physicalTraits by remember { mutableStateOf("") }
    var subclazz by remember { mutableStateOf("") }

    val statNames = listOf("Fuerza", "Destreza", "Constitución", "Inteligencia", "Sabiduría", "Carisma")
    var selectedStats by remember { mutableStateOf(statNames.associateWith { 10 }.toMutableMap()) }
    var bonusPlus2 by remember { mutableStateOf<String?>(null) }
    var bonusPlus1 by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && physicalTraits.isNotBlank() && !isGenerating,
                onClick = {
                    val finalStats = selectedStats.mapValues { (statName, value) ->
                        var total = value
                        if (statName == bonusPlus2) total += 2
                        if (statName == bonusPlus1) total += 1
                        total
                    }
                    onCreate(name, race, clazz, finalStats, physicalTraits)
                }
            ) {
                if (isGenerating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("Generando...")
                    }
                } else {
                    Text("Generar Personaje")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancelar")
            }
        },
        title = {
            Text("Nuevo Aventurero", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // SECCIÓN 1: DATOS BÁSICOS
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del héroe") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating
                )

                Spacer(Modifier.height(12.dp))

                // Definición de la estructura de datos para Clases y Subclases
                val subclassesByClass = mapOf(
                    "Artífice" to listOf("Alquimista", "Armero", "Artillero", "Herrero de Batalla"),
                    "Bárbaro" to listOf("Senda del Berserker", "Senda del Guerrero Totémico", "Senda de la Magia Salvaje", "Senda del Guardián Ancestral"),
                    "Bardo" to listOf("Colegio del Conocimiento", "Colegio del Valor", "Colegio de la Elocuencia", "Colegio de las Espadas"),
                    "Brujo" to listOf("El Archihada", "El Primordial", "建筑师", "El Celestial", "El Genio", "El Hexblade"),
                    "Clérigo" to listOf("Dominio del Conocimiento", "Dominio de la Vida", "Dominio de la Luz", "Dominio de la Naturaleza", "Dominio de la Tempestad", "Dominio del Engaño", "Dominio de la Guerra"),
                    "Druida" to listOf("Círculo de la Tierra", "Círculo de la Luna", "Círculo de los Sueños", "Círculo del Pastor", "Círculo de las Esporas"),
                    "Explorador" to listOf("Cazador", "Maestro de Bestias", "Acechador de las Sombras", "Cazador de Monstruos", "Guardián del Enjambre"),
                    "Guerrero" to listOf("Campeón", "Maestro de Batalla", "Caballero Eldritch", "Estratega", "Caballero Eco"),
                    "Hechicero" to listOf("Linaje Dracónico", "Magia Salvaje", "Alma Favorecida", "Mente Aberrante", "Magia de Relojería"),
                    "Mago" to listOf("Escuela de Abjuración", "Escuela de Conjuración", "Escuela de Adivinación", "Escuela de Evocación", "Escuela de Ilusión", "Escuela de Nigromancia", "Escuela de Transmutación", "Escuela de Encantamiento"),
                    "Monje" to listOf("Camino de la Mano Abierta", "Camino de la Sombra", "Camino de los Cuatro Elementos", "Camino del Maestro Borracho", "Camino Kensei"),
                    "Paladín" to listOf("Juramento de Devoción", "Juramento de los Antiguos", "Juramento de Venganza", "Juramento de Conquista", "Juramento de Redención"),
                    "Pícaro" to listOf("Ladrón", "Asesino", "Embaucador Arcano", "Espadachín", "Inquisitivo", "Explorador")
                )

                // Interfaz de usuario
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            Dropdown(
                                label = "Raza",
                                options = listOf(
                                    // Manual del Jugador
                                    "Dracónidos", "Elfos", "Elfo oscuro", "Enanos", "Gnomos", "Humanos", "Medianos", "Semielfos", "Semiorcos", "Tiflin",
                                    // Elemental Evil
                                    "Aarakocras", "Genasi", "Goliats",
                                    // Guía de Monstruos de Volo
                                    "Aasimar", "Firbolgs", "Goblins", "Hobgoblins", "Hombres lagarto", "Kenkus", "Kobolds", "Orcos", "Osgos", "Tabaxis", "Tritones", "Yuan-Ti Purasangres",
                                    // Guildmasters' Guide to Ravnica
                                    "Centauros", "Híbridos Simic", "Loxodon", "Minotauros", "Vedalken",
                                    // Eberron: Rising from the Last War
                                    "Cambiantes", "Forjados", "Kalashtar", "Mutadores", "Orcos de Eberron",
                                    // Otras fuentes y suplementos
                                    "Gith", "Grungs", "Huecos", "Locathah", "Tortogas", "Verdan", "Chico Slime"
                                ),
                                selected = race,
                                onSelect = { race = it },
                                enabled = !isGenerating
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            Dropdown(
                                label = "Clase",
                                options = subclassesByClass.keys.toList(),
                                selected = clazz,
                                onSelect = {
                                    clazz = it
                                    subclazz = ""
                                },
                                enabled = !isGenerating
                            )
                        }
                    }

                    // Desplegable de Subclase (solo se habilita si hay una clase seleccionada)
                    Box(Modifier.fillMaxWidth()) {
                        Dropdown(
                            label = "Subclase",
                            options = subclassesByClass[clazz] ?: listOf("Selecciona una clase primero"),
                            selected = subclazz,
                            onSelect = { subclazz = it },
                            enabled = !isGenerating && clazz.isNotEmpty()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // SECCIÓN 2: RASGOS FÍSICOS (Para la IA)
                Text("Apariencia Física 🎨", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Describe cómo se ve tu personaje. Esto se usará para generar su imagen con IA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = physicalTraits,
                    onValueChange = { physicalTraits = it },
                    placeholder = { Text("Ej: Joven, cicatriz en el ojo, armadura dorada, pelo largo plateado...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    enabled = !isGenerating
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // SECCIÓN 3: ESTADÍSTICAS (Estilo BG3)
                Text("Atributos ⚔️", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ajusta los valores entre 8-15 y asigna bonos (+2 y +1)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))

                statNames.forEach { stat ->
                    StatRow(
                        name = stat,
                        value = selectedStats[stat] ?: 10,
                        isPlus2 = bonusPlus2 == stat,
                        isPlus1 = bonusPlus1 == stat,
                        onStatChange = { newVal ->
                            val nextMap = selectedStats.toMutableMap()
                            nextMap[stat] = newVal
                            selectedStats = nextMap
                        },
                        onTogglePlus2 = {
                            bonusPlus2 = if (bonusPlus2 == stat) null else stat
                            if (bonusPlus1 == stat) bonusPlus1 = null
                        },
                        onTogglePlus1 = {
                            bonusPlus1 = if (bonusPlus1 == stat) null else stat
                            if (bonusPlus2 == stat) bonusPlus2 = null
                        },
                        enabled = !isGenerating
                    )
                }

                if (isGenerating) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                "Generando imagen con IA...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun StatRow(
    name: String,
    value: Int,
    isPlus2: Boolean,
    isPlus1: Boolean,
    onStatChange: (Int) -> Unit,
    onTogglePlus2: () -> Unit,
    onTogglePlus1: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name.take(3).uppercase(),
            Modifier.width(40.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        IconButton(
            onClick = { if (value > 8) onStatChange(value - 1) },
            enabled = enabled
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Disminuir", modifier = Modifier.size(16.dp))
        }

        Text(
            "$value",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )

        IconButton(
            onClick = { if (value < 15) onStatChange(value + 1) },
            enabled = enabled
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.weight(1f))

        // Botones de bono estilo BG3
        FilterChip(
            selected = isPlus2,
            onClick = onTogglePlus2,
            label = { Text("+2", fontSize = 12.sp) },
            enabled = enabled
        )
        Spacer(Modifier.width(4.dp))
        FilterChip(
            selected = isPlus1,
            onClick = onTogglePlus1,
            label = { Text("+1", fontSize = 12.sp) },
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}