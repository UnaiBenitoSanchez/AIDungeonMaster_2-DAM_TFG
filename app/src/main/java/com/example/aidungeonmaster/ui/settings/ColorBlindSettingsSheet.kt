package com.example.aidungeonmaster.ui.settings

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidungeonmaster.ui.theme.ColorBlindType
import com.example.aidungeonmaster.ui.theme.colorMatrixForType

/**
 * Bottom Sheet que permite al usuario seleccionar su tipo de daltonismo.
 *
 * NOTA: ModalBottomSheet renderiza en su propia ventana de sistema, por lo que
 * el graphicsLayer global de MainActivity NO se aplica aquí. Eso nos permite
 * aplicar la matriz del tipo *pendiente* directamente sobre el preview para que
 * el usuario vea exactamente cómo quedará la paleta de colores.
 *
 * @param currentType    Tipo actualmente activo en la app.
 * @param onTypeSelected Callback invocado al confirmar la selección.
 * @param onDismiss      Callback invocado al cerrar sin confirmar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de color blind settings sheet.
fun ColorBlindSettingsSheet(
    currentType: ColorBlindType,
    onTypeSelected: (ColorBlindType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingType by remember { mutableStateOf(currentType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Cabecera ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Modo daltónico",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecciona tu tipo de daltonismo para aplicar un filtro de color en toda la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Paleta de vista previa ────────────────────────────────────────
            // FIX: se pasa pendingType para que la fila de colores aplique
            // la matriz correspondiente y el usuario vea el cambio en tiempo real.
            ColorPreviewRow(type = pendingType)

            Spacer(Modifier.height(4.dp))

            // ── Opciones ─────────────────────────────────────────────────────
            ColorBlindType.entries.forEach { type ->
                ColorBlindOptionItem(
                    type       = type,
                    isSelected = pendingType == type,
                    onClick    = { pendingType = type }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Botones de acción ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onTypeSelected(pendingType)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Aplicar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Vista previa de colores ─────────────────────────────────────────────────
//
// FIX: La Row de colores ahora lleva su propio graphicsLayer con la matriz
// del tipo pendiente. Como ModalBottomSheet corre en una ventana propia
// (separada del graphicsLayer raíz de MainActivity), esto funciona
// directamente sin interferencias.

@Composable
// Ejecuta la lógica de color preview row.
private fun ColorPreviewRow(type: ColorBlindType) {
    val sampleColors = listOf(
        Color(0xFFE53935), // rojo
        Color(0xFF43A047), // verde
        Color(0xFF1E88E5), // azul
        Color(0xFFFFD600), // amarillo
        Color(0xFFFF6F00), // naranja
        Color(0xFFAB47BC), // morado
    )

    // Obtenemos la matriz del tipo pendiente (null = sin filtro)
    val matrix = remember(type) { colorMatrixForType(type) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Vista previa de colores",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))

            // FIX: aplicamos la matriz del tipo seleccionado a esta Row.
            // Al estar en una ventana separada, el filtro refleja exactamente
            // cómo percibirá el usuario daltónico esos colores.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (matrix != null) {
                            Modifier.graphicsLayer {
                                colorFilter = ColorFilter.colorMatrix(matrix)
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                sampleColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = when (type) {
                    ColorBlindType.NONE          -> "Los colores se muestran tal como son"
                    ColorBlindType.PROTANOPIA    -> "Filtro activo: sin distinción de rojos"
                    ColorBlindType.DEUTERANOPIA  -> "Filtro activo: sin distinción de verdes"
                    ColorBlindType.TRITANOPIA    -> "Filtro activo: sin distinción de azules"
                    ColorBlindType.ACHROMATOPSIA -> "Filtro activo: escala de grises"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Elemento de opción individual ──────────────────────────────────────────

@Composable
// Ejecuta la lógica de color blind option item.
private fun ColorBlindOptionItem(
    type: ColorBlindType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "borderColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else Color.Transparent,
        animationSpec = tween(200),
        label = "bgColor"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
