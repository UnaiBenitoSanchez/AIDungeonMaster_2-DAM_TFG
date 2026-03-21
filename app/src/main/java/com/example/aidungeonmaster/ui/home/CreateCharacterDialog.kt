package com.example.aidungeonmaster.ui.home

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidungeonmaster.utils.ImageUtils
import kotlinx.coroutines.launch

// ── DADOS D&D — 4d6 DESCARTA EL MENOR ───────────────────────────────────────
private fun rollDnDStat(): Pair<Int, List<Int>> {
    val rolls = List(4) { (1..6).random() }
    val total = rolls.sum() - rolls.min()
    return total to rolls
}

private fun rollAllStats(statNames: List<String>): Map<String, Pair<Int, List<Int>>> =
    statNames.associateWith { rollDnDStat() }

// ── ESTADOS DEL RETRATO ──────────────────────────────────────────────────────
private sealed class PortraitState {
    object Idle    : PortraitState()
    object Loading : PortraitState()
    data class Ready(val bitmap: Bitmap, val base64: String) : PortraitState()
    data class Failed(val reason: String) : PortraitState()
}

// ── DIÁLOGO PRINCIPAL ────────────────────────────────────────────────────────
@Composable
fun CreateCharacterDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, race: String, clazz: String, stats: Map<String, Int>, traits: String, portraitUrl: String) -> Unit,
    isGenerating: Boolean = false
) {
    var name           by remember { mutableStateOf("") }
    var race           by remember { mutableStateOf("Humano") }
    var clazz          by remember { mutableStateOf("Guerrero") }
    var subclazz       by remember { mutableStateOf("") }
    var physicalTraits by remember { mutableStateOf("") }

    val statNames = listOf("Fuerza", "Destreza", "Constitución", "Inteligencia", "Sabiduría", "Carisma")
    var diceResults    by remember { mutableStateOf(statNames.associateWith { 10 to emptyList<Int>() }) }
    var bonusPlus2     by remember { mutableStateOf<String?>(null) }
    var bonusPlus1     by remember { mutableStateOf<String?>(null) }
    var diceModeActive by remember { mutableStateOf(false) }

    var portraitState  by remember { mutableStateOf<PortraitState>(PortraitState.Idle) }
    val scope          = rememberCoroutineScope()

    val canGenerate = race.isNotBlank() && clazz.isNotBlank() && physicalTraits.isNotBlank()

    val subclassesByClass = mapOf(
        "Artífice"               to listOf("Alquimista","Armero","Artillero","Herrero de Batalla"),
        "Bardo"                  to listOf("Colegio de la Elocuencia","Colegio de las Espadas","Colegio del Conocimiento","Colegio del Valor"),
        "Bárbaro"                to listOf("Senda de la Magia Salvaje","Senda del Berserker","Senda del Guardián Ancestral","Senda del Guerrero Totémico"),
        "Brujo"                  to listOf("El Archihada","El Celestial","El Genio","El Hexblade","El Primordial"),
        "Caballero de la Muerte" to listOf("Caballero del Ocaso","Jinete Sombrío","Nigromante de Batalla","Señor de la Muerte"),
        "Chamán"                 to listOf("Chamán de la Tierra","Chamán de la Tormenta","Chamán de los Espíritus","Chamán del Fuego"),
        "Clérigo"                to listOf("Dominio de la Guerra","Dominio de la Luz","Dominio de la Naturaleza","Dominio de la Tempestad","Dominio de la Vida","Dominio del Conocimiento","Dominio del Engaño"),
        "Corsario"               to listOf("Capitán del Abismo","Cazaprimas","Corsario del Viento","Pistolero"),
        "Druida"                 to listOf("Círculo de la Luna","Círculo de la Tierra","Círculo de las Esporas","Círculo de los Sueños","Círculo del Pastor"),
        "Exorcista"              to listOf("Cazador de Almas","Exorcista Oscuro","Purificador Sagrado","Sellador del Vacío"),
        "Explorador"             to listOf("Acechador de las Sombras","Cazador","Cazador de Monstruos","Guardián del Enjambre","Maestro de Bestias"),
        "Guerrero"               to listOf("Caballero Eco","Caballero Eldritch","Campeón","Estratega","Maestro de Batalla"),
        "Hechicero"              to listOf("Alma Favorecida","Linaje Dracónico","Magia de Relojería","Magia Salvaje","Mente Aberrante"),
        "Mago"                   to listOf("Escuela de Abjuración","Escuela de Adivinación","Escuela de Conjuración","Escuela de Encantamiento","Escuela de Evocación","Escuela de Ilusión","Escuela de Nigromancia","Escuela de Transmutación"),
        "Monje"                  to listOf("Camino de la Mano Abierta","Camino de la Sombra","Camino de los Cuatro Elementos","Camino del Maestro Borracho","Camino Kensei"),
        "Paladín"                to listOf("Juramento de Conquista","Juramento de Devoción","Juramento de Redención","Juramento de Venganza","Juramento de los Antiguos"),
        "Pícaro"                 to listOf("Asesino","Embaucador Arcano","Espadachín","Explorador","Inquisitivo","Ladrón")
    )

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && physicalTraits.isNotBlank() && !isGenerating,
                onClick = {
                    val finalStats = diceResults.mapValues { (statName, pair) ->
                        var total = pair.first
                        if (statName == bonusPlus2) total += 2
                        if (statName == bonusPlus1) total += 1
                        total
                    }
                    val base64 = (portraitState as? PortraitState.Ready)?.base64 ?: ""
                    onCreate(name, race, clazz, finalStats, physicalTraits, base64)
                }
            ) {
                if (isGenerating) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Guardando...")
                    }
                } else {
                    Text("Crear Personaje")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) { Text("Cancelar") }
        },
        title = { Text("Nuevo Aventurero ⚔️", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

                // ── NOMBRE ───────────────────────────────────────────────────
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre del héroe") },
                    modifier = Modifier.fillMaxWidth(), enabled = !isGenerating
                )
                Spacer(Modifier.height(12.dp))

                // ── RAZA / CLASE / SUBCLASE ──────────────────────────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        Dropdown(
                            label = "Raza",
                            options = listOf(
                                "Aarakocras","Aasimar","Cambiantes","Centauro","Chico pollo","Chico Slime",
                                "Deidad","Demonio","Dracónidos","Elemental","Elfo oscuro","Elfos","Enanos",
                                "Espectro","Espíritu","Etergénito","Firbolgs","Forjados","Genasi","Gith",
                                "Gnomos","Goblins","Golem","Goliats","Grungs","Híbridos Simic","Hobgoblins",
                                "Hombre lobo","Hombres lagarto","Humanos","Huecos","Ilusión","Kalashtar",
                                "Kenkus","Kobolds","Locathah","Loxodon","Medianos","Minotauros","Mutadores",
                                "Orcos","Orcos de Eberron","Osgos","Polimorfo","Quimera","Rápido","Semielfos",
                                "Semiorcos","Sátiro","Tabaxis","Tiflin","Tortogas","Trasgo","Tritones",
                                "Vedalken","Verdan","Vampiro","Yuan-Ti Purasangres","Zombie"
                            ),
                            selected = race,
                            onSelect = { race = it; portraitState = PortraitState.Idle },
                            enabled = !isGenerating
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        Dropdown(
                            label = "Clase",
                            options = subclassesByClass.keys.toList(),
                            selected = clazz,
                            onSelect = { clazz = it; subclazz = ""; portraitState = PortraitState.Idle },
                            enabled = !isGenerating
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Dropdown(
                    label = "Subclase",
                    options = subclassesByClass[clazz] ?: listOf("Selecciona una clase primero"),
                    selected = subclazz,
                    onSelect = { subclazz = it },
                    enabled = !isGenerating && clazz.isNotEmpty()
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // ── APARIENCIA + RETRATO IA ──────────────────────────────────
                Text("Apariencia Física 🎨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Describe cómo se ve tu personaje. La IA generará su retrato.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = physicalTraits,
                    onValueChange = { physicalTraits = it; portraitState = PortraitState.Idle },
                    placeholder = { Text("Ej: Joven con cicatriz en el ojo, pelo largo plateado, armadura dorada...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    enabled = !isGenerating
                )
                Spacer(Modifier.height(10.dp))

                // ── BOTÓN GENERAR RETRATO ────────────────────────────────────
                if (canGenerate) {
                    val isLoading = portraitState is PortraitState.Loading
                    Button(
                        onClick = {
                            portraitState = PortraitState.Loading
                            scope.launch {
                                try {
                                    val base64 = ImageUtils.generatePortraitBase64(race, clazz, physicalTraits)
                                    val bitmap = ImageUtils.base64ToBitmap(base64)
                                    portraitState = PortraitState.Ready(bitmap, base64)
                                } catch (e: Exception) {
                                    Log.e("PORTRAIT", "Error generando retrato: ${e.message}")
                                    portraitState = PortraitState.Failed(e.message ?: "Error desconocido")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
                    ) {
                        if (isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Text("Pintando el retrato...", color = Color.White, fontSize = 13.sp)
                            }
                        } else {
                            val label = if (portraitState is PortraitState.Ready) "🔄 Regenerar Retrato" else "🖼️ Generar Retrato con IA"
                            Text(label, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── PANEL DE RESULTADO DEL RETRATO ──────────────────────────
                when (val state = portraitState) {

                    is PortraitState.Ready -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFF7B1FA2), RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A0030)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = state.bitmap.asImageBitmap(),
                                contentDescription = "Retrato del personaje",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pulsa el botón para regenerar un retrato diferente",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is PortraitState.Failed -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3E0000)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("⚠️ Error al generar el retrato", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(state.reason, color = Color(0xFFFF8A80), fontSize = 11.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Puedes crear el personaje igualmente sin retrato, o volver a intentarlo.",
                                    color = Color.Gray, fontSize = 11.sp
                                )
                            }
                        }
                    }

                    is PortraitState.Loading -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0030)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF7B1FA2)
                                )
                                Column {
                                    Text("Generando retrato con IA...", color = Color(0xFFCE93D8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Puede tardar entre 30 y 90 segundos. Por favor espera.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    else -> Unit
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // ── ATRIBUTOS D&D ────────────────────────────────────────────
                Text("Atributos ⚔️", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2F)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Sistema D&D — 4d6 descarta el menor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFD700))
                        Text("Tira 4 dados de 6, descarta el resultado más bajo y suma los 3 restantes.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            diceResults = rollAllStats(statNames)
                            diceModeActive = true; bonusPlus2 = null; bonusPlus1 = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        enabled = !isGenerating
                    ) { Text("🎲 Tirar Dados", fontSize = 13.sp) }

                    if (diceModeActive) {
                        OutlinedButton(
                            onClick = {
                                diceResults = statNames.associateWith { 10 to emptyList() }
                                diceModeActive = false; bonusPlus2 = null; bonusPlus1 = null
                            },
                            modifier = Modifier.weight(1f), enabled = !isGenerating
                        ) { Text("↩ Manual", fontSize = 13.sp) }
                    }
                }
                Spacer(Modifier.height(8.dp))

                statNames.forEach { stat ->
                    val (value, rolls) = diceResults[stat] ?: (10 to emptyList())
                    StatRow(
                        name = stat, value = value,
                        isPlus2 = bonusPlus2 == stat, isPlus1 = bonusPlus1 == stat,
                        diceRolls = rolls,
                        onStatChange = { newVal ->
                            diceResults = diceResults.toMutableMap().also { it[stat] = newVal to emptyList() }
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

                if (diceModeActive) {
                    Spacer(Modifier.height(8.dp))
                    val total   = diceResults.values.sumOf { it.first }
                    val average = if (diceResults.isNotEmpty()) total / diceResults.size else 0
                    val quality = when {
                        average >= 13 -> "🌟 Excelente"
                        average >= 11 -> "✅ Buena"
                        average >= 9  -> "⚠️ Normal"
                        else           -> "💀 Mala suerte"
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2615)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Suma: $total", color = Color(0xFF81C784), fontSize = 12.sp)
                            Text("Media: $average", color = Color(0xFF81C784), fontSize = 12.sp)
                            Text(quality, color = Color(0xFF81C784), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Asigna los bonos raciales: +2 y +1 a los atributos que prefieras.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
            }
        }
    )
}