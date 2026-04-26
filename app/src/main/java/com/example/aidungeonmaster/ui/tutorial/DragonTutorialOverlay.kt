package com.example.aidungeonmaster.ui.tutorial

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class TutorialStep(
    val targetKey: String,
    val title: String,
    val description: String,
    @DrawableRes val mascotRes: Int
)

fun Modifier.tutorialAnchor(
    key: String,
    targets: SnapshotStateMap<String, Rect>
): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        targets[key] = coordinates.boundsInRoot()
    }
)

@Composable
fun DragonTutorialOverlay(
    visible: Boolean,
    steps: List<TutorialStep>,
    currentStepIndex: Int,
    targets: Map<String, Rect>,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    if (!visible || steps.isEmpty()) return

    val safeIndex = currentStepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[safeIndex]
    val rawTarget = targets[step.targetKey]

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val hasTarget = rawTarget != null && rawTarget.width > 0f && rawTarget.height > 0f

        val fallbackRect = Rect(
            left = screenWidthPx * 0.35f,
            top = screenHeightPx * 0.25f,
            right = screenWidthPx * 0.65f,
            bottom = screenHeightPx * 0.35f
        )

        val baseTargetRect = rawTarget ?: fallbackRect

        val correctionYPx = with(density) { 30.dp.toPx() }

        val targetRect = Rect(
            left = baseTargetRect.left,
            top = (baseTargetRect.top - correctionYPx).coerceAtLeast(0f),
            right = baseTargetRect.right,
            bottom = (baseTargetRect.bottom - correctionYPx).coerceAtLeast(0f)
        )

        val highlightPaddingPx = with(density) { 6.dp.toPx() }
        val screenMarginPx = with(density) { 14.dp.toPx() }

        val isSocialStep = step.targetKey.startsWith("social_")
        val isTargetUpperHalf = targetRect.center.y < screenHeightPx / 2f
        val isTargetOnLeft = targetRect.center.x < screenWidthPx * 0.5f
        val isTargetNearBottom = targetRect.center.y > screenHeightPx * 0.66f

        val cardAlignment = when {
            isSocialStep -> Alignment.TopCenter
            isTargetUpperHalf -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }

        val cardPaddingTop = when {
            isSocialStep -> 18.dp
            isTargetUpperHalf -> 0.dp
            else -> 70.dp
        }

        val cardPaddingBottom = when {
            isSocialStep -> 0.dp
            isTargetUpperHalf -> 32.dp
            else -> 0.dp
        }

        val mascotSizeDp = 220.dp
        val mascotSizePx = with(density) { mascotSizeDp.toPx() }
        val gapPx = with(density) { 22.dp.toPx() }

        val forceAboveTarget = step.targetKey == "btn_social" ||
                step.targetKey == "btn_create_character" ||
                isSocialStep ||
                isTargetNearBottom

        val mascotX = when {
            step.targetKey == "btn_social" -> {
                // Botón abajo izquierda: dragón a la derecha del botón.
                targetRect.right + gapPx
            }

            step.targetKey == "btn_create_character" -> {
                // Botón abajo derecha: dragón a la izquierda del botón.
                targetRect.left - mascotSizePx - gapPx
            }

            isSocialStep -> {
                // Opciones del panel social: dragón a la derecha si puede.
                targetRect.right + gapPx
            }

            isTargetOnLeft -> {
                targetRect.right + gapPx
            }

            else -> {
                targetRect.left - mascotSizePx - gapPx
            }
        }.coerceIn(
            screenMarginPx,
            screenWidthPx - mascotSizePx - screenMarginPx
        )

        val mascotY = when {
            forceAboveTarget -> {
                targetRect.top - mascotSizePx - gapPx
            }

            isTargetUpperHalf -> {
                targetRect.bottom + gapPx
            }

            else -> {
                targetRect.top - mascotSizePx - gapPx
            }
        }.coerceIn(
            screenMarginPx + with(density) { 70.dp.toPx() },
            screenHeightPx - mascotSizePx - screenMarginPx
        )

        val mirrorDragon = isTargetOnLeft

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            drawRect(Color.Black.copy(alpha = 0.46f))

            if (hasTarget) {
                val left = targetRect.left - highlightPaddingPx
                val top = targetRect.top - highlightPaddingPx
                val width = targetRect.width + highlightPaddingPx * 2f
                val height = targetRect.height + highlightPaddingPx * 2f
                val radius = CornerRadius(22.dp.toPx(), 22.dp.toPx())

                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = radius,
                    blendMode = BlendMode.Clear
                )

                drawRoundRect(
                    color = Color(0x22FFD54F),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = radius
                )

                drawRoundRect(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        Image(
            painter = painterResource(id = step.mascotRes),
            contentDescription = "Mascota tutorial",
            modifier = Modifier
                .size(mascotSizeDp)
                .graphicsLayer {
                    scaleX = if (mirrorDragon) -1f else 1f
                }
                .offset {
                    IntOffset(
                        x = mascotX.roundToInt(),
                        y = mascotY.roundToInt()
                    )
                }
        )

        Surface(
            modifier = Modifier
                .align(cardAlignment)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = cardPaddingTop,
                    bottom = cardPaddingBottom
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2A120B).copy(alpha = 0.97f),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Tutorial ${safeIndex + 1}/${steps.size}",
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = step.title,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = step.description,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Saltar", color = Color(0xFFFFD54F))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (safeIndex > 0) {
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text("Atrás")
                            }
                        }

                        Button(
                            onClick = {
                                if (safeIndex == steps.lastIndex) {
                                    onFinish()
                                } else {
                                    onNext()
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD54F),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(if (safeIndex == steps.lastIndex) "Entendido" else "Siguiente")
                        }
                    }
                }
            }
        }
    }
}