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

import com.example.aidungeonmaster.utils.AdventureMusicEngine

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.LocationLifeState
import com.example.aidungeonmaster.viewmodel.InventoryViewModel

private class GalleryModelBridge(
    private val canOpenShop: () -> Boolean,
    private val onOpenShop: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onModelTap() {
        if (canOpenShop()) {
            handler.post { onOpenShop() }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationsGalleryScreen(
    mapState: WorldMapState,
    charId: String,
    characterName: String,
    onBack: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    var selectedLocation by remember(mapState.locations) {
        mutableStateOf(mapState.locations.firstOrNull())
    }

    val character by inventoryViewModel.character.collectAsState()
    var showShop by remember { mutableStateOf(false) }

    LaunchedEffect(charId) {
        inventoryViewModel.loadInventory(charId)
    }

    DisposableEffect(Unit) {
        AdventureMusicEngine.setScreen(AdventureMusicEngine.MusicScreen.GALLERY)
        onDispose {
            AdventureMusicEngine.releaseScreen(700L)
        }
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
                val currentLifeState = mapState.locationStates[currentLoc.id]

                val baseVisualType = resolveBaseLocationVisualType(currentLoc)
                val visualType = resolveLocationVisualType(currentLoc, currentLifeState)
                val isInteractiveShop = currentLoc.isCurrentLocation && baseVisualType == "tienda"

                val latestIsInteractiveShop by rememberUpdatedState(isInteractiveShop)
                val latestOpenShopAction by rememberUpdatedState(newValue = { showShop = true })

                val html = remember(
                    currentLoc.id,
                    currentLifeState?.danger,
                    currentLifeState?.mood,
                    currentLifeState?.corruption,
                    isInteractiveShop
                ) {
                    buildLocationHtml(
                        location = currentLoc,
                        lifeState = currentLifeState,
                        isShopInteractive = isInteractiveShop
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            webViewClient = WebViewClient()
                            setBackgroundColor(android.graphics.Color.parseColor("#0D0700"))

                            addJavascriptInterface(
                                GalleryModelBridge(
                                    canOpenShop = { latestIsInteractiveShop },
                                    onOpenShop = { latestOpenShopAction() }
                                ),
                                "AndroidGallery"
                            )
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
                                visualType.replaceFirstChar { it.uppercase() },
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

                if (isInteractiveShop) {
                    Text(
                        "🛒 Toca el modelo para abrir la tienda",
                        color = Color(0xAAFFD700),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                    )
                }
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

    val shopLocation = selectedLocation ?: mapState.locations.firstOrNull()

    if (showShop && shopLocation != null) {
        SupermarketShopOverlay(
            supermarketName = shopLocation.name,
            gameId = charId,
            currentCoins = character?.coins ?: 0,
            inventoryViewModel = inventoryViewModel,
            onDismiss = { showShop = false }
        )
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

private fun normalizeText(value: String): String =
    Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

private fun containsAny(text: String, options: List<String>): Boolean =
    options.any { text.contains(it) }

private fun resolveBaseLocationVisualType(location: WorldLocation): String {
    val context = normalizeText("${location.type} ${location.name} ${location.description}")

    return when {
        containsAny(context, listOf("cascada", "catarata", "salto de agua")) -> "cascada"
        containsAny(context, listOf("rio", "arroyo", "quebrada", "ribera", "afluente")) -> "rio"
        containsAny(context, listOf("oceano", "alta mar", "mar abierto")) -> "océano"
        containsAny(context, listOf("mar", "playa", "costa", "litoral", "bahia", "muelle", "puerto")) -> "mar"
        containsAny(context, listOf("lago", "laguna", "estanque")) -> "lago"
        containsAny(
            context,
            listOf("tienda", "mercado", "puesto", "comercio", "almacen", "provisiones", "comida", "shop", "store")
        ) -> "tienda"
        containsAny(context, listOf("cabana", "choza", "refugio", "casita", "cottage", "hut")) -> "cabana"
        else -> normalizeLocationType(location.type)
    }
}

private fun resolveLocationVisualType(
    location: WorldLocation,
    lifeState: LocationLifeState? = null
): String {
    val base = resolveBaseLocationVisualType(location)
    val fullText = normalizeText("${location.name} ${location.description} ${location.type}")

    val isBesiegedByName = containsAny(
        fullText,
        listOf("asediada", "asediado", "asedio")
    )

    return if (isBesiegedByName) "asediada" else base
}

// ── ANÁLISIS DE DESCRIPCIÓN ───────────────────────────────────────────────────

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
        listOf("cascada", "catarata", "salto de agua").any { it in normalized } -> "cascada"
        listOf("rio", "arroyo", "quebrada", "afluente", "ribera").any { it in normalized } -> "rio"
        listOf("oceano", "alta mar", "mar abierto").any { it in normalized } -> "océano"
        listOf("puerto", "mar", "playa", "costa", "litoral", "bahia", "muelle").any { it in normalized } -> "mar"
        listOf("lago", "laguna", "estanque").any { it in normalized } -> "lago"
        listOf("cueva", "gruta", "caverna").any { it in normalized } -> "cueva"
        listOf("montana", "pico", "cordillera").any { it in normalized } -> "montaña"
        else -> normalized
    }
}

// ── GENERADOR DE HTML Three.js ────────────────────────────────────────────────

private fun buildLocationHtml(
    location: WorldLocation,
    lifeState: LocationLifeState? = null,
    isShopInteractive: Boolean = false
): String {
    val type = resolveLocationVisualType(location, lifeState)
    val rawTags = parseDescriptionTags(location.description)
    val tags = if (type in setOf("lago", "mar", "océano", "oceano", "rio", "cascada")) {
        rawTags - "agua_fondo"
    } else {
        rawTags
    }
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
function makeMat(color, rough, metal, emissive, emissiveInt, opacity, transparent) {
    return new THREE.MeshStandardMaterial({
        color: color,
        roughness: rough !== undefined ? rough : 0.8,
        metalness: metal !== undefined ? metal : 0.0,
        emissive: emissive !== undefined ? emissive : 0x000000,
        emissiveIntensity: emissiveInt !== undefined ? emissiveInt : 0.0,
        opacity: opacity !== undefined ? opacity : 1.0,
        transparent: transparent !== undefined ? transparent : (opacity !== undefined && opacity < 1.0)
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

function addRibbonXZ(points, width, mat, y) {
    var left = [];
    var right = [];

    for (var i = 0; i < points.length; i++) {
        var p0 = points[Math.max(0, i - 1)];
        var p1 = points[Math.min(points.length - 1, i + 1)];

        var dx = p1[0] - p0[0];
        var dz = p1[1] - p0[1];
        var len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.0001) len = 1.0;

        var nx = -dz / len;
        var nz =  dx / len;

        left.push(new THREE.Vector2(
            points[i][0] + nx * width * 0.5,
            points[i][1] + nz * width * 0.5
        ));
        right.push(new THREE.Vector2(
            points[i][0] - nx * width * 0.5,
            points[i][1] - nz * width * 0.5
        ));
    }

    var shape = new THREE.Shape();
    shape.moveTo(left[0].x, left[0].y);

    for (var li = 1; li < left.length; li++) {
        shape.lineTo(left[li].x, left[li].y);
    }
    for (var ri = right.length - 1; ri >= 0; ri--) {
        shape.lineTo(right[ri].x, right[ri].y);
    }
    shape.closePath();

    var geo = new THREE.ShapeGeometry(shape);
    var mesh = new THREE.Mesh(geo, mat);
    mesh.rotation.x = -Math.PI / 2;
    mesh.position.y = y || 0.0;
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    scene.add(mesh);
    return mesh;
}

// ── MODELO BASE ───────────────────────────────────────────────────────────────
$modelCode

// ── EXTRAS DE DESCRIPCIÓN ─────────────────────────────────────────────────────
$extraCode

// ── CONTROL DE ÓRBITA ─────────────────────────────────────────────────────────
var theta = 0.3, phi = 0.35, radius = 5.5;
var dragging = false, lastX = 0, lastY = 0;
var autoRotate = true;

var SHOP_INTERACTIVE = ${if (isShopInteractive) "true" else "false"};
var tapMoved = false;
var tapStartX = 0;
var tapStartY = 0;

function beginTap(x, y) {
    tapMoved = false;
    tapStartX = x || 0;
    tapStartY = y || 0;
}

function updateTap(x, y) {
    var dx = Math.abs((x || 0) - tapStartX);
    var dy = Math.abs((y || 0) - tapStartY);
    if (dx > 8 || dy > 8) tapMoved = true;
}

function maybeOpenShop() {
    if (
        SHOP_INTERACTIVE &&
        !tapMoved &&
        window.AndroidGallery &&
        window.AndroidGallery.onModelTap
    ) {
        window.AndroidGallery.onModelTap();
    }
}

function applyCamera() {
    camera.position.x = radius * Math.cos(phi) * Math.sin(theta);
    camera.position.y = radius * Math.sin(phi) + 0.5;
    camera.position.z = radius * Math.cos(phi) * Math.cos(theta);
    camera.lookAt(0, 0.6, 0);
}

document.addEventListener('touchstart', function(e) {
    dragging = true;
    autoRotate = false;
    lastX = e.touches[0].clientX;
    lastY = e.touches[0].clientY;
    beginTap(lastX, lastY);
}, {passive:true});

document.addEventListener('touchmove', function(e) {
    if (!dragging) return;

    var x = e.touches[0].clientX;
    var y = e.touches[0].clientY;

    updateTap(x, y);

    theta -= (x - lastX) * 0.012;
    phi = Math.max(-0.5, Math.min(1.0, phi + (y - lastY) * 0.006));
    lastX = x;
    lastY = y;
    applyCamera();
}, {passive:true});

document.addEventListener('touchend', function() {
    dragging = false;
    maybeOpenShop();
}, {passive:true});

document.addEventListener('mousedown', function(e) {
    dragging = true;
    autoRotate = false;
    lastX = e.clientX;
    lastY = e.clientY;
    beginTap(lastX, lastY);
});

document.addEventListener('mousemove', function(e) {
    if (!dragging) return;

    updateTap(e.clientX, e.clientY);

    theta -= (e.clientX - lastX) * 0.009;
    phi = Math.max(-0.5, Math.min(1.0, phi + (e.clientY - lastY) * 0.005));
    lastX = e.clientX;
    lastY = e.clientY;
    applyCamera();
});

document.addEventListener('mouseup', function() {
    dragging = false;
    maybeOpenShop();
});

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
    "asediada"           -> "0x2A120C"
    "bosque"             -> "0x0A1505"
    "cueva"              -> "0x0A0A0F"
    "mazmorra"           -> "0x080305"
    "ciudad"             -> "0x0D1015"
    "pueblo"             -> "0x0D0F0A"
    "montaña", "montana" -> "0x080D12"
    "templo"             -> "0x0D0D10"
    "torre"              -> "0x080810"
    "lago"               -> "0x0A1624"
    "rio"                -> "0x0A1B22"
    "cascada"            -> "0x09161D"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0x69B6D1"
    "océano", "oceano"   -> "0x061630"
    "desierto"           -> "0x150C05"
    "taberna"            -> "0x0D0905"
    "tienda"             -> "0x140C05"
    "cabana"             -> "0x0B1208"
    "ruina"              -> "0x0C0C0A"
    else                 -> "0x0D0700"
}

private fun getAmbientColor(type: String) = when (type) {
    "asediada"           -> "0x34130C"
    "bosque"             -> "0x0D2205"
    "cueva", "mazmorra"  -> "0x140F1A"
    "lago"               -> "0x0A2038"
    "rio"                -> "0x123247"
    "cascada"            -> "0x16394A"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0x5CA8C5"
    "océano", "oceano"   -> "0x0B2545"
    "desierto"           -> "0x201505"
    "taberna"            -> "0x201005"
    "tienda"             -> "0x261305"
    "cabana"             -> "0x14240C"
    else                 -> "0x111118"
}

private fun getSunColor(type: String) = when (type) {
    "asediada"           -> "0xFF8A4A"
    "bosque"             -> "0xAAFF88"
    "cueva", "mazmorra"  -> "0xFF7722"
    "lago"               -> "0x9FDBFF"
    "rio"                -> "0xB9ECFF"
    "cascada"            -> "0xD9F7FF"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0xFFF1D0"
    "océano", "oceano"   -> "0x7FC8FF"
    "desierto"           -> "0xFFDD88"
    "templo"             -> "0xFFEECC"
    "taberna"            -> "0xFF9944"
    "tienda"             -> "0xFFD089"
    "cabana"             -> "0xFFE4B0"
    else                 -> "0xFFDDAA"
}

private fun getGroundColor(type: String) = when (type) {
    "asediada"           -> "0x5A3A1E"
    "bosque"             -> "0x1A2E0A"
    "cueva"              -> "0x2A2830"
    "mazmorra"           -> "0x151212"
    "ciudad"             -> "0x333340"
    "pueblo"             -> "0x2A2010"
    "montaña", "montana" -> "0x303030"
    "templo"             -> "0xD4C8A0"
    "lago"               -> "0x123A58"
    "rio"                -> "0x314B36"
    "cascada"            -> "0x39454E"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0xD6BA8C"
    "océano", "oceano"   -> "0x0A2746"
    "desierto"           -> "0x8B6914"
    "taberna"            -> "0x2A1A08"
    "tienda"             -> "0x3A260F"
    "cabana"             -> "0x253018"
    "ruina"              -> "0x282820"
    else                 -> "0x1A1505"
}

private fun getFogDensity(type: String) = when (type) {
    "asediada"          -> "0.05"
    "bosque"            -> "0.06"
    "cueva", "mazmorra" -> "0.06"
    "lago"              -> "0.045"
    "rio"               -> "0.04"
    "cascada"           -> "0.055"
    "mar", "playa", "costa", "litoral", "bahía", "bahia" -> "0.02"
    "océano", "oceano"  -> "0.055"
    "tienda"            -> "0.03"
    "cabana"            -> "0.04"
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
            w.position.y = (w.userData.baseY || 0.04) + Math.sin(t*0.8+i*1.1)*0.018;
            if(w.material) w.material.emissiveIntensity = 0.03 + (Math.sin(t*0.9+i)+1)*0.02;
        });"""

    "rio" ->
        """if(window._riverSegments) window._riverSegments.forEach(function(seg,i){
            seg.position.y = seg.userData.baseY + Math.sin(t*1.2 + i*0.8) * 0.004;
        });
        if(window._riverCurrents) window._riverCurrents.forEach(function(c,i){
            c.position.x = c.userData.baseX + Math.sin(t*1.5 + i*0.7) * 0.025;
            c.position.z = c.userData.baseZ + Math.cos(t*1.1 + i*0.6) * 0.015;
            c.material.emissiveIntensity = 0.05 + (Math.sin(t*1.9 + i) + 1.0) * 0.02;
        });"""

    "cascada" ->
        """if(window._fallStrands) window._fallStrands.forEach(function(s,i){
            s.position.x = (s.userData.baseX || 0) + Math.sin(t*2.6+i)*0.02;
            if(s.material) s.material.emissiveIntensity = 0.08 + (Math.sin(t*3.2+i)+1)*0.04;
        });
        if(window._fallDrops) window._fallDrops.forEach(function(d){
            d.position.y -= d.userData.speed;
            if(d.position.y < d.userData.minY){
                d.position.y = d.userData.maxY;
                d.position.x = d.userData.baseX + (Math.random()-0.5)*0.5;
                d.position.z = d.userData.baseZ + (Math.random()-0.5)*0.08;
            }
        });
        if(window._spray) window._spray.forEach(function(s,i){
            s.position.y = s.userData.baseY + Math.sin(t*3.0+i)*0.05;
            s.position.x = s.userData.baseX + Math.sin(t*1.6+i)*0.04;
        });
        if(window._poolWaves) window._poolWaves.forEach(function(w,i){
            w.position.y = (w.userData.baseY || 0.055) + Math.sin(t*1.3+i)*0.012;
        });"""

    "mar", "playa", "costa", "litoral", "bahía", "bahia" ->
        """if(window._waves) window._waves.forEach(function(w,i){
            w.position.y = w.userData.baseY + Math.sin(t*1.05 + i*0.7) * 0.008;
            w.position.z = w.userData.baseZ + Math.cos(t*0.8 + i*0.6) * 0.025;
        });
        if(window._shoreFoam) window._shoreFoam.forEach(function(f,i){
            f.position.z = f.userData.baseZ + Math.sin(t*1.5 + i*0.8) * 0.03;
            f.material.emissiveIntensity = 0.08 + (Math.sin(t*1.8 + i) + 1.0) * 0.03;
        });"""

    "océano", "oceano" ->
        """if(window._waves) window._waves.forEach(function(w,i){
            w.position.y = 0.08 + Math.sin(t*1.7+i*1.5)*0.07;
            if(w.material) w.material.opacity = 0.70 + (Math.sin(t*1.2+i)+1)*0.08;
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

    "tienda" ->
        """if(window._shopLanterns) window._shopLanterns.forEach(function(l,i){
            l.intensity = 0.95 + Math.sin(t*5 + i*1.6)*0.18;
        });
        if(window._shopSign){
            window._shopSign.rotation.z = Math.sin(t*1.6)*0.08;
        }"""

    "cabana" ->
        """if(window._cabinFire){
            window._cabinFire.intensity = 1.15 + Math.sin(t*4.2)*0.22;
        }
        if(window._smoke) window._smoke.forEach(function(s,i){
            s.position.y = 2.45 + ((t*0.45 + i*0.28) % 1.0);
            s.position.x = 0.82 + Math.sin(t*1.3 + i)*0.04;
            s.position.z = -0.20 + Math.cos(t*1.1 + i)*0.03;
        });"""

    "mazmorra" ->
        """if(window._torches) window._torches.forEach(function(tp,i){
            tp.intensity = 0.8 + Math.sin(t*4+i*1.5)*0.3;
        });"""

    "asediada" ->
        """if(window._siegeFires) window._siegeFires.forEach(function(f,i){
            f.intensity = 0.95 + Math.sin(t*5.2 + i*1.4) * 0.35;
        });
        if(window._siegeFireMats) window._siegeFireMats.forEach(function(m,i){
            m.emissiveIntensity = 1.1 + Math.sin(t*4.4 + i) * 0.25;
        });
        if(window._siegeSmoke) window._siegeSmoke.forEach(function(s,i){
            s.position.y = s.userData.baseY + ((t*0.28 + i*0.17) % 1.15);
            s.position.x = s.userData.baseX + Math.sin(t*1.1 + i) * 0.04;
            s.position.z = s.userData.baseZ + Math.cos(t*0.9 + i) * 0.03;
        });"""

    else -> "// no animation"
}

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

    // ── CASCADA ────────────────────────────────────────────────────────────────

    "cascada" -> """
var cliffMat      = makeMat(0x58636B,0.96,0.02);
var darkCliffMat  = makeMat(0x424C54,0.97,0.02);
var mossMat       = makeMat(0x456A3D,0.92,0.0);
var fallMat       = makeMat(0x7FD8FF,0.10,0.06,0xBEEFFF,0.08,0.72,true);
var fallCoreMat   = makeMat(0xDFF8FF,0.06,0.04,0xD8F4FF,0.10,0.58,true);
var poolMat       = makeMat(0x3BA7CF,0.10,0.06,0x12374C,0.05,0.92,true);
var poolDeepMat   = makeMat(0x236B8E,0.12,0.08,0x0A2230,0.05,0.96,true);
var foamMat       = makeMat(0xF2FBFF,0.16,0.03,0xD6F4FF,0.10,0.82,true);

// Pared rocosa
addMesh(new THREE.BoxGeometry(4.2,2.8,1.6), cliffMat, 0,1.4,-0.9);
addMesh(new THREE.BoxGeometry(2.2,0.28,1.0), darkCliffMat, 0,2.15,0.05);
addMesh(new THREE.BoxGeometry(1.2,0.18,0.7), darkCliffMat, 0,2.00,0.55);

// Salientes laterales
[[-1.8,1.0,0.0,0.9],[1.7,0.8,-0.1,0.8],[-1.2,2.0,-0.2,0.6]].forEach(function(r){
    addMesh(new THREE.SphereGeometry(r[3],8,6), darkCliffMat, r[0],r[1],r[2], 0,Math.random()*Math.PI,0, 1.2,0.55,1.0);
});

// Musgo
[[-1.35,1.45,0.1],[1.2,1.25,0.0],[-0.6,2.0,-0.15]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(0.24,7,5), mossMat, p[0],p[1],p[2], 0,0,0, 1.2,0.35,0.8);
});

// Láminas de agua
var fallStrands = [];
for(var i=0;i<7;i++){
    var x = -0.72 + i*0.24;
    var strand = addMesh(
        new THREE.BoxGeometry(0.18 + Math.random()*0.03, 2.15, 0.09),
        i % 2 === 0 ? fallMat : fallCoreMat,
        x, 1.02, 0.55,
        0,0,(i%2===0 ? 0.01 : -0.01)
    );
    strand.userData.baseX = x;
    fallStrands.push(strand);
}
window._fallStrands = fallStrands;

// Poza
addMesh(new THREE.CircleGeometry(1.85,32), poolMat, 0,0.03,1.55, -Math.PI/2);
addMesh(new THREE.CircleGeometry(1.05,28), poolDeepMat, 0,0.036,1.55, -Math.PI/2);

// Ondas de la poza
var poolWaves = [];
[0.45,0.78,1.10].forEach(function(r, i){
    var ring = addMesh(
        new THREE.TorusGeometry(r,0.03,6,32),
        foamMat,
        0,0.055 + i*0.004,1.55,
        Math.PI/2,0,0
    );
    ring.userData.baseY = 0.055 + i*0.004;
    poolWaves.push(ring);
});
window._poolWaves = poolWaves;

// Gotas cayendo
var fallDrops = [];
for(var di=0; di<16; di++){
    var d = addMesh(
        new THREE.SphereGeometry(0.05 + Math.random()*0.03,5,4),
        foamMat,
        (Math.random()-0.5)*0.6,
        1.8 - Math.random()*1.4,
        0.58 + (Math.random()-0.5)*0.06
    );
    d.userData.baseX = 0;
    d.userData.baseZ = 0.58;
    d.userData.minY = 0.18;
    d.userData.maxY = 2.05;
    d.userData.speed = 0.035 + Math.random()*0.025;
    fallDrops.push(d);
}
window._fallDrops = fallDrops;

// Neblina/spray
var spray = [];
for(var si=0; si<14; si++){
    var s = addMesh(
        new THREE.SphereGeometry(0.07 + Math.random()*0.05,5,4),
        makeMat(0xF4FCFF,0.10,0.02,0xD7F4FF,0.08,0.55,true),
        (Math.random()-0.5)*1.0,
        0.22 + Math.random()*0.25,
        1.15 + Math.random()*0.55
    );
    s.userData.baseX = s.position.x;
    s.userData.baseY = s.position.y;
    spray.push(s);
}
window._spray = spray;

// Rocas de base
[[-1.1,0.14,1.0],[1.05,0.16,1.15],[-0.55,0.10,2.0],[0.75,0.12,2.1]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],7,5), darkCliffMat, p[0],p[1]*0.45,p[2], 0,Math.random()*Math.PI,0, 1.5,0.55,1.0);
});

// Luz
var fallGlow = new THREE.PointLight(0xA9EFFF, 1.0, 8);
fallGlow.position.set(0,1.7,1.0);
scene.add(fallGlow);

ambient.color.setHex(0x16394A);
ambient.intensity = 0.90;
scene.fog = new THREE.FogExp2(0x09161D, 0.055);
""".trimIndent()

    // ── RÍO ────────────────────────────────────────────────────────────────

    "rio" -> """
var grassMat      = makeMat(0x476E49,0.97,0.0);
var bankMat       = makeMat(0x7E9860,0.95,0.0);
var riverMat      = makeMat(0x59CBEA,0.12,0.03,0x113B56,0.03,0.94,true);
var riverCoreMat  = makeMat(0x2E99C5,0.10,0.05,0x0B2433,0.04,0.97,true);
var currentMat    = makeMat(0xF0FCFF,0.18,0.02,0xD7F3FF,0.08,0.72,true);
var stoneMat      = makeMat(0x737974,0.96,0.02);
var reedMat       = makeMat(0x6C8B3D,0.88,0.0);

// Terreno
addMesh(new THREE.CircleGeometry(6.8,40), grassMat, 0,-0.02,0, -Math.PI/2);

// Orillas suaves
[[-2.8,2.2,0.50,1.7],[-1.7,1.1,0.42,1.4],[-0.2,-0.2,0.35,1.2],[1.6,-1.6,0.46,1.5],[2.5,-2.3,0.38,1.3],[-2.3,-1.9,0.50,1.5]].forEach(function(b){
    addMesh(new THREE.SphereGeometry(b[2],9,6), bankMat, b[0],-b[2]*0.28,b[1], 0,0,0, b[3],0.45,1.1);
});

// Trazado principal del río
var riverPath = [
    [-2.35,  2.25],
    [-1.65,  1.45],
    [-0.88,  0.62],
    [-0.08, -0.18],
    [ 0.72, -1.02],
    [ 1.52, -1.82],
    [ 2.15, -2.48]
];

// Cuerpo del río
var riverBody = addRibbonXZ(riverPath, 1.38, riverMat, 0.028);
riverBody.userData.baseY = 0.028;

var riverCore = addRibbonXZ(riverPath, 0.82, riverCoreMat, 0.034);
riverCore.userData.baseY = 0.034;

window._riverSegments = [riverBody, riverCore];

// Corrientes suaves
var currents = [];
[
    [-1.90,  1.72, 0.18, 0.42, 0.10],
    [-1.12,  0.92, 0.10, 0.38, 0.09],
    [-0.28,  0.08, 0.02, 0.36, 0.09],
    [ 0.52, -0.72,-0.08, 0.40, 0.10],
    [ 1.30, -1.52,-0.15, 0.34, 0.08]
].forEach(function(c){
    var streak = addMesh(
        new THREE.CircleGeometry(1.0,20),
        currentMat,
        c[0], 0.048, c[1],
        -Math.PI/2, 0, c[2],
        c[3], c[4], 1.0
    );
    streak.userData = {
        baseX: c[0],
        baseZ: c[1]
    };
    currents.push(streak);
});
window._riverCurrents = currents;

// Rocas
[[-2.9,0.16,1.2],[-2.0,0.12,0.35],[-0.1,0.13,1.55],[1.1,0.12,-0.30],[2.3,0.15,-1.05]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],7,5), stoneMat, p[0],p[1]*0.45,p[2], 0,Math.random()*Math.PI,0, 1.3,0.55,1.0);
});

// Juncos
[[-2.45,0.38,1.75],[-1.85,0.38,1.38],[-0.95,0.38,1.85],[1.55,0.38,-0.72],[2.18,0.38,-1.40]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.02,0.025,0.55,5), reedMat, p[0],p[1],p[2]);
    addMesh(new THREE.BoxGeometry(0.04,0.18,0.01), reedMat, p[0]+0.03,p[1]+0.15,p[2], 0,0,0.35);
    addMesh(new THREE.BoxGeometry(0.04,0.18,0.01), reedMat, p[0]-0.03,p[1]+0.12,p[2], 0,0,-0.35);
});

var riverGlow = new THREE.PointLight(0x8BE7FF, 0.75, 7);
riverGlow.position.set(-0.2,1.1,0);
scene.add(riverGlow);

ambient.color.setHex(0x143448);
ambient.intensity = 0.78;
scene.fog = new THREE.FogExp2(0x0A1B22, 0.038);
""".trimIndent()

    // ── CABAÑA ────────────────────────────────────────────────────────────────

    "cabana" -> """
var logMat      = makeMat(0x7A4A24,0.92,0.02);
var darkLogMat  = makeMat(0x563116,0.95,0.02);
var roofMat     = makeMat(0x4A2A18,0.96,0.02);
var stoneMat    = makeMat(0x6E6A64,0.95,0.02);
var windowGlow  = makeMat(0x000000,0.0,0.0,0xFFCC66,1.7);
var leafMat     = makeMat(0x2A6B1B,0.90,0.0);

// Suelo de la cabaña
addMesh(new THREE.BoxGeometry(2.4,0.14,2.2), darkLogMat, 0,0.07,0);

// Cuerpo principal
addMesh(new THREE.BoxGeometry(2.0,1.35,1.7), logMat, 0,0.74,0);

// Troncos frontales decorativos
for(var ly=0; ly<6; ly++){
    addMesh(new THREE.CylinderGeometry(0.055,0.055,2.02,8), darkLogMat, 0,0.16+ly*0.22,0.86, 0,0,Math.PI/2);
    addMesh(new THREE.CylinderGeometry(0.055,0.055,2.02,8), darkLogMat, 0,0.16+ly*0.22,-0.86, 0,0,Math.PI/2);
}

// Tejado inclinado
addMesh(new THREE.BoxGeometry(2.28,0.12,1.12), roofMat, 0,1.62,0.38, 0.55,0,0);
addMesh(new THREE.BoxGeometry(2.28,0.12,1.12), roofMat, 0,1.62,-0.38, -0.55,0,0);
addMesh(new THREE.BoxGeometry(1.9,0.07,0.10), darkLogMat, 0,1.87,0);

// Porche
addMesh(new THREE.BoxGeometry(1.50,0.12,0.72), darkLogMat, 0,0.12,1.18);
[[-0.58,0.34,1.18],[0.58,0.34,1.18]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.09,0.68,0.09), darkLogMat, p[0],p[1],p[2]);
});
addMesh(new THREE.BoxGeometry(0.70,0.10,0.36), makeMat(0x6B4A2B,0.95,0.0), 0,0.05,1.56);

// Puerta
addMesh(new THREE.BoxGeometry(0.42,0.82,0.06), darkLogMat, 0,0.42,0.88);
addMesh(new THREE.SphereGeometry(0.03,5,4), makeMat(0xCCAA55,0.4,0.5), 0.12,0.40,0.92);

// Ventanas
[[-0.62,0.92,0.87],[0.62,0.92,0.87]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.28,0.26,0.05), windowGlow, p[0],p[1],p[2]);
    addMesh(new THREE.BoxGeometry(0.04,0.26,0.06), darkLogMat, p[0],p[1],p[2]+0.01);
    addMesh(new THREE.BoxGeometry(0.28,0.04,0.06), darkLogMat, p[0],p[1],p[2]+0.01);
});

// Chimenea
addMesh(new THREE.BoxGeometry(0.28,0.95,0.28), stoneMat, 0.80,2.00,-0.18);
addMesh(new THREE.BoxGeometry(0.36,0.08,0.36), stoneMat, 0.80,2.50,-0.18);

// Humo
var smoke = [];
[0,1,2].forEach(function(i){
    var puff = addMesh(
        new THREE.SphereGeometry(0.12 + i*0.03,6,5),
        makeMat(0x7B7B7B,0.96,0.0),
        0.82, 2.45 + i*0.25, -0.20
    );
    smoke.push(puff);
});
window._smoke = smoke;

// Leña apilada
[[-1.18,0.12,0.78],[-1.05,0.12,0.78],[-0.92,0.12,0.78]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.06,0.06,0.42,8), makeMat(0x6A4324,0.94,0.0), p[0],p[1],p[2], Math.PI/2,0,0);
});

// Tocón
addMesh(new THREE.CylinderGeometry(0.16,0.18,0.28,10), makeMat(0x7A4A24,0.92,0.02), 1.28,0.14,1.00);

// Pino lateral
addMesh(new THREE.CylinderGeometry(0.07,0.10,0.85,8), makeMat(0x5C3A1E,0.92,0.0), -1.55,0.42,-0.50);
addMesh(new THREE.ConeGeometry(0.42,0.75,7), leafMat, -1.55,1.05,-0.50);
addMesh(new THREE.ConeGeometry(0.30,0.55,7), makeMat(0x2F7A20,0.88,0.0), -1.55,1.40,-0.50);

// Luz cálida interior
var cabinFire = new THREE.PointLight(0xFF9933,1.15,4.5);
cabinFire.position.set(0,1.00,0.55);
scene.add(cabinFire);
window._cabinFire = cabinFire;

// Luz tenue exterior
var porchGlow = new THREE.PointLight(0xFFCC88,0.5,3.0);
porchGlow.position.set(0,1.2,1.2);
scene.add(porchGlow);
""".trimIndent()

    // ── TIENDA ────────────────────────────────────────────────────────────────

    "tienda" -> """
var wallMat     = makeMat(0xC9B58A,0.92,0.02);
var woodMat     = makeMat(0x8B5A2B,0.90,0.02);
var darkWoodMat = makeMat(0x5C3416,0.94,0.02);
var roofMat     = makeMat(0x6B2A1E,0.88,0.04);
var clothRed    = makeMat(0x9E2F22,0.78,0.02);
var clothGold   = makeMat(0xC9971A,0.76,0.02);
var glowMat     = makeMat(0x000000,0.0,0.0,0xFFBB55,2.0);
var fruitRed    = makeMat(0xC63B2D,0.72,0.0);
var fruitGreen  = makeMat(0x6FAF2D,0.72,0.0);

// Base
addMesh(new THREE.BoxGeometry(2.6,0.18,2.0), darkWoodMat, 0,0.09,0);
addMesh(new THREE.BoxGeometry(2.2,1.4,1.5), wallMat, 0,0.79,0);

// Marcos laterales
[[-1.0,0.8,0.0],[1.0,0.8,0.0]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.12,1.5,1.55), darkWoodMat, p[0],p[1],p[2]);
});
addMesh(new THREE.BoxGeometry(2.2,0.12,1.55), darkWoodMat, 0,1.50,0);

// Tejado
addMesh(new THREE.BoxGeometry(2.45,0.12,1.05), roofMat, 0,1.78,0.34, 0.42,0,0);
addMesh(new THREE.BoxGeometry(2.45,0.12,1.05), roofMat, 0,1.78,-0.34, -0.42,0,0);
addMesh(new THREE.BoxGeometry(2.0,0.08,0.12), darkWoodMat, 0,1.95,0);

// Puerta
addMesh(new THREE.BoxGeometry(0.42,0.82,0.06), darkWoodMat, -0.62,0.41,0.78);
addMesh(new THREE.SphereGeometry(0.03,5,4), makeMat(0xCCAA55,0.4,0.5), -0.48,0.40,0.83);

// Ventana
addMesh(new THREE.BoxGeometry(0.34,0.34,0.05), glowMat, 0.65,0.95,0.79);
addMesh(new THREE.BoxGeometry(0.04,0.34,0.06), darkWoodMat, 0.65,0.95,0.81);
addMesh(new THREE.BoxGeometry(0.34,0.04,0.06), darkWoodMat, 0.65,0.95,0.81);

// Toldo delantero
addMesh(new THREE.BoxGeometry(2.0,0.06,0.82), clothRed, 0,1.12,1.06, -0.22,0,0);
addMesh(new THREE.BoxGeometry(2.0,0.03,0.14), clothGold, 0,0.96,1.42);
[[-0.92,0.58,1.10],[0.92,0.58,1.10]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.08,1.08,0.08), woodMat, p[0],p[1],p[2]);
});

// Mostrador
addMesh(new THREE.BoxGeometry(1.75,0.46,0.42), woodMat, 0,0.38,1.08);
addMesh(new THREE.BoxGeometry(1.84,0.06,0.48), darkWoodMat, 0,0.64,1.08);

// Cajas con provisiones
[[-0.62,0.18,1.36],[0.62,0.18,1.36]].forEach(function(p, idx){
    addMesh(new THREE.BoxGeometry(0.42,0.22,0.34), darkWoodMat, p[0],p[1],p[2]);
    addMesh(new THREE.SphereGeometry(0.08,6,5), idx===0 ? fruitRed : fruitGreen, p[0]-0.08,0.28,p[2]-0.04);
    addMesh(new THREE.SphereGeometry(0.08,6,5), idx===0 ? fruitGreen : fruitRed, p[0]+0.08,0.28,p[2]+0.02);
    addMesh(new THREE.SphereGeometry(0.07,6,5), fruitRed, p[0],0.30,p[2]+0.06);
});

// Barriles laterales
[[1.28,0.20,0.72],[-1.28,0.20,0.58]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.16,0.16,0.36,10), woodMat, p[0],p[1],p[2]);
    addMesh(new THREE.TorusGeometry(0.16,0.015,6,18), makeMat(0x2C2C2C,0.7,0.4), p[0],p[1]+0.11,p[2], Math.PI/2);
    addMesh(new THREE.TorusGeometry(0.16,0.015,6,18), makeMat(0x2C2C2C,0.7,0.4), p[0],p[1]-0.11,p[2], Math.PI/2);
});

// Letrero
addMesh(new THREE.BoxGeometry(0.70,0.06,0.06), darkWoodMat, 0,1.57,1.12);
var shopSign = addMesh(new THREE.BoxGeometry(0.78,0.24,0.05), makeMat(0xC49A3A,0.75,0.03), 0,1.40,1.16);
window._shopSign = shopSign;
addMesh(new THREE.BoxGeometry(0.50,0.05,0.03), woodMat, 0,1.40,1.19);

// Faroles
var shopLanterns = [];
[[-0.92,1.17,1.06],[0.92,1.17,1.06]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.015,0.015,0.16,6), darkWoodMat, p[0],p[1]+0.10,p[2]);
    addMesh(new THREE.SphereGeometry(0.08,6,5), glowMat, p[0],p[1],p[2]);
    var lt = new THREE.PointLight(0xFFBB55,1.0,3.2);
    lt.position.set(p[0],p[1],p[2]);
    scene.add(lt);
    shopLanterns.push(lt);
});
window._shopLanterns = shopLanterns;

// Camino
addMesh(new THREE.BoxGeometry(1.4,0.01,2.2), makeMat(0x8A7350,0.97,0.0), 0,0.005,1.9);

// Iluminación ambiente
var shopGlow = new THREE.PointLight(0xFFB866,0.8,4.0);
shopGlow.position.set(0,1.2,0.9);
scene.add(shopGlow);
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
var sandMat      = makeMat(0xD0BA8E,0.97,0.0);
var wetSandMat   = makeMat(0xB59668,0.98,0.0);
var seaNearMat   = makeMat(0x4CC5E2,0.12,0.04,0x14384A,0.02,0.95,true);
var seaMidMat    = makeMat(0x289EC7,0.14,0.06,0x102A3A,0.03,0.97,true);
var seaFarMat    = makeMat(0x126E9C,0.16,0.08,0x0B2031,0.04,0.98,true);
var crestMat     = makeMat(0xEAF9FF,0.18,0.02,0xD5F1FF,0.08,0.75,true);
var foamMat      = makeMat(0xF7FCFF,0.18,0.01,0xDDF4FF,0.10,0.82,true);
var rockMat      = makeMat(0x7B7972,0.96,0.02);
var driftWoodMat = makeMat(0x8A6842,0.92,0.02);

// Playa
addMesh(new THREE.BoxGeometry(8.5,0.08,4.0), sandMat, 0,-0.04,1.95);

// Dunas suaves
[[-2.8,2.35,0.48,1.7],[-1.0,2.15,0.40,1.5],[1.35,2.40,0.45,1.7],[3.0,2.0,0.34,1.2]].forEach(function(d){
    addMesh(new THREE.SphereGeometry(d[2],10,6), sandMat, d[0],-d[2]*0.28,d[1], 0,0,0, d[3],0.45,1.1);
});

// Orilla húmeda
addMesh(new THREE.BoxGeometry(8.45,0.03,0.95), wetSandMat, 0,0.005,0.45, 0.03,0,0);

// Mar en capas
addMesh(new THREE.BoxGeometry(8.8,0.05,3.0), seaNearMat, 0,0.024,-1.10);
addMesh(new THREE.BoxGeometry(8.8,0.04,3.8), seaMidMat, 0,0.021,-4.10);
addMesh(new THREE.BoxGeometry(8.8,0.03,6.8), seaFarMat, 0,0.018,-8.70);

// Crestas curvas cercanas a la orilla
var waves = [];
[
    [[-3.8, 0.28],[-1.8, 0.08],[0.0, 0.15],[1.9,-0.02],[3.8, 0.18]],
    [[-3.7,-0.35],[-1.7,-0.55],[0.1,-0.42],[1.9,-0.60],[3.7,-0.46]],
    [[-3.5,-1.00],[-1.6,-1.18],[0.1,-1.08],[1.8,-1.22],[3.5,-1.12]],
    [[-3.2,-1.75],[-1.4,-1.92],[0.2,-1.85],[1.6,-2.00],[3.2,-1.90]]
].forEach(function(points, i){
    var wave = addRibbonXZ(points, 0.14 - i*0.01, crestMat, 0.060 + i*0.008);
    wave.userData = {
        baseY: 0.060 + i*0.008,
        baseZ: 0.0,
        baseRz: 0.0
    };
    waves.push(wave);
});
window._waves = waves;

// Espuma de la orilla
var shoreFoam = [];
[
    [[-3.7, 0.48],[-2.0, 0.34],[-0.3, 0.40],[1.5, 0.20],[3.5, 0.28]],
    [[-3.5, 0.72],[-1.7, 0.58],[0.0, 0.65],[1.8, 0.48],[3.3, 0.55]]
].forEach(function(points, i){
    var foam = addRibbonXZ(points, 0.12 - i*0.015, foamMat, 0.048 + i*0.004);
    foam.userData = { baseZ: 0.0 };
    shoreFoam.push(foam);
});
window._shoreFoam = shoreFoam;

// Rocas costeras
[[2.45,0.18,0.95],[-1.8,0.14,0.75],[3.0,0.12,1.28],[-2.9,0.16,1.08]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],8,6), rockMat, p[0],p[1]*0.45,p[2], 0,Math.random()*Math.PI,0, 1.5,0.55,1.0);
});

// Tronco
addMesh(new THREE.CylinderGeometry(0.05,0.07,0.92,7), driftWoodMat, 1.4,0.07,1.72, 0.08,0,0.95);

// Islotes lejanos
[[-2.8,0.20,-6.8,1.7],[2.6,0.24,-7.7,2.0]].forEach(function(r){
    addMesh(new THREE.SphereGeometry(r[1],8,6), makeMat(0x697078,0.95,0.02), r[0],r[1]*0.30,r[2], 0,Math.random()*Math.PI,0, r[3],0.55,1.0);
});

var waterGlow = new THREE.PointLight(0x86DEFF, 0.95, 8);
waterGlow.position.set(0,1.25,-2.0);
scene.add(waterGlow);

ambient.color.setHex(0x5AA6C0);
ambient.intensity = 0.84;
scene.fog = new THREE.FogExp2(0x69B6D1, 0.019);
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

    "asediada" -> """
var timberMat    = makeMat(0x704321,0.92,0.02);
var darkWoodMat  = makeMat(0x3A2415,0.96,0.02);
var wallMat      = makeMat(0xB28A62,0.95,0.0);
var roofMat      = makeMat(0x6A2A1E,0.90,0.02);
var ashMat       = makeMat(0x2A2624,0.98,0.02);
var deadCropMat  = makeMat(0x4B3816,0.97,0.0);
var fieldMat     = makeMat(0x6A5522,0.96,0.0);
var emberMatA    = makeMat(0x000000,0.0,0.0,0xFF5522,1.2,0.92,true);
var emberMatB    = makeMat(0x000000,0.0,0.0,0xFFAA33,1.0,0.90,true);

// Suelo castigado
addMesh(new THREE.CircleGeometry(6.6,40), makeMat(0x5A3A1E,0.98,0.0), 0,0.01,0, -Math.PI/2);
[[-2.2,0.0,1.0,1.8],[1.7,-0.8,0.8,1.4],[0.0,1.7,0.7,1.3],[-0.5,-1.7,0.6,1.1]].forEach(function(b){
    addMesh(new THREE.SphereGeometry(b[2],10,6), ashMat, b[0],-b[2]*0.26,b[1], 0,0,0, b[3],0.35,1.0);
});

// Casa medio en pie
addMesh(new THREE.BoxGeometry(1.45,0.92,1.12), wallMat, -1.45,0.47,-0.15);
addMesh(new THREE.BoxGeometry(1.55,0.08,0.70), roofMat, -1.45,1.05,0.14, 0.36,0,0);
addMesh(new THREE.BoxGeometry(0.86,0.08,0.55), roofMat, -1.10,1.02,-0.30, -0.52,0,0);
addMesh(new THREE.BoxGeometry(0.26,0.48,0.06), darkWoodMat, -1.42,0.26,0.44);

// Restos de segunda casa
addMesh(new THREE.BoxGeometry(0.95,0.55,0.80), wallMat, 1.35,0.28,0.15);
addMesh(new THREE.BoxGeometry(1.10,0.08,0.34), roofMat, 1.15,0.62,0.02, 0.18,0,0.22);
addMesh(new THREE.BoxGeometry(0.55,0.08,0.28), darkWoodMat, 1.58,0.58,0.32, -0.35,0,-0.28);

// Barricadas improvisadas
[[-0.25,0.22,1.10],[0.15,0.22,1.22],[0.55,0.22,1.06]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.10,0.44,0.10), timberMat, p[0],p[1],p[2], 0,0,0.18);
    addMesh(new THREE.BoxGeometry(0.52,0.08,0.10), darkWoodMat, p[0],p[1]+0.06,p[2], 0,0,-0.35);
});

// Campo arrasado
for (var r=0; r<5; r++) {
    var z = -1.85 + r*0.32;
    addMesh(new THREE.BoxGeometry(2.2,0.03,0.08), fieldMat, 0.95,0.02,z);
}
[[-0.10,-1.80],[0.38,-1.55],[0.85,-1.35],[1.20,-1.05],[1.55,-0.82],[0.60,-0.95]].forEach(function(c){
    addMesh(new THREE.BoxGeometry(0.06,0.18,0.02), deadCropMat, c[0]+1.0,0.10,c[1], 0,0,0.15);
    addMesh(new THREE.BoxGeometry(0.06,0.14,0.02), deadCropMat, c[0]+1.06,0.08,c[1]+0.03, 0,0,-0.22);
});

// Carreta rota
addMesh(new THREE.BoxGeometry(0.48,0.18,0.32), timberMat, 0.15,0.11,0.55, 0,0,0.18);
addMesh(new THREE.TorusGeometry(0.10,0.018,6,14), darkWoodMat, -0.02,0.10,0.42, Math.PI/2,0,0.25);
addMesh(new THREE.TorusGeometry(0.10,0.018,6,14), darkWoodMat, 0.30,0.07,0.66, Math.PI/2,0,1.0);

// Brasas y fuego
var siegeFires = [];
var siegeFireMats = [];
[
    [-1.85,0.26,-0.05, emberMatA],
    [ 1.05,0.22, 0.42, emberMatB],
    [ 0.25,0.18, 1.05, emberMatA]
].forEach(function(f){
    addMesh(new THREE.SphereGeometry(0.10,6,5), f[3], f[0],f[1],f[2]);
    var light = new THREE.PointLight(0xFF6A2A, 1.0, 3.6);
    light.position.set(f[0], f[1]+0.10, f[2]);
    scene.add(light);
    siegeFires.push(light);
    siegeFireMats.push(f[3]);
});
window._siegeFires = siegeFires;
window._siegeFireMats = siegeFireMats;

// Humo
var siegeSmoke = [];
[
    [-1.85,1.10,-0.05],
    [ 1.05,0.95, 0.42],
    [ 0.25,0.78, 1.05]
].forEach(function(p, i){
    var puff = addMesh(
        new THREE.SphereGeometry(0.16 + i*0.03,6,5),
        makeMat(0x5A5A5A,0.98,0.0,0x000000,0.0,0.45,true),
        p[0], p[1], p[2],
        0,0,0, 1.0,0.75,1.0
    );
    puff.userData = { baseX: p[0], baseY: p[1], baseZ: p[2] };
    siegeSmoke.push(puff);
});
window._siegeSmoke = siegeSmoke;

// Resplandor de amenaza
var siegeGlow = new THREE.PointLight(0xAA3311, 0.55, 6.0);
siegeGlow.position.set(0,1.2,0.2);
scene.add(siegeGlow);
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