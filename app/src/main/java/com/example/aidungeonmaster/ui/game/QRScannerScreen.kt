package com.example.aidungeonmaster.ui.game

import com.example.aidungeonmaster.ui.i18n.Text

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// ── LISTA DE PALABRAS CLAVE DE SUPERMERCADOS ──────────────────────────────────
private val SUPERMARKET_KEYWORDS = mapOf(
    "carrefour" to "CARREFOUR",
    "mercadona" to "MERCADONA",
    "lidl" to "LIDL",
    "aldi" to "ALDI",
    "dia" to "DIA",
    "lupa" to "LUPA",
    "eroski" to "EROSKI",
    "consum" to "CONSUM",
    "alcampo" to "ALCAMPO",
    "hipercor" to "HIPERCOR",
    "supercor" to "SUPERCOR",
    "corte ingles" to "EL CORTE INGLÉS",
    "el corte ingles" to "EL CORTE INGLÉS",
    "spar" to "SPAR",
    "bon preu" to "BON PREU",
    "simply" to "SIMPLY",
    "auchan" to "AUCHAN",
    "froiz" to "FROIZ",
    "gadis" to "GADIS"
)

// ── LISTA DE PALABRAS CLAVE DE BANCOS ─────────────────────────────────────────
private val BANK_KEYWORDS = mapOf(
    "santander" to "Santander",
    "caixabank" to "CaixaBank",
    "caixa bank" to "CaixaBank",
    "caixa" to "CaixaBank",
    "bbva" to "BBVA",
    "banco sabadell" to "Sabadell",
    "sabadell" to "Sabadell",
    "bankinter" to "Bankinter",
    "unicaja" to "Unicaja",
    "abanca" to "Abanca",
    "kutxabank" to "Kutxabank",
    "cajamar" to "Cajamar",
    "ibercaja" to "Ibercaja"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de qrscanner screen.
fun QRScannerScreen(
    gameId: String,
    onBack: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedItem by remember { mutableStateOf<Item?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var detectedSupermarket by remember { mutableStateOf<String?>(null) }
    var supermarketCooldown by remember { mutableStateOf(false) }

    var detectedBank by remember { mutableStateOf<String?>(null) }
    var bankCooldown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        hasCameraPermission = it
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(gameId) {
        inventoryViewModel.loadInventory(gameId)
    }

    val character by inventoryViewModel.character.collectAsState()

    MedievalBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { MedievalTitle("ESCANEAR") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    when {
                        !hasCameraPermission -> {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Se requiere permiso de cámara", color = Color.Red)
                            }
                        }

                        detectedItem != null -> {
                            BotinEncontradoDialog(
                                item = detectedItem!!,
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

                        else -> {
                            // ── Panel de instrucciones ─────────────────────────
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x99000000)
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "📦  QR de objeto → formato AIDO:tipo|nombre|descripcion|efecto",
                                        color = Color(0xFFFFD700),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "🏪  Supermercado real → apunta al cartel, ticket o texto visible para abrir la tienda",
                                        color = Color(0xFF90CAF9),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "🏦  Banco real → apunta al cartel, cajero o texto visible para acceder al banco",
                                        color = Color(0xFFA5D6A7),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // ── Visor de cámara ────────────────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        2.dp,
                                        Color(0xFFFFD700),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        val cameraProviderFuture =
                                            ProcessCameraProvider.getInstance(ctx)

                                        cameraProviderFuture.addListener({
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }

                                            val executor = Executors.newSingleThreadExecutor()

                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(
                                                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                                )
                                                .build()

                                            imageAnalysis.setAnalyzer(executor) { proxy ->
                                                analyzeFrame(
                                                    proxy = proxy,
                                                    onQrFound = { qrContent ->
                                                        if (detectedItem == null &&
                                                            detectedSupermarket == null &&
                                                            detectedBank == null
                                                        ) {
                                                            parseQrToItem(qrContent)?.let {
                                                                detectedItem = it
                                                            }
                                                        }
                                                    },
                                                    onSupermarketFound = { shopName ->
                                                        if (!supermarketCooldown &&
                                                            detectedSupermarket == null &&
                                                            detectedBank == null &&
                                                            detectedItem == null
                                                        ) {
                                                            detectedSupermarket = shopName
                                                        }
                                                    },
                                                    onBankFound = { bankName ->
                                                        if (!bankCooldown &&
                                                            detectedBank == null &&
                                                            detectedSupermarket == null &&
                                                            detectedItem == null
                                                        ) {
                                                            detectedBank = bankName
                                                        }
                                                    }
                                                )
                                            }

                                            try {
                                                cameraProvider.unbindAll()
                                                cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                                    preview,
                                                    imageAnalysis
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))

                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // ── Indicador de escaneo animado ───────────────
                                val transition = rememberInfiniteTransition(label = "scan")
                                val alpha by transition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        tween(900),
                                        RepeatMode.Reverse
                                    ),
                                    label = "scanAlpha"
                                )

                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = "🔍  Escaneando...",
                                        color = Color(0xFFFFD700).copy(alpha = alpha),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Overlay de la tienda ───────────────────────────────────────────
            AnimatedVisibility(
                visible = detectedSupermarket != null,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                detectedSupermarket?.let { shopName ->
                    SupermarketShopOverlay(
                        supermarketName = shopName,
                        gameId = gameId,
                        currentCoins = character?.coins ?: 0,
                        inventoryViewModel = inventoryViewModel,
                        onDismiss = {
                            detectedSupermarket = null
                            scope.launch {
                                supermarketCooldown = true
                                kotlinx.coroutines.delay(5000)
                                supermarketCooldown = false
                            }
                        }
                    )
                }
            }

            // ── Overlay del banco ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = detectedBank != null,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                detectedBank?.let { bankName ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BankOverlay(
                            bankName = bankName,
                            gameId = gameId,
                            currentCoins = character?.coins ?: 0,
                            inventoryViewModel = inventoryViewModel,
                            onDismiss = {
                                detectedBank = null
                                scope.launch {
                                    bankCooldown = true
                                    kotlinx.coroutines.delay(5000)
                                    bankCooldown = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── ANALIZADOR DE FRAMES COMBINADO (QR + TEXTO) ───────────────────────────────

@androidx.annotation.OptIn(ExperimentalGetImage::class)
// Ejecuta la lógica de analyze frame.
private fun analyzeFrame(
    proxy: ImageProxy,
    onQrFound: (String) -> Unit,
    onSupermarketFound: (String) -> Unit,
    onBankFound: (String) -> Unit
) {
    val mediaImage = proxy.image ?: run {
        proxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)

    var pending = 2
    // Ejecuta la lógica de try close.
    fun tryClose() {
        if (--pending <= 0) proxy.close()
    }

    // 1. Detección de QR ───────────────────────────────────────────────────────
    BarcodeScanning.getClient()
        .process(image)
        .addOnSuccessListener { barcodes ->
            for (b in barcodes) {
                if (b.valueType == Barcode.TYPE_TEXT) {
                    onQrFound(b.rawValue ?: "")
                    break
                }
            }
        }
        .addOnCompleteListener { tryClose() }

    // 2. Reconocimiento de texto (supermercados y bancos) ─────────────────────
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { visionText ->
            val fullText = visionText.text.lowercase()

            for ((keyword, displayName) in SUPERMARKET_KEYWORDS) {
                val pattern = Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(fullText)) {
                    onSupermarketFound(displayName)
                    return@addOnSuccessListener
                }
            }

            for ((keyword, displayName) in BANK_KEYWORDS) {
                val pattern = Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(fullText)) {
                    onBankFound(displayName)
                    return@addOnSuccessListener
                }
            }
        }
        .addOnCompleteListener { tryClose() }
}

// ── PARSER DE QR → ITEM ───────────────────────────────────────────────────────

private fun parseQrToItem(content: String): Item? {
    return try {
        when {
            content.uppercase().startsWith("AIDO:") -> {
                val body = content.removePrefix("AIDO:").removePrefix("aido:")
                val parts = body.split("|")
                if (parts.size < 2) return null

                val type = normalizeItemType(parts.getOrElse(0) { "consumible" })
                val name = parts.getOrElse(1) { "Objeto misterioso" }
                val desc = parts.getOrElse(2) { "" }
                val effect = parts.getOrElse(3) { defaultEffectForType(type) }

                Item(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    description = desc,
                    type = type,
                    effect = effect
                )
            }

            content.trimStart().startsWith("{") -> {
                val name = content.substringAfter("\"name\":\"").substringBefore("\"").trim()
                val type = normalizeItemType(
                    content.substringAfter("\"type\":\"").substringBefore("\"").trim()
                )
                val desc = content.substringAfter("\"description\":\"").substringBefore("\"").trim()
                val eff = content.substringAfter("\"effect\":\"").substringBefore("\"").trim()

                if (name.isBlank()) {
                    null
                } else {
                    Item(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        description = desc,
                        type = type,
                        effect = eff.ifBlank { defaultEffectForType(type) }
                    )
                }
            }

            content.isNotBlank() -> Item(
                id = System.currentTimeMillis().toString(),
                name = content.take(60),
                description = "Objeto encontrado en la aventura",
                type = "consumible",
                effect = ""
            )

            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

// Ejecuta la lógica de normalize item type.
private fun normalizeItemType(raw: String): String = when (raw.lowercase().trim()) {
    "pocion", "poción", "potion", "bebida", "elixir" -> "pocion"
    "arma", "weapon", "espada", "hacha", "daga", "arco", "lanza" -> "arma"
    "armadura", "armor", "escudo", "casco", "yelmo", "peto" -> "armadura"
    "pergamino", "scroll", "hechizo", "magia" -> "pergamino"
    "veneno", "poison" -> "veneno"
    "granada", "explosivo", "bomba" -> "explosivo"
    "reliquia", "artefacto", "artifact" -> "reliquia"
    "comida", "ración", "food" -> "consumible"
    else -> "consumible"
}

// Ejecuta la lógica de default effect for type.
private fun defaultEffectForType(type: String): String = when (type) {
    "pocion" -> "cura:1d8+2"
    "arma" -> "daño:1d6"
    "armadura" -> "+2 CA"
    "pergamino" -> "daño:2d6"
    "veneno" -> "veneno:1d4"
    "explosivo" -> "daño:2d8"
    else -> ""
}

// ── DIÁLOGO DE BOTÍN ──────────────────────────────────────────────────────────

@Composable
// Ejecuta la lógica de botin encontrado dialog.
fun BotinEncontradoDialog(
    item: Item,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val typeEmoji = when (item.type) {
        "pocion" -> "🧪"
        "arma" -> "⚔️"
        "armadura" -> "🛡️"
        "pergamino" -> "📜"
        "veneno" -> "☠️"
        "explosivo" -> "💣"
        "reliquia" -> "✨"
        else -> "🎒"
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
            Text(
                "¡BOTÍN ENCONTRADO!",
                color = Color(0xFFFFD700),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(typeEmoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            InventoryItemRow(item)
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onCancel, enabled = !isSaving) {
                    Text("Desechar", color = Color.Red)
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black
                        )
                    } else {
                        Text("Guardar en mochila", color = Color.Black)
                    }
                }
            }
        }
    }
}
