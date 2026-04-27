package com.example.aidungeonmaster.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import com.example.aidungeonmaster.data.model.Character
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

object CharacterSheetPdfExporter {

    fun exportCharacterSheet(
        context: Context,
        uri: Uri,
        character: Character
    ): Result<Unit> {
        return runCatching {
            val document = PdfDocument()

            val pageWidth = 595
            val pageHeight = 842

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawSheet(canvas, pageWidth, pageHeight, character)

            document.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { output ->
                document.writeTo(output)
            } ?: error("No se pudo abrir el destino del PDF.")

            document.close()
        }
    }

    private fun drawSheet(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        character: Character
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val parchment = Color.rgb(245, 232, 196)
        val parchmentLight = Color.rgb(250, 240, 210)
        val darkBrown = Color.rgb(54, 32, 18)
        val mediumBrown = Color.rgb(111, 72, 42)
        val gold = Color.rgb(171, 124, 35)
        val red = Color.rgb(105, 30, 25)
        val modifierRed = Color.rgb(170, 70, 45)

        val contentOffsetY = 12f

        canvas.drawColor(parchment)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = darkBrown
        canvas.drawRoundRect(
            RectF(24f, 24f, pageWidth - 24f, pageHeight - 24f),
            18f,
            18f,
            paint
        )

        paint.strokeWidth = 2f
        paint.color = gold
        canvas.drawRoundRect(
            RectF(34f, 34f, pageWidth - 34f, pageHeight - 34f),
            14f,
            14f,
            paint
        )

        drawTitle(canvas, "FICHA DE AVENTURERO", pageWidth / 2f, 62f + contentOffsetY, darkBrown)
        drawSubtitle(canvas, character.name, pageWidth / 2f, 80f + contentOffsetY, mediumBrown)
        drawSubtitle(canvas, "AI Dungeon Master", pageWidth / 2f, 96f + contentOffsetY, mediumBrown)

        val sectionX = 48f
        val sectionW = 499f

        // ─────────────────────────────
        // IDENTIDAD
        // ─────────────────────────────

        drawSectionFrame(
            canvas = canvas,
            x = sectionX,
            y = 118f + contentOffsetY,
            w = sectionW,
            h = 174f,
            title = "IDENTIDAD",
            borderColor = darkBrown,
            accentColor = gold
        )

        drawPortrait(
            canvas = canvas,
            character = character,
            x = 66f,
            y = 150f + contentOffsetY,
            w = 124f,
            h = 124f,
            borderColor = darkBrown
        )

        drawLabelValue(
            canvas = canvas,
            label = "Nombre",
            value = character.name,
            x = 212f,
            y = 150f + contentOffsetY,
            lineWidth = 150f,
            color = darkBrown
        )

        drawLabelValue(
            canvas = canvas,
            label = "Raza",
            value = character.race,
            x = 212f,
            y = 202f + contentOffsetY,
            lineWidth = 135f,
            color = darkBrown
        )

        drawLabelValue(
            canvas = canvas,
            label = "Clase",
            value = character.characterClass,
            x = 380f,
            y = 202f + contentOffsetY,
            lineWidth = 130f,
            color = darkBrown
        )

        drawLabelValue(
            canvas = canvas,
            label = "Tema / mundo",
            value = character.gameTheme.orEmpty().ifBlank { "Sin mundo asignado" },
            x = 212f,
            y = 250f + contentOffsetY,
            lineWidth = 298f,
            color = darkBrown
        )

        // ─────────────────────────────
        // PROGRESO
        // ─────────────────────────────

        drawSectionFrame(
            canvas = canvas,
            x = sectionX,
            y = 314f + contentOffsetY,
            w = sectionW,
            h = 124f,
            title = "PROGRESO",
            borderColor = darkBrown,
            accentColor = gold
        )

        val progressBoxW = 126f
        val progressBoxH = 58f
        val progressGap = 28f
        val progressTotalW = progressBoxW * 3f + progressGap * 2f
        val progressStartX = sectionX + (sectionW - progressTotalW) / 2f
        val progressY = 348f + contentOffsetY

        drawBigStat(
            canvas = canvas,
            label = "NIVEL",
            value = character.level.toString(),
            x = progressStartX,
            y = progressY,
            w = progressBoxW,
            h = progressBoxH,
            borderColor = darkBrown,
            accentColor = red
        )

        drawBigStat(
            canvas = canvas,
            label = "XP",
            value = "${character.xp}/${character.xpToNextLevel}",
            x = progressStartX + progressBoxW + progressGap,
            y = progressY,
            w = progressBoxW,
            h = progressBoxH,
            borderColor = darkBrown,
            accentColor = red
        )

        drawBigStat(
            canvas = canvas,
            label = "VIDA",
            value = "${character.hpCurrent}/${character.hpMax}",
            x = progressStartX + (progressBoxW + progressGap) * 2f,
            y = progressY,
            w = progressBoxW,
            h = progressBoxH,
            borderColor = darkBrown,
            accentColor = red
        )

        drawProgressBar(
            canvas = canvas,
            x = progressStartX,
            y = 418f + contentOffsetY,
            w = progressTotalW,
            h = 7f,
            progress = if (character.xpToNextLevel > 0) {
                character.xp.toFloat() / character.xpToNextLevel.toFloat()
            } else {
                0f
            },
            fillColor = red,
            trackColor = Color.rgb(206, 174, 95)
        )

        // ─────────────────────────────
        // ATRIBUTOS
        // ─────────────────────────────

        drawSectionFrame(
            canvas = canvas,
            x = sectionX,
            y = 462f + contentOffsetY,
            w = sectionW,
            h = 206f,
            title = "ATRIBUTOS",
            borderColor = darkBrown,
            accentColor = gold
        )

        val stats = listOf(
            "Fuerza" to character.strTotal,
            "Destreza" to character.dexTotal,
            "Constitución" to character.conTotal,
            "Inteligencia" to character.intTotal,
            "Sabiduría" to character.wisTotal,
            "Carisma" to character.chaTotal
        )

        val boxW = 136f
        val boxH = 70f
        val gapX = 22f
        val gapY = 18f
        val statsTotalW = boxW * 3f + gapX * 2f
        val statsTotalH = boxH * 2f + gapY
        val startX = sectionX + (sectionW - statsTotalW) / 2f
        val startY = 462f + contentOffsetY + (206f - statsTotalH) / 2f + 10f

        stats.forEachIndexed { index, stat ->
            val col = index % 3
            val row = index / 3
            val x = startX + col * (boxW + gapX)
            val y = startY + row * (boxH + gapY)

            drawAttributeBox(
                canvas = canvas,
                label = stat.first,
                value = stat.second,
                modifierValue = abilityModifier(stat.second),
                x = x,
                y = y,
                w = boxW,
                h = boxH,
                borderColor = darkBrown,
                textColor = mediumBrown,
                modifierColor = modifierRed
            )
        }

        // ─────────────────────────────
        // DETALLES
        // ─────────────────────────────

        drawSectionFrame(
            canvas = canvas,
            x = sectionX,
            y = 692f + contentOffsetY,
            w = sectionW,
            h = 84f,
            title = "DETALLES DE PARTIDA",
            borderColor = darkBrown,
            accentColor = gold
        )

        val detailGap = 14f
        val detailH = 40f
        val detailY = 724f + contentOffsetY
        val detailW1 = 176f
        val detailW2 = 124f
        val detailW3 = 124f
        val detailTotalW = detailW1 + detailW2 + detailW3 + detailGap * 2f
        val detailStartX = sectionX + (sectionW - detailTotalW) / 2f

        drawSmallCard(
            canvas = canvas,
            label = "Última partida",
            value = formatLastPlayedPdf(character.lastPlayed),
            x = detailStartX,
            y = detailY,
            w = detailW1,
            h = detailH,
            borderColor = darkBrown,
            fillColor = parchmentLight
        )

        drawSmallCard(
            canvas = canvas,
            label = "Clase de armadura",
            value = character.armorClass.toString(),
            x = detailStartX + detailW1 + detailGap,
            y = detailY,
            w = detailW2,
            h = detailH,
            borderColor = darkBrown,
            fillColor = parchmentLight
        )

        drawSmallCard(
            canvas = canvas,
            label = "Competencia",
            value = "+${character.profBonus}",
            x = detailStartX + detailW1 + detailGap + detailW2 + detailGap,
            y = detailY,
            w = detailW3,
            h = detailH,
            borderColor = darkBrown,
            fillColor = parchmentLight
        )

        paint.style = Paint.Style.FILL
        paint.color = mediumBrown
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.textAlign = Paint.Align.CENTER
    }

    private fun drawTitle(canvas: Canvas, text: String, x: Float, y: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y, paint)
    }

    private fun drawSubtitle(canvas: Canvas, text: String, x: Float, y: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text.ifBlank { "—" }, x, y, paint)
    }

    private fun drawSectionFrame(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        borderColor: Int,
        accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(26, 92, 53, 22)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 16f, 16f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 16f, 16f, paint)

        paint.style = Paint.Style.FILL
        paint.color = accentColor
        canvas.drawRoundRect(RectF(x + 16f, y - 12f, x + 182f, y + 14f), 10f, 10f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, x + 26f, y + 7f, paint)
    }

    private fun drawPortrait(
        canvas: Canvas,
        character: Character,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        borderColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(230, 210, 164)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 14f, 14f, paint)

        val bitmap = decodePortrait(character.portraitUrl)
        if (bitmap != null) {
            val dst = RectF(x + 6f, y + 6f, x + w - 6f, y + h - 6f)
            canvas.drawBitmap(bitmap, null, dst, paint)
        } else {
            paint.color = borderColor
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            paint.textSize = 40f
            canvas.drawText(
                character.name.take(1).uppercase().ifBlank { "?" },
                x + w / 2f,
                y + h / 2f + 14f,
                paint
            )
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 14f, 14f, paint)
    }

    private fun drawLabelValue(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        lineWidth: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = color
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText(label.uppercase(), x, y, paint)

        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        paint.textSize = 18f

        val fittedValue = fitText(value.ifBlank { "—" }, paint, lineWidth)
        canvas.drawText(fittedValue, x, y + 24f, paint)

        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(130, 54, 32, 18)
        canvas.drawLine(x, y + 30f, x + lineWidth, y + 30f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawBigStat(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        borderColor: Int,
        accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 241, 200)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 14f, 14f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.7f
        paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 14f, 14f, paint)

        paint.style = Paint.Style.FILL
        paint.color = accentColor
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 10.5f
        canvas.drawText(label, x + w / 2f, y + 20f, paint)

        paint.color = borderColor
        paint.textSize = 20f
        canvas.drawText(fitText(value, paint, w - 16f), x + w / 2f, y + 45f, paint)
    }

    private fun drawProgressBar(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        progress: Float,
        fillColor: Int,
        trackColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = trackColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), h / 2f, h / 2f, paint)

        paint.color = fillColor
        val fillW = (w * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)
        canvas.drawRoundRect(RectF(x, y, x + fillW, y + h), h / 2f, h / 2f, paint)
    }

    private fun drawAttributeBox(
        canvas: Canvas,
        label: String,
        value: Int,
        modifierValue: Int,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        borderColor: Int,
        textColor: Int,
        modifierColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(250, 240, 210)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 12f, 12f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.7f
        paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 12f, 12f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 9.2f

        val fittedLabel = fitText(label.uppercase(), paint, w - 16f)
        canvas.drawText(fittedLabel, x + w / 2f, y + 17f, paint)

        paint.color = borderColor
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 22f
        canvas.drawText(value.toString(), x + w / 2f, y + 42f, paint)

        val modText = signed(modifierValue)

        val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        capsulePaint.style = Paint.Style.FILL
        capsulePaint.color = Color.argb(45, 171, 124, 35)

        val capsuleW = 34f
        val capsuleH = 15f
        val capsuleLeft = x + (w - capsuleW) / 2f
        val capsuleTop = y + h - 21f

        canvas.drawRoundRect(
            RectF(capsuleLeft, capsuleTop, capsuleLeft + capsuleW, capsuleTop + capsuleH),
            8f,
            8f,
            capsulePaint
        )

        paint.color = modifierColor
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            modText,
            x + w / 2f,
            capsuleTop + 11f,
            paint
        )
    }

    private fun drawSmallCard(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        borderColor: Int,
        fillColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(103, 62, 28)
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 8.2f
        canvas.drawText(
            fitText(label.uppercase(), paint, w - 18f),
            x + 10f,
            y + 15f,
            paint
        )

        paint.color = borderColor
        paint.textSize = 11.2f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        canvas.drawText(
            fitText(value, paint, w - 18f),
            x + 10f,
            y + 33f,
            paint
        )
    }

    private fun decodePortrait(portraitUrl: String): Bitmap? {
        if (portraitUrl.isBlank()) return null

        return runCatching {
            val rawBase64 = when {
                portraitUrl.startsWith("data:image") -> portraitUrl.substringAfter("base64,", "")
                portraitUrl.startsWith("iVBOR") || portraitUrl.length > 100 -> portraitUrl
                else -> ""
            }

            if (rawBase64.isBlank()) return null

            val bytes = Base64.decode(rawBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun fitText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): String {
        if (paint.measureText(text) <= maxWidth) return text

        var result = text
        while (result.length > 1 && paint.measureText("$result…") > maxWidth) {
            result = result.dropLast(1)
        }
        return "$result…"
    }

    private fun abilityModifier(score: Int): Int {
        return floor((score - 10) / 2.0).toInt()
    }

    private fun signed(value: Int): String {
        return if (value >= 0) "+$value" else value.toString()
    }

    private fun formatLastPlayedPdf(timestamp: Long): String {
        if (timestamp <= 0L) return "Sin partidas"
        return SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}