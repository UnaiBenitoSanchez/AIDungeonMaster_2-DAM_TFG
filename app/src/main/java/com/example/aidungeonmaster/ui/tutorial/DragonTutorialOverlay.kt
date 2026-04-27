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

        fun safeCoerce(value: Float, min: Float, max: Float): Float {
            return if (max < min) min else value.coerceIn(min, max)
        }

        val isWelcomeStep = step.targetKey == "welcome"
        val hasTarget = !isWelcomeStep && rawTarget != null && rawTarget.width > 0f && rawTarget.height > 0f

        val fallbackRect = if (isWelcomeStep) {
            Rect(
                left = screenWidthPx * 0.25f,
                top = screenHeightPx * 0.42f,
                right = screenWidthPx * 0.75f,
                bottom = screenHeightPx * 0.58f
            )
        } else {
            Rect(
                left = screenWidthPx * 0.35f,
                top = screenHeightPx * 0.25f,
                right = screenWidthPx * 0.65f,
                bottom = screenHeightPx * 0.35f
            )
        }

        val baseTargetRect = rawTarget ?: fallbackRect

        /*
         * Ajuste vertical del foco.
         * Sube o baja el recuadro amarillo.
         * Si lo ves un poco bajo: sube a 12.dp o 14.dp.
         * Si lo ves alto: baja a 6.dp.
         */
        val correctionYPx = with(density) { 30.dp.toPx() }

        val targetRect = Rect(
            left = baseTargetRect.left,
            top = (baseTargetRect.top - correctionYPx).coerceAtLeast(0f),
            right = baseTargetRect.right,
            bottom = (baseTargetRect.bottom - correctionYPx).coerceAtLeast(0f)
        )

        /*
         * Tamaño y margen del recuadro amarillo.
         */
        val highlightPaddingPx = with(density) { 4.dp.toPx() }
        val highlightRadiusPx = with(density) { 14.dp.toPx() }
        val highlightStrokePx = with(density) { 2.5.dp.toPx() }

        /*
         * Tamaño de la mascota.
         * En móviles normales se verá más grande, pero sin salirse.
         */
        val mascotSizeDp = when {
            maxWidth < 360.dp -> 150.dp
            maxWidth < 420.dp -> 180.dp
            else -> 200.dp
        }

        val mascotSizePx = with(density) { mascotSizeDp.toPx() }

        /*
         * Margen visual real. Antes estaba en 12.dp y para imágenes grandes
         * quedaba demasiado pegada al borde. Con 34.dp no se corta.
         */
        val screenMarginPx = with(density) { 34.dp.toPx() }
        val gapPx = with(density) { 24.dp.toPx() }

        val isSocialStep = step.targetKey.startsWith("social_")
        val isTargetOnLeft = targetRect.center.x < screenWidthPx * 0.5f
        val isTargetOnRight = targetRect.center.x >= screenWidthPx * 0.5f
        val isTargetUpperHalf = targetRect.center.y < screenHeightPx * 0.5f
        val isTargetNearBottom = targetRect.center.y > screenHeightPx * 0.68f
        val isTopBarTarget = targetRect.top < with(density) { 90.dp.toPx() }

        val cardAlignment = when {
            isWelcomeStep -> Alignment.TopCenter
            isSocialStep -> Alignment.TopCenter
            isTargetUpperHalf -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }

        val cardPaddingTop = when {
            isWelcomeStep -> 48.dp
            isSocialStep -> 14.dp
            isTargetUpperHalf -> 0.dp
            else -> 52.dp
        }

        val cardPaddingBottom = when {
            isSocialStep -> 0.dp
            isTargetUpperHalf -> 24.dp
            else -> 0.dp
        }

        val cardApproxHeightPx = with(density) { 210.dp.toPx() }
        val cardTopPx = when (cardAlignment) {
            Alignment.TopCenter -> with(density) { cardPaddingTop.toPx() }
            else -> screenHeightPx - cardApproxHeightPx - with(density) { cardPaddingBottom.toPx() }
        }
        val cardBottomPx = cardTopPx + cardApproxHeightPx

        /*
         * X de la mascota.
         *
         * Cambio importante:
         * - Si el objetivo está a la derecha, NO la pegamos al objetivo.
         *   La mandamos a una zona segura izquierda con margen amplio.
         * - Si el objetivo está a la izquierda, la mandamos a una zona segura derecha.
         */
        val preferredMascotX = when {
            step.targetKey == "welcome" -> {
                (screenWidthPx - mascotSizePx) / 2f
            }

            step.targetKey == "btn_social" -> {
                screenWidthPx - mascotSizePx - screenMarginPx
            }

            step.targetKey == "btn_create_character" -> {
                screenMarginPx
            }

            step.targetKey == "btn_characters_title" -> {
                screenWidthPx - mascotSizePx - screenMarginPx
            }

            isSocialStep -> {
                screenWidthPx - mascotSizePx - screenMarginPx
            }

            isTargetOnRight -> {
                screenMarginPx
            }

            else -> {
                screenWidthPx - mascotSizePx - screenMarginPx
            }
        }

        val mascotX = safeCoerce(
            value = preferredMascotX,
            min = screenMarginPx,
            max = screenWidthPx - mascotSizePx - screenMarginPx
        )

        /*
         * Y de la mascota.
         *
         * Si el objetivo está en la barra superior, la bajamos debajo de la barra.
         * Si está abajo, la subimos.
         */
        val preferredMascotY = when {
            step.targetKey == "welcome" -> {
                screenHeightPx * 0.44f
            }

            step.targetKey == "btn_social" -> {
                targetRect.top - mascotSizePx - gapPx
            }

            step.targetKey == "btn_create_character" -> {
                targetRect.top - mascotSizePx - gapPx
            }

            isSocialStep -> {
                targetRect.top - mascotSizePx - gapPx
            }

            isTargetNearBottom -> {
                targetRect.top - mascotSizePx - gapPx
            }

            isTopBarTarget -> {
                targetRect.bottom + gapPx
            }

            isTargetUpperHalf -> {
                targetRect.bottom + gapPx
            }

            else -> {
                targetRect.top - mascotSizePx - gapPx
            }
        }

        val minMascotYPx = when (cardAlignment) {
            Alignment.TopCenter -> cardBottomPx + with(density) { 8.dp.toPx() }
            else -> screenMarginPx + with(density) { 52.dp.toPx() }
        }

        val maxMascotYPx = when (cardAlignment) {
            Alignment.BottomCenter -> cardTopPx - mascotSizePx - with(density) { 8.dp.toPx() }
            else -> screenHeightPx - mascotSizePx - screenMarginPx
        }

        val fallbackMascotY = when {
            isTargetUpperHalf -> targetRect.bottom + gapPx
            else -> targetRect.top - mascotSizePx - gapPx
        }

        val mascotY = if (maxMascotYPx < minMascotYPx) {
            safeCoerce(
                value = fallbackMascotY,
                min = screenMarginPx + with(density) { 52.dp.toPx() },
                max = screenHeightPx - mascotSizePx - screenMarginPx
            )
        } else {
            safeCoerce(
                value = preferredMascotY,
                min = minMascotYPx,
                max = maxMascotYPx
            )
        }

        /*
         * Espejo:
         * Si el objetivo está a la derecha, se invierte.
         * Si está a la izquierda, se queda normal.
         */
        val mirrorMascot = when {
            step.targetKey == "welcome" -> false
            step.targetKey == "btn_social" -> false
            step.targetKey == "btn_create_character" -> true
            step.targetKey == "btn_characters_title" -> false
            isSocialStep -> false
            else -> isTargetOnRight
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            drawRect(Color.Black.copy(alpha = 0.42f))

            if (hasTarget) {
                val left = targetRect.left - highlightPaddingPx
                val top = targetRect.top - highlightPaddingPx
                val width = targetRect.width + highlightPaddingPx * 2f
                val height = targetRect.height + highlightPaddingPx * 2f
                val radius = CornerRadius(highlightRadiusPx, highlightRadiusPx)

                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = radius,
                    blendMode = BlendMode.Clear
                )

                drawRoundRect(
                    color = Color(0x1CFFD54F),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = radius
                )

                drawRoundRect(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = radius,
                    style = Stroke(width = highlightStrokePx)
                )
            }
        }

        Image(
            painter = painterResource(id = step.mascotRes),
            contentDescription = "Mascota tutorial",
            modifier = Modifier
                .size(mascotSizeDp)
                .graphicsLayer {
                    scaleX = if (mirrorMascot) -1f else 1f
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
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF2A120B).copy(alpha = 0.97f),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Text(
                                if (safeIndex == steps.lastIndex) {
                                    "Entendido"
                                } else {
                                    "Siguiente"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}