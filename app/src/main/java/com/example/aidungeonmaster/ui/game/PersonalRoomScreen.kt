package com.example.aidungeonmaster.ui.game

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.PERSONAL_ROOM_CATALOG
import com.example.aidungeonmaster.data.model.PERSONAL_ROOM_SLOTS
import com.example.aidungeonmaster.data.model.PersonalRoomDecoration
import com.example.aidungeonmaster.data.model.PersonalRoomState
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.example.aidungeonmaster.viewmodel.PersonalRoomViewModel
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PersonalRoomScreen(
    charId: String,
    characterName: String,
    onBack: () -> Unit,
    roomViewModel: PersonalRoomViewModel = viewModel(),
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val roomState by roomViewModel.roomState.collectAsState()
    val character by inventoryViewModel.character.collectAsState()
    val scope = rememberCoroutineScope()

    var feedback by remember { mutableStateOf("Tu refugio personal está listo para decorarse.") }

    LaunchedEffect(charId) {
        inventoryViewModel.loadInventory(charId)
        roomViewModel.loadRoom(charId)
    }

    val html = remember(roomState.placedDecorations, roomState.roomTheme) {
        buildPersonalRoomHtml(roomState)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0A08))
    ) {
        Surface(color = Color(0xEE1B120D)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFFFFD700))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🏰 Fortaleza de $characterName",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "🪙 ${character?.coins ?: 0}  •  ${roomState.placedDecorations.size}/${PERSONAL_ROOM_SLOTS.size} decoraciones colocadas",
                        color = Color(0xFFBBAE9C),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        webViewClient = WebViewClient()
                        setBackgroundColor(android.graphics.Color.parseColor("#0E0A08"))
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

            Surface(
                color = Color(0xCC1B120D),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Sala personal",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = feedback,
                        color = Color(0xFFD7C8B6),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "↔ Arrastra para rotar la cámara",
                color = Color(0x88E4D9C8),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }

        Surface(color = Color(0xEE1B120D)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Color(0xFF463225), thickness = 1.dp)

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PERSONAL_ROOM_CATALOG) { decoration ->
                        DecorationCard(
                            decoration = decoration,
                            owned = roomState.ownedDecorationIds.contains(decoration.id),
                            placed = roomState.placedDecorations.any { it.decorationId == decoration.id },
                            canAfford = (character?.coins ?: 0) >= decoration.price,
                            onBuy = {
                                roomViewModel.buyDecoration(
                                    charId = charId,
                                    decorationId = decoration.id,
                                    spendCoins = { amount -> inventoryViewModel.spendCoins(charId, amount) }
                                ) { message ->
                                    feedback = message
                                }
                            },
                            onPlace = {
                                roomViewModel.placeDecoration(decoration.id) { message ->
                                    feedback = message
                                }
                            },
                            onRemove = {
                                roomViewModel.removeDecoration(decoration.id) { message ->
                                    feedback = message
                                }
                            }
                        )
                    }
                }

                if (roomState.placedDecorations.isNotEmpty()) {
                    val placedNames = roomState.placedDecorations.mapNotNull { placed ->
                        PERSONAL_ROOM_CATALOG.firstOrNull { it.id == placed.decorationId }?.name
                    }
                    Text(
                        text = "Colocadas: ${placedNames.joinToString()}" ,
                        color = Color(0xFF9C8E7E),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DecorationCard(
    decoration: PersonalRoomDecoration,
    owned: Boolean,
    placed: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onPlace: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = Color(0xFF241711),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF4A3528)),
        modifier = Modifier.width(200.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = decoration.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = decoration.name,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = decoration.description,
                color = Color(0xFFD6C7B8),
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🪙 ${decoration.price}",
                color = if (canAfford || owned) Color(0xFFFFD700) else Color(0xFFFF8A80),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            when {
                !owned -> {
                    Button(
                        onClick = onBuy,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canAfford,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (canAfford) "Comprar" else "Sin monedas")
                    }
                }
                placed -> {
                    Button(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Quitar")
                    }
                }
                else -> {
                    Button(
                        onClick = onPlace,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Colocar")
                    }
                }
            }
        }
    }
}

private fun buildPersonalRoomHtml(state: PersonalRoomState): String {
    val decorationCode = state.placedDecorations.joinToString("\n") { placed ->
        buildDecorationJs(placed.decorationId, placed.slotId)
    }

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"/>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { background:#0E0A08; overflow:hidden; touch-action:none; }
canvas { display:block; }
</style>
</head>
<body>
<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
<script>
var W = window.innerWidth, H = window.innerHeight;
var scene = new THREE.Scene();
scene.background = new THREE.Color(0x120C0A);
scene.fog = new THREE.FogExp2(0x120C0A, 0.03);

var camera = new THREE.PerspectiveCamera(55, W/H, 0.05, 100);
camera.position.set(0, 2.8, 6.8);

var renderer = new THREE.WebGLRenderer({antialias:true});
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(W, H);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
document.body.appendChild(renderer.domElement);

var ambient = new THREE.AmbientLight(0xC89A6A, 0.85);
scene.add(ambient);

var sun = new THREE.DirectionalLight(0xFFD9A8, 1.15);
sun.position.set(5, 8, 4);
sun.castShadow = true;
sun.shadow.mapSize.width = 1024;
sun.shadow.mapSize.height = 1024;
scene.add(sun);

var fill = new THREE.PointLight(0x88B8FF, 0.35, 12);
fill.position.set(-4, 3, -3);
scene.add(fill);

function makeMat(color, rough, metal, emissive, emissiveInt, opacity, transparent) {
    return new THREE.MeshStandardMaterial({
        color: color,
        roughness: rough !== undefined ? rough : 0.85,
        metalness: metal !== undefined ? metal : 0.0,
        emissive: emissive !== undefined ? emissive : 0x000000,
        emissiveIntensity: emissiveInt !== undefined ? emissiveInt : 0.0,
        opacity: opacity !== undefined ? opacity : 1.0,
        transparent: transparent !== undefined ? transparent : (opacity !== undefined && opacity < 1.0)
    });
}

function addMesh(geo, mat, x, y, z, rx, ry, rz, sx, sy, sz) {
    var m = new THREE.Mesh(geo, mat);
    m.position.set(x || 0, y || 0, z || 0);
    if (rx || ry || rz) m.rotation.set(rx || 0, ry || 0, rz || 0);
    if (sx) m.scale.set(sx, sy || sx, sz || sx);
    m.castShadow = true;
    m.receiveShadow = true;
    scene.add(m);
    return m;
}

function slotPos(slotId) {
    var slots = {
        wall_left:  { x:-2.35, y:1.95, z:-1.85 },
        wall_right: { x: 2.35, y:1.95, z:-1.85 },
        back_left:  { x:-1.85, y:0.00, z:-1.55 },
        back_right: { x: 1.85, y:0.00, z:-1.55 },
        front_left: { x:-1.95, y:0.00, z: 1.55 },
        front_right:{ x: 1.95, y:0.00, z: 1.55 },
        center:     { x: 0.00, y:0.00, z: 0.55 },
        table:      { x: 0.00, y:0.78, z: 0.25 }
    };
    return slots[slotId] || slots.center;
}

var stoneMat = makeMat(0x6C625C, 0.98, 0.02);
var darkStoneMat = makeMat(0x403A37, 0.99, 0.02);
var woodMat = makeMat(0x7A4D29, 0.92, 0.02);
var darkWoodMat = makeMat(0x4A2D18, 0.95, 0.02);
var goldMat = makeMat(0xC9A34A, 0.45, 0.35);
var clothRed = makeMat(0x8D1F1F, 0.90, 0.0);
var clothBlue = makeMat(0x253A7A, 0.90, 0.0);
var greenMat = makeMat(0x2E6A2E, 0.88, 0.0);
var fireMat = makeMat(0x000000, 0.0, 0.0, 0xFF7A22, 1.6);
var crystalMat = makeMat(0x7D7DFF, 0.18, 0.1, 0x6A6AFF, 0.8, 0.88, true);

// Suelo
addMesh(new THREE.BoxGeometry(7.4, 0.15, 7.4), darkStoneMat, 0, -0.08, 0);
addMesh(new THREE.BoxGeometry(6.8, 0.05, 6.8), stoneMat, 0, 0.00, 0);

// Muros
addMesh(new THREE.BoxGeometry(7.0, 3.4, 0.22), stoneMat, 0, 1.7, -3.35);
addMesh(new THREE.BoxGeometry(0.22, 3.4, 7.0), stoneMat, -3.35, 1.7, 0);
addMesh(new THREE.BoxGeometry(0.22, 3.4, 7.0), stoneMat,  3.35, 1.7, 0);

// Techo parcial y vigas
addMesh(new THREE.BoxGeometry(6.9, 0.15, 6.9), darkStoneMat, 0, 3.35, 0);
[-2.2, 0, 2.2].forEach(function(x){
    addMesh(new THREE.BoxGeometry(0.18, 0.24, 6.6), darkWoodMat, x, 3.18, 0);
});

// Ventanal
addMesh(new THREE.BoxGeometry(1.6, 1.3, 0.05), makeMat(0x000000,0.2,0.0,0x8EB8FF,0.22,0.65,true), 0, 1.9, -3.20);

// Alfombra base tenue
addMesh(new THREE.BoxGeometry(2.8, 0.02, 3.8), makeMat(0x402018, 0.96, 0.0), 0, 0.02, 0.8);

// Mesa base
addMesh(new THREE.BoxGeometry(1.35, 0.12, 0.9), woodMat, 0, 0.76, 0.25);
[[-0.5,0.36,-0.05],[0.5,0.36,-0.05],[-0.5,0.36,0.55],[0.5,0.36,0.55]].forEach(function(p){
    addMesh(new THREE.BoxGeometry(0.10,0.72,0.10), darkWoodMat, p[0],p[1],p[2]);
});

// Antorchas fijas ambientales
var baseTorchL = new THREE.PointLight(0xFFAA55, 0.55, 5);
baseTorchL.position.set(-2.8, 2.0, -2.8);
scene.add(baseTorchL);
var baseTorchR = new THREE.PointLight(0xFFAA55, 0.55, 5);
baseTorchR.position.set( 2.8, 2.0, -2.8);
scene.add(baseTorchR);
addMesh(new THREE.SphereGeometry(0.08, 6, 5), fireMat, -2.8, 1.95, -2.85);
addMesh(new THREE.SphereGeometry(0.08, 6, 5), fireMat,  2.8, 1.95, -2.85);

// Decoraciones colocadas
$decorationCode

var theta = 0.0, phi = 0.35, radius = 6.8;
var dragging = false, lastX = 0, lastY = 0;
var autoRotate = true;

function applyCamera() {
    camera.position.x = radius * Math.cos(phi) * Math.sin(theta);
    camera.position.y = radius * Math.sin(phi) + 1.05;
    camera.position.z = radius * Math.cos(phi) * Math.cos(theta);
    camera.lookAt(0, 1.15, 0);
}

function startDrag(x, y) {
    dragging = true;
    autoRotate = false;
    lastX = x;
    lastY = y;
}

function moveDrag(x, y) {
    if (!dragging) return;
    theta -= (x - lastX) * 0.010;
    phi = Math.max(-0.2, Math.min(0.95, phi + (y - lastY) * 0.006));
    lastX = x;
    lastY = y;
    applyCamera();
}

document.addEventListener('touchstart', function(e){
    startDrag(e.touches[0].clientX, e.touches[0].clientY);
}, { passive:true });

document.addEventListener('touchmove', function(e){
    moveDrag(e.touches[0].clientX, e.touches[0].clientY);
}, { passive:true });

document.addEventListener('touchend', function(){ dragging = false; }, { passive:true });

document.addEventListener('mousedown', function(e){ startDrag(e.clientX, e.clientY); });
document.addEventListener('mousemove', function(e){ moveDrag(e.clientX, e.clientY); });
document.addEventListener('mouseup', function(){ dragging = false; });

window.addEventListener('resize', function() {
    W = window.innerWidth;
    H = window.innerHeight;
    camera.aspect = W / H;
    camera.updateProjectionMatrix();
    renderer.setSize(W, H);
});

applyCamera();

var t = 0;
function animate() {
    requestAnimationFrame(animate);
    t += 0.016;
    if (autoRotate) {
        theta += 0.0025;
        applyCamera();
    }
    if (window._roomFires) {
        window._roomFires.forEach(function(light, i){
            light.intensity = light.userData.baseI + Math.sin(t * 4.5 + i) * 0.15;
        });
    }
    if (window._magicCrystal) {
        window._magicCrystal.rotation.y += 0.01;
        window._magicCrystal.position.y = window._magicCrystal.userData.baseY + Math.sin(t * 2.2) * 0.04;
    }
    renderer.render(scene, camera);
}
animate();
</script>
</body>
</html>
    """.trimIndent()
}

private fun buildDecorationJs(decorationId: String, slotId: String): String {
    return when (decorationId) {
        "banner_royal" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(0.10,0.70,0.10), goldMat, p.x, p.y + 0.35, p.z);
addMesh(new THREE.BoxGeometry(0.95,0.08,0.08), goldMat, p.x, p.y + 0.68, p.z + 0.02);
addMesh(new THREE.BoxGeometry(0.82,0.70,0.03), clothRed, p.x, p.y + 0.28, p.z + 0.06);
addMesh(new THREE.BoxGeometry(0.18,0.14,0.03), goldMat, p.x, p.y - 0.08, p.z + 0.06);
        """.trimIndent()

        "torch_pair" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(0.12,0.32,0.10), darkWoodMat, p.x, p.y, p.z);
addMesh(new THREE.SphereGeometry(0.10,6,5), fireMat, p.x, p.y + 0.24, p.z + 0.04);
window._roomFires = window._roomFires || [];
var fireLight = new THREE.PointLight(0xFF8A2A, 0.95, 4.0);
fireLight.position.set(p.x, p.y + 0.28, p.z + 0.05);
fireLight.userData.baseI = 0.95;
scene.add(fireLight);
window._roomFires.push(fireLight);
        """.trimIndent()

        "weapon_rack" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(0.95,0.10,0.16), woodMat, p.x, 0.40, p.z);
addMesh(new THREE.BoxGeometry(0.10,0.90,0.16), darkWoodMat, p.x - 0.36, 0.45, p.z);
addMesh(new THREE.BoxGeometry(0.10,0.90,0.16), darkWoodMat, p.x + 0.36, 0.45, p.z);
addMesh(new THREE.BoxGeometry(0.06,0.85,0.06), goldMat, p.x - 0.10, 0.78, p.z + 0.02, 0,0,0.65);
addMesh(new THREE.BoxGeometry(0.55,0.05,0.05), goldMat, p.x + 0.10, 0.68, p.z + 0.02, 0,0,-0.35);
        """.trimIndent()

        "treasure_chest" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(0.95,0.52,0.60), woodMat, p.x, 0.26, p.z);
addMesh(new THREE.BoxGeometry(0.98,0.16,0.62), darkWoodMat, p.x, 0.58, p.z, 0.35,0,0);
addMesh(new THREE.BoxGeometry(0.14,0.18,0.05), goldMat, p.x, 0.30, p.z + 0.31);
        """.trimIndent()

        "red_rug" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(2.15,0.02,2.95), clothRed, p.x, 0.03, p.z);
addMesh(new THREE.BoxGeometry(1.95,0.01,2.75), goldMat, p.x, 0.04, p.z, 0,0,0, 1,1,1);
        """.trimIndent()

        "crystal_orb" -> """
var p = slotPos('$slotId');
addMesh(new THREE.CylinderGeometry(0.14,0.18,0.20,8), goldMat, p.x, p.y - 0.10, p.z);
window._magicCrystal = addMesh(new THREE.OctahedronGeometry(0.24,0), crystalMat, p.x, p.y + 0.18, p.z, 0.2,0.4,0);
window._magicCrystal.userData.baseY = p.y + 0.18;
        """.trimIndent()

        "book_stack" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(0.90,1.15,0.28), darkWoodMat, p.x, 0.58, p.z);
[0,1,2,3].forEach(function(i){
    addMesh(new THREE.BoxGeometry(0.65,0.10,0.22), i % 2 === 0 ? clothBlue : clothRed, p.x, 0.22 + i*0.18, p.z + 0.01);
});
        """.trimIndent()

        "potted_tree" -> """
var p = slotPos('$slotId');
addMesh(new THREE.CylinderGeometry(0.22,0.18,0.34,10), goldMat, p.x, 0.17, p.z);
addMesh(new THREE.CylinderGeometry(0.05,0.06,0.70,8), woodMat, p.x, 0.55, p.z);
addMesh(new THREE.SphereGeometry(0.42,8,7), greenMat, p.x, 1.05, p.z);
addMesh(new THREE.SphereGeometry(0.28,8,7), makeMat(0x3C7F38,0.88,0.0), p.x - 0.18, 0.90, p.z + 0.08);
addMesh(new THREE.SphereGeometry(0.25,8,7), makeMat(0x3C7F38,0.88,0.0), p.x + 0.16, 0.92, p.z - 0.04);
        """.trimIndent()

        "war_table" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(1.65,0.12,1.05), woodMat, p.x, p.y - 0.04, p.z);
[[-0.62,-0.42],[0.62,-0.42],[-0.62,0.42],[0.62,0.42]].forEach(function(o){
    addMesh(new THREE.BoxGeometry(0.10,0.78,0.10), darkWoodMat, p.x + o[0], 0.38, p.z + o[1]);
});
addMesh(new THREE.BoxGeometry(1.20,0.03,0.70), clothBlue, p.x, p.y + 0.03, p.z);
addMesh(new THREE.CylinderGeometry(0.05,0.05,0.42,8), goldMat, p.x + 0.30, p.y + 0.10, p.z, 0,0,1.2);
        """.trimIndent()

        "throne_seat" -> """
var p = slotPos('$slotId');
addMesh(new THREE.BoxGeometry(0.92,0.22,0.82), goldMat, p.x, 0.28, p.z);
addMesh(new THREE.BoxGeometry(0.78,1.00,0.18), clothRed, p.x, 0.86, p.z - 0.28);
addMesh(new THREE.BoxGeometry(0.14,0.72,0.82), goldMat, p.x - 0.34, 0.56, p.z);
addMesh(new THREE.BoxGeometry(0.14,0.72,0.82), goldMat, p.x + 0.34, 0.56, p.z);
        """.trimIndent()

        else -> ""
    }
}
