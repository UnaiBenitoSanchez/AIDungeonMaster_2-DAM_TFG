package com.example.aidungeonmaster.ui.game

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.WorldLocation
import com.example.aidungeonmaster.data.model.WorldMapState
import com.example.aidungeonmaster.viewmodel.WorldMapViewModel

import com.example.aidungeonmaster.data.model.LocationLifeState
import java.text.Normalizer

// ── BOTÓN FLOTANTE PARA ABRIR EL MAPA ────────────────────────────────────────

@Composable
fun WorldMapFab(
    mapViewModel: WorldMapViewModel = viewModel(),
    onOpenAR: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMap by remember { mutableStateOf(false) }
    val mapState by mapViewModel.worldMapState.collectAsState()

    // Pulso animado cuando hay una nueva ubicación descubierta
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { showMap = true },
            containerColor = Color(0xFF2A1A00),
            contentColor   = Color(0xFFFFD700),
            modifier       = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector   = Icons.Default.Map,
                contentDescription = "Abrir Mapa",
                modifier      = Modifier
                    .size(28.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }

        // Badge con número de ubicaciones descubiertas
        if (mapState.locations.isNotEmpty()) {
            Badge(
                containerColor = Color(0xFFFF6B00),
                modifier       = Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp)
            ) {
                Text(
                    text  = mapState.locations.size.toString(),
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }

    if (showMap) {
        WorldMapDialog(
            mapState  = mapState,
            onDismiss = { showMap = false },
            onOpenAR  = {
                showMap = false
                onOpenAR()
            }
        )
    }
}

// ── DIALOG DEL MAPA ───────────────────────────────────────────────────────────

@Composable
fun WorldMapDialog(
    mapState: WorldMapState,
    onDismiss: () -> Unit,
    onOpenAR: () -> Unit = {}
) {
    var selectedLocation by remember(mapState.locations, mapState.currentLocationId) {
        mutableStateOf(
            mapState.locations.find { it.id == mapState.currentLocationId }
                ?: mapState.locations.find { it.isCurrentLocation }
                ?: mapState.locations.firstOrNull()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        containerColor   = Color(0xFF1A0F00),
        shape            = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = "🗺️  ${mapState.mapName}",
                    color      = Color(0xFFFFD700),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    modifier   = Modifier.weight(1f)
                )
                // ── BOTÓN AR ───────────────────────────────────────
                IconButton(
                    onClick  = onOpenAR,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("AR", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                // ─────────────────────────────────────────────────
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Cerrar", tint = Color(0xFFFFD700))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {

                if (mapState.locations.isEmpty()) {
                    EmptyMapPlaceholder()
                } else {
                    // ── MAPA INTERACTIVO ─────────────────────────────────
                    WorldMapCanvas(
                        mapState         = mapState,
                        selectedLocation = selectedLocation,
                        onLocationClick  = { selectedLocation = it },
                        modifier         = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── DETALLE DEL LUGAR SELECCIONADO ───────────────────
                    selectedLocation?.let { loc ->
                        LocationDetailCard(
                            location = loc,
                            worldState = mapState.locationStates[loc.id]
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── LEYENDA DE LUGARES VISITADOS ─────────────────────
                    LocationLegend(
                        locations = mapState.locations,
                        onLocationClick = { selectedLocation = it },
                        modifier  = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

// ── LIENZO DEL MAPA ───────────────────────────────────────────────────────────

@Composable
fun WorldMapCanvas(
    mapState: WorldMapState,
    selectedLocation: WorldLocation?,
    onLocationClick: (WorldLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    val backdrop = remember(mapState.locations, mapState.currentLocationId) {
        resolveMapBackdrop(mapState.locations)
    }

    // Pulsación animada para la posición actual del jugador
    val infiniteTransition = rememberInfiniteTransition(label = "player_pulse")
    val playerPulseRadius by infiniteTransition.animateFloat(
        initialValue   = 14f,
        targetValue    = 22f,
        animationSpec  = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label          = "player_radius"
    )
    val playerPulseAlpha by infiniteTransition.animateFloat(
        initialValue   = 0.7f,
        targetValue    = 0.0f,
        animationSpec  = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label          = "player_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(backdrop.top, backdrop.mid, backdrop.bottom)
                )
            )

            .onSizeChanged { canvasSize = it }
            .drawBehind {
                drawMapBackground(this, backdrop)
                drawMapTerrainHints(this, backdrop)
                drawMapGrid(this)
            }

            .pointerInput(mapState.locations, canvasSize) {
                detectTapGestures { tapOffset ->
                    // Detectar si el toque está cerca de alguna ubicación
                    val tapped = mapState.locations.minByOrNull { loc ->
                        val lx = loc.x * canvasSize.width
                        val ly = loc.y * canvasSize.height
                        val dx = tapOffset.x - lx
                        val dy = tapOffset.y - ly
                        dx * dx + dy * dy
                    }
                    if (tapped != null) {
                        val lx = tapped.x * canvasSize.width
                        val ly = tapped.y * canvasSize.height
                        val dx = tapOffset.x - lx
                        val dy = tapOffset.y - ly
                        val dist = Math.sqrt((dx * dx + dy * dy).toDouble())
                        if (dist < 60) onLocationClick(tapped)
                    }
                }
            }
    ) {
        // ── LÍNEAS ENTRE UBICACIONES (CAMINOS) ───────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLocationPaths(mapState.locations)

            // Pulsación del jugador
            mapState.locations.find { it.isCurrentLocation }?.let { current ->
                val cx = current.x * canvasSize.width
                val cy = current.y * canvasSize.height
                drawCircle(
                    color  = Color(0xFFFFD700).copy(alpha = playerPulseAlpha),
                    radius = playerPulseRadius,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 3f)
                )
            }
        }

        // ── MARCADORES DE UBICACIONES ─────────────────────────────────────
        mapState.locations.forEach { location ->
            val x = location.x * canvasSize.width
            val y = location.y * canvasSize.height

            LocationMarker(
                location         = location,
                isSelected       = selectedLocation?.id == location.id,
                modifier         = Modifier
                    .offset {
                        IntOffset(
                            (x - 20.dp.toPx()).toInt(),
                            (y - 20.dp.toPx()).toInt()
                        )
                    }
                    .size(40.dp)
                    .clickable { onLocationClick(location) }
            )
        }
    }
}

private data class MapBackdropPalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val accent: Color,
    val terrainType: String
)

private fun resolveMapBackdrop(locations: List<WorldLocation>): MapBackdropPalette {
    val current = locations.find { it.isCurrentLocation }?.type?.normalizeBiome()
    val dominant = current ?: locations
        .groupingBy { it.type.normalizeBiome() }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    ?: "lugar"

    return when (dominant) {
        "mar", "océano" -> MapBackdropPalette(
            top = Color(0xFF8FD3FF),
            mid = Color(0xFF3B82B8),
            bottom = Color(0xFF0E355A),
            accent = Color(0x66DFF6FF),
            terrainType = "waves"
        )
        "lago" -> MapBackdropPalette(
            top = Color(0xFFB9E7FF),
            mid = Color(0xFF4A90B8),
            bottom = Color(0xFF173D55),
            accent = Color(0x556EE7FF),
            terrainType = "lake"
        )
        "bosque" -> MapBackdropPalette(
            top = Color(0xFF5A8F5A),
            mid = Color(0xFF244B24),
            bottom = Color(0xFF102110),
            accent = Color(0x3346A35C),
            terrainType = "forest"
        )
        "desierto" -> MapBackdropPalette(
            top = Color(0xFFF1D08B),
            mid = Color(0xFFC3923C),
            bottom = Color(0xFF6B4518),
            accent = Color(0x33FFF0B2),
            terrainType = "dunes"
        )
        "montaña" -> MapBackdropPalette(
            top = Color(0xFFC8D2DC),
            mid = Color(0xFF5E6B78),
            bottom = Color(0xFF222C35),
            accent = Color(0x33E6EEF8),
            terrainType = "mountains"
        )
        "cueva", "mazmorra", "ruina" -> MapBackdropPalette(
            top = Color(0xFF4B4656),
            mid = Color(0xFF1E1C28),
            bottom = Color(0xFF08070F),
            accent = Color(0x22B084FF),
            terrainType = "cavern"
        )
        "ciudad", "pueblo", "taberna", "templo", "torre" -> MapBackdropPalette(
            top = Color(0xFFD8BA8A),
            mid = Color(0xFF8A5B2F),
            bottom = Color(0xFF2C1808),
            accent = Color(0x33FFD700),
            terrainType = "settlement"
        )
        else -> MapBackdropPalette(
            top = Color(0xFF7DAA72),
            mid = Color(0xFF355F35),
            bottom = Color(0xFF162516),
            accent = Color(0x2244AA66),
            terrainType = "plains"
        )
    }
}

private fun String.normalizeBiome(): String {
    val normalized = Normalizer.normalize(lowercase().trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    return when {
        listOf("oceano", "alta mar", "mar abierto").any { it in normalized } -> "océano"
        listOf("mar", "playa", "costa", "litoral", "bahia", "puerto", "muelle").any { it in normalized } -> "mar"
        listOf("lago", "rio", "laguna", "arroyo", "estanque").any { it in normalized } -> "lago"
        listOf("bosque", "selva", "arboleda").any { it in normalized } -> "bosque"
        listOf("desierto", "duna", "arena", "arido").any { it in normalized } -> "desierto"
        listOf("montana", "pico", "cordillera").any { it in normalized } -> "montaña"
        listOf("cueva", "gruta", "caverna", "mazmorra", "ruina").any { it in normalized } -> "cueva"
        listOf("ciudad", "pueblo", "taberna", "templo", "torre").any { it in normalized } -> "ciudad"
        else -> "lugar"
    }
}


private fun DrawScope.drawMapBackground(scope: DrawScope, palette: MapBackdropPalette) {
    val vignette = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color.Transparent,
            0.7f to Color.Transparent,
            1.0f to Color(0x66000000)
        )
    )
    scope.drawRect(brush = vignette)
}


private fun DrawScope.drawMapTerrainHints(scope: DrawScope, palette: MapBackdropPalette) {
    when (palette.terrainType) {
        "waves", "lake" -> {
            for (i in 0..5) {
                val y = size.height * (0.18f + i * 0.12f)
                scope.drawLine(
                    color = palette.accent,
                    start = Offset(size.width * 0.08f, y),
                    end = Offset(size.width * 0.92f, y + if (i % 2 == 0) 8f else -8f),
                    strokeWidth = 2f
                )
            }
        }

        "forest" -> {
            for (i in 0..8) {
                val x = size.width * (0.08f + i * 0.1f)
                scope.drawCircle(
                    color = palette.accent,
                    radius = 18f + (i % 3) * 4f,
                    center = Offset(x, size.height * 0.78f)
                )
            }
        }

        "dunes" -> {
            for (i in 0..4) {
                val y = size.height * (0.45f + i * 0.10f)
                scope.drawLine(
                    color = palette.accent,
                    start = Offset(size.width * 0.10f, y),
                    end = Offset(size.width * 0.90f, y - 18f),
                    strokeWidth = 6f
                )
            }
        }

        "mountains" -> {
            val peaks = listOf(
                Offset(size.width * 0.20f, size.height * 0.65f),
                Offset(size.width * 0.45f, size.height * 0.52f),
                Offset(size.width * 0.70f, size.height * 0.68f)
            )
            peaks.forEach { peak ->
                scope.drawLine(palette.accent, Offset(peak.x - 35f, size.height * 0.88f), peak, 5f)
                scope.drawLine(palette.accent, peak, Offset(peak.x + 35f, size.height * 0.88f), 5f)
            }
        }

        "cavern" -> {
            scope.drawCircle(
                color = palette.accent,
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.5f, size.height * 0.58f)
            )
        }

        "settlement" -> {
            for (i in 0..5) {
                val x = size.width * (0.12f + i * 0.12f)
                scope.drawRect(
                    color = palette.accent,
                    topLeft = Offset(x, size.height * 0.70f),
                    size = androidx.compose.ui.geometry.Size(18f, 28f + (i % 3) * 8f)
                )
            }
        }
    }
}

private fun DrawScope.drawMapGrid(scope: DrawScope) {
    val gridColor = Color(0x15FFFFFF)
    val step = size.width / 8f
    var x = 0f
    while (x <= size.width) {
        scope.drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
        x += step
    }
    val stepY = size.height / 6f
    var y = 0f
    while (y <= size.height) {
        scope.drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
        y += stepY
    }
}

private fun DrawScope.drawLocationPaths(locations: List<WorldLocation>) {
    if (locations.size < 2) return
    val pathColor = Color(0x40FFD700)
    for (i in 1 until locations.size) {
        val prev = locations[i - 1]
        val curr = locations[i]
        drawLine(
            color       = pathColor,
            start       = Offset(prev.x * size.width, prev.y * size.height),
            end         = Offset(curr.x * size.width, curr.y * size.height),
            strokeWidth = 2f,
            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )
    }
}

// ── MARCADOR DE UBICACIÓN ─────────────────────────────────────────────────────

@Composable
fun LocationMarker(
    location: WorldLocation,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val normalizedType = java.text.Normalizer.normalize(location.type.lowercase().trim(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    val typeColor = when {
        listOf("oceano", "alta mar", "mar abierto").any { it in normalizedType } -> Color(0xFF0D47A1)
        listOf("mar", "playa", "costa", "litoral", "bahia", "puerto", "muelle").any { it in normalizedType } -> Color(0xFF0288D1)
        listOf("lago", "rio", "laguna", "arroyo", "estanque").any { it in normalizedType } -> Color(0xFF4FC3F7)
        listOf("bosque", "selva", "arboleda").any { it in normalizedType } -> Color(0xFF2E7D32)
        listOf("cueva", "gruta", "caverna").any { it in normalizedType } -> Color(0xFF37474F)
        listOf("montana", "pico", "cordillera").any { it in normalizedType } -> Color(0xFF546E7A)
        listOf("ciudad", "metropoli", "capital").any { it in normalizedType } -> Color(0xFF1565C0)
        else -> Color(0xFF8D6E63)
    }
    val borderColor = when {
        location.isCurrentLocation -> Color(0xFFFFD700)
        isSelected                 -> Color(0xFFFF9900)
        else                       -> typeColor.copy(alpha = 0.95f)
    }
    val bgColor = when {
        location.isCurrentLocation -> Color(0x99FFD700)
        isSelected                 -> typeColor.copy(alpha = 0.45f)
        else                       -> typeColor.copy(alpha = 0.26f)
    }

    Box(
        modifier         = modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width  = if (location.isCurrentLocation || isSelected) 2.dp else 1.dp,
                color  = borderColor,
                shape  = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text     = location.icon,
            fontSize = 16.sp
        )
    }
}

// ── DETALLE DEL LUGAR SELECCIONADO ───────────────────────────────────────────

@Composable
fun LocationDetailCard(
    location: WorldLocation,
    worldState: LocationLifeState? = null
) {
    Surface(
        color  = Color(0xFF2A1800),
        shape  = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
    ) {
        Row(
            modifier           = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment  = Alignment.CenterVertically
        ) {
            Text(location.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = location.name,
                        color      = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        fontFamily = FontFamily.Serif
                    )
                    if (location.isCurrentLocation) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "📍 Aquí",
                                color    = Color.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (location.description.isNotBlank()) {
                    Text(
                        text     = location.description,
                        color    = Color(0xFFCCBBAA),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text     = location.type.replaceFirstChar { it.uppercase() },
                    color    = Color(0xFF888877),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                worldState?.let { state ->
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        color = Color(0x1100FFAA),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x3300FFAA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Estado del lugar",
                                color = Color(0xFF9BE7C4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            Text(
                                text = "Prosperidad ${state.prosperity} • Seguridad ${state.security}",
                                color = Color(0xFFCCF5E5),
                                fontSize = 10.sp
                            )

                            Text(
                                text = "Peligro ${state.danger} • Corrupción ${state.corruption}",
                                color = Color(0xFFFFC9C9),
                                fontSize = 10.sp
                            )

                            Text(
                                text = "Ánimo: ${state.mood.replaceFirstChar { it.uppercase() }}",
                                color = Color(0xFFCCBBAA),
                                fontSize = 10.sp
                            )

                            if (state.lastEventSummary.isNotBlank()) {
                                Text(
                                    text = state.lastEventSummary,
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── LEYENDA ───────────────────────────────────────────────────────────────────

@Composable
fun LocationLegend(
    locations: List<WorldLocation>,
    onLocationClick: (WorldLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text       = "Lugares visitados (${locations.size})",
            color      = Color(0xFF888877),
            fontSize   = 11.sp,
            modifier   = Modifier.padding(bottom = 4.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(locations.sortedByDescending { it.discoveredAt }) { loc ->
                Row(
                    modifier           = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (loc.isCurrentLocation) Color(0x22FFD700) else Color(0x11FFFFFF)
                        )
                        .clickable { onLocationClick(loc) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment  = Alignment.CenterVertically
                ) {
                    Text(loc.icon, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text       = loc.name,
                        color      = if (loc.isCurrentLocation) Color(0xFFFFD700) else Color(0xFFCCBBAA),
                        fontSize   = 12.sp,
                        fontWeight = if (loc.isCurrentLocation) FontWeight.Bold else FontWeight.Normal,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (loc.isCurrentLocation) {
                        Text("📍", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── PLACEHOLDER CUANDO NO HAY LUGARES ────────────────────────────────────────

@Composable
fun EmptyMapPlaceholder() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("🗺️", fontSize = 64.sp)
            Text(
                text      = "El mapa está en blanco",
                color     = Color(0xFFFFD700),
                fontSize  = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "A medida que explores el mundo, los lugares que visites irán apareciendo aquí.",
                color     = Color(0xFF888877),
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}