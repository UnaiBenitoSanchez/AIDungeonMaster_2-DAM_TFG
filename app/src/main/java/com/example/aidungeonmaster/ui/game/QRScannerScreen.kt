package com.example.aidungeonmaster.ui.game

import android.Manifest
import android.media.MediaRouter2.getInstance
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import com.google.common.util.concurrent.ListenableFuture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    gameId: String,
    onBack: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedItem by remember { mutableStateOf<Item?>(null) }
    var isSaving     by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }

    LaunchedEffect(Unit) { if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(gameId) { inventoryViewModel.loadInventory(gameId) }

    MedievalBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { MedievalTitle("ESCANEAR BOTÍN") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                when {
                    !hasCameraPermission -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Se requiere permiso de cámara", color = Color.Red)
                        }
                    }
                    detectedItem == null -> {
                        // ── Instrucciones de formato ──────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x99000000)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                "Apunta a un QR con formato AIDO:tipo|nombre|efecto",
                                modifier = Modifier.padding(10.dp),
                                color = Color(0xFFFFD700),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // ── Visor de cámara ───────────────────────────────────
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                        ) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)

                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                                            processImageProxy(proxy) { qrContent ->
                                                if (detectedItem == null) {
                                                    val item = parseQrToItem(qrContent)
                                                    if (item != null) detectedItem = item
                                                }
                                            }
                                        }
                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview, imageAnalysis
                                            )
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        BotinEncontradoDialog(
                            item     = detectedItem!!,
                            isSaving = isSaving,
                            onCancel = { detectedItem = null },
                            onConfirm = {
                                scope.launch {
                                    isSaving = true
                                    inventoryViewModel.addItemToInventory(gameId, detectedItem!!)
                                    kotlinx.coroutines.delay(1000)
                                    isSaving = false
                                    onBack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── PROCESADO DE IMAGEN CON ML KIT ───────────────────────────────────────────

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(imageProxy: ImageProxy, onQrFound: (String) -> Unit) {
    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    BarcodeScanning.getClient().process(image)
        .addOnSuccessListener { barcodes ->
            for (b in barcodes) {
                if (b.valueType == Barcode.TYPE_TEXT) {
                    onQrFound(b.rawValue ?: "")
                    return@addOnSuccessListener
                }
            }
        }
        .addOnCompleteListener { imageProxy.close() }
}

// ── PARSER DE QR → ITEM ───────────────────────────────────────────────────────
//
//  FORMATOS SOPORTADOS (ver guía al final del archivo):
//
//  1. AIDO:tipo|nombre|descripcion|efecto
//     Ej: AIDO:pocion|Poción de Curación|Restaura energía vital|cura:2d4+2
//
//  2. JSON  {"name":"X","type":"Y","description":"Z","effect":"W"}
//
//  3. Texto plano → consumible básico sin efecto

private fun parseQrToItem(content: String): Item? {
    return try {
        when {
            // ── Formato AIDO: ─────────────────────────────────────────────
            content.uppercase().startsWith("AIDO:") -> {
                val body  = content.removePrefix("AIDO:").removePrefix("aido:")
                val parts = body.split("|")
                if (parts.size < 2) return null
                val type  = normalizeItemType(parts.getOrElse(0) { "consumible" })
                val name  = parts.getOrElse(1) { "Objeto misterioso" }
                val desc  = parts.getOrElse(2) { "" }
                val effect = parts.getOrElse(3) { defaultEffectForType(type) }
                Item(
                    id          = System.currentTimeMillis().toString(),
                    name        = name,
                    description = desc,
                    type        = type,
                    effect      = effect
                )
            }

            // ── Formato JSON ──────────────────────────────────────────────
            content.trimStart().startsWith("{") -> {
                val name  = content.substringAfter("\"name\":\"").substringBefore("\"").trim()
                val type  = normalizeItemType(content.substringAfter("\"type\":\"").substringBefore("\"").trim())
                val desc  = content.substringAfter("\"description\":\"").substringBefore("\"").trim()
                val eff   = content.substringAfter("\"effect\":\"").substringBefore("\"").trim()
                if (name.isBlank()) null
                else Item(
                    id          = System.currentTimeMillis().toString(),
                    name        = name,
                    description = desc,
                    type        = type,
                    effect      = eff.ifBlank { defaultEffectForType(type) }
                )
            }

            // ── Texto plano ───────────────────────────────────────────────
            content.isNotBlank() -> Item(
                id          = System.currentTimeMillis().toString(),
                name        = content.take(60),
                description = "Objeto encontrado en la aventura",
                type        = "consumible",
                effect      = ""
            )

            else -> null
        }
    } catch (e: Exception) { null }
}

/** Normaliza sinónimos del campo tipo */
private fun normalizeItemType(raw: String): String = when (raw.lowercase().trim()) {
    "pocion", "poción", "potion", "bebida", "elixir"                  -> "pocion"
    "arma", "weapon", "espada", "hacha", "daga", "arco", "lanza"      -> "arma"
    "armadura", "armor", "escudo", "casco", "yelmo", "peto"           -> "armadura"
    "pergamino", "scroll", "hechizo", "magia"                         -> "pergamino"
    "veneno", "poison"                                                 -> "veneno"
    "granada", "explosivo", "bomba"                                    -> "explosivo"
    "reliquia", "artefacto", "artifact"                               -> "reliquia"
    "comida", "ración", "food"                                        -> "consumible"
    else                                                               -> "consumible"
}

/** Efecto por defecto según tipo cuando el QR no especifica uno */
private fun defaultEffectForType(type: String): String = when (type) {
    "pocion"     -> "cura:1d8+2"
    "arma"       -> "daño:1d6"
    "armadura"   -> "+2 CA"
    "pergamino"  -> "daño:2d6"
    "veneno"     -> "veneno:1d4"
    "explosivo"  -> "daño:2d8"
    else         -> ""
}

// ── DIÁLOGO DE BOTÍN ─────────────────────────────────────────────────────────

@Composable
fun BotinEncontradoDialog(
    item: Item, isSaving: Boolean,
    onCancel: () -> Unit, onConfirm: () -> Unit
) {
    val typeEmoji = when (item.type) {
        "pocion"    -> "🧪"
        "arma"      -> "⚔️"
        "armadura"  -> "🛡️"
        "pergamino" -> "📜"
        "veneno"    -> "☠️"
        "explosivo" -> "💣"
        "reliquia"  -> "✨"
        else        -> "🎒"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA000000))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¡BOTÍN ENCONTRADO!", color = Color(0xFFFFD700),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(typeEmoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            InventoryItemRow(item)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = onCancel, enabled = !isSaving) {
                    Text("Desechar", color = Color.Red)
                }
                Button(
                    onClick  = onConfirm,
                    enabled  = !isSaving,
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black)
                    else Text("Guardar en mochila", color = Color.Black)
                }
            }
        }
    }
}