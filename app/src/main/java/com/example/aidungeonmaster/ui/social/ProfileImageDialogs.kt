package com.example.aidungeonmaster.ui.social

import com.example.aidungeonmaster.ui.i18n.Text
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProfilePhotoPreviewDialog(
    photoUrl: String,
    displayName: String,
    accentColor: Color,
    backgroundColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text(displayName) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(accentColor, backgroundColor, accentColor)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(4.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                SocialUserAvatar(
                    photoUrl = photoUrl,
                    displayName = displayName,
                    size = 260.dp,
                    accent = accentColor,
                    modifier = Modifier.clip(RoundedCornerShape(22.dp))
                )
            }
        }
    )
}

@Composable
fun ProfileImageCropperDialog(
    sourceBitmap: Bitmap,
    onCancel: () -> Unit,
    onCropConfirmed: (String) -> Unit
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var cropBoxSize by remember { mutableStateOf(IntSize.Zero) }
    val cropBoxDp = 300.dp
    val density = LocalDensity.current

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(
                onClick = {
                    val cropPx = if (cropBoxSize.width > 0) {
                        cropBoxSize.width
                    } else {
                        with(density) { cropBoxDp.roundToPx() }
                    }

                    val cropped = cropVisibleSquare(
                        source = sourceBitmap,
                        cropBoxPx = cropPx,
                        zoom = zoom,
                        offset = offset
                    )

                    onCropConfirmed(bitmapToJpegDataUrl(cropped))
                }
            ) {
                Text("Usar esta foto")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
        title = { Text("Ajustar foto de perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Arrastra la imagen y pellizca para ampliar. El círculo marca la parte que se verá en tu perfil.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Box(
                    modifier = Modifier
                        .size(cropBoxDp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Black)
                        .onSizeChanged { cropBoxSize = it }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = "Foto seleccionada",
                        modifier = Modifier
                            .size(cropBoxDp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .size(cropBoxDp)
                            .padding(18.dp)
                            .border(3.dp, Color.White, CircleShape)
                    )
                }

                Column {
                    Text("Zoom", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it.coerceIn(1f, 4f) },
                        valueRange = 1f..4f
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            offset = Offset.Zero
                            zoom = 1f
                        }
                    ) {
                        Text("Recentrar")
                    }

                    Spacer(Modifier.weight(1f))
                }
            }
        }
    )
}

fun decodeBitmapFromUri(context: Context, uri: android.net.Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
    }
}

private fun cropVisibleSquare(
    source: Bitmap,
    cropBoxPx: Int,
    zoom: Float,
    offset: Offset
): Bitmap {
    val safeCropBoxPx = cropBoxPx.coerceAtLeast(1)

    val baseScale = max(
        safeCropBoxPx.toFloat() / source.width.toFloat(),
        safeCropBoxPx.toFloat() / source.height.toFloat()
    )

    val totalScale = baseScale * zoom.coerceAtLeast(1f)

    val renderedWidth = source.width * totalScale
    val renderedHeight = source.height * totalScale

    val leftInBox = (safeCropBoxPx - renderedWidth) / 2f + offset.x
    val topInBox = (safeCropBoxPx - renderedHeight) / 2f + offset.y

    val srcLeft = ((0f - leftInBox) / totalScale)
        .toInt()
        .coerceIn(0, source.width - 1)

    val srcTop = ((0f - topInBox) / totalScale)
        .toInt()
        .coerceIn(0, source.height - 1)

    val srcSize = min(
        min(
            (safeCropBoxPx / totalScale).toInt().coerceAtLeast(1),
            source.width - srcLeft
        ),
        source.height - srcTop
    ).coerceAtLeast(1)

    val square = Bitmap.createBitmap(source, srcLeft, srcTop, srcSize, srcSize)

    return Bitmap.createScaledBitmap(square, 512, 512, true)
}

private fun bitmapToJpegDataUrl(bitmap: Bitmap): String {
    var quality = 88
    var bytes: ByteArray

    do {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        bytes = stream.toByteArray()
        quality -= 8
    } while (bytes.size > 350_000 && quality >= 48)

    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

    return "data:image/jpeg;base64,$base64"
}