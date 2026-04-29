package com.example.aidungeonmaster.ui.game

import com.example.aidungeonmaster.ui.i18n.Text

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aidungeonmaster.R
@Composable
fun MedievalBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2B2B2B))) {
        // IMAGEN DE FONDO (Requiere que añadas una imagen de pergamino a res/drawable)
        // Si no tienes, usa un color sólido (Color(0xFFEEDCBB))
        Image(
            painter = painterResource(id = R.drawable.background_parchment), // Necesitas añadir parch_bg.jpg
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.3f),
            contentScale = ContentScale.Crop
        )
        content()
    }
}

// Componente para Títulos con estética de D&D
@Composable
fun MedievalTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(
            color = Color(0xFFFFD700), // Dorado
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif, // Usa una fuente medieval si la tienes cargada
            letterSpacing = 2.sp
        )
    )
}