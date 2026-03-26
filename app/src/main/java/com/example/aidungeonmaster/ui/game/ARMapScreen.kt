package com.example.aidungeonmaster.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.aidungeonmaster.data.model.WorldLocation
import com.example.aidungeonmaster.data.model.WorldMapState
import com.example.aidungeonmaster.viewmodel.WorldMapViewModel
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.android.filament.Colors
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

// ── CONSTANTES ────────────────────────────────────────────────────────────────

/** Tamaño del mapa en metros (el mapa ocupa MAP_SCALE × MAP_SCALE metros en el mundo real) */
private const val MAP_SCALE = 0.8f

/** Radio de las esferas 3D que representan cada ubicación */
private const val SPHERE_RADIUS = 0.025f

/** Altura a la que flotan las esferas sobre el plano detectado */
private const val SPHERE_HEIGHT = 0.05f

// ── PANTALLA PRINCIPAL DE AR ──────────────────────────────────────────────────

/**
 * Pantalla de realidad aumentada que proyecta el mapa del mundo sobre
 * una superficie plana detectada por ARCore.
 *
 * Flujo de usuario:
 * 1. La cámara se abre y busca superficies planas (planeRenderer activo).
 * 2. En cuanto detecta un plano horizontal, coloca automáticamente el mapa.
 * 3. Cada ubicación aparece como una esfera 3D flotando sobre el plano.
 *    - Ubicación actual → esfera dorada (pulsante en la UI 2D overlay).
 *    - Otras ubicaciones → esfera azul-plateada.
 * 4. Al tocar una esfera se muestra la tarjeta de detalle en la parte inferior.
 * 5. El botón ↩ vuelve al juego. El botón 🔄 resetea el mapa para reposicionarlo.
 *
 * @param mapState      Estado del mapa con todas las ubicaciones descubiertas.
 * @param onBack        Callback para volver a la pantalla anterior.
 */
@Composable
fun ARMapScreen(
    mapState: WorldMapState,
    onBack: () -> Unit,
    onOpen3DGallery: () -> Unit = {}
) {
    // ── SceneView / Filament ───────────────────────────────────────────────
    val engine         = rememberEngine()
    val modelLoader    = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes     = rememberNodes()
    val view           = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)

    // ── Estado de la sesión AR ─────────────────────────────────────────────
    var isMapPlaced             by remember { mutableStateOf(false) }
    var selectedLocation        by remember { mutableStateOf<WorldLocation?>(null) }
    var trackingFailureReason   by remember { mutableStateOf<TrackingFailureReason?>(null) }

    // ── Interceptar el botón atrás del sistema ANTES que SceneView ───────
    BackHandler { onBack() }

    // ── Animación de pulso para el indicador de escaneo ───────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "ar_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "pulse_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── FONDO TEMÁTICO (visible mientras ARCore inicializa la cámara) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A0F00),
                            Color(0xFF0D0700),
                            Color(0xFF060300)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗺️", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = "Iniciando cámara AR…",
                    color     = Color(0xFF888877),
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // ── ESCENA AR (fondo completo) ─────────────────────────────────────
        ARScene(
            modifier   = Modifier.fillMaxSize(),
            engine     = engine,
            view       = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            childNodes = childNodes,
            // Mostrar el renderizador de planos solo mientras no se ha colocado el mapa
            planeRenderer = !isMapPlaced,
            sessionConfiguration = { session, config ->
                // Profundidad automática si el dispositivo la soporta
                config.depthMode =
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                        Config.DepthMode.AUTOMATIC
                    else
                        Config.DepthMode.DISABLED
                // Placement instantáneo para mayor velocidad de detección
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                // Solo planos horizontales (mesas, suelo)
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onTrackingFailureChanged = { reason ->
                trackingFailureReason = reason
            },
            onSessionUpdated = { _, frame ->
                // Cuando aún no se ha colocado el mapa, buscar el primer plano horizontal
                if (!isMapPlaced && childNodes.isEmpty() && mapState.locations.isNotEmpty()) {
                    frame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { plane ->
                            buildARMap(
                                plane          = plane,
                                locations      = mapState.locations,
                                engine         = engine,
                                materialLoader = materialLoader,
                                childNodes     = childNodes
                            )
                            isMapPlaced = true
                        }
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { _, node ->
                    // Seleccionar la ubicación asociada al nodo tocado (por nombre)
                    val tapped = mapState.locations.firstOrNull { it.id == node?.name }
                    selectedLocation =
                        if (selectedLocation?.id == tapped?.id) null else tapped
                }
            )
        )

        // ── BARRA SUPERIOR ─────────────────────────────────────────────────
        ARTopBar(
            title      = mapState.mapName,
            isMapPlaced = isMapPlaced,
            trackingFailureReason = trackingFailureReason,
            pulseAlpha  = pulseAlpha,
            onBack      = onBack,
            onReset     = {
                childNodes.clear()
                selectedLocation = null
                isMapPlaced = false
            },
            onOpen3DGallery = onOpen3DGallery,
            modifier    = Modifier.align(Alignment.TopCenter)
        )

        // ── INDICADOR DE ESCANEO (solo cuando busca planos) ───────────────
        if (!isMapPlaced) {
            ARScanningHint(
                trackingFailureReason = trackingFailureReason,
                hasLocations          = mapState.locations.isNotEmpty(),
                pulseAlpha            = pulseAlpha,
                modifier              = Modifier.align(Alignment.Center)
            )
        }

        // ── LEYENDA DE COLORES ─────────────────────────────────────────────
        if (isMapPlaced) {
            ARLegend(
                locationCount = mapState.locations.size,
                modifier      = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 12.dp)
            )
        }

        // ── TARJETA DE DETALLE (al tocar una esfera) ──────────────────────
        selectedLocation?.let { loc ->
            ARLocationDetail(
                location = loc,
                onDismiss = { selectedLocation = null },
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

// ── CONSTRUCCIÓN DEL MAPA AR ──────────────────────────────────────────────────

/**
 * Crea un [AnchorNode] en el centro del plano detectado y añade una [SphereNode]
 * por cada ubicación del mapa, escalando sus coordenadas relativas (0..1)
 * al espacio 3D del mundo real (en metros).
 */
private fun buildARMap(
    plane:          Plane,
    locations:      List<WorldLocation>,
    engine:         com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    childNodes:     MutableList<io.github.sceneview.node.Node>
) {
    val anchor     = plane.createAnchor(plane.centerPose)
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)

    locations.forEach { location ->
        val xOffset = (location.x - 0.5f) * MAP_SCALE
        val zOffset = (location.y - 0.5f) * MAP_SCALE

        // ── Color y tamaño según el tipo de ubicación ───────────────────
        val (baseColor, baseRadius, metallic, roughness) = getLocationStyle(location)

        val mat = materialLoader.createColorInstance(
            color       = baseColor,
            metallic    = metallic,
            roughness   = roughness,
            reflectance = 0.5f
        )

        // Forma principal
        val mainSphere = SphereNode(
            engine           = engine,
            radius           = baseRadius,
            materialInstance = mat
        ).apply {
            name     = location.id
            position = Position(xOffset, SPHERE_HEIGHT, zOffset)
        }

        // ── Esfera pequeña flotante sobre la forma para indicar tipo ────
        val accentColor = getLocationAccentColor(location)
        val accentMat = materialLoader.createColorInstance(
            color       = accentColor,
            metallic    = 0.9f,
            roughness   = 0.1f,
            reflectance = 0.8f
        )
        val accentSphere = SphereNode(
            engine           = engine,
            radius           = baseRadius * 0.4f,
            materialInstance = accentMat
        ).apply {
            name     = location.id
            position = Position(xOffset, SPHERE_HEIGHT + baseRadius + 0.012f, zOffset)
        }

        // ── Indicador dorado pulsante si es la ubicación actual ─────────
        if (location.isCurrentLocation) {
            val pulseRing = SphereNode(
                engine = engine,
                radius = baseRadius * 1.6f,
                materialInstance = materialLoader.createColorInstance(
                    color       = Color(0xFFFFD700),
                    metallic    = 0.9f,
                    roughness   = 0.1f,
                    reflectance = 0.9f
                )
            ).apply {
                name     = location.id
                position = Position(xOffset, SPHERE_HEIGHT * 0.3f, zOffset)
            }
            anchorNode.addChildNode(pulseRing)
        }

        anchorNode.addChildNode(mainSphere)
        anchorNode.addChildNode(accentSphere)
    }

    childNodes += anchorNode
}

/** Devuelve (color, radio, metallic, roughness) según el tipo de ubicación */
private fun getLocationStyle(location: WorldLocation): LocationStyle {
    val isCurrent = location.isCurrentLocation
    return when (location.type.lowercase().trim()) {
        "bosque"            -> LocationStyle(Color(0xFF2E7D32), 0.028f, 0.0f, 0.85f)
        "cueva"             -> LocationStyle(Color(0xFF37474F), 0.020f, 0.2f, 0.9f)
        "mazmorra"          -> LocationStyle(Color(0xFF4A148C), 0.022f, 0.3f, 0.8f)
        "ciudad"            -> LocationStyle(Color(0xFF1565C0), 0.032f, 0.4f, 0.5f)
        "pueblo"            -> LocationStyle(Color(0xFF827717), 0.025f, 0.1f, 0.85f)
        "montaña", "montana"-> LocationStyle(Color(0xFF546E7A), 0.030f, 0.1f, 0.9f)
        "templo"            -> LocationStyle(Color(0xFFF9A825), 0.028f, 0.6f, 0.3f)
        "torre"             -> LocationStyle(Color(0xFF283593), 0.022f, 0.5f, 0.4f)
        "lago"              -> LocationStyle(Color(0xFF0277BD), 0.026f, 0.5f, 0.2f)
        "mar"               -> LocationStyle(Color(0xFF01579B), 0.032f, 0.4f, 0.3f)
        "desierto"          -> LocationStyle(Color(0xFFF57F17), 0.028f, 0.0f, 0.95f)
        "taberna"           -> LocationStyle(Color(0xFF6D4C41), 0.024f, 0.1f, 0.9f)
        "ruina"             -> LocationStyle(Color(0xFF4E342E), 0.025f, 0.05f, 0.95f)
        "llanura"           -> LocationStyle(Color(0xFF388E3C), 0.026f, 0.0f, 0.9f)
        else -> if (isCurrent)
            LocationStyle(Color(0xFFFFD700), 0.030f, 0.8f, 0.2f)
        else
            LocationStyle(Color(0xFF87CEFA), 0.025f, 0.3f, 0.5f)
    }
}

/** Color del acento (esfera pequeña flotante) que identifica el tipo visualmente */
private fun getLocationAccentColor(location: WorldLocation): Color =
    when (location.type.lowercase().trim()) {
        "bosque"            -> Color(0xFF66BB6A)  // verde claro
        "cueva"             -> Color(0xFF4FC3F7)  // azul cristal
        "mazmorra"          -> Color(0xFFCE93D8)  // morado
        "ciudad"            -> Color(0xFF90CAF9)  // azul claro
        "pueblo"            -> Color(0xFFFFCC02)  // amarillo
        "montaña", "montana"-> Color(0xFFECEFF1)  // blanco nieve
        "templo"            -> Color(0xFFFFFFCC)  // dorado pálido
        "torre"             -> Color(0xFF7986CB)  // índigo
        "lago", "mar"       -> Color(0xFFB3E5FC)  // azul agua
        "desierto"          -> Color(0xFFFFE082)  // arena
        "taberna"           -> Color(0xFFFFAB40)  // naranja
        "ruina"             -> Color(0xFFA1887F)  // marrón claro
        else                -> Color(0xFFFFFFFF)
    }

// Data class auxiliar para agrupar los 4 valores de estilo
data class LocationStyle(
    val color: Color,
    val radius: Float,
    val metallic: Float,
    val roughness: Float
)

operator fun LocationStyle.component1() = color
operator fun LocationStyle.component2() = radius
operator fun LocationStyle.component3() = metallic
operator fun LocationStyle.component4() = roughness

// ── COMPONENTES UI ────────────────────────────────────────────────────────────

@Composable
private fun ARTopBar(
    title:                String,
    isMapPlaced:          Boolean,
    trackingFailureReason: TrackingFailureReason?,
    pulseAlpha:           Float,
    onBack:               () -> Unit,
    onReset:              () -> Unit,
    onOpen3DGallery:      () -> Unit = {},
    modifier:             Modifier = Modifier
) {
    Surface(
        color    = Color(0xCC1A0F00),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = Color(0xFFFFD700))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "🗺️ $title  •  AR",
                    color      = Color(0xFFFFD700),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                val statusText = when {
                    trackingFailureReason == TrackingFailureReason.EXCESSIVE_MOTION ->
                        "⚠️ Mueve el teléfono más despacio"
                    trackingFailureReason == TrackingFailureReason.INSUFFICIENT_LIGHT ->
                        "⚠️ Necesitas más luz"
                    trackingFailureReason == TrackingFailureReason.INSUFFICIENT_FEATURES ->
                        "⚠️ Apunta a una superficie con textura"
                    !isMapPlaced ->
                        "🔍 Buscando superficie plana…"
                    else ->
                        "✅ Mapa activo – toca un lugar"
                }
                Text(
                    text     = statusText,
                    color    = Color(0xFFCCBBAA).copy(alpha = if (!isMapPlaced) pulseAlpha else 1f),
                    fontSize = 11.sp
                )
            }

            // Botón galería 3D
            IconButton(onClick = onOpen3DGallery) {
                Text("🏛️", fontSize = 20.sp)
            }

            // Botón reset (solo cuando el mapa ya está colocado)
            if (isMapPlaced) {
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reposicionar mapa",
                        tint = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
private fun ARScanningHint(
    trackingFailureReason: TrackingFailureReason?,
    hasLocations:          Boolean,
    pulseAlpha:            Float,
    modifier:              Modifier = Modifier
) {
    val (icon, message) = when {
        !hasLocations ->
            "🗺️" to "Aún no has explorado ningún lugar.\nJuega una partida para descubrir el mundo."
        trackingFailureReason != null ->
            "⚠️" to "Mueve el dispositivo lentamente\nhacia una superficie plana y bien iluminada."
        else ->
            "📡" to "Apunta la cámara hacia\nuna mesa o el suelo"
    }

    Box(
        modifier         = modifier
            .background(Color(0xBB000000), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text      = message,
                color     = Color.White.copy(alpha = pulseAlpha),
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ARLegend(
    locationCount: Int,
    modifier:      Modifier = Modifier
) {
    Surface(
        color    = Color(0xCC1A0F00),
        shape    = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "$locationCount lugar${if (locationCount != 1) "es" else ""}",
                color = Color(0xFF888877), fontSize = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            LegendRow(color = Color(0xFFFFD700), label = "Aquí ahora")
            LegendRow(color = Color(0xFF66BB6A), label = "Bosque")
            LegendRow(color = Color(0xFF1565C0), label = "Ciudad")
            LegendRow(color = Color(0xFF4FC3F7), label = "Cueva")
            LegendRow(color = Color(0xFFF9A825), label = "Templo")
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFFCCBBAA), fontSize = 10.sp)
    }
}

@Composable
private fun ARLocationDetail(
    location: WorldLocation,
    onDismiss: () -> Unit,
    modifier:  Modifier = Modifier
) {
    Surface(
        color    = Color(0xEE2A1800),
        shape    = RoundedCornerShape(14.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(location.icon, fontSize = 32.sp, modifier = Modifier.padding(end = 14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = location.name,
                        color      = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
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
                                color      = Color.Black,
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text     = location.type.replaceFirstChar { it.uppercase() },
                    color    = Color(0xFF888877),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (location.description.isNotBlank()) {
                    Text(
                        text      = location.description,
                        color     = Color(0xFFCCBBAA),
                        fontSize  = 12.sp,
                        maxLines  = 3,
                        overflow  = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        modifier  = Modifier.padding(top = 6.dp)
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Text("✕", color = Color(0xFF888877), fontSize = 16.sp)
            }
        }
    }
}