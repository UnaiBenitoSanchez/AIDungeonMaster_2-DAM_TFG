package com.example.aidungeonmaster.ui.game

import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aidungeonmaster.data.model.Item
import com.example.aidungeonmaster.viewmodel.InventoryViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    gameId: String,
    onBack: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current // Corregido
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedItem by remember { mutableStateOf<Item?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(gameId) {
        inventoryViewModel.loadInventory(gameId)
    }

    MedievalBackground {
        Scaffold(
            topBar = {
                TopAppBar( // Usando TopAppBar estándar de M3
                    title = { MedievalTitle("ESCANEAR BOTÍN") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                if (!hasCameraPermission) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Se requiere permiso de cámara", color = Color.Red)
                    }
                } else if (detectedItem == null) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                    ) {
                        AndroidView(
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

                                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                        processImageProxy(imageProxy) { qrContent ->
                                            if (detectedItem == null) {
                                                val item = parseQrToItem(qrContent)
                                                if (item != null) {
                                                    detectedItem = item
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    BotinEncontradoDialog(
                        item = detectedItem!!,
                        isSaving = isSaving,
                        onCancel = { detectedItem = null },
                        onConfirm = {
                            // Usamos el scope que declaramos arriba
                            scope.launch {
                                isSaving = true
                                inventoryViewModel.addItemToInventory(gameId, detectedItem!!)

                                // Esperamos para asegurar la escritura y dar feedback
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

// Lógica de ML Kit para procesar la imagen de la cámara
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(imageProxy: ImageProxy, onQrFound: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT -> {
                            onQrFound(barcode.rawValue ?: "")
                            return@addOnSuccessListener
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

// Lógica para convertir el texto crudo del QR en un Objeto
private fun parseQrToItem(content: String): Item? {
    return try {
        // Opción 1: El QR contiene un JSON rústico (Ej: {"name":"Pocion", "type":"pocion"})
        // Usa Gson si lo prefieres, aquí lo parseamos manual por simplicidad
        if (content.startsWith("{") && content.endsWith("}")) {
            val name = content.substringAfter("\"name\":\"").substringBefore("\"")
            val type = content.substringAfter("\"type\":\"").substringBefore("\"")
            val effect = content.substringAfter("\"effect\":\"").substringBefore("\"")
            if (name.isNotBlank()) Item(name = name, type = type, effect = effect) else null
        } else if (content.startsWith("AIDO:")) {
            // Opción 2: Formato propio (Ej: AIDO:pocion|Pocion de Curación|Cura 2d4)
            val parts = content.removePrefix("AIDO:").split("|")
            Item(type = parts[0], name = parts[1], effect = parts[2])
        } else {
            // Opción 3: Solo texto (lo convertimos en consumible básico)
            Item(name = content, type = "consumible")
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun BotinEncontradoDialog(item: Item, isSaving: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA000000))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("¡BOTÍN ENCONTRADO!", color = Color(0xFFFFD700), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            InventoryItemRow(item) // Reutilizamos la vista de la mochila
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = onCancel, enabled = !isSaving) {
                    Text("Desechar", color = Color.Red)
                }
                Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)), enabled = !isSaving) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text("Guardar", color = Color.Black)
                    }
                }
            }
        }
    }
}