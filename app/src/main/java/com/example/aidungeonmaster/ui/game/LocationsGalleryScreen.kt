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
import com.example.aidungeonmaster.data.model.WorldLocation
import com.example.aidungeonmaster.data.model.WorldMapState

/**
 * Pantalla que muestra una galería 3D de todas las ubicaciones visitadas por el personaje.
 *
 * Usa WebView + Three.js para renderizar modelos 3D procedurales para cada tipo de lugar.
 * No requiere pago ni instalaciones adicionales: Three.js se carga desde cdnjs.cloudflare.com.
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
                            // Color de fondo mientras carga
                            setBackgroundColor(android.graphics.Color.parseColor("#0D0700"))
                        }
                    },
                    update = { webView ->
                        // "https://cdnjs.cloudflare.com" como base para que el script CDN cargue
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

// ── GENERADOR DE HTML Three.js ────────────────────────────────────────────────

/**
 * Genera un documento HTML autocontenido con Three.js (cargado desde cdnjs) que muestra
 * un modelo 3D procedural del tipo de ubicación indicado.
 *
 * - Three.js r128 desde cdnjs.cloudflare.com (gratis, sin instalar nada)
 * - Modelos construidos con geometrías básicas: cono, cilindro, caja, esfera
 * - Rotación orbital con arrastre táctil / ratón
 */
private fun buildLocationHtml(location: WorldLocation): String {
    val type = location.type.lowercase().trim()
    val modelCode = getModelCode(type)
    val bgColor = getBackgroundColor(type)
    val fogColor = bgColor
    val ambientColor = getAmbientColor(type)
    val lightColor = getSunColor(type)
    val groundColor = getGroundColor(type)
    val fogDensity = getFogDensity(type)

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

// ── MODELO DE UBICACIÓN ───────────────────────────────────────────────────────
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

$modelCode

// ── CONTROL DE ÓRBITA TÁCTIL / RATÓN ─────────────────────────────────────────
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
    "bosque"          -> "0x0A1505"
    "cueva"           -> "0x050508"
    "mazmorra"        -> "0x080305"
    "ciudad"          -> "0x0D1015"
    "pueblo"          -> "0x0D0F0A"
    "montaña", "montana" -> "0x080D12"
    "templo"          -> "0x0D0D10"
    "torre"           -> "0x080810"
    "lago"            -> "0x080D12"
    "mar"             -> "0x050B12"
    "desierto"        -> "0x150C05"
    "taberna"         -> "0x0D0905"
    "ruina"           -> "0x0C0C0A"
    else              -> "0x0D0700"
}

private fun getAmbientColor(type: String) = when (type) {
    "bosque"          -> "0x0D2205"
    "cueva", "mazmorra" -> "0x050408"
    "lago", "mar"     -> "0x051020"
    "desierto"        -> "0x201505"
    "taberna"         -> "0x201005"
    else              -> "0x111118"
}

private fun getSunColor(type: String) = when (type) {
    "bosque"          -> "0xAAFF88"
    "cueva", "mazmorra" -> "0x4433AA"
    "lago", "mar"     -> "0x88CCFF"
    "desierto"        -> "0xFFDD88"
    "templo"          -> "0xFFEECC"
    "taberna"         -> "0xFF9944"
    else              -> "0xFFDDAA"
}

private fun getGroundColor(type: String) = when (type) {
    "bosque"          -> "0x1A2E0A"
    "cueva"           -> "0x1A1A1E"
    "mazmorra"        -> "0x151212"
    "ciudad"          -> "0x333340"
    "pueblo"          -> "0x2A2010"
    "montaña", "montana" -> "0x303030"
    "templo"          -> "0xD4C8A0"
    "lago", "mar"     -> "0x0A2040"
    "desierto"        -> "0x8B6914"
    "taberna"         -> "0x2A1A08"
    "ruina"           -> "0x282820"
    else              -> "0x1A1505"
}

private fun getFogDensity(type: String) = when (type) {
    "bosque"          -> "0.06"
    "cueva", "mazmorra" -> "0.10"
    "lago", "mar"     -> "0.04"
    else              -> "0.03"
}

private fun getAnimationCode(type: String) = when (type) {
    "bosque" ->
        """if(window._leaves) window._leaves.forEach(function(l,i){
            l.position.y += Math.sin(t*1.2+i)*0.0008;
            l.rotation.y += 0.005;
        });"""
    "lago", "mar" ->
        """if(window._waves) window._waves.forEach(function(w,i){
            w.position.y = Math.sin(t*0.8+i*1.1)*0.05;
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
 * Todo usa geometrías básicas (ConeGeometry, CylinderGeometry, BoxGeometry, SphereGeometry)
 * sin necesidad de archivos de modelo externos ni servicios de pago.
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
    var trunk = addMesh(
        new THREE.CylinderGeometry(0.07,0.11,h,8),
        makeMat(0x5C3A1E, 0.9, 0.0), p[0], h/2, p[1]
    );
    var canopyH = 1.2 + Math.random()*0.6;
    var leaf = addMesh(
        new THREE.ConeGeometry(0.55+Math.random()*0.2, canopyH, 7),
        makeMat(0x1E6B1A + (i*0x050800&0xFFFF), 0.9, 0.0),
        p[0], h + canopyH/2 - 0.15, p[1]
    );
    leaves.push(leaf);
    // Segunda capa
    var leaf2 = addMesh(
        new THREE.ConeGeometry(0.38, canopyH*0.75, 7),
        makeMat(0x25801E, 0.85, 0.0),
        p[0], h + canopyH*0.7, p[1]
    );
    leaves.push(leaf2);
});
// Rocas
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
// Entrada (arco de roca)
addMesh(new THREE.SphereGeometry(2.2,12,8),
    makeMat(0x222228,0.95), 0,1.5,0, 0,0,0, 1,0.7,0.8);
addMesh(new THREE.SphereGeometry(1.4,12,8),
    makeMat(0x050508,0.98), 0,1.2,0.5, 0,0,0, 1,0.75,0.5);
// Estalactitas
var staPositions = [[0.4,0.3],[-0.5,0.1],[0.1,0.5],[-0.2,-0.3],[0.6,-0.4]];
staPositions.forEach(function(p,i){
    var h = 0.4+Math.random()*0.5;
    addMesh(new THREE.ConeGeometry(0.07,h,6),
        makeMat(0x333338,0.9), p[0],2.2-Math.random()*0.3,p[1], Math.PI,0,0);
});
// Estalagmitas
[[-0.7,0.4],[0.5,-0.5],[-0.3,-0.6],[0.8,0.2]].forEach(function(p,i){
    addMesh(new THREE.ConeGeometry(0.06,0.35+Math.random()*0.3,6),
        makeMat(0x2A2A30,0.92), p[0],0.2,p[1]);
});
// Cristales luminosos
var crystalMat = makeMat(0x1133AA,0.3,0.2,0x2255FF,1.2);
[[0.9,0.0],[−0.8,0.5]].forEach(function(p){
    addMesh(new THREE.ConeGeometry(0.09,0.5,5), crystalMat, p[0],0.25,p[1]);
    addMesh(new THREE.ConeGeometry(0.06,0.35,5), crystalMat, p[0]+0.12,0.2,p[1]-0.1);
});
// Luz azulada de los cristales
var cLight = new THREE.PointLight(0x1144FF,1.2,4);
cLight.position.set(0.9,0.6,0);
scene.add(cLight);
var cLight2 = new THREE.PointLight(0x0033AA,0.8,3);
cLight2.position.set(-0.8,0.5,0.5);
scene.add(cLight2);
""".trimIndent()

    // ── CIUDAD ───────────────────────────────────────────────────────────────
    "ciudad" -> """
var buildingConfigs = [
    [0,0,1.0,0.5,2.5],[-1.4,0.3,0.8,0.4,1.8],[1.3,-0.2,0.9,0.5,3.0],
    [-0.5,-1.3,0.7,0.6,1.4],[1.0,1.2,0.6,0.5,2.0],[-1.8,-0.8,0.7,0.45,1.6],
    [0.4,-0.5,1.1,0.45,1.2]
];
var wallMat = makeMat(0x445566,0.8,0.1);
var roofMat = makeMat(0x334455,0.7,0.2);
var windowMat = makeMat(0x000000,0.2,0.0,0xFFCC44,0.8);
buildingConfigs.forEach(function(c){
    var bx=c[0],bz=c[1],bw=c[2],bd=c[3],bh=c[4];
    addMesh(new THREE.BoxGeometry(bw,bh,bd), wallMat, bx,bh/2,bz);
    addMesh(new THREE.BoxGeometry(bw+0.08,0.1,bd+0.08), roofMat, bx,bh+0.05,bz);
    // Ventanas iluminadas
    addMesh(new THREE.BoxGeometry(0.18,0.22,0.02), windowMat,
        bx+bw*0.15, bh*0.55, bz+bd/2+0.01);
    addMesh(new THREE.BoxGeometry(0.18,0.22,0.02), windowMat,
        bx-bw*0.15, bh*0.4, bz+bd/2+0.01);
});
// Calle central
addMesh(new THREE.BoxGeometry(0.6,0.02,5.0), makeMat(0x555560,0.9), 0,0.01,0);
// Farola
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
    // Tejado triangular (cono aplanado)
    addMesh(new THREE.ConeGeometry(Math.max(w,d)*0.8,0.5,4),
        makeMat(0x662222,0.85), x,ht+0.2,z, 0,Math.PI/4,0);
    // Puerta
    addMesh(new THREE.BoxGeometry(0.22,0.38,0.02),makeMat(0x553311,0.95),x,0.19,z+d/2+0.01);
});
// Pozo central
addMesh(new THREE.CylinderGeometry(0.3,0.3,0.5,12), makeMat(0x888880,0.9),-0.3,0.25,-0.3);
addMesh(new THREE.TorusGeometry(0.3,0.04,8,20), makeMat(0x666655,0.8),-0.3,0.5,-0.3,Math.PI/2);
// Árbol lateral
addMesh(new THREE.CylinderGeometry(0.06,0.09,0.9,8), makeMat(0x5C3A1E,0.9),-1.8,0.45,1.2);
addMesh(new THREE.SphereGeometry(0.45,8,7), makeMat(0x226611,0.9),-1.8,1.2,1.2);
// Camino
addMesh(new THREE.BoxGeometry(0.5,0.01,4.0), makeMat(0x887755,0.95),0,0.005,0);
""".trimIndent()

    // ── MAZMORRA ──────────────────────────────────────────────────────────────
    "mazmorra" -> """
var stoneMat = makeMat(0x252522,0.95,0.05);
// Paredes de bloques
[[−2,0.5,0],[2,0.5,0],[0,0.5,-2]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.6,1.0,0.4),stoneMat,p[0],p[1],p[2]);
    addMesh(new THREE.BoxGeometry(0.6,0.5,0.4),stoneMat,p[0],p[1]+0.75,p[2]+0.1);
});
// Suelo de losas
for(var gi=0;gi<16;gi++){
    var gx=(gi%4)*0.9-1.35, gz=Math.floor(gi/4)*0.9-1.35;
    addMesh(new THREE.BoxGeometry(0.85,0.05,0.85),
        makeMat(0x1E1C1A,0.97), gx,0.025,gz);
}
// Columnas
[[-1.2,-1.2],[1.2,-1.2],[-1.2,1.2],[1.2,1.2]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.12,0.14,2.0,8),stoneMat,p[0],1.0,p[1]);
    addMesh(new THREE.BoxGeometry(0.32,0.12,0.32),stoneMat,p[0],2.05,p[1]);
});
// Antorchas en paredes
var torches = [];
[[-1.9,1.1,-0.3],[1.9,1.1,0.4]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.03,0.04,0.2,6),
        makeMat(0x553311,0.9),p[0],p[1],p[2], 0,0,Math.PI/8);
    var fl = addMesh(new THREE.SphereGeometry(0.08,6,6),
        makeMat(0x000000,0.0,0.0,0xFF4400,2.0),p[0],p[1]+0.15,p[2]);
    var tl = new THREE.PointLight(0xFF6600,0.8,3.5);
    tl.position.set(p[0],p[1]+0.2,p[2]);
    scene.add(tl);
    torches.push(tl);
});
window._torches = torches;
// Cadenas sugeridas
addMesh(new THREE.TorusGeometry(0.1,0.015,6,12),
    makeMat(0x888877,0.5,0.5),-0.5,1.8,-1.8,Math.PI/2);
""".trimIndent()

    // ── MONTAÑA ───────────────────────────────────────────────────────────────
    "montaña", "montana" -> """
// Pico principal
addMesh(new THREE.ConeGeometry(2.0,3.5,7), makeMat(0x4A4A50,0.95), 0,1.75,0);
// Nieve en la cima
addMesh(new THREE.ConeGeometry(0.7,0.8,7), makeMat(0xEEEEFF,0.6), 0,3.2,0);
addMesh(new THREE.SphereGeometry(0.55,8,6), makeMat(0xDDDDFF,0.5), 0,3.55,0,
    0,0,0, 1,0.4,1);
// Picos secundarios
[[-1.8,0.5],[1.6,-0.3],[-1.0,-1.5]].forEach(function(p){
    var h = 1.0+Math.random()*1.0;
    addMesh(new THREE.ConeGeometry(0.8,h+0.5,6), makeMat(0x404048,0.96), p[0],h/2,p[1]);
    addMesh(new THREE.ConeGeometry(0.25,0.3,6), makeMat(0xDDDDFF,0.6), p[0],(h+0.55)*0.9,p[1]);
});
// Rocas en la base
[[0.8,1.2],[−1.2,0.8],[1.5,−0.5],[−0.5,1.6]].forEach(function(r){
    addMesh(new THREE.SphereGeometry(0.18+Math.random()*0.12,6,5),
        makeMat(0x505050,0.95), r[0],0.12,r[1]);
});
// Cielo nocturno simulado con niebla
scene.fog = new THREE.FogExp2(0x080D12, 0.025);
var moonLight = new THREE.PointLight(0xAAAACC,0.6,20);
moonLight.position.set(-5,8,-3);
scene.add(moonLight);
""".trimIndent()

    // ── TEMPLO ────────────────────────────────────────────────────────────────
    "templo" -> """
// Base escalonada (pirámide)
var stepMat = makeMat(0xD4C090,0.7,0.1);
[3.0,2.2,1.5,0.9].forEach(function(s,i){
    addMesh(new THREE.BoxGeometry(s,0.32,s),stepMat,0,i*0.32+0.16,0);
});
// Cella (sala principal)
addMesh(new THREE.BoxGeometry(1.4,1.0,1.0), makeMat(0xE8D8A0,0.6,0.05), 0,1.4,0);
// Tejado triangular / frontón
addMesh(new THREE.ConeGeometry(1.1,0.5,4), makeMat(0xD4B060,0.65,0.1), 0,2.15,0, 0,Math.PI/4,0);
// Columnas (6)
var colPositions = [
    [-0.7,1.8],[-0.7,0.0],[0.7,1.8],[0.7,0.0],[-1.2,0.9],[1.2,0.9]
];
colPositions.forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.1,0.12,1.2,10),
        makeMat(0xE0D0A0,0.65), p[0],1.6,p[1]);
    // Capitel
    addMesh(new THREE.BoxGeometry(0.26,0.1,0.26),
        makeMat(0xD4C080,0.6), p[0],2.22,p[1]);
});
// Antorchas en escalinata
var torches = [];
[[-1.6,1.1,1.6],[1.6,1.1,1.6]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.04,0.05,0.35,6),
        makeMat(0x8B6914,0.8),p[0],p[1],p[2]);
    var fl = new THREE.Mesh(
        new THREE.SphereGeometry(0.09,6,6),
        makeMat(0x000000,0.0,0.0,0xFFAA00,2.0)
    );
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
// Cuerpo principal de la torre
addMesh(new THREE.CylinderGeometry(0.7,0.8,3.5,10), stoneMat, 0,1.75,0);
// Almenas
for(var a=0;a<8;a++){
    var angle = (a/8)*Math.PI*2;
    addMesh(new THREE.BoxGeometry(0.22,0.35,0.22), stoneMat,
        Math.sin(angle)*0.72, 3.6, Math.cos(angle)*0.72);
}
// Suelo de la torre (interior visible)
addMesh(new THREE.CylinderGeometry(0.68,0.68,0.1,10),
    makeMat(0x334455,0.9), 0,3.48,0);
// Puerta de arco
addMesh(new THREE.BoxGeometry(0.36,0.7,0.12), makeMat(0x221A14,0.95), 0,0.35,0.76);
addMesh(new THREE.SphereGeometry(0.18,8,6), makeMat(0x221A14,0.95), 0,0.7,0.76,
    0,0,0, 1,1,0.4);
// Ventana con luz
addMesh(new THREE.BoxGeometry(0.18,0.26,0.05),
    makeMat(0x000000,0.1,0.0,0xFFCC44,1.5), 0,2.2,0.72);
var windowGlow = new THREE.PointLight(0xFFAA22,0.8,2.5);
windowGlow.position.set(0,2.2,0.5);
scene.add(windowGlow);
// Bandera en lo alto
addMesh(new THREE.BoxGeometry(0.02,0.7,0.02), makeMat(0x553311,0.9), 0,4.0,0);
addMesh(new THREE.BoxGeometry(0.45,0.28,0.02),
    makeMat(0xCC2222,0.8,0.05,0x000000,0), 0.225,4.2,0);
""".trimIndent()

    // ── LAGO / MAR ────────────────────────────────────────────────────────────
    "lago", "mar" -> """
// Agua (varias capas para efecto de ondas)
var waterMat = makeMat(0x1144AA,0.0,0.3,0x224488,0.3);
var waves = [];
for(var w=0;w<5;w++){
    var wm = addMesh(new THREE.CircleGeometry(2.2-w*0.2,24),
        makeMat(0x0A2A6A+(w*0x050800),0.0,0.2+w*0.05), 0,0.01+w*0.02,0, -Math.PI/2);
    waves.push(wm);
}
window._waves = waves;
// Ribera con vegetación
[[1.8,0.6],[−1.7,0.8],[0.5,1.9],[−0.8,-1.8],[2.0,-0.5]].forEach(function(p){
    // Caña
    addMesh(new THREE.CylinderGeometry(0.025,0.035,0.8,5), makeMat(0x4A7A1A,0.9), p[0],0.4,p[1]);
    addMesh(new THREE.ConeGeometry(0.1,0.3,5), makeMat(0x8B5E3C,0.85), p[0],0.85,p[1]);
});
// Piedras orilla
[[1.2,0.1],[−1.0,-0.3],[0.2,1.4]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(0.18,6,5), makeMat(0x555550,0.95), p[0],0.1,p[1]);
});
// Luz reflejada en el agua
var waterLight = new THREE.PointLight(0x224499,1.0,6);
waterLight.position.set(0,1.5,0);
scene.add(waterLight);
// Neblina sobre el agua
scene.fog = new THREE.FogExp2(0x080D12, 0.05);
""".trimIndent()

    // ── DESIERTO ──────────────────────────────────────────────────────────────
    "desierto" -> """
// Dunas (media esferas)
[[0,0.6],[-1.5,0.25],[1.2,0.18],[0.3,-1.4],[-0.8,-0.9],[2.0,-0.2]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(p[1],10,6), makeMat(0xC8960A,0.97),
        p[0],-p[1]*0.3,0, 0,0,0, 1,0.35,1);
});
// Cactus principal
addMesh(new THREE.CylinderGeometry(0.12,0.15,1.4,7), makeMat(0x2A5A1A,0.9), 0.8,0.7,0.4);
addMesh(new THREE.CylinderGeometry(0.08,0.10,0.5,7), makeMat(0x2A5A1A,0.9), 0.8,1.35,0.4);
// Brazo del cactus
addMesh(new THREE.CylinderGeometry(0.07,0.08,0.4,7), makeMat(0x2A5A1A,0.9),
    0.55,1.0,0.4, 0,0,Math.PI/2.2);
addMesh(new THREE.CylinderGeometry(0.06,0.07,0.35,7), makeMat(0x316A20,0.9),
    0.35,1.15,0.4);
// Cactus pequeño
addMesh(new THREE.CylinderGeometry(0.07,0.09,0.7,6), makeMat(0x2D5E15,0.9),-1.2,0.35,-0.5);
// Calavera (roca con forma)
addMesh(new THREE.SphereGeometry(0.22,7,6), makeMat(0xD4C090,0.85),-0.6,0.22,0.9);
// Sol intenso simulado
sun.intensity = 2.0;
sun.color = new THREE.Color(0xFFEE66);
// Arena volandera (partículas estáticas)
for(var s=0;s<12;s++){
    addMesh(new THREE.SphereGeometry(0.04,4,3),
        makeMat(0xCC9900,0.9),
        (Math.random()-0.5)*4, 0.1+Math.random()*0.3, (Math.random()-0.5)*4);
}
""".trimIndent()

    // ── TABERNA ───────────────────────────────────────────────────────────────
    "taberna" -> """
// Edificio principal
addMesh(new THREE.BoxGeometry(2.2,1.4,1.8), makeMat(0x8B6327,0.9), 0,0.7,0);
// Tejado a dos aguas
addMesh(new THREE.CylinderGeometry(0.02,1.6,0.8,4,1,false,Math.PI/4),
    makeMat(0x5C1A1A,0.85), 0,1.5,0, 0,0,0, 1,1,0.85);
// Chimenea
addMesh(new THREE.BoxGeometry(0.3,0.7,0.3), makeMat(0x555548,0.95), 0.6,1.85,-0.4);
// Humo (esferas difuminadas hacia arriba)
[0.6,0.8,1.0].forEach(function(yy){
    addMesh(new THREE.SphereGeometry(0.1+yy*0.05,6,5),
        makeMat(0x555555,0.9,0.0,0x111111,0.3), 0.6+yy*0.03,1.9+yy,−0.4);
});
// Letrero
addMesh(new THREE.BoxGeometry(0.8,0.3,0.04), makeMat(0x6B3E12,0.85), 0,1.6,0.92);
addMesh(new THREE.SphereGeometry(0.08,6,5),
    makeMat(0x000000,0.0,0.0,0xFFAA00,1.5), -0.2,1.6,0.94);
addMesh(new THREE.SphereGeometry(0.08,6,5),
    makeMat(0x000000,0.0,0.0,0xFF8800,1.5), 0.2,1.6,0.94);
// Puerta
addMesh(new THREE.BoxGeometry(0.4,0.7,0.04), makeMat(0x4A2A0A,0.9), 0,0.35,0.92);
// Barriles
[[0.9,0.2,0.6],[1.1,0.2,-0.4]].forEach(function(p){
    addMesh(new THREE.CylinderGeometry(0.18,0.18,0.38,10),
        makeMat(0x7B4A15,0.85), p[0],p[1],p[2]);
    addMesh(new THREE.TorusGeometry(0.18,0.025,6,16),
        makeMat(0x333222,0.7,0.3), p[0],p[1]+0.12,p[2], Math.PI/2);
});
// Fuego en la chimenea (visible por ventana)
var fireMat = makeMat(0x000000,0.0,0.0,0xFF5500,2.0);
var fireMesh = addMesh(new THREE.SphereGeometry(0.12,6,5), fireMat, 0.6,1.3,-0.3);
var fireLight = new THREE.PointLight(0xFF6600,1.5,4.5);
fireLight.position.set(0,1.0,0.2);
scene.add(fireLight);
window._fire = fireLight;
window._fireM = fireMat;
""".trimIndent()

    // ── RUINAS ────────────────────────────────────────────────────────────────
    "ruina" -> """
var ruinMat = makeMat(0x5A5548,0.97,0.02);
// Muros derrumbados
[[0,0.7,-1.5,2.4,1.4,0.3],[−1.5,0.5,0,0.35,1.0,1.8]].forEach(function(w){
    addMesh(new THREE.BoxGeometry(w[3],w[4],w[5]), ruinMat, w[0],w[1],w[2]);
    // Grietas (cajas más oscuras incrustadas)
    addMesh(new THREE.BoxGeometry(w[3]*0.1,w[4]*0.6,w[5]+0.01),
        makeMat(0x333330,0.99), w[0]+w[3]*0.2,w[1]-0.1,w[2]);
});
// Columna rota
addMesh(new THREE.CylinderGeometry(0.18,0.20,1.5,10), ruinMat, 1.3,0.75,0.3);
addMesh(new THREE.CylinderGeometry(0.18,0.20,0.6,10), ruinMat, 1.3,0.3,0.3,
    0,0,0.4);  // caída
// Fragmentos de piedra dispersos
[[0.4,0.12,0.8],[−0.8,0.1,−0.6],[0.9,0.15,−1.0],[−1.2,0.08,0.5],[0.1,0.1,1.4]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.2+Math.random()*0.25,0.1+Math.random()*0.15,0.15+Math.random()*0.2),
        ruinMat, p[0],p[1],p[2], Math.random()*0.3,Math.random()*Math.PI,Math.random()*0.2);
});
// Enredadera (esferas verdes diminutas)
[[0,0.9,-1.48],[−0.2,1.1,-1.5],[0.3,0.6,-1.49]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(0.08,5,4), makeMat(0x1A4A0A,0.95), p[0],p[1],p[2]);
});
// Charco oscuro
addMesh(new THREE.CircleGeometry(0.5,12), makeMat(0x1A1A20,0.1,0.4), -0.3,0.01,0.8,-Math.PI/2);
""".trimIndent()

    // ── DEFAULT (taberna, llanura, u otros tipos no clasificados) ──────────────
    else -> """
// Orbe mágico flotante
var orbMat = makeMat(0x331155,0.1,0.3,0xAA44FF,1.5);
var orb = addMesh(new THREE.SphereGeometry(0.65,16,14), orbMat, 0,1.5,0);
// Anillos orbitales
[0.9,1.1,1.25].forEach(function(r,i){
    var ringMat = makeMat(0x000000,0.1,0.5,0x6622AA+(i*0x111111),0.8);
    var ring = addMesh(new THREE.TorusGeometry(r,0.04,8,40), ringMat, 0,1.5,0);
    ring.rotation.x = i*0.4;
    ring.rotation.z = i*0.6;
});
// Estrellas/partículas
for(var s=0;s<20;s++){
    var angle = (s/20)*Math.PI*2;
    var dist = 1.4+Math.random()*0.5;
    addMesh(new THREE.SphereGeometry(0.04,4,3),
        makeMat(0x000000,0.0,0.0,0xFFFFFF+(Math.floor(Math.random()*0xAAAAAA)),1.0),
        Math.sin(angle)*dist, 1.5+Math.sin(angle*2+s)*0.4, Math.cos(angle)*dist);
}
// Luz pulsante (animación manejada en 'animCode' sección default es sin animación extra)
var orbLight = new THREE.PointLight(0x9933FF,1.5,5);
orbLight.position.set(0,1.5,0);
scene.add(orbLight);
""".trimIndent()
}