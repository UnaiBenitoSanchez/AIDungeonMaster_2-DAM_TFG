package com.example.aidungeonmaster.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ColorMatrix

// ─── Tipos de daltonismo soportados ─────────────────────────────────────────

enum class ColorBlindType(
    val displayName: String,
    val description: String
) {
    NONE(
        displayName  = "Sin filtro",
        description  = "Visión de color estándar"
    ),
    PROTANOPIA(
        displayName  = "Protanopía",
        description  = "Dificultad para distinguir tonos rojos"
    ),
    DEUTERANOPIA(
        displayName  = "Deuteranopía",
        description  = "Dificultad para distinguir tonos verdes"
    ),
    TRITANOPIA(
        displayName  = "Tritanopía",
        description  = "Dificultad para distinguir tonos azules"
    ),
    ACHROMATOPSIA(
        displayName  = "Acromatopsia",
        description  = "Visión en escala de grises (monocromática)"
    )
}

// ─── Matrices de simulación de daltonismo ────────────────────────────────────
// Basadas en los modelos de Brettel, Viénot & Mollon (1997) simplificados.
// Las matrices transforman el espacio sRGB para simular cómo percibe los
// colores una persona con cada tipo de daltonismo, de modo que los colores
// elegidos por el diseñador resulten distinguibles para ese usuario.

fun colorMatrixForType(type: ColorBlindType): ColorMatrix? = when (type) {

    ColorBlindType.NONE -> null

    // Protanopía: insensibilidad al rojo (conos L ausentes)
    ColorBlindType.PROTANOPIA -> ColorMatrix(
        floatArrayOf(
            0.567f,  0.433f,  0.000f,  0f, 0f,
            0.558f,  0.442f,  0.000f,  0f, 0f,
            0.000f,  0.242f,  0.758f,  0f, 0f,
            0.000f,  0.000f,  0.000f,  1f, 0f
        )
    )

    // Deuteranopía: insensibilidad al verde (conos M ausentes)
    ColorBlindType.DEUTERANOPIA -> ColorMatrix(
        floatArrayOf(
            0.625f,  0.375f,  0.000f,  0f, 0f,
            0.700f,  0.300f,  0.000f,  0f, 0f,
            0.000f,  0.300f,  0.700f,  0f, 0f,
            0.000f,  0.000f,  0.000f,  1f, 0f
        )
    )

    // Tritanopía: insensibilidad al azul (conos S ausentes)
    ColorBlindType.TRITANOPIA -> ColorMatrix(
        floatArrayOf(
            0.950f,  0.050f,  0.000f,  0f, 0f,
            0.000f,  0.433f,  0.567f,  0f, 0f,
            0.000f,  0.475f,  0.525f,  0f, 0f,
            0.000f,  0.000f,  0.000f,  1f, 0f
        )
    )

    // Acromatopsia: visión monocromática (luminancia estándar ITU-R BT.601)
    ColorBlindType.ACHROMATOPSIA -> ColorMatrix(
        floatArrayOf(
            0.299f,  0.587f,  0.114f,  0f, 0f,
            0.299f,  0.587f,  0.114f,  0f, 0f,
            0.299f,  0.587f,  0.114f,  0f, 0f,
            0.000f,  0.000f,  0.000f,  1f, 0f
        )
    )
}

// ─── CompositionLocal ────────────────────────────────────────────────────────
// Permite que cualquier Composable de la jerarquía consulte el tipo activo
// sin necesidad de pasar el parámetro a mano por cada nivel.

val LocalColorBlindType = staticCompositionLocalOf<ColorBlindType> {
    ColorBlindType.NONE
}
