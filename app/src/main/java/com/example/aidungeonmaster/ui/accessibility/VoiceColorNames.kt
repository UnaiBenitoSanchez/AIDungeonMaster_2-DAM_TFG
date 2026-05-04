package com.example.aidungeonmaster.ui.accessibility

// Clase que encapsula la lógica de voice named color.
data class VoiceNamedColor(
    val displayName: String,
    val hex: String,
    val aliases: List<String>
)

private val guildVoiceColors = listOf(
    VoiceNamedColor("rojo", "#B71C1C", listOf("rojo", "granate", "carmesí", "carmesi", "escarlata")),
    VoiceNamedColor("naranja", "#EF6C00", listOf("naranja", "anaranjado", "ámbar", "ambar")),
    VoiceNamedColor("dorado", "#D4AF37", listOf("dorado", "oro", "amarillo dorado")),
    VoiceNamedColor("amarillo", "#FDD835", listOf("amarillo", "luz", "solar")),
    VoiceNamedColor("verde", "#2E7D32", listOf("verde", "bosque", "esmeralda oscuro")),
    VoiceNamedColor("esmeralda", "#00897B", listOf("esmeralda", "turquesa", "verde azulado")),
    VoiceNamedColor("azul", "#1565C0", listOf("azul", "azul real", "marino")),
    VoiceNamedColor("cian", "#00838F", listOf("cian", "celeste", "azul claro")),
    VoiceNamedColor("morado", "#6A1B9A", listOf("morado", "púrpura", "purpura", "violeta", "lila")),
    VoiceNamedColor("rosa", "#AD1457", listOf("rosa", "fucsia", "magenta")),
    VoiceNamedColor("negro", "#1A1A1A", listOf("negro", "oscuro", "sombra")),
    VoiceNamedColor("gris", "#616161", listOf("gris", "plateado", "plata"))
)

// Ejecuta la lógica de find voice named color.
fun findVoiceNamedColor(spokenValue: String): VoiceNamedColor? {
    val normalizedValue = spokenValue.normalizedForVoiceCommand()
    if (normalizedValue.isBlank()) return null

    guildVoiceColors.firstOrNull { color ->
        color.aliases.any { alias ->
            val normalizedAlias = alias.normalizedForVoiceCommand()
            normalizedAlias == normalizedValue ||
                    normalizedValue.contains(normalizedAlias) ||
                    normalizedAlias.contains(normalizedValue)
        }
    }?.let { return it }

    val allAliases = guildVoiceColors.flatMap { color ->
        color.aliases + color.displayName
    }

    val bestAlias = findBestVoiceOption(spokenValue, allAliases) ?: return null
    val normalizedBestAlias = bestAlias.normalizedForVoiceCommand()

    return guildVoiceColors.firstOrNull { color ->
        color.aliases.any { it.normalizedForVoiceCommand() == normalizedBestAlias } ||
                color.displayName.normalizedForVoiceCommand() == normalizedBestAlias
    }
}

// Ejecuta la lógica de guild voice color help.
fun guildVoiceColorHelp(): String =
    guildVoiceColors.joinToString(", ") { it.displayName }
