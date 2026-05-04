package com.example.aidungeonmaster.ui.game

import com.example.aidungeonmaster.ui.i18n.Text

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.PERSONAL_ROOM_CATALOG
import com.example.aidungeonmaster.data.model.PERSONAL_ROOM_SLOTS
import com.example.aidungeonmaster.data.model.PersonalRoomDecoration
import com.example.aidungeonmaster.data.model.PersonalRoomState
import com.example.aidungeonmaster.data.model.personalRoomDecorationById
import com.example.aidungeonmaster.data.model.personalRoomSlotById
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.example.aidungeonmaster.viewmodel.PersonalRoomViewModel

// Puente de integración para personal room.
private class PersonalRoomBridge(
    private val onSlotFocus: (slotId: String?) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    // Gestiona el evento de slot focus.
    fun onSlotFocus(slotId: String) {
        handler.post {
            onSlotFocus(slotId.ifBlank { null })
        }
    }

    @JavascriptInterface
    // Gestiona el evento de slot clear.
    fun onSlotClear() {
        handler.post {
            onSlotFocus(null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
// Ejecuta la lógica de personal room screen.
fun PersonalRoomScreen(
    charId: String,
    characterName: String,
    onBack: () -> Unit,
    readOnly: Boolean = false,
    roomViewModel: PersonalRoomViewModel = viewModel(),
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val roomState by roomViewModel.roomState.collectAsState()
    val character by inventoryViewModel.character.collectAsState()

    var feedback by remember(readOnly) {
        mutableStateOf(
            if (readOnly) {
                "Modo visita: puedes recorrer la sala, pero no modificar la decoración."
            } else {
                "Usa el joystick para caminar por la sala y pisa una baldosa para decorarla."
            }
        )
    }

    var activeSlotId by remember { mutableStateOf<String?>(null) }
    var activeSlotLabel by remember { mutableStateOf<String?>(null) }
    var activeSlotDecorationId by remember { mutableStateOf<String?>(null) }
    var showDecorationPanel by remember { mutableStateOf(false) }
    var showPlacementSheet by remember { mutableStateOf(false) }

    LaunchedEffect(charId, readOnly) {
        if (!readOnly) {
            inventoryViewModel.loadInventory(charId)
        }

        roomViewModel.loadRoom(charId)
    }

    val html = remember(roomState.placedDecorations, roomState.roomTheme) {
        buildPersonalRoomHtml(roomState)
    }

    val placedByDecoration = remember(roomState.placedDecorations) {
        roomState.placedDecorations.associate { it.decorationId to it.slotId }
    }

    val occupiedDecoration = activeSlotDecorationId?.let { personalRoomDecorationById(it) }

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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFFFFD700)
                    )
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
                        text = if (readOnly) {
                            "👁️ Solo visita • ${roomState.placedDecorations.size}/${PERSONAL_ROOM_SLOTS.size} baldosas decoradas"
                        } else {
                            "🪙 ${character?.coins ?: 0} • ${roomState.placedDecorations.size}/${PERSONAL_ROOM_SLOTS.size} baldosas ocupadas"
                        },
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

                        addJavascriptInterface(
                            PersonalRoomBridge(
                                onSlotFocus = { slotId ->
                                    activeSlotId = slotId
                                    activeSlotLabel = slotId?.let { personalRoomSlotById(it)?.label }
                                    activeSlotDecorationId = slotId?.let { currentSlotId ->
                                        roomState.placedDecorations.firstOrNull { it.slotId == currentSlotId }?.decorationId
                                    }
                                }
                            ),
                            "AndroidRoom"
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

            if (!readOnly) {
                Button(
                    onClick = { showDecorationPanel = !showDecorationPanel },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xCC2A180F)
                    )
                ) {
                    Text(
                        if (showDecorationPanel) "Ocultar decoración" else "🧰 Decorar",
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }

        val isOnSlot = activeSlotId != null
        val currentSlotLabel = activeSlotLabel ?: "Ninguna"

        Surface(
            color = Color(0xFF1A120D),
            border = BorderStroke(1.dp, Color(0xFF4A3528)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Baldosa activa: $currentSlotLabel",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    readOnly -> {
                        Text(
                            text = occupiedDecoration?.let {
                                "Decoración: ${it.emoji} ${it.name}"
                            } ?: "Solo estás visitando esta sala.",
                            color = Color(0xFFD7C8B6),
                            fontSize = 11.sp
                        )
                    }

                    !isOnSlot -> {
                        Text(
                            text = "No estás sobre ninguna baldosa.",
                            color = Color(0xFFD7C8B6),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { },
                            enabled = false,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Elegir decoración")
                        }
                    }

                    occupiedDecoration != null -> {
                        Text(
                            text = "Ahora mismo tiene: ${occupiedDecoration.emoji} ${occupiedDecoration.name}",
                            color = Color(0xFFD7C8B6),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showPlacementSheet = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cambiar")
                            }

                            Button(
                                onClick = {
                                    activeSlotId?.let { slotId ->
                                        roomViewModel.removeDecorationFromSlot(slotId) { message ->
                                            feedback = message
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A2A20)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Quitar")
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Esta baldosa está libre.",
                            color = Color(0xFFD7C8B6),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showPlacementSheet = true },
                            enabled = isOnSlot,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Elegir decoración")
                        }
                    }
                }
            }
        }

        if (!readOnly && showDecorationPanel) {
            Surface(color = Color(0xEE1B120D)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = Color(0xFF463225),
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Decoraciones",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Button(
                            onClick = { showDecorationPanel = false },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A2116)
                            )
                        ) {
                            Text("Cerrar")
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(PERSONAL_ROOM_CATALOG) { decoration ->
                            val owned = roomState.ownedDecorationIds.contains(decoration.id)
                            val placedSlotId = placedByDecoration[decoration.id]
                            val placedSlotLabel = placedSlotId?.let { personalRoomSlotById(it)?.label }
                            val canAfford = (character?.coins ?: 0) >= decoration.price
                            val activeOccupiedByOther =
                                activeSlotDecorationId != null && activeSlotDecorationId != decoration.id

                            DecorationCard(
                                decoration = decoration,
                                owned = owned,
                                placedSlotLabel = placedSlotLabel,
                                activeSlotLabel = activeSlotLabel,
                                canAfford = canAfford,
                                activeSlotOccupiedByOther = activeOccupiedByOther,
                                onBuy = {
                                    roomViewModel.buyDecoration(
                                        charId = charId,
                                        decorationId = decoration.id,
                                        spendCoins = { amount ->
                                            inventoryViewModel.spendCoins(charId, amount)
                                        }
                                    ) { message ->
                                        feedback = message
                                    }
                                },
                                onPlace = {
                                    val targetSlot = activeSlotId ?: return@DecorationCard
                                    roomViewModel.placeDecoration(
                                        decorationId = decoration.id,
                                        slotId = targetSlot
                                    ) { message ->
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
                }
            }
        }
    }

    if (!readOnly && showPlacementSheet && activeSlotId != null) {
        val availableDecorations = PERSONAL_ROOM_CATALOG

        ModalBottomSheet(
            onDismissRequest = { showPlacementSheet = false },
            containerColor = Color(0xFF1A120D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Colocar en ${activeSlotLabel ?: "baldosa"}",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(availableDecorations) { decoration ->
                        val owned = roomState.ownedDecorationIds.contains(decoration.id)
                        val canAfford = (character?.coins ?: 0) >= decoration.price

                        Surface(
                            color = Color(0xFF241711),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4A3528)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        owned -> {
                                            roomViewModel.placeDecoration(
                                                decorationId = decoration.id,
                                                slotId = activeSlotId!!
                                            ) { message ->
                                                feedback = message
                                            }
                                            showPlacementSheet = false
                                        }

                                        canAfford -> {
                                            roomViewModel.buyDecoration(
                                                charId = charId,
                                                decorationId = decoration.id,
                                                spendCoins = { amount ->
                                                    inventoryViewModel.spendCoins(charId, amount)
                                                }
                                            ) { message ->
                                                feedback = message
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = decoration.emoji,
                                    fontSize = 24.sp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = decoration.name,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = decoration.description,
                                        color = Color(0xFFD7C8B6),
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🪙 ${decoration.price}",
                                        color = if (canAfford || owned) Color(0xFFFFD700) else Color(0xFFFF8A80),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = when {
                                        owned -> "Colocar"
                                        canAfford -> "Comprar"
                                        else -> "Sin monedas"
                                    },
                                    color = when {
                                        owned -> Color(0xFF9BE7C4)
                                        canAfford -> Color(0xFFFFD700)
                                        else -> Color(0xFFFF8A80)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
// Ejecuta la lógica de decoration card.
private fun DecorationCard(
    decoration: PersonalRoomDecoration,
    owned: Boolean,
    placedSlotLabel: String?,
    activeSlotLabel: String?,
    canAfford: Boolean,
    activeSlotOccupiedByOther: Boolean,
    onBuy: () -> Unit,
    onPlace: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = Color(0xFF241711),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF4A3528)),
        modifier = Modifier.width(215.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    !owned -> "No comprada"
                    placedSlotLabel != null -> "Colocada en $placedSlotLabel"
                    else -> "Comprada, sin colocar"
                },
                color = Color(0xFFBBAE9C),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                !owned -> {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (canAfford) "Comprar" else "Sin monedas")
                    }
                }

                else -> {
                    Button(
                        onClick = onPlace,
                        enabled = activeSlotLabel != null && !activeSlotOccupiedByOther,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            when {
                                activeSlotLabel == null -> "Pisa una baldosa"
                                placedSlotLabel == null -> "Colocar aquí"
                                else -> "Mover aquí"
                            }
                        )
                    }

                    if (placedSlotLabel != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onRemove,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5A2A1A)
                            )
                        ) {
                            Text("Quitar")
                        }
                    }
                }
            }
        }
    }
}

// Construye personal room html.
private fun buildPersonalRoomHtml(state: PersonalRoomState): String {
    val slotsJs = PERSONAL_ROOM_SLOTS.joinToString(",\n") { slot ->
        """{ id: "${slot.id}", label: "${slot.label}", x: ${slot.x}, z: ${slot.z} }"""
    }

    val decorationCode = state.placedDecorations.joinToString("\n") { placed ->
        """
${buildDecorationJs(placed.decorationId, placed.slotId)}
occupiedBySlot["${placed.slotId}"] = "${placed.decorationId}";
        """.trimIndent()
    }

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"/>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { background:#0E0A08; overflow:hidden; touch-action:none; font-family:sans-serif; }
canvas { display:block; }
#joystickBase {
    position: fixed;
    left: 18px;
    bottom: 18px;
    width: 120px;
    height: 120px;
    border-radius: 60px;
    background: rgba(255, 215, 0, 0.10);
    border: 2px solid rgba(255, 215, 0, 0.25);
    z-index: 5;
}
#joystickKnob {
    position: absolute;
    left: 40px;
    top: 40px;
    width: 36px;
    height: 36px;
    border-radius: 18px;
    background: rgba(255, 215, 0, 0.65);
    border: 2px solid rgba(255, 245, 200, 0.9);
}
#roomHint {
    position: fixed;
    right: 14px;
    bottom: 18px;
    color: rgba(255,240,210,0.82);
    font-size: 12px;
    text-align: right;
    z-index: 5;
}
</style>
</head>
<body>
<div id="joystickBase">
    <div id="joystickKnob"></div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
<script>
var W = window.innerWidth, H = window.innerHeight;
var scene = new THREE.Scene();
scene.background = new THREE.Color(0x120C0A);
scene.fog = new THREE.FogExp2(0x120C0A, 0.018);

var camera = new THREE.PerspectiveCamera(58, W/H, 0.05, 160);
var renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(W, H);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
document.body.appendChild(renderer.domElement);

var ambient = new THREE.AmbientLight(0xC89A6A, 0.95);
scene.add(ambient);

var sun = new THREE.DirectionalLight(0xFFD9A8, 1.35);
sun.position.set(8, 12, 8);
sun.castShadow = true;
sun.shadow.mapSize.width = 1024;
sun.shadow.mapSize.height = 1024;
sun.shadow.camera.near = 0.5;
sun.shadow.camera.far = 50;
sun.shadow.camera.left = -12;
sun.shadow.camera.right = 12;
sun.shadow.camera.top = 12;
sun.shadow.camera.bottom = -12;
scene.add(sun);

var fill = new THREE.PointLight(0x88B8FF, 0.30, 18);
fill.position.set(-5, 4, -5);
scene.add(fill);

function makeMat(color, rough, metal, emissive, emissiveInt, opacity, transparent) {
    return new THREE.MeshStandardMaterial({
        color: color,
        roughness: rough !== undefined ? rough : 0.88,
        metalness: metal !== undefined ? metal : 0.0,
        emissive: emissive !== undefined ? emissive : 0x000000,
        emissiveIntensity: emissiveInt !== undefined ? emissiveInt : 0.0,
        opacity: opacity !== undefined ? opacity : 1.0,
        transparent: transparent !== undefined ? transparent : (opacity !== undefined && opacity < 1.0)
    });
}

function addMesh(geo, mat, x, y, z, rx, ry, rz, sx, sy, sz) {
    var mesh = new THREE.Mesh(geo, mat);
    mesh.position.set(x || 0, y || 0, z || 0);
    if (rx || ry || rz) mesh.rotation.set(rx || 0, ry || 0, rz || 0);
    if (sx) mesh.scale.set(sx, sy || sx, sz || sx);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    scene.add(mesh);
    return mesh;
}

function clamp(v, min, max) {
    return Math.max(min, Math.min(max, v));
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

var occupiedBySlot = {};
var SLOTS = [
$slotsJs
];

function slotPos(slotId) {
    for (var i = 0; i < SLOTS.length; i++) {
        if (SLOTS[i].id === slotId) return SLOTS[i];
    }
    return { x: 0, z: 0, label: "Centro" };
}

// Sala abierta frontalmente para que siempre se vea bien
addMesh(new THREE.BoxGeometry(12.8, 0.18, 12.8), makeMat(0x5C4636, 0.98, 0.02), 0, -0.10, 0);
addMesh(new THREE.BoxGeometry(12.0, 0.04, 12.0), makeMat(0xC49B72, 0.96, 0.02), 0, 0.00, 0);

// Pared trasera
addMesh(new THREE.BoxGeometry(12.2, 4.0, 0.25), stoneMat, 0, 2.0, -6.05);

// Paredes laterales
addMesh(new THREE.BoxGeometry(0.25, 4.0, 12.2), stoneMat, -6.05, 2.0, 0);
addMesh(new THREE.BoxGeometry(0.25, 4.0, 12.2), stoneMat,  6.05, 2.0, 0);

// Sin pared frontal y sin techo cerrado, tipo diorama jugable
addMesh(new THREE.BoxGeometry(12.2, 0.20, 0.30), darkStoneMat, 0, 4.0, -6.0);

// Columnas del fondo
[-4.0, -1.3, 1.3, 4.0].forEach(function(x){
    addMesh(new THREE.BoxGeometry(0.22, 3.7, 0.22), darkWoodMat, x, 1.85, -5.55);
});

// Columnas laterales
[-4.0, -1.3, 1.3, 4.0].forEach(function(z){
    addMesh(new THREE.BoxGeometry(0.22, 3.7, 0.22), darkWoodMat, -5.55, 1.85, z);
    addMesh(new THREE.BoxGeometry(0.22, 3.7, 0.22), darkWoodMat,  5.55, 1.85, z);
});

// Alfombra central para dar referencia visual
addMesh(new THREE.BoxGeometry(4.0, 0.02, 6.2), makeMat(0x8A2D2D, 0.95, 0.0), 0, 0.03, 0.3);
addMesh(new THREE.BoxGeometry(3.4, 0.01, 5.6), makeMat(0xC9A34A, 0.60, 0.10), 0, 0.04, 0.3);

// iluminación ambiente fija
window._roomFires = [];
[[-4.8, 2.0, -4.8], [4.8, 2.0, -4.8], [-4.8, 2.0, 4.8], [4.8, 2.0, 4.8]].forEach(function(p){
    addMesh(new THREE.SphereGeometry(0.10, 6, 5), fireMat, p[0], p[1], p[2]);
    var light = new THREE.PointLight(0xFFAA55, 0.55, 5.5);
    light.position.set(p[0], p[1] + 0.08, p[2]);
    light.userData.baseI = 0.55;
    scene.add(light);
    window._roomFires.push(light);
});

// Baldosas
var tileMeshes = [];
function createTile(slot) {
    var baseMat = makeMat(0xB8966C, 0.98, 0.02);
    var borderMat = makeMat(0x6E4F33, 0.95, 0.02);
    var glowMat = makeMat(0xFFD700, 0.2, 0.0, 0xFFD700, 0.0, 0.10, true);

    var border = addMesh(new THREE.BoxGeometry(1.72, 0.025, 1.72), borderMat, slot.x, 0.015, slot.z);
    var base = addMesh(new THREE.BoxGeometry(1.54, 0.035, 1.54), baseMat, slot.x, 0.035, slot.z);
    var glow = addMesh(new THREE.BoxGeometry(1.58, 0.01, 1.58), glowMat, slot.x, 0.06, slot.z);

    glow.renderOrder = 3;
    glow.material.depthWrite = false;

    tileMeshes.push({
        id: slot.id,
        label: slot.label,
        base: base,
        border: border,
        glow: glow
    });
}
SLOTS.forEach(createTile);

// Personaje
var player = new THREE.Group();

var robeMat = makeMat(0xE9E1D2, 0.88, 0.02);
var trimMat = makeMat(0xC9A34A, 0.45, 0.20);
var skinMat = makeMat(0xE6C29A, 0.82, 0.02);
var hoodMat = makeMat(0x8A2D2D, 0.90, 0.0);

// túnica
var torso = new THREE.Mesh(
    new THREE.CylinderGeometry(0.16, 0.28, 0.62, 10),
    robeMat
);
torso.position.y = 0.34;
torso.castShadow = true;
torso.receiveShadow = true;
player.add(torso);

// borde dorado cintura
var belt = new THREE.Mesh(
    new THREE.TorusGeometry(0.19, 0.025, 6, 20),
    trimMat
);
belt.rotation.x = Math.PI / 2;
belt.position.y = 0.27;
player.add(belt);

// cabeza
var head = new THREE.Mesh(
    new THREE.SphereGeometry(0.15, 10, 10),
    skinMat
);
head.position.y = 0.76;
head.castShadow = true;
player.add(head);

// capucha
var hood = new THREE.Mesh(
    new THREE.ConeGeometry(0.17, 0.22, 10),
    hoodMat
);
hood.position.y = 0.92;
hood.rotation.x = 0.08;
hood.castShadow = true;
player.add(hood);

// base brillante
var marker = new THREE.Mesh(
    new THREE.TorusGeometry(0.34, 0.025, 8, 28),
    makeMat(0xFFD700, 0.18, 0.0, 0xFFD700, 0.55, 0.95, true)
);
marker.rotation.x = Math.PI / 2;
marker.position.y = 0.04;
player.add(marker);

scene.add(player);

var playerX = 0.0;
var playerZ = 2.8;
camera.position.set(0, 5.2, 7.2);

// decoraciones colocadas
window._magicCrystals = [];
$decorationCode

// joystick
var joyBase = document.getElementById('joystickBase');
var joyKnob = document.getElementById('joystickKnob');
var joyX = 0;
var joyY = 0;
var joyActive = false;

function setJoystick(clientX, clientY) {
    var rect = joyBase.getBoundingClientRect();
    var cx = rect.left + rect.width / 2;
    var cy = rect.top + rect.height / 2;
    var dx = clientX - cx;
    var dy = clientY - cy;
    var dist = Math.sqrt(dx * dx + dy * dy);
    var maxDist = 36;

    if (dist > maxDist) {
        dx = dx / dist * maxDist;
        dy = dy / dist * maxDist;
    }

    joyKnob.style.left = (40 + dx) + 'px';
    joyKnob.style.top = (40 + dy) + 'px';

    joyX = dx / maxDist;
    joyY = dy / maxDist;
}

function resetJoystick() {
    joyKnob.style.left = '40px';
    joyKnob.style.top = '40px';
    joyX = 0;
    joyY = 0;
    joyActive = false;
}

joyBase.addEventListener('touchstart', function(e) {
    joyActive = true;
    setJoystick(e.touches[0].clientX, e.touches[0].clientY);
    e.preventDefault();
}, { passive: false });

joyBase.addEventListener('touchmove', function(e) {
    if (!joyActive) return;
    setJoystick(e.touches[0].clientX, e.touches[0].clientY);
    e.preventDefault();
}, { passive: false });

joyBase.addEventListener('touchend', function() {
    resetJoystick();
}, { passive: true });

joyBase.addEventListener('mousedown', function(e) {
    joyActive = true;
    setJoystick(e.clientX, e.clientY);
});

document.addEventListener('mousemove', function(e) {
    if (!joyActive) return;
    setJoystick(e.clientX, e.clientY);
});

document.addEventListener('mouseup', function() {
    resetJoystick();
});

// cámara
function updateCamera() {
    var targetCamX = playerX * 0.35;
    var targetCamY = 5.2;
    var targetCamZ = 7.2 + playerZ * 0.10;

    camera.position.x += (targetCamX - camera.position.x) * 0.08;
    camera.position.y += (targetCamY - camera.position.y) * 0.08;
    camera.position.z += (targetCamZ - camera.position.z) * 0.08;

    camera.lookAt(playerX, 0.9, playerZ - 1.6);
}

// slot activa
var activeSlotId = null;

function updateSlotHighlight() {
    var nearest = null;
    var nearestDist = 999;

    for (var i = 0; i < SLOTS.length; i++) {
        var slot = SLOTS[i];
        var dx = playerX - slot.x;
        var dz = playerZ - slot.z;
        var dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < nearestDist) {
            nearestDist = dist;
            nearest = slot;
        }
    }

    var newActiveId = nearestDist <= 0.75 ? nearest.id : null;

    if (newActiveId === activeSlotId) return;

    activeSlotId = newActiveId;

    for (var j = 0; j < tileMeshes.length; j++) {
        var tile = tileMeshes[j];
        var occupied = !!occupiedBySlot[tile.id];

        if (tile.id === activeSlotId) {
            tile.base.material.color.setHex(0xC9A34A);
            tile.border.material.color.setHex(0xFFD700);
            tile.glow.material.emissiveIntensity = 0.7;
        } else if (occupied) {
            tile.base.material.color.setHex(0x6E8A5E);
            tile.border.material.color.setHex(0x9CCB7A);
            tile.glow.material.emissiveIntensity = 0.18;
        } else {
            tile.base.material.color.setHex(0x8A7A68);
            tile.border.material.color.setHex(0x4A3528);
            tile.glow.material.emissiveIntensity = 0.0;
        }
    }

    if (activeSlotId) {
        if (window.AndroidRoom && window.AndroidRoom.onSlotFocus) {
            window.AndroidRoom.onSlotFocus(activeSlotId);
        }
    } else {
        if (window.AndroidRoom && window.AndroidRoom.onSlotClear) {
            window.AndroidRoom.onSlotClear();
        }
    }
}

// movimiento
function updateMovement() {
    var speed = 0.028;
    playerX = clamp(playerX + joyX * speed, -4.9, 4.9);
    playerZ = clamp(playerZ + joyY * speed, -4.9, 4.9);

    player.position.x = playerX;
    player.position.z = playerZ;

    if (Math.abs(joyX) > 0.05 || Math.abs(joyY) > 0.05) {
        player.rotation.y = Math.atan2(joyX, joyY);
    }
}

window.addEventListener('resize', function() {
    W = window.innerWidth;
    H = window.innerHeight;
    camera.aspect = W / H;
    camera.updateProjectionMatrix();
    renderer.setSize(W, H);
});

var t = 0;
function animate() {
    requestAnimationFrame(animate);
    t += 0.016;

    updateMovement();
    updateCamera();
    updateSlotHighlight();

    if (window._roomFires) {
        window._roomFires.forEach(function(light, i) {
            light.intensity = light.userData.baseI + Math.sin(t * 4.3 + i) * 0.10;
        });
    }

    if (window._magicCrystals) {
        window._magicCrystals.forEach(function(crystal, i) {
            crystal.rotation.y += 0.01;
            crystal.position.y = crystal.userData.baseY + Math.sin(t * 2.2 + i) * 0.04;
        });
    }

    renderer.render(scene, camera);
}
animate();
</script>
</body>
</html>
    """.trimIndent()
}

// Construye decoration js.
private fun buildDecorationJs(decorationId: String, slotId: String): String {
    return when (decorationId) {
        "banner_royal" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.05,0.06,1.75,8), darkWoodMat, p.x, 0.88, p.z);
addMesh(new THREE.BoxGeometry(1.05,0.08,0.08), goldMat, p.x, 1.70, p.z);
addMesh(new THREE.BoxGeometry(0.88,0.78,0.05), clothRed, p.x, 1.26, p.z + 0.03);
        """.trimIndent()

        "torch_pair" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.18,0.22,0.38,8), darkWoodMat, p.x, 0.20, p.z);
addMesh(new THREE.CylinderGeometry(0.07,0.10,0.70,8), goldMat, p.x, 0.72, p.z);
addMesh(new THREE.SphereGeometry(0.12,6,5), fireMat, p.x, 1.16, p.z);
var fireLight = new THREE.PointLight(0xFF8A2A, 0.95, 4.0);
fireLight.position.set(p.x, 1.20, p.z);
fireLight.userData.baseI = 0.95;
scene.add(fireLight);
window._roomFires.push(fireLight);
        """.trimIndent()

        "weapon_rack" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(0.90,0.10,0.20), woodMat, p.x, 0.45, p.z);
addMesh(new THREE.BoxGeometry(0.10,1.00,0.20), darkWoodMat, p.x - 0.34, 0.50, p.z);
addMesh(new THREE.BoxGeometry(0.10,1.00,0.20), darkWoodMat, p.x + 0.34, 0.50, p.z);
addMesh(new THREE.BoxGeometry(0.08,0.90,0.08), goldMat, p.x - 0.08, 0.92, p.z, 0,0,0.60);
addMesh(new THREE.BoxGeometry(0.60,0.05,0.05), goldMat, p.x + 0.06, 0.80, p.z, 0,0,-0.30);
        """.trimIndent()

        "treasure_chest" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(0.95,0.50,0.62), woodMat, p.x, 0.26, p.z);
addMesh(new THREE.BoxGeometry(0.98,0.16,0.65), darkWoodMat, p.x, 0.56, p.z, 0.30,0,0);
addMesh(new THREE.BoxGeometry(0.14,0.18,0.04), goldMat, p.x, 0.28, p.z + 0.32);
        """.trimIndent()

        "red_rug" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(1.35,0.02,1.35), clothRed, p.x, 0.03, p.z);
addMesh(new THREE.BoxGeometry(1.12,0.01,1.12), goldMat, p.x, 0.04, p.z);
        """.trimIndent()

        "crystal_orb" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.14,0.18,0.22,8), goldMat, p.x, 0.12, p.z);
var crystal = addMesh(new THREE.OctahedronGeometry(0.28, 0), crystalMat, p.x, 0.48, p.z, 0.2,0.4,0);
crystal.userData.baseY = 0.48;
window._magicCrystals.push(crystal);
        """.trimIndent()

        "book_stack" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(0.95,1.05,0.26), darkWoodMat, p.x, 0.52, p.z);
[0,1,2,3].forEach(function(i){
    addMesh(new THREE.BoxGeometry(0.70,0.10,0.20), i % 2 === 0 ? clothBlue : clothRed, p.x, 0.18 + i*0.18, p.z + 0.01);
});
        """.trimIndent()

        "potted_tree" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.22,0.18,0.34,10), goldMat, p.x, 0.17, p.z);
addMesh(new THREE.CylinderGeometry(0.06,0.08,0.75,8), woodMat, p.x, 0.64, p.z);
addMesh(new THREE.SphereGeometry(0.42,8,7), greenMat, p.x, 1.18, p.z);
addMesh(new THREE.SphereGeometry(0.26,8,7), makeMat(0x3C7F38,0.88,0.0), p.x - 0.22, 1.02, p.z + 0.10);
addMesh(new THREE.SphereGeometry(0.24,8,7), makeMat(0x3C7F38,0.88,0.0), p.x + 0.18, 1.00, p.z - 0.08);
        """.trimIndent()

        "war_table" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(1.20,0.12,0.82), woodMat, p.x, 0.62, p.z);
[[-0.45,-0.28],[0.45,-0.28],[-0.45,0.28],[0.45,0.28]].forEach(function(o){
    addMesh(new THREE.BoxGeometry(0.10,0.62,0.10), darkWoodMat, p.x + o[0], 0.30, p.z + o[1]);
});
addMesh(new THREE.BoxGeometry(0.92,0.03,0.60), clothBlue, p.x, 0.70, p.z);
        """.trimIndent()

        "throne_seat" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(0.92,0.22,0.82), goldMat, p.x, 0.28, p.z);
addMesh(new THREE.BoxGeometry(0.78,1.00,0.18), clothRed, p.x, 0.86, p.z - 0.28);
addMesh(new THREE.BoxGeometry(0.14,0.72,0.82), goldMat, p.x - 0.34, 0.56, p.z);
addMesh(new THREE.BoxGeometry(0.14,0.72,0.82), goldMat, p.x + 0.34, 0.56, p.z);
        """.trimIndent()

        "armor_stand" -> """
var p = slotPos("$slotId");
var steelMat = makeMat(0x9AA3AD, 0.35, 0.70);
addMesh(new THREE.CylinderGeometry(0.16,0.22,0.10,10), darkWoodMat, p.x, 0.05, p.z);
addMesh(new THREE.CylinderGeometry(0.05,0.06,1.10,8), darkWoodMat, p.x, 0.58, p.z);
addMesh(new THREE.BoxGeometry(0.42,0.46,0.20), steelMat, p.x, 0.78, p.z);
addMesh(new THREE.SphereGeometry(0.13,8,8), steelMat, p.x, 1.12, p.z);
addMesh(new THREE.BoxGeometry(0.62,0.08,0.08), steelMat, p.x, 0.84, p.z);
addMesh(new THREE.BoxGeometry(0.10,0.65,0.10), steelMat, p.x - 0.18, 0.42, p.z);
addMesh(new THREE.BoxGeometry(0.10,0.65,0.10), steelMat, p.x + 0.18, 0.42, p.z);
addMesh(new THREE.BoxGeometry(0.08,0.90,0.08), goldMat, p.x + 0.28, 0.55, p.z, 0,0,0.45);
""".trimIndent()

        "library_shelf" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(1.10,1.25,0.28), darkWoodMat, p.x, 0.63, p.z);
[-0.46,0,0.46].forEach(function(xo){
    addMesh(new THREE.BoxGeometry(0.08,1.25,0.30), woodMat, p.x + xo, 0.63, p.z);
});
[0.18,0.50,0.82,1.10].forEach(function(y){
    addMesh(new THREE.BoxGeometry(1.02,0.06,0.26), woodMat, p.x, y, p.z);
});
var bookColors = [clothRed, clothBlue, greenMat, goldMat];
for (var row = 0; row < 4; row++) {
    for (var i = 0; i < 5; i++) {
        addMesh(
            new THREE.BoxGeometry(0.12,0.22,0.18),
            bookColors[(row + i) % bookColors.length],
            p.x - 0.34 + i*0.17,
            0.22 + row*0.30,
            p.z
        );
    }
}
""".trimIndent()

        "brazier_gold" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.24,0.18,0.16,10), goldMat, p.x, 0.08, p.z);
addMesh(new THREE.CylinderGeometry(0.05,0.06,0.42,8), darkWoodMat, p.x, 0.34, p.z);
addMesh(new THREE.CylinderGeometry(0.22,0.18,0.16,10), darkStoneMat, p.x, 0.58, p.z);
addMesh(new THREE.SphereGeometry(0.13,6,5), fireMat, p.x, 0.76, p.z);
var brazierLight = new THREE.PointLight(0xFF9A33, 1.0, 4.2);
brazierLight.position.set(p.x, 0.82, p.z);
brazierLight.userData.baseI = 1.0;
scene.add(brazierLight);
window._roomFires.push(brazierLight);
""".trimIndent()

        "war_banner_blue" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.05,0.06,1.75,8), darkWoodMat, p.x, 0.88, p.z);
addMesh(new THREE.BoxGeometry(1.05,0.08,0.08), goldMat, p.x, 1.70, p.z);
addMesh(new THREE.BoxGeometry(0.88,0.78,0.05), clothBlue, p.x, 1.26, p.z + 0.03);
addMesh(new THREE.BoxGeometry(0.16,0.16,0.05), goldMat, p.x, 0.92, p.z + 0.03);
""".trimIndent()

        "feast_table" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(1.24,0.12,0.78), woodMat, p.x, 0.62, p.z);
[[-0.45,-0.28],[0.45,-0.28],[-0.45,0.28],[0.45,0.28]].forEach(function(o){
    addMesh(new THREE.BoxGeometry(0.10,0.62,0.10), darkWoodMat, p.x + o[0], 0.30, p.z + o[1]);
});
addMesh(new THREE.BoxGeometry(0.96,0.03,0.54), makeMat(0xD8C7A2,0.92,0.0), p.x, 0.70, p.z);
addMesh(new THREE.SphereGeometry(0.10,7,6), makeMat(0x9C4A22,0.82,0.0), p.x - 0.24, 0.77, p.z - 0.06);
addMesh(new THREE.SphereGeometry(0.08,7,6), makeMat(0x4C8A2A,0.82,0.0), p.x, 0.76, p.z + 0.02);
addMesh(new THREE.SphereGeometry(0.08,7,6), makeMat(0xC94A3A,0.82,0.0), p.x + 0.22, 0.76, p.z - 0.04);
addMesh(new THREE.CylinderGeometry(0.05,0.05,0.14,8), goldMat, p.x - 0.12, 0.79, p.z + 0.18);
addMesh(new THREE.CylinderGeometry(0.05,0.05,0.14,8), goldMat, p.x + 0.14, 0.79, p.z + 0.16);
""".trimIndent()

        "training_dummy" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.20,0.24,0.14,10), darkWoodMat, p.x, 0.07, p.z);
addMesh(new THREE.CylinderGeometry(0.06,0.07,1.25,8), darkWoodMat, p.x, 0.68, p.z);
addMesh(new THREE.CylinderGeometry(0.22,0.26,0.52,10), makeMat(0xB89452,0.96,0.0), p.x, 0.92, p.z);
addMesh(new THREE.BoxGeometry(0.72,0.08,0.08), woodMat, p.x, 0.96, p.z);
addMesh(new THREE.SphereGeometry(0.14,8,8), makeMat(0xB89452,0.96,0.0), p.x, 1.30, p.z);
""".trimIndent()

        "royal_window" -> """
var p = slotPos("$slotId");
var frameMat = makeMat(0x6E4F33, 0.92, 0.04);
var glassMat = makeMat(0x6FA8FF, 0.12, 0.04, 0x6FA8FF, 0.35, 0.72, true);
addMesh(new THREE.BoxGeometry(0.95,1.50,0.12), frameMat, p.x, 0.75, p.z);
addMesh(new THREE.BoxGeometry(0.72,1.18,0.05), glassMat, p.x, 0.80, p.z + 0.03);
addMesh(new THREE.BoxGeometry(0.06,1.18,0.07), goldMat, p.x, 0.80, p.z + 0.04);
addMesh(new THREE.BoxGeometry(0.72,0.06,0.07), goldMat, p.x, 0.80, p.z + 0.04);
var winLight = new THREE.PointLight(0x88BBFF, 0.55, 3.2);
winLight.position.set(p.x, 0.95, p.z + 0.18);
scene.add(winLight);
""".trimIndent()

        "small_fountain" -> """
var p = slotPos("$slotId");
var waterMat = makeMat(0x5BBEF0, 0.10, 0.05, 0x5BBEF0, 0.20, 0.84, true);
addMesh(new THREE.CylinderGeometry(0.52,0.56,0.16,18), stoneMat, p.x, 0.08, p.z);
addMesh(new THREE.CylinderGeometry(0.36,0.36,0.08,18), waterMat, p.x, 0.16, p.z);
addMesh(new THREE.CylinderGeometry(0.08,0.10,0.42,10), darkStoneMat, p.x, 0.37, p.z);
addMesh(new THREE.SphereGeometry(0.07,7,6), crystalMat, p.x, 0.68, p.z);
addMesh(new THREE.TorusGeometry(0.24,0.025,6,24), makeMat(0xDDF8FF,0.14,0.0,0x9EDFFF,0.18,0.82,true), p.x, 0.19, p.z, Math.PI/2, 0, 0);
""".trimIndent()

        "trophy_pedestal" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(0.48,0.82,0.48), darkStoneMat, p.x, 0.41, p.z);
addMesh(new THREE.BoxGeometry(0.58,0.08,0.58), stoneMat, p.x, 0.86, p.z);
addMesh(new THREE.CylinderGeometry(0.12,0.16,0.18,10), goldMat, p.x, 1.00, p.z);
addMesh(new THREE.BoxGeometry(0.32,0.10,0.12), goldMat, p.x, 1.10, p.z);
addMesh(new THREE.TorusGeometry(0.14,0.03,6,18), goldMat, p.x - 0.14, 1.10, p.z, 0,0,Math.PI/2);
addMesh(new THREE.TorusGeometry(0.14,0.03,6,18), goldMat, p.x + 0.14, 1.10, p.z, 0,0,Math.PI/2);
""".trimIndent()

        "music_corner" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.18,0.20,0.30,10), woodMat, p.x - 0.20, 0.15, p.z + 0.08);
addMesh(new THREE.BoxGeometry(0.42,0.06,0.42), darkWoodMat, p.x - 0.20, 0.34, p.z + 0.08);
addMesh(new THREE.SphereGeometry(0.18,8,8), makeMat(0xB97A3B,0.84,0.02), p.x + 0.18, 0.38, p.z);
addMesh(new THREE.BoxGeometry(0.08,0.52,0.08), darkWoodMat, p.x + 0.30, 0.62, p.z, 0,0,-0.40);
addMesh(new THREE.BoxGeometry(0.32,0.02,0.02), goldMat, p.x + 0.20, 0.42, p.z + 0.02, 0,0,-0.32);
""".trimIndent()

        "alchemy_set" -> """
var p = slotPos("$slotId");
addMesh(new THREE.BoxGeometry(1.10,0.10,0.60), woodMat, p.x, 0.56, p.z);
[[-0.40,-0.20],[0.40,-0.20],[-0.40,0.20],[0.40,0.20]].forEach(function(o){
    addMesh(new THREE.BoxGeometry(0.08,0.56,0.08), darkWoodMat, p.x + o[0], 0.28, p.z + o[1]);
});
addMesh(new THREE.CylinderGeometry(0.08,0.10,0.22,8), crystalMat, p.x - 0.20, 0.72, p.z);
addMesh(new THREE.CylinderGeometry(0.06,0.08,0.18,8), makeMat(0x7AFFB0,0.12,0.03,0x7AFFB0,0.30,0.80,true), p.x + 0.02, 0.68, p.z + 0.10);
addMesh(new THREE.CylinderGeometry(0.07,0.09,0.20,8), makeMat(0xFF77D8,0.12,0.03,0xFF77D8,0.30,0.80,true), p.x + 0.24, 0.70, p.z - 0.06);
addMesh(new THREE.BoxGeometry(0.32,0.04,0.18), goldMat, p.x, 0.66, p.z - 0.14);
var alcLight = new THREE.PointLight(0x88FFCC, 0.45, 2.6);
alcLight.position.set(p.x, 0.92, p.z);
scene.add(alcLight);
""".trimIndent()

        "crystal_cluster" -> """
var p = slotPos("$slotId");
addMesh(new THREE.CylinderGeometry(0.18,0.22,0.12,10), darkStoneMat, p.x, 0.06, p.z);
var c1 = addMesh(new THREE.OctahedronGeometry(0.18, 0), crystalMat, p.x - 0.12, 0.34, p.z + 0.04, 0.2,0.4,0);
var c2 = addMesh(new THREE.OctahedronGeometry(0.22, 0), crystalMat, p.x + 0.10, 0.42, p.z - 0.06, 0.0,0.2,0);
var c3 = addMesh(new THREE.OctahedronGeometry(0.14, 0), crystalMat, p.x + 0.02, 0.26, p.z + 0.14, 0.1,0.6,0);
c1.userData.baseY = 0.34;
c2.userData.baseY = 0.42;
c3.userData.baseY = 0.26;
window._magicCrystals.push(c1);
window._magicCrystals.push(c2);
window._magicCrystals.push(c3);
var crystalLight = new THREE.PointLight(0x7D7DFF, 0.65, 3.4);
crystalLight.position.set(p.x, 0.58, p.z);
scene.add(crystalLight);
""".trimIndent()

        else -> ""
    }
}
