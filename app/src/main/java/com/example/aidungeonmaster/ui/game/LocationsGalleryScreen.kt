package com.example.aidungeonmaster.ui.game

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.Normalizer
import com.example.aidungeonmaster.data.model.WorldLocation
import com.example.aidungeonmaster.data.model.WorldMapState

/**
 * Pantalla que muestra una galería 3D de todas las ubicaciones visitadas por el personaje.
 *
 * Usa WebView + Three.js para renderizar modelos 3D procedurales para cada tipo de lugar.
 * No requiere pago ni instalaciones adicionales: Three.js se carga desde cdnjs.cloudflare.com.
 *
 * Los modelos varían en función de palabras clave detectadas en la descripción del lugar:
 *  - "montaña/s" → añade picos montañosos al fondo del escenario
 *  - "lago/río/arroyo" → añade agua al fondo
 *  - "bosque/árboles" → añade vegetación extra al fondo
 *  - "niebla/bruma/neblina" → aumenta la densidad de niebla
 *  - "oscur/tenebroso" → oscurece la iluminación general
 *  - "mágico/encantado" → añade partículas brillantes flotantes
 *  - "ruina/s" → añade fragmentos de piedra y muros rotos al fondo
 *  - "pintoresco/hermoso" → aumenta la luz y añade flores de colores
 *  - "nieve/nevado" → añade capa de nieve y copos
 *
 * @param mapState      Estado del mapa con las ubicaciones de ESTE personaje.
 * @param characterName Nombre del personaje, para mostrarlo en el título.
 * @param onBack        Vuelve a la pantalla anterior.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationsGalleryScreen(
    mapState: WorldMapState,
    characterName: String,
    onBack: () -> Unit
) {
    var selectedLocation by remember(mapState.locations) {
        mutableStateOf(mapState.locations.firstOrNull())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0700))
    ) {
        // ── CABECERA ──────────────────────────────────────────────────────
        Surface(color = Color(0xEE1A0F00)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color(0xFFFFD700))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🏛️ Lugares Descubiertos",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp
                    )
                    Text(
                        "$characterName  •  ${mapState.locations.size} ubicaciones",
                        color = Color(0xFF888877),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (mapState.locations.isEmpty()) {
            // ── ESTADO VACÍO ──────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗺️", fontSize = 72.sp)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Aún no has explorado ningún lugar.\nJuega una aventura para descubrir el mundo.",
                        color = Color(0xFF888877),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            // ── VISTA 3D (WebView con Three.js) ──────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Re-genera el HTML cada vez que cambia la ubicación seleccionada
                val currentLoc = selectedLocation ?: mapState.locations.first()
                val html = remember(currentLoc.id) { buildLocationHtml(currentLoc) }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            webViewClient = WebViewClient()
                            setBackgroundColor(android.graphics.Color.parseColor("#0D0700"))
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            "https://cdnjs.cloudflare.com",
                            html,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // ── OVERLAY: info de la ubicación ──────────────────────
                Surface(
                    color = Color(0xCC1A0F00),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            currentLoc.icon,
                            fontSize = 30.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    currentLoc.name,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 17.sp
                                )
                                if (currentLoc.isCurrentLocation) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFFFD700),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "📍 Aquí",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(
                                                horizontal = 5.dp, vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                            Text(
                                currentLoc.type.replaceFirstChar { it.uppercase() },
                                color = Color(0xFF888877),
                                fontSize = 11.sp
                            )
                            if (currentLoc.description.isNotBlank()) {
                                Text(
                                    currentLoc.description,
                                    color = Color(0xFFCCBBAA),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }

                // ── HINT de rotación ──────────────────────────────────
                Text(
                    "↔  Arrastra para rotar el modelo",
                    color = Color(0x77CCBBAA),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }

            // ── SELECTOR HORIZONTAL DE UBICACIONES ───────────────────────
            Surface(color = Color(0xEE1A0F00)) {
                Column {
                    HorizontalDivider(color = Color(0xFF443322), thickness = 1.dp)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mapState.locations) { loc ->
                            LocationChip(
                                location = loc,
                                isSelected = loc.id == selectedLocation?.id,
                                onClick = { selectedLocation = loc }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── CHIP DE SELECCIÓN ─────────────────────────────────────────────────────────

@Composable
private fun LocationChip(
    location: WorldLocation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF3A2800) else Color(0xFF1A1000),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) Color(0xFFFFD700) else Color(0xFF443322)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(88.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(location.icon, fontSize = 22.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                location.name,
                color = if (isSelected) Color(0xFFFFD700) else Color(0xFFCCBBAA),
                fontSize = 10.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
        }
    }
}

// ── ANÁLISIS DE DESCRIPCIÓN ───────────────────────────────────────────────────

/**
 * Extrae modificadores visuales de la descripción del lugar.
 * Devuelve un conjunto de tags que el generador de modelos usa para añadir
 * elementos 3D extra al escenario base.
 *
 * Ejemplos:
 *  "Pueblo pintoresco rodeado de montañas"  → setOf("montanas_fondo", "pintoresco")
 *  "Cueva húmeda y oscura junto a un lago"  → setOf("agua_fondo", "oscuro")
 *  "Taberna mágica en medio del bosque"     → setOf("magico", "arboles_fondo")
 */
private fun parseDescriptionTags(description: String): Set<String> {
    val desc = description.lowercase()
    val tags = mutableSetOf<String>()

    // ── Entorno geográfico ────────────────────────────────────────────────────
    if (desc.contains("montaña") || desc.contains("monte") || desc.contains("pico") ||
        desc.contains("cumbre") || desc.contains("sierra") || desc.contains("colina"))
        tags.add("montanas_fondo")

    if (desc.contains("lago") || desc.contains("río") || desc.contains("rio") ||
        desc.contains("arroyo") || desc.contains("estanque") || desc.contains("agua") ||
        desc.contains("cascada") || desc.contains("corriente"))
        tags.add("agua_fondo")

    if (desc.contains("bosque") || desc.contains("árbol") || desc.contains("arbol") ||
        desc.contains("selva") || desc.contains("vegetaci") || desc.contains("floresta"))
        tags.add("arboles_fondo")

    if (desc.contains("desierto") || desc.contains("arena") || desc.contains("árido") ||
        desc.contains("arido") || desc.contains("estepa"))
        tags.add("arena_fondo")

    if (desc.contains("ruina") || desc.contains("abandonad") || desc.contains("destruid") ||
        desc.contains("derruido") || desc.contains("derrumbad") || desc.contains("escombro"))
        tags.add("ruinas_fondo")

    // ── Atmósfera ─────────────────────────────────────────────────────────────
    if (desc.contains("niebla") || desc.contains("bruma") || desc.contains("neblina") ||
        desc.contains("neblinoso") || desc.contains("brumoso"))
        tags.add("niebla")

    if (desc.contains("oscur") || desc.contains("tenebroso") || desc.contains("lóbreg") ||
        desc.contains("lobreg") || desc.contains("sombr") || desc.contains("tiniebla"))
        tags.add("oscuro")

    if (desc.contains("pintoresc") || desc.contains("hermoso") || desc.contains("hermosa") ||
        desc.contains("idílico") || desc.contains("idilico") || desc.contains("encantador") ||
        desc.contains("alegre") || desc.contains("luminoso") || desc.contains("soleado"))
        tags.add("pintoresco")

    // ── Elementos mágicos ─────────────────────────────────────────────────────
    if (desc.contains("mágic") || desc.contains("magic") || desc.contains("encantad") ||
        desc.contains("místic") || desc.contains("mistic") || desc.contains("hechizado") ||
        desc.contains("arcano") || desc.contains("sobrenatural"))
        tags.add("magico")

    // ── Nieve / frío ──────────────────────────────────────────────────────────
    if (desc.contains("nieve") || desc.contains("nevado") || desc.contains("helado") ||
        desc.contains("glaciar") || desc.contains("frío") || desc.contains("frio") ||
        desc.contains("ventisca") || desc.contains("tundra"))
        tags.add("nieve")

    return tags
}

/**
 * Genera código JavaScript adicional basado en los tags de descripción.
 * Se inyecta en la escena después del modelo base, añadiendo elementos
 * contextuales en el fondo o modificando la iluminación.
 */
private fun getDescriptionExtras(tags: Set<String>): String {
    val sb = StringBuilder()

    // ── Montañas en el fondo ──────────────────────────────────────────────────
    if (tags.contains("montanas_fondo")) {
        sb.append("""
// ── MONTAÑAS AL FONDO (descrip.) ─────────────────────────────────────────────
var bgMtMat  = makeMat(0x3A3A44, 0.97, 0.0);
var bgSnowMat = makeMat(0xDDDDEE, 0.6, 0.0);
[[-5.0,-6.0,2.5],[5.5,-5.5,2.0],[-3.5,-5.0,1.6],[2.5,-5.8,1.8],[0.0,-7.0,3.0]].forEach(function(m){
    var h = m[2]*1.8;
    addMesh(new THREE.ConeGeometry(m[2],h,6), bgMtMat, m[0],h*0.5,m[1]);
    addMesh(new THREE.ConeGeometry(m[2]*0.3,h*0.3,6), bgSnowMat, m[0],h*0.88,m[1]);
});
""")
    }

    // ── Lago / río en el fondo ────────────────────────────────────────────────
    if (tags.contains("agua_fondo")) {
        sb.append("""
// ── AGUA AL FONDO (descrip.) ─────────────────────────────────────────────────
var bgWaterMat = makeMat(0x0A2A6A,0.05,0.3,0x112255,0.2);
addMesh(new THREE.CircleGeometry(2.5,28), bgWaterMat, -3.0,0.02,-3.5, -Math.PI/2);
[[-2.2,0.4,-2.8],[-3.5,0.4,-2.5],[-3.8,0.4,-3.8]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.025,0.03,0.7,5), makeMat(0x4A7A1A,0.9), p[0],p[1],p[2]);
    addMesh(new THREE.ConeGeometry(0.08,0.22,5), makeMat(0x8B5E3C,0.85), p[0],p[1]+0.42,p[2]);
});
var bgWL = new THREE.PointLight(0x1144AA,0.6,5);
bgWL.position.set(-3.0,0.8,-3.5);
scene.add(bgWL);
""")
    }

    // ── Árboles en el fondo ───────────────────────────────────────────────────
    if (tags.contains("arboles_fondo")) {
        sb.append("""
// ── ÁRBOLES AL FONDO (descrip.) ──────────────────────────────────────────────
var bgTrunkMat = makeMat(0x5C3A1E,0.9,0.0);
var bgLeafMatA = makeMat(0x1A5A14,0.9,0.0);
var bgLeafMatB = makeMat(0x236B1C,0.85,0.0);
[[3.5,-5.0],[-4.0,-5.5],[5.0,-4.5],[-3.0,-6.0],[4.5,-6.0],[-5.0,-4.8]].forEach(function(p){
    var h = 1.1+Math.random()*0.7;
    addMesh(new THREE.CylinderGeometry(0.07,0.11,h,7), bgTrunkMat, p[0],h*0.5,p[1]);
    var ch = 1.0+Math.random()*0.5;
    addMesh(new THREE.ConeGeometry(0.5,ch,7),   bgLeafMatA, p[0],h+ch*0.45,p[1]);
    addMesh(new THREE.ConeGeometry(0.35,ch*0.7,7),bgLeafMatB,p[0],h+ch*0.75,p[1]);
});
""")
    }

    // ── Ruinas en el fondo ────────────────────────────────────────────────────
    if (tags.contains("ruinas_fondo")) {
        sb.append("""
// ── RUINAS AL FONDO (descrip.) ───────────────────────────────────────────────
var bgRuinMat = makeMat(0x4A4840,0.97,0.02);
[[3.5,0.5,-4.0,0.5,1.0,0.3],[-4.0,0.4,-3.8,0.3,0.8,0.5]].forEach(function(w){
    addMesh(new THREE.BoxGeometry(w[3],w[4],w[5]), bgRuinMat, w[0],w[1],w[2]);
});
addMesh(new THREE.CylinderGeometry(0.15,0.17,1.2,8), bgRuinMat, -3.5,0.6,-4.5);
addMesh(new THREE.CylinderGeometry(0.15,0.17,0.5,8), bgRuinMat, -3.4,0.25,-4.4, 0,0,0.5);
[[4.0,0.08,-3.2],[3.0,0.07,-4.5],[-3.0,0.07,-4.0]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.3,0.12,0.22), bgRuinMat,
        p[0],p[1],p[2], Math.random()*0.3,Math.random()*Math.PI,0);
});
""")
    }

    // ── Arena / dunas de fondo ────────────────────────────────────────────────
    if (tags.contains("arena_fondo")) {
        sb.append("""
// ── DUNAS AL FONDO (descrip.) ────────────────────────────────────────────────
var bgSandMat = makeMat(0xC8960A,0.97,0.0);
[[-4.0,-5.0,0.5],[3.5,-5.5,0.6],[-2.0,-6.0,0.4],[4.5,-4.5,0.45]].forEach(function(d){
    addMesh(new THREE.SphereGeometry(d[2],9,6), bgSandMat, d[0],-d[2]*0.3,d[1], 0,0,0, 1,0.35,1);
});
""")
    }

    // ── Niebla densa ─────────────────────────────────────────────────────────
    if (tags.contains("niebla")) {
        sb.append("""
// ── NIEBLA DENSA (descrip.) ───────────────────────────────────────────────────
scene.fog = new THREE.FogExp2(scene.background.getHex(), 0.14);
""")
    }

    // ── Ambiente oscuro ───────────────────────────────────────────────────────
    if (tags.contains("oscuro")) {
        sb.append("""
// ── AMBIENTE OSCURO (descrip.) ────────────────────────────────────────────────
ambient.intensity = 0.25;
sun.intensity     = 0.6;
""")
    }

    // ── Pintoresco / luminoso ─────────────────────────────────────────────────
    if (tags.contains("pintoresco")) {
        sb.append("""
// ── AMBIENTE PINTORESCO (descrip.) ───────────────────────────────────────────
ambient.intensity = 1.0;
sun.intensity     = 1.8;
var fColors = [0xFF4466,0xFFDD00,0xFF8800,0xCC44FF,0x44DDFF];
for(var fi=0;fi<18;fi++){
    var fa=(fi/18)*Math.PI*2, fd=1.4+Math.random()*2.0, fc=fColors[fi%fColors.length];
    addMesh(new THREE.SphereGeometry(0.07,5,4), makeMat(fc,0.7,0.0,fc,0.3),
        Math.sin(fa)*fd,0.12,Math.cos(fa)*fd);
    addMesh(new THREE.CylinderGeometry(0.015,0.02,0.18,5), makeMat(0x2A6A10,0.9),
        Math.sin(fa)*fd,0.06,Math.cos(fa)*fd);
}
""")
    }

    // ── Partículas mágicas ────────────────────────────────────────────────────
    if (tags.contains("magico")) {
        sb.append("""
// ── PARTÍCULAS MÁGICAS (descrip.) ────────────────────────────────────────────
var mColors=[0xAA44FF,0x44AAFF,0xFF44AA,0xFFAA44,0x44FFAA];
for(var mi=0;mi<25;mi++){
    var ma=(mi/25)*Math.PI*2, mr=1.0+Math.random()*2.5, my=0.3+Math.random()*2.5;
    addMesh(new THREE.SphereGeometry(0.04+Math.random()*0.04,4,3),
        makeMat(0x000000,0.0,0.0,mColors[mi%mColors.length],1.5+Math.random()),
        Math.sin(ma)*mr, my, Math.cos(ma)*mr);
}
var mLight = new THREE.PointLight(0x8844FF,0.8,6);
mLight.position.set(0,2.0,0);
scene.add(mLight);
""")
    }

    // ── Nieve ─────────────────────────────────────────────────────────────────
    if (tags.contains("nieve")) {
        sb.append("""
// ── NIEVE (descrip.) ─────────────────────────────────────────────────────────
var snowMat = makeMat(0xEEEEFF,0.5,0.0);
addMesh(new THREE.CircleGeometry(6.5,32), snowMat, 0,0.015,0, -Math.PI/2);
for(var si=0;si<30;si++){
    var sa=(si/30)*Math.PI*2, sr=Math.random()*4;
    addMesh(new THREE.SphereGeometry(0.04+Math.random()*0.03,4,3), snowMat,
        Math.sin(sa)*sr, 0.5+Math.random()*2.5, Math.cos(sa)*sr);
}
ambient.color.setHex(0xAABBCC);
sun.color.setHex(0xCCDDFF);
""")
    }

    return sb.toString()
}

private fun normalizeLocationType(rawType: String): String {
    val normalized = Normalizer.normalize(rawType.lowercase().trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    return when {
        listOf("oceano", "alta mar", "mar abierto").any { it in normalized } -> "océano"
        listOf("puerto", "mar", "playa", "costa", "litoral", "bahia", "muelle").any { it in normalized } -> "mar"
        listOf("lago", "rio", "laguna", "arroyo", "estanque").any { it in normalized } -> "lago"
        listOf("cueva", "gruta", "caverna").any { it in normalized } -> "cueva"
        listOf("montana", "pico", "cordillera").any { it in normalized } -> "montaña"
        else -> normalized
    }
}

// ── GENERADOR DE HTML Three.js ────────────────────────────────────────────────

/**
 * Genera un documento HTML autocontenido con Three.js que muestra un modelo 3D
 * procedural del tipo de ubicación, enriquecido con extras de la descripción.
 *
 * IMPORTANTE: Todos los literales negativos en el JS usan el guión ASCII (U+002D, '-'),
 * NO el signo menos Unicode (U+2212, '-'). Confundirlos rompe silenciosamente el parser JS.
 */
private fun buildLocationHtml(location: WorldLocation): String {
    val type         = normalizeLocationType(location.type)
    val tags         = parseDescriptionTags(location.description)
    val modelCode    = getModelCode(type)
    val extraCode    = getDescriptionExtras(tags)
    val bgColor      = getBackgroundColor(type)
    val fogColor     = bgColor
    val ambientColor = getAmbientColor(type)
    val lightColor   = getSunColor(type)
    val groundColor  = getGroundColor(type)
    val fogDensity   = getFogDensity(type)

    return """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"/>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { background:$bgColor; overflow:hidden; touch-action:none; }
canvas { display:block; }
</style>
</head>
<body>
<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
<script>
// ── ESCENA ────────────────────────────────────────────────────────────────────
var W = window.innerWidth, H = window.innerHeight;
var scene = new THREE.Scene();
scene.background = new THREE.Color($bgColor);
scene.fog = new THREE.FogExp2($fogColor, $fogDensity);

var camera = new THREE.PerspectiveCamera(55, W/H, 0.05, 200);
camera.position.set(0, 2.8, 5.5);
camera.lookAt(0, 0.6, 0);

var renderer = new THREE.WebGLRenderer({antialias:true});
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(W, H);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
document.body.appendChild(renderer.domElement);

// ── ILUMINACIÓN ───────────────────────────────────────────────────────────────
var ambient = new THREE.AmbientLight($ambientColor, 0.7);
scene.add(ambient);

var sun = new THREE.DirectionalLight($lightColor, 1.4);
sun.position.set(6, 10, 6);
sun.castShadow = true;
sun.shadow.mapSize.width = 512;
sun.shadow.mapSize.height = 512;
sun.shadow.camera.near = 0.5;
sun.shadow.camera.far = 40;
sun.shadow.camera.left = -8;
sun.shadow.camera.right = 8;
sun.shadow.camera.top = 8;
sun.shadow.camera.bottom = -8;
scene.add(sun);

var fill = new THREE.DirectionalLight(0x334466, 0.4);
fill.position.set(-4, 3, -5);
scene.add(fill);

// ── SUELO ─────────────────────────────────────────────────────────────────────
var groundGeo = new THREE.CircleGeometry(7, 40);
var groundMat = new THREE.MeshStandardMaterial({
    color: $groundColor, roughness: 0.95, metalness: 0.0
});
var ground = new THREE.Mesh(groundGeo, groundMat);
ground.rotation.x = -Math.PI / 2;
ground.receiveShadow = true;
scene.add(ground);

// ── HELPERS ───────────────────────────────────────────────────────────────────
function makeMat(color, rough, metal, emissive, emissiveInt) {
    return new THREE.MeshStandardMaterial({
        color: color,
        roughness: rough !== undefined ? rough : 0.8,
        metalness: metal !== undefined ? metal : 0.0,
        emissive: emissive !== undefined ? emissive : 0x000000,
        emissiveIntensity: emissiveInt !== undefined ? emissiveInt : 0.0
    });
}
function addMesh(geo, mat, x, y, z, rx, ry, rz, sx, sy, sz) {
    var m = new THREE.Mesh(geo, mat);
    m.position.set(x||0, y||0, z||0);
    if(rx||ry||rz) m.rotation.set(rx||0, ry||0, rz||0);
    if(sx) m.scale.set(sx, sy||sx, sz||sx);
    m.castShadow = true;
    m.receiveShadow = true;
    scene.add(m);
    return m;
}

// ── MODELO BASE ───────────────────────────────────────────────────────────────
$modelCode

// ── EXTRAS DE DESCRIPCIÓN ─────────────────────────────────────────────────────
$extraCode

// ── CONTROL DE ÓRBITA ─────────────────────────────────────────────────────────
var theta = 0.3, phi = 0.35, radius = 5.5;
var dragging = false, lastX = 0, lastY = 0;
var autoRotate = true;

function applyCamera() {
    camera.position.x = radius * Math.cos(phi) * Math.sin(theta);
    camera.position.y = radius * Math.sin(phi) + 0.5;
    camera.position.z = radius * Math.cos(phi) * Math.cos(theta);
    camera.lookAt(0, 0.6, 0);
}

document.addEventListener('touchstart', function(e) {
    dragging = true; autoRotate = false;
    lastX = e.touches[0].clientX; lastY = e.touches[0].clientY;
}, {passive:true});
document.addEventListener('touchmove', function(e) {
    if (!dragging) return;
    theta -= (e.touches[0].clientX - lastX) * 0.012;
    phi = Math.max(-0.5, Math.min(1.0, phi + (e.touches[0].clientY - lastY) * 0.006));
    lastX = e.touches[0].clientX; lastY = e.touches[0].clientY;
    applyCamera();
}, {passive:true});
document.addEventListener('touchend', function() { dragging = false; });
document.addEventListener('mousedown', function(e) {
    dragging = true; autoRotate = false;
    lastX = e.clientX; lastY = e.clientY;
});
document.addEventListener('mousemove', function(e) {
    if (!dragging) return;
    theta -= (e.clientX - lastX) * 0.009;
    phi = Math.max(-0.5, Math.min(1.0, phi + (e.clientY - lastY) * 0.005));
    lastX = e.clientX; lastY = e.clientY;
    applyCamera();
});
document.addEventListener('mouseup', function() { dragging = false; });
window.addEventListener('resize', function() {
    W = window.innerWidth; H = window.innerHeight;
    camera.aspect = W / H;
    camera.updateProjectionMatrix();
    renderer.setSize(W, H);
});

applyCamera();

// ── BUCLE DE ANIMACIÓN ────────────────────────────────────────────────────────
var t = 0;
function animate() {
    requestAnimationFrame(animate);
    t += 0.016;
    if (autoRotate) { theta += 0.005; applyCamera(); }
    ${getAnimationCode(type)}
    renderer.render(scene, camera);
}
animate();
</script>
</body>
</html>""".trimIndent()
}

// ── CONFIGURACIÓN POR TIPO ────────────────────────────────────────────────────

private fun getBackgroundColor(type: String) = when (type) {
    "bosque"             -> "0x0A1505"
    "cueva"              -> "0x0A0A0F"
    "mazmorra"           -> "0x080305"
    "ciudad"             -> "0x0D1015"
    "pueblo"             -> "0x0D0F0A"
    "montaña", "montana" -> "0x080D12"
    "templo"             -> "0x0D0D10"
    "torre"              -> "0x080810"
    "lago"               -> "0x0A1624"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0x74C0D8"
    "océano", "oceano"  -> "0x061630"
    "desierto"           -> "0x150C05"
    "taberna"            -> "0x0D0905"
    "ruina"              -> "0x0C0C0A"
    else                 -> "0x0D0700"
}

private fun getAmbientColor(type: String) = when (type) {
    "bosque"             -> "0x0D2205"
    "cueva", "mazmorra"  -> "0x140F1A"
    "lago"               -> "0x0A2038"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0x5FA8C8"
    "océano", "oceano"  -> "0x0B2545"
    "desierto"           -> "0x201505"
    "taberna"            -> "0x201005"
    else                 -> "0x111118"
}

private fun getSunColor(type: String) = when (type) {
    "bosque"             -> "0xAAFF88"
    "cueva", "mazmorra"  -> "0xFF7722"
    "lago"               -> "0x9FDBFF"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0xFFF1D0"
    "océano", "oceano"  -> "0x7FC8FF"
    "desierto"           -> "0xFFDD88"
    "templo"             -> "0xFFEECC"
    "taberna"            -> "0xFF9944"
    else                 -> "0xFFDDAA"
}

private fun getGroundColor(type: String) = when (type) {
    "bosque"             -> "0x1A2E0A"
    "cueva"              -> "0x2A2830"
    "mazmorra"           -> "0x151212"
    "ciudad"             -> "0x333340"
    "pueblo"             -> "0x2A2010"
    "montaña", "montana" -> "0x303030"
    "templo"             -> "0xD4C8A0"
    "lago"               -> "0x123A58"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0xD2B07C"
    "océano", "oceano"  -> "0x0A2746"
    "desierto"           -> "0x8B6914"
    "taberna"            -> "0x2A1A08"
    "ruina"              -> "0x282820"
    else                 -> "0x1A1505"
}

private fun getFogDensity(type: String) = when (type) {
    "bosque"            -> "0.06"
    "cueva", "mazmorra" -> "0.06"
    "lago"              -> "0.045"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0.022"
    "océano", "oceano" -> "0.055"
    else                -> "0.03"
}

private fun getAnimationCode(type: String) = when (type) {
    "bosque" ->
        """if(window._leaves) window._leaves.forEach(function(l,i){
            l.position.y += Math.sin(t*1.2+i)*0.0008;
            l.rotation.y += 0.005;
        });"""
    "lago" ->
        """if(window._waves) window._waves.forEach(function(w,i){
            w.position.y = 0.04 + Math.sin(t*0.8+i*1.1)*0.018;
            w.material.opacity = 0.18+Math.sin(t*0.6+i)*0.08;
        });"""
    "mar", "playa", "costa", "litoral", "bahía", "bahia" ->
        """if(window._waves) window._waves.forEach(function(w,i){
            w.position.y = 0.06 + Math.sin(t*1.15+i*1.3)*0.04;
            w.material.opacity = 0.24+Math.sin(t*0.9+i)*0.11;
        });"""
    "océano", "oceano" ->
        """if(window._waves) window._waves.forEach(function(w,i){
            w.position.y = 0.08 + Math.sin(t*1.7+i*1.5)*0.07;
            w.material.opacity = 0.28+Math.sin(t*1.2+i)*0.14;
        });"""
    "templo" ->
        """if(window._torches) window._torches.forEach(function(tp,i){
            tp.intensity = 1.2 + Math.sin(t*4+i*2)*0.4;
        });"""
    "taberna" ->
        """if(window._fire) {
            window._fire.intensity = 1.5 + Math.sin(t*5)*0.6;
            window._fireM.emissiveIntensity = 0.6 + Math.sin(t*3)*0.2;
        }"""
    "mazmorra" ->
        """if(window._torches) window._torches.forEach(function(tp,i){
            tp.intensity = 0.8 + Math.sin(t*4+i*1.5)*0.3;
        });"""
    else -> "// no animation"
}

/**
 * Genera el código JavaScript de Three.js que construye el modelo 3D
 * característico de cada tipo de ubicación.
 *
 * TODOS los literales negativos usan el guión ASCII '-' (U+002D).
 * El signo menos Unicode '-' (U+2212) rompe el parser de JavaScript en WebView.
 */
private fun getModelCode(type: String): String = when (type) {

    // ── BOSQUE ────────────────────────────────────────────────────────────────
    "bosque" -> """
var leaves = [];
var treePositions = [
    [0,0],[1.6,0.4],[-1.5,0.6],[0.8,-1.4],[-0.9,-1.2],[2.2,-0.5],[-2.0,0.0]
];
treePositions.forEach(function(p, i) {
    var h = 1.0 + Math.random()*0.8;
    addMesh(new THREE.CylinderGeometry(0.07,0.11,h,8),
        makeMat(0x5C3A1E, 0.9, 0.0), p[0], h/2, p[1]);
    var canopyH = 1.2 + Math.random()*0.6;
    var leaf = addMesh(
        new THREE.ConeGeometry(0.55+Math.random()*0.2, canopyH, 7),
        makeMat(0x1E6B1A + (i*0x050800&0xFFFF), 0.9, 0.0),
        p[0], h + canopyH/2 - 0.15, p[1]
    );
    leaves.push(leaf);
    var leaf2 = addMesh(
        new THREE.ConeGeometry(0.38, canopyH*0.75, 7),
        makeMat(0x25801E, 0.85, 0.0),
        p[0], h + canopyH*0.7, p[1]
    );
    leaves.push(leaf2);
});
[[0.6,0.9],[-0.8,0.5],[1.2,-0.8]].forEach(function(r){
    addMesh(new THREE.SphereGeometry(0.2,6,5), makeMat(0x555548,0.95), r[0],0.12,r[1]);
});
window._leaves = leaves;
var ptLight = new THREE.PointLight(0x44FF22, 0.4, 8);
ptLight.position.set(0, 3, 0);
scene.add(ptLight);
""".trimIndent()

    // ── CUEVA ────────────────────────────────────────────────────────────────
    "cueva" -> """
// ── SUELO ROCOSO ─────────────────────────────────────────────────────────────
var rockMat   = makeMat(0x3A3A40,0.95,0.05);
var darkRockM = makeMat(0x282830,0.97,0.03);
addMesh(new THREE.BoxGeometry(7.0,0.12,6.0), rockMat, 0,-0.06,0);

// ── ARCO DE ENTRADA DE LA CUEVA ──────────────────────────────────────────────
// Pilares laterales
addMesh(new THREE.BoxGeometry(0.55,2.2,0.6), rockMat, -1.1,1.1,0);
addMesh(new THREE.BoxGeometry(0.55,2.2,0.6), rockMat,  1.1,1.1,0);
// Dintel (piedra superior del arco)
addMesh(new THREE.BoxGeometry(2.85,0.55,0.6), rockMat, 0,2.42,0);
// Bóveda curva central (semiarco con esferas aplastadas)
for(var ai=0;ai<6;ai++){
    var ang = (ai/5)*Math.PI;
    addMesh(new THREE.SphereGeometry(0.38,7,6), darkRockM,
        Math.cos(ang)*0.9, 1.5+Math.abs(Math.sin(ang))*0.7, -0.05,
        0,0,0, 1.0,0.7,0.55);
}

// ── OSCURIDAD INTERIOR ────────────────────────────────────────────────────────
addMesh(new THREE.BoxGeometry(2.0,2.0,0.3), makeMat(0x060606,0.98), 0,1.1,-0.15);

// ── LUZ SALIENDO DEL INTERIOR ────────────────────────────────────────────────
var innerGlow = new THREE.PointLight(0xFF9933, 1.4, 5);
innerGlow.position.set(0,1.0,-0.8);
scene.add(innerGlow);
// Destellos de cristal azul en los laterales
[[1.6,0.3,0.6],[-1.5,0.4,0.8],[1.8,0.6,-0.5],[-1.7,0.5,-0.4]].forEach(function(p){
    addMesh(new THREE.ConeGeometry(0.08,0.55,5), makeMat(0x1133AA,0.3,0.2,0x2255FF,1.2),
        p[0],p[1],p[2]);
});

// ── ESTALACTITAS Y ESTALAGMITAS ───────────────────────────────────────────────
[[0.5,2.95,0.2],[-0.4,2.90,0.1],[0.1,2.85,0.4],[-0.6,2.98,-0.1],[0.7,2.92,-0.2]].forEach(function(p){
    addMesh(new THREE.ConeGeometry(0.05,0.35+Math.random()*0.2,6), rockMat,
        p[0],p[1],p[2], Math.PI,0,0);
});
[[-0.7,0.22,0.5],[0.5,0.18,-0.4],[1.4,0.25,0.3],[-1.4,0.20,0.6]].forEach(function(p){
    addMesh(new THREE.ConeGeometry(0.06,0.3+Math.random()*0.2,6), darkRockM,
        p[0],p[1],p[2]);
});

// ── ROCAS EN EL SUELO ────────────────────────────────────────────────────────
[[1.8,0.15,1.0],[-1.6,0.18,0.8],[2.2,0.12,0.5],[-2.0,0.14,-0.5],[0.9,0.1,-1.2]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],6,5), rockMat,
        p[0],p[1]*0.5,p[2], 0,Math.random()*Math.PI,0, 1,0.65,1);
});

// ── ILUMINACIÓN PRINCIPAL ─────────────────────────────────────────────────────
var torchLight = new THREE.PointLight(0xFF7722, 1.8, 7);
torchLight.position.set(0,1.8,1.5);
scene.add(torchLight);
var caveAmb = new THREE.PointLight(0x4466AA, 0.9, 8);
caveAmb.position.set(0,2.5,0);
scene.add(caveAmb);
ambient.color.setHex(0x221A2A);
ambient.intensity = 0.55;
scene.fog = new THREE.FogExp2(0x0A080F, 0.06);
""".trimIndent()

    // ── CIUDAD ───────────────────────────────────────────────────────────────
    "ciudad" -> """
var buildingConfigs = [
    [0,0,1.0,0.5,2.5],[-1.4,0.3,0.8,0.4,1.8],[1.3,-0.2,0.9,0.5,3.0],
    [-0.5,-1.3,0.7,0.6,1.4],[1.0,1.2,0.6,0.5,2.0],[-1.8,-0.8,0.7,0.45,1.6],
    [0.4,-0.5,1.1,0.45,1.2]
];
var wallMat   = makeMat(0x445566,0.8,0.1);
var roofMat   = makeMat(0x334455,0.7,0.2);
var windowMat = makeMat(0x000000,0.2,0.0,0xFFCC44,0.8);
buildingConfigs.forEach(function(c){
    var bx=c[0],bz=c[1],bw=c[2],bd=c[3],bh=c[4];
    addMesh(new THREE.BoxGeometry(bw,bh,bd), wallMat, bx,bh/2,bz);
    addMesh(new THREE.BoxGeometry(bw+0.08,0.1,bd+0.08), roofMat, bx,bh+0.05,bz);
    addMesh(new THREE.BoxGeometry(0.18,0.22,0.02), windowMat, bx+bw*0.15,bh*0.55,bz+bd/2+0.01);
    addMesh(new THREE.BoxGeometry(0.18,0.22,0.02), windowMat, bx-bw*0.15,bh*0.4, bz+bd/2+0.01);
});
addMesh(new THREE.BoxGeometry(0.6,0.02,5.0), makeMat(0x555560,0.9), 0,0.01,0);
addMesh(new THREE.CylinderGeometry(0.03,0.04,1.6,6), makeMat(0x334444,0.6,0.3),0.7,0.8,-0.5);
var lampLight = new THREE.PointLight(0xFFDD88,1.2,3.5);
lampLight.position.set(0.7,1.65,-0.5);
scene.add(lampLight);
""".trimIndent()

    // ── PUEBLO ───────────────────────────────────────────────────────────────
    "pueblo" -> """
var houseDat = [
    [0,0,1.0,0.7,0.9,0xAA8855],[1.5,0.3,0.85,0.65,0.8,0x997744],
    [-1.4,-0.2,0.9,0.6,0.85,0xBB9966],[0.4,-1.4,0.75,0.6,0.7,0xAA7733]
];
houseDat.forEach(function(h){
    var x=h[0],z=h[1],w=h[2],d=h[3],ht=h[4],col=h[5];
    addMesh(new THREE.BoxGeometry(w,ht,d), makeMat(col,0.9), x,ht/2,z);
    addMesh(new THREE.ConeGeometry(Math.max(w,d)*0.8,0.5,4),
        makeMat(0x662222,0.85), x,ht+0.2,z, 0,Math.PI/4,0);
    addMesh(new THREE.BoxGeometry(0.22,0.38,0.02),makeMat(0x553311,0.95),x,0.19,z+d/2+0.01);
});
addMesh(new THREE.CylinderGeometry(0.3,0.3,0.5,12), makeMat(0x888880,0.9),-0.3,0.25,-0.3);
addMesh(new THREE.TorusGeometry(0.3,0.04,8,20), makeMat(0x666655,0.8),-0.3,0.5,-0.3,Math.PI/2);
addMesh(new THREE.CylinderGeometry(0.06,0.09,0.9,8), makeMat(0x5C3A1E,0.9),-1.8,0.45,1.2);
addMesh(new THREE.SphereGeometry(0.45,8,7), makeMat(0x226611,0.9),-1.8,1.2,1.2);
addMesh(new THREE.BoxGeometry(0.5,0.01,4.0), makeMat(0x887755,0.95),0,0.005,0);
""".trimIndent()

    // ── MAZMORRA ──────────────────────────────────────────────────────────────
    "mazmorra" -> """
var stoneMat = makeMat(0x252522,0.95,0.05);
[[-2,0.5,0],[2,0.5,0],[0,0.5,-2]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.6,1.0,0.4),stoneMat,p[0],p[1],p[2]);
    addMesh(new THREE.BoxGeometry(0.6,0.5,0.4),stoneMat,p[0],p[1]+0.75,p[2]+0.1);
});
for(var gi=0;gi<16;gi++){
    var gx=(gi%4)*0.9-1.35, gz=Math.floor(gi/4)*0.9-1.35;
    addMesh(new THREE.BoxGeometry(0.85,0.05,0.85), makeMat(0x1E1C1A,0.97), gx,0.025,gz);
}
[[-1.2,-1.2],[1.2,-1.2],[-1.2,1.2],[1.2,1.2]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.12,0.14,2.0,8),stoneMat,p[0],1.0,p[1]);
    addMesh(new THREE.BoxGeometry(0.32,0.12,0.32),stoneMat,p[0],2.05,p[1]);
});
var torches = [];
[[-1.9,1.1,-0.3],[1.9,1.1,0.4]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.03,0.04,0.2,6),
        makeMat(0x553311,0.9),p[0],p[1],p[2], 0,0,Math.PI/8);
    addMesh(new THREE.SphereGeometry(0.08,6,6),
        makeMat(0x000000,0.0,0.0,0xFF4400,2.0),p[0],p[1]+0.15,p[2]);
    var tl = new THREE.PointLight(0xFF6600,0.8,3.5);
    tl.position.set(p[0],p[1]+0.2,p[2]);
    scene.add(tl);
    torches.push(tl);
});
window._torches = torches;
addMesh(new THREE.TorusGeometry(0.1,0.015,6,12),
    makeMat(0x888877,0.5,0.5),-0.5,1.8,-1.8,Math.PI/2);
""".trimIndent()

    // ── MONTAÑA ───────────────────────────────────────────────────────────────
    "montaña", "montana" -> """
addMesh(new THREE.ConeGeometry(2.0,3.5,7), makeMat(0x4A4A50,0.95), 0,1.75,0);
addMesh(new THREE.ConeGeometry(0.7,0.8,7), makeMat(0xEEEEFF,0.6), 0,3.2,0);
addMesh(new THREE.SphereGeometry(0.55,8,6), makeMat(0xDDDDFF,0.5), 0,3.55,0, 0,0,0, 1,0.4,1);
[[-1.8,0.5],[1.6,-0.3],[-1.0,-1.5]].forEach(function(p){
    var h = 1.0+Math.random()*1.0;
    addMesh(new THREE.ConeGeometry(0.8,h+0.5,6), makeMat(0x404048,0.96), p[0],h/2,p[1]);
    addMesh(new THREE.ConeGeometry(0.25,0.3,6), makeMat(0xDDDDFF,0.6), p[0],(h+0.55)*0.9,p[1]);
});
[[0.8,1.2],[-1.2,0.8],[1.5,-0.5],[-0.5,1.6]].forEach(function(r){
    addMesh(new THREE.SphereGeometry(0.18+Math.random()*0.12,6,5),
        makeMat(0x505050,0.95), r[0],0.12,r[1]);
});
scene.fog = new THREE.FogExp2(0x080D12, 0.025);
var moonLight = new THREE.PointLight(0xAAAACC,0.6,20);
moonLight.position.set(-5,8,-3);
scene.add(moonLight);
""".trimIndent()

    // ── TEMPLO ────────────────────────────────────────────────────────────────
    "templo" -> """
var stepMat = makeMat(0xD4C090,0.7,0.1);
[3.0,2.2,1.5,0.9].forEach(function(s,i){
    addMesh(new THREE.BoxGeometry(s,0.32,s),stepMat,0,i*0.32+0.16,0);
});
addMesh(new THREE.BoxGeometry(1.4,1.0,1.0), makeMat(0xE8D8A0,0.6,0.05), 0,1.4,0);
addMesh(new THREE.ConeGeometry(1.1,0.5,4), makeMat(0xD4B060,0.65,0.1), 0,2.15,0, 0,Math.PI/4,0);
[[-0.7,1.8],[-0.7,0.0],[0.7,1.8],[0.7,0.0],[-1.2,0.9],[1.2,0.9]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.1,0.12,1.2,10), makeMat(0xE0D0A0,0.65), p[0],1.6,p[1]);
    addMesh(new THREE.BoxGeometry(0.26,0.1,0.26), makeMat(0xD4C080,0.6), p[0],2.22,p[1]);
});
var torches = [];
[[-1.6,1.1,1.6],[1.6,1.1,1.6]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.04,0.05,0.35,6), makeMat(0x8B6914,0.8),p[0],p[1],p[2]);
    var fl = new THREE.Mesh(new THREE.SphereGeometry(0.09,6,6), makeMat(0x000000,0.0,0.0,0xFFAA00,2.0));
    fl.position.set(p[0],p[1]+0.2,p[2]);
    fl.castShadow=false;
    scene.add(fl);
    window._fireM = fl.material;
    var tl = new THREE.PointLight(0xFF8800,1.0,4);
    tl.position.set(p[0],p[1]+0.25,p[2]);
    scene.add(tl);
    torches.push(tl);
});
window._torches = torches;
""".trimIndent()

    // ── TORRE ────────────────────────────────────────────────────────────────
    "torre" -> """
var stoneMat = makeMat(0x445566,0.85,0.15);
addMesh(new THREE.CylinderGeometry(0.7,0.8,3.5,10), stoneMat, 0,1.75,0);
for(var a=0;a<8;a++){
    var angle = (a/8)*Math.PI*2;
    addMesh(new THREE.BoxGeometry(0.22,0.35,0.22), stoneMat,
        Math.sin(angle)*0.72, 3.6, Math.cos(angle)*0.72);
}
addMesh(new THREE.CylinderGeometry(0.68,0.68,0.1,10), makeMat(0x334455,0.9), 0,3.48,0);
addMesh(new THREE.BoxGeometry(0.36,0.7,0.12), makeMat(0x221A14,0.95), 0,0.35,0.76);
addMesh(new THREE.SphereGeometry(0.18,8,6), makeMat(0x221A14,0.95), 0,0.7,0.76, 0,0,0, 1,1,0.4);
addMesh(new THREE.BoxGeometry(0.18,0.26,0.05), makeMat(0x000000,0.1,0.0,0xFFCC44,1.5), 0,2.2,0.72);
var windowGlow = new THREE.PointLight(0xFFAA22,0.8,2.5);
windowGlow.position.set(0,2.2,0.5);
scene.add(windowGlow);
addMesh(new THREE.BoxGeometry(0.02,0.7,0.02), makeMat(0x553311,0.9), 0,4.0,0);
addMesh(new THREE.BoxGeometry(0.45,0.28,0.02), makeMat(0xCC2222,0.8,0.05,0x000000,0), 0.225,4.2,0);
""".trimIndent()

    // ── LAGO ──────────────────────────────────────────────────────────────────
    "lago" -> """
var bankMat = makeMat(0x2F5D3A,0.95,0.0);
var shoreMat = makeMat(0x6D8E5A,0.95,0.0);
addMesh(new THREE.CircleGeometry(6.5,40), bankMat, 0,-0.02,0, -Math.PI/2);
addMesh(new THREE.CircleGeometry(3.1,36), shoreMat, 0,-0.005,0, -Math.PI/2);
var lakeMat = makeMat(0x4FC3F7,0.05,0.18);
var lakeDeepMat = makeMat(0x236B8E,0.08,0.14);
addMesh(new THREE.CircleGeometry(2.5,40), lakeMat, 0,0.03,0, -Math.PI/2);
addMesh(new THREE.CircleGeometry(1.6,32), lakeDeepMat, 0,0.035,0, -Math.PI/2);
var waves = [];
for(var w=0; w<4; w++){
    var ring = addMesh(new THREE.TorusGeometry(0.55 + w*0.42, 0.03, 8, 48), makeMat(0xDFF6FF,0.12,0.2),
        0, 0.055 + w*0.003, 0, Math.PI/2, 0, 0);
    waves.push(ring);
}
window._waves = waves;
[[2.5,0.35,-0.8],[-2.2,0.32,0.4],[1.8,0.28,1.3],[-1.4,0.3,-1.6]].forEach(function(r){
    addMesh(new THREE.SphereGeometry(r[1],8,6), makeMat(0x6B706A,0.95), r[0], r[1]*0.35, r[2], 0, Math.random()*Math.PI, 0, 1.2,0.55,1.0);
});
[[-2.8,0.45,-0.9],[-2.4,0.45,-0.2],[-2.2,0.45,0.5],[2.7,0.45,1.0],[2.3,0.45,1.5]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.03,0.04,0.7,6), makeMat(0x567A2F,0.9), p[0], p[1], p[2]);
    addMesh(new THREE.ConeGeometry(0.09,0.24,6), makeMat(0x7FAE45,0.86), p[0], p[1]+0.42, p[2]);
});
var waterGlow = new THREE.PointLight(0x7FD8FF, 0.9, 6.5);
waterGlow.position.set(0,1.2,0);
scene.add(waterGlow);
ambient.color.setHex(0x0A2038);
ambient.intensity = 0.75;
scene.fog = new THREE.FogExp2(0x0A1624, 0.045);
""".trimIndent()

    // ── MAR / COSTA ───────────────────────────────────────────────────────────
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> """
var sandMat = makeMat(0xD2B07C,0.95,0.0);
addMesh(new THREE.BoxGeometry(8.0,0.06,3.7), sandMat, 0,-0.03,1.5);
addMesh(new THREE.BoxGeometry(8.0,0.04,0.7), makeMat(0xC6A16A,0.97), 0,-0.01,-0.25, 0.06,0,0);
var seaMat  = makeMat(0x2C99C8,0.02,0.18);
var sea2Mat = makeMat(0x1D7FB5,0.02,0.16);
var sea3Mat = makeMat(0x125C96,0.04,0.14);
addMesh(new THREE.BoxGeometry(8.0,0.04,5.0), seaMat,  0,0.02,-2.5);
addMesh(new THREE.BoxGeometry(8.0,0.03,3.0), sea2Mat, 0,0.03,-5.0);
addMesh(new THREE.BoxGeometry(8.0,0.02,6.0), sea3Mat, 0,0.01,-9.0);
var waves = [];
for(var w=0;w<4;w++){
    var waveMat = makeMat(0xD9F6FF,0.15,0.3);
    var wv = addMesh(new THREE.BoxGeometry(7.5,0.07,0.35), waveMat, 0, 0.06+w*0.015, -0.8-w*0.9);
    waves.push(wv);
}
window._waves = waves;
for(var f=0;f<12;f++){
    var fx=(Math.random()-0.5)*6.0;
    addMesh(new THREE.SphereGeometry(0.12+Math.random()*0.1,6,4), makeMat(0xEEF6FF,0.25,0.1), fx, 0.04, -0.1+Math.random()*0.25, 0,0,0, 1,0.3,1);
}
[[1.8,0.15,0.6],[-1.5,0.18,0.4],[2.6,0.12,1.0],[-2.4,0.14,0.8],[0.4,0.1,1.4]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],7,5), makeMat(0x888070,0.95), p[0],p[1]*0.5,p[2], Math.random()*0.3,Math.random()*Math.PI,0, 1,0.7+Math.random()*0.3,1);
});
addMesh(new THREE.CylinderGeometry(0.055,0.08,1.5,7), makeMat(0x7B5E3A,0.9), -1.2,0.75,1.8, 0,0,0.07);
addMesh(new THREE.SphereGeometry(0.38,7,6),  makeMat(0x2D7A1A,0.85), -1.27,1.6,1.8);
[[0,0,0.5],[0.4,0,0.2],[-0.4,0,0.2],[0.25,0,-0.3],[-0.25,0,-0.3]].forEach(function(d){
    addMesh(new THREE.BoxGeometry(0.06,0.025,0.55), makeMat(0x3A9A20,0.85), -1.27+d[0]*0.35, 1.65+d[1], 1.8+d[2]*0.4, 0.15,d[0]*1.2,0.1);
});
var sunlight = new THREE.DirectionalLight(0xFFEECC, 1.8);
sunlight.position.set(3,5,2);
scene.add(sunlight);
var waterGlow = new THREE.PointLight(0x66CCFF, 1.2, 8);
waterGlow.position.set(0,1.5,-2);
scene.add(waterGlow);
ambient.color.setHex(0x5FA8C8);
ambient.intensity = 0.9;
scene.fog = new THREE.FogExp2(0x74C0D8, 0.022);
""".trimIndent()

    // ── OCÉANO ────────────────────────────────────────────────────────────────
    "océano", "oceano" -> """
var seaMat  = makeMat(0x0E4A80,0.02,0.16);
var sea2Mat = makeMat(0x0A3A6A,0.03,0.18);
var sea3Mat = makeMat(0x07284E,0.04,0.2);
addMesh(new THREE.BoxGeometry(10.0,0.05,6.0), seaMat,  0,0.02,0.0);
addMesh(new THREE.BoxGeometry(10.0,0.04,6.0), sea2Mat, 0,0.05,-5.5);
addMesh(new THREE.BoxGeometry(10.0,0.03,7.0), sea3Mat, 0,0.03,-11.5);
var waves = [];
for(var w=0; w<6; w++){
    var waveMat = makeMat(0xA7D8FF,0.08,0.28,0x0A1E44,0.08);
    var wv = addMesh(new THREE.BoxGeometry(9.0,0.09 + w*0.01,0.42), waveMat,
        (w%2===0 ? -0.25 : 0.25), 0.08+w*0.01, -1.0-w*1.15, 0,0,(w%2===0 ? 0.02 : -0.02));
    waves.push(wv);
}
window._waves = waves;
for(var f=0;f<18;f++){
    var fx=(Math.random()-0.5)*7.5;
    addMesh(new THREE.SphereGeometry(0.1+Math.random()*0.08,6,4), makeMat(0xEAF7FF,0.2,0.08), fx, 0.08, -0.4-Math.random()*6.0, 0,0,0, 1,0.25,1.4);
}
[[2.6,0.22,2.0],[-2.4,0.18,1.6],[1.0,0.16,2.5]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],7,5), makeMat(0x5B6474,0.92), p[0], p[1]*0.45, p[2], 0, Math.random()*Math.PI, 0, 1.4,0.55,1.0);
});
var moonGlow = new THREE.PointLight(0x4AA3FF, 1.5, 12);
moonGlow.position.set(0,2.8,-4.5);
scene.add(moonGlow);
ambient.color.setHex(0x0B2545);
ambient.intensity = 0.82;
sun.color = new THREE.Color(0x7FC8FF);
sun.intensity = 1.6;
scene.fog = new THREE.FogExp2(0x061630, 0.055);
""".trimIndent()

    // ── DESIERTO ──────────────────────────────────────────────────────────────
    "desierto" -> """
[[0,0.6],[-1.5,0.25],[1.2,0.18],[0.3,-1.4],[-0.8,-0.9],[2.0,-0.2]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],10,6), makeMat(0xC8960A,0.97),
        p[0],-p[1]*0.3,0, 0,0,0, 1,0.35,1);
});
addMesh(new THREE.CylinderGeometry(0.12,0.15,1.4,7), makeMat(0x2A5A1A,0.9), 0.8,0.7,0.4);
addMesh(new THREE.CylinderGeometry(0.08,0.10,0.5,7), makeMat(0x2A5A1A,0.9), 0.8,1.35,0.4);
addMesh(new THREE.CylinderGeometry(0.07,0.08,0.4,7), makeMat(0x2A5A1A,0.9),
    0.55,1.0,0.4, 0,0,Math.PI/2.2);
addMesh(new THREE.CylinderGeometry(0.06,0.07,0.35,7), makeMat(0x316A20,0.9), 0.35,1.15,0.4);
addMesh(new THREE.CylinderGeometry(0.07,0.09,0.7,6), makeMat(0x2D5E15,0.9),-1.2,0.35,-0.5);
addMesh(new THREE.SphereGeometry(0.22,7,6), makeMat(0xD4C090,0.85),-0.6,0.22,0.9);
sun.intensity = 2.0;
sun.color = new THREE.Color(0xFFEE66);
for(var s=0;s<12;s++){
    addMesh(new THREE.SphereGeometry(0.04,4,3), makeMat(0xCC9900,0.9),
        (Math.random()-0.5)*4, 0.1+Math.random()*0.3, (Math.random()-0.5)*4);
}
""".trimIndent()

    // ── TABERNA ───────────────────────────────────────────────────────────────
    "taberna" -> """
addMesh(new THREE.BoxGeometry(2.2,1.4,1.8), makeMat(0x8B6327,0.9), 0,0.7,0);
addMesh(new THREE.CylinderGeometry(0.02,1.6,0.8,4,1,false,Math.PI/4),
    makeMat(0x5C1A1A,0.85), 0,1.5,0, 0,0,0, 1,1,0.85);
addMesh(new THREE.BoxGeometry(0.3,0.7,0.3), makeMat(0x555548,0.95), 0.6,1.85,-0.4);
[0.6,0.8,1.0].forEach(function(yy){
    addMesh(new THREE.SphereGeometry(0.1+yy*0.05,6,5),
        makeMat(0x555555,0.9,0.0,0x111111,0.3), 0.6+yy*0.03,1.9+yy,-0.4);
});
addMesh(new THREE.BoxGeometry(0.8,0.3,0.04), makeMat(0x6B3E12,0.85), 0,1.6,0.92);
addMesh(new THREE.SphereGeometry(0.08,6,5), makeMat(0x000000,0.0,0.0,0xFFAA00,1.5), -0.2,1.6,0.94);
addMesh(new THREE.SphereGeometry(0.08,6,5), makeMat(0x000000,0.0,0.0,0xFF8800,1.5),  0.2,1.6,0.94);
addMesh(new THREE.BoxGeometry(0.4,0.7,0.04), makeMat(0x4A2A0A,0.9), 0,0.35,0.92);
[[0.9,0.2,0.6],[1.1,0.2,-0.4]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.18,0.18,0.38,10), makeMat(0x7B4A15,0.85), p[0],p[1],p[2]);
    addMesh(new THREE.TorusGeometry(0.18,0.025,6,16),
        makeMat(0x333222,0.7,0.3), p[0],p[1]+0.12,p[2], Math.PI/2);
});
var fireMat = makeMat(0x000000,0.0,0.0,0xFF5500,2.0);
addMesh(new THREE.SphereGeometry(0.12,6,5), fireMat, 0.6,1.3,-0.3);
var fireLight = new THREE.PointLight(0xFF6600,1.5,4.5);
fireLight.position.set(0,1.0,0.2);
scene.add(fireLight);
window._fire = fireLight;
window._fireM = fireMat;
""".trimIndent()

    // ── RUINAS ────────────────────────────────────────────────────────────────
    "ruina" -> """
var ruinMat = makeMat(0x5A5548,0.97,0.02);
[[0,0.7,-1.5,2.4,1.4,0.3],[-1.5,0.5,0,0.35,1.0,1.8]].forEach(function(w){
    addMesh(new THREE.BoxGeometry(w[3],w[4],w[5]), ruinMat, w[0],w[1],w[2]);
    addMesh(new THREE.BoxGeometry(w[3]*0.1,w[4]*0.6,w[5]+0.01),
        makeMat(0x333330,0.99), w[0]+w[3]*0.2,w[1]-0.1,w[2]);
});
addMesh(new THREE.CylinderGeometry(0.18,0.20,1.5,10), ruinMat, 1.3,0.75,0.3);
addMesh(new THREE.CylinderGeometry(0.18,0.20,0.6,10), ruinMat, 1.3,0.3,0.3, 0,0,0.4);
[[0.4,0.12,0.8],[-0.8,0.1,-0.6],[0.9,0.15,-1.0],[-1.2,0.08,0.5],[0.1,0.1,1.4]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.2+Math.random()*0.25,0.1+Math.random()*0.15,0.15+Math.random()*0.2),
        ruinMat, p[0],p[1],p[2], Math.random()*0.3,Math.random()*Math.PI,Math.random()*0.2);
});
[[0,0.9,-1.48],[-0.2,1.1,-1.5],[0.3,0.6,-1.49]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(0.08,5,4), makeMat(0x1A4A0A,0.95), p[0],p[1],p[2]);
});
addMesh(new THREE.CircleGeometry(0.5,12), makeMat(0x1A1A20,0.1,0.4), -0.3,0.01,0.8,-Math.PI/2);
""".trimIndent()

    // ── DEFAULT (tipo no reconocido) ──────────────────────────────────────────
    else -> """
var orbMat = makeMat(0x331155,0.1,0.3,0xAA44FF,1.5);
addMesh(new THREE.SphereGeometry(0.65,16,14), orbMat, 0,1.5,0);
[0.9,1.1,1.25].forEach(function(r,i){
    var ringMat = makeMat(0x000000,0.1,0.5,0x6622AA+(i*0x111111),0.8);
    var ring = addMesh(new THREE.TorusGeometry(r,0.04,8,40), ringMat, 0,1.5,0);
    ring.rotation.x = i*0.4;
    ring.rotation.z = i*0.6;
});
for(var s=0;s<20;s++){
    var angle = (s/20)*Math.PI*2;
    var dist = 1.4+Math.random()*0.5;
    addMesh(new THREE.SphereGeometry(0.04,4,3),
        makeMat(0x000000,0.0,0.0,0xFFFFFF+(Math.floor(Math.random()*0xAAAAAA)),1.0),
        Math.sin(angle)*dist, 1.5+Math.sin(angle*2+s)*0.4, Math.cos(angle)*dist);
}
var orbLight = new THREE.PointLight(0x9933FF,1.5,5);
orbLight.position.set(0,1.5,0);
scene.add(orbLight);
""".trimIndent()
}