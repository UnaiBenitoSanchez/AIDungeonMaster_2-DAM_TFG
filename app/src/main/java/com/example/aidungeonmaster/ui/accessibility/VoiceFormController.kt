package com.example.aidungeonmaster.ui.accessibility

import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

// Clase que encapsula la lógica de voice input type.
enum class VoiceInputType {
    TEXT,
    HUMAN_NAME,
    EMAIL,
    USERNAME,
    PASSWORD,
    NUMBER
}

// Clase que encapsula la lógica de voice form field.
data class VoiceFormField(
    val label: String,
    val aliases: List<String>,
    val inputType: VoiceInputType = VoiceInputType.TEXT,
    val onValue: (String) -> Unit,
    val feedback: (String) -> String = { value ->
        if (inputType == VoiceInputType.PASSWORD) {
            "Campo $label actualizado."
        } else {
            "$label actualizado a $value."
        }
    }
)

// Clase que encapsula la lógica de voice form action.
data class VoiceFormAction(
    val label: String,
    val aliases: List<String>,
    val onRun: () -> Unit,
    val enabled: () -> Boolean = { true },
    val disabledFeedback: String = "No puedo ejecutar esa acción todavía. Revisa los campos obligatorios.",
    val feedback: String = "$label ejecutado."
)

// Pantalla que representa voice form.
data class VoiceFormScreen(
    val screenName: String,
    val fields: List<VoiceFormField> = emptyList(),
    val actions: List<VoiceFormAction> = emptyList()
) {
    // Ejecuta la lógica de try handle.
    fun tryHandle(rawCommand: String): String? {
        val command = rawCommand.normalizedForVoiceCommand()
        if (command.isBlank()) return null

        if (
            command.contains("ayuda formulario") ||
            command.contains("ayuda del formulario") ||
            command.contains("campos disponibles") ||
            command.contains("que campos puedo rellenar")
        ) {
            return buildHelpText()
        }

        fields
            .sortedByDescending { field -> field.aliases.maxOfOrNull { it.length } ?: 0 }
            .forEach { field ->
                val spokenValue = extractFieldValue(command, field.aliases)
                if (!spokenValue.isNullOrBlank()) {
                    val finalValue = spokenValue.toVoiceFormValue(field.inputType)

                    if (finalValue.isBlank()) {
                        return "No he entendido el valor para $screenName, campo ${field.label}."
                    }

                    field.onValue(finalValue)
                    return field.feedback(finalValue)
                }
            }

        actions.forEach { action ->
            if (command.matchesVoiceAction(action.aliases)) {
                return if (action.enabled()) {
                    action.onRun()
                    action.feedback
                } else {
                    action.disabledFeedback
                }
            }
        }

        return null
    }

    // Construye help text.
    private fun buildHelpText(): String {
        val fieldNames = fields.joinToString(", ") { it.label }
        val actionNames = actions.joinToString(", ") { it.label }

        return when {
            fields.isNotEmpty() && actions.isNotEmpty() ->
                "En $screenName puedes rellenar: $fieldNames. También puedes decir: $actionNames."

            fields.isNotEmpty() ->
                "En $screenName puedes rellenar: $fieldNames."

            actions.isNotEmpty() ->
                "En $screenName puedes decir: $actionNames."

            else ->
                "No hay campos de formulario registrados en esta pantalla."
        }
    }
}

// Clase que encapsula la lógica de voice form registration.
class VoiceFormRegistration internal constructor(
    private val onDispose: () -> Unit
) {
    // Ejecuta la lógica de dispose.
    fun dispose() = onDispose()
}

// Clase que encapsula la lógica de voice form registry.
object VoiceFormRegistry {
    private val screenStack = mutableListOf<VoiceFormScreen>()

    // Ejecuta la lógica de register.
    fun register(screen: VoiceFormScreen): VoiceFormRegistration {
        screenStack.remove(screen)
        screenStack.add(screen)

        return VoiceFormRegistration {
            screenStack.remove(screen)
        }
    }

    // Ejecuta la lógica de try handle.
    fun tryHandle(rawCommand: String): String? {
        return screenStack.lastOrNull()?.tryHandle(rawCommand)
    }
}

// Ejecuta la lógica de string.
fun String.normalizedForVoiceCommand(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")

    return withoutAccents
        .lowercase(Locale("es", "ES"))
        .replace(Regex("[^a-z0-9ñ@._\\- ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

// Ejecuta la lógica de find best voice option.
fun findBestVoiceOption(
    spokenValue: String,
    options: List<String>
): String? {
    val normalizedValue = spokenValue.normalizedForVoiceCommand()
    if (normalizedValue.isBlank()) return null

    val normalizedOptions = options.map { option ->
        option to option.normalizedForVoiceCommand()
    }

    normalizedOptions.firstOrNull { (_, normalizedOption) ->
        normalizedOption == normalizedValue
    }?.let { return it.first }

    normalizedOptions.firstOrNull { (_, normalizedOption) ->
        normalizedOption.contains(normalizedValue) || normalizedValue.contains(normalizedOption)
    }?.let { return it.first }

    return normalizedOptions
        .map { (option, normalizedOption) ->
            option to levenshteinDistance(normalizedValue, normalizedOption)
        }
        .minByOrNull { it.second }
        ?.takeIf { (_, distance) ->
            distance <= maxOf(2, normalizedValue.length / 3)
        }
        ?.first
}

// Ejecuta la lógica de string.
private fun String.matchesVoiceAction(aliases: List<String>): Boolean {
    return aliases.any { alias ->
        val normalizedAlias = alias.normalizedForVoiceCommand()
        this == normalizedAlias ||
                this.contains(normalizedAlias) ||
                this.startsWith("$normalizedAlias ")
    }
}

// Ejecuta la lógica de extract field value.
private fun extractFieldValue(
    command: String,
    aliases: List<String>
): String? {
    val normalizedAliases = aliases
        .map { it.normalizedForVoiceCommand() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedByDescending { it.length }

    val starters = listOf(
        "rellena",
        "rellenar",
        "completa",
        "completar",
        "pon",
        "poner",
        "escribe",
        "escribir",
        "introduce",
        "introducir",
        "dicta",
        "dictar",
        "establece",
        "cambia",
        "cambiar",
        "busca",
        "buscar"
    )

    val connectors = listOf(
        " con ",
        " como ",
        " a ",
        " en ",
        " es ",
        " igual a ",
        " "
    )

    for (alias in normalizedAliases) {
        val aliasForms = listOf(
            alias,
            "el $alias",
            "la $alias",
            "campo $alias"
        )

        for (aliasForm in aliasForms) {
            for (starter in starters) {
                for (connector in connectors) {
                    val prefix = "$starter $aliasForm$connector"
                    if (command.startsWith(prefix)) {
                        return command.removePrefix(prefix).trim()
                    }
                }
            }

            val directPrefixes = listOf(
                "$aliasForm es ",
                "$aliasForm igual a ",
                "$aliasForm con ",
                "$aliasForm "
            )

            for (prefix in directPrefixes) {
                if (command.startsWith(prefix)) {
                    return command.removePrefix(prefix).trim()
                }
            }
        }
    }

    return null
}

// Ejecuta la lógica de string.
private fun String.toVoiceFormValue(type: VoiceInputType): String {
    val normalized = this.normalizedForVoiceCommand()

    if (type == VoiceInputType.NUMBER) {
        return parseSpanishNumber(normalized)?.toString()
            ?: normalized.filter { it.isDigit() }
    }

    var value = " $normalized "

    val replacements = listOf(
        " arroba " to "@",
        " at " to "@",
        " punto " to ".",
        " dot " to ".",
        " guion bajo " to "_",
        " guión bajo " to "_",
        " barra baja " to "_",
        " guion medio " to "-",
        " guión medio " to "-",
        " guion " to "-",
        " guión " to "-",
        " barra " to "/",
        " espacio " to " "
    )

    replacements.forEach { (spoken, symbol) ->
        value = value.replace(spoken, symbol)
    }

    value = value
        .replace(Regex("\\s+"), " ")
        .trim()

    return when (type) {
        VoiceInputType.EMAIL -> value
            .replace(" ", "")
            .lowercase(Locale("es", "ES"))

        VoiceInputType.USERNAME -> value
            .replace(" ", "_")
            .lowercase(Locale("es", "ES"))
            .filter { it.isLetterOrDigit() || it == '_' || it == '.' }

        VoiceInputType.PASSWORD -> value
            .replace(" ", "")

        VoiceInputType.HUMAN_NAME -> value
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale("es", "ES")) else char.toString()
                }
            }

        VoiceInputType.TEXT,
        VoiceInputType.NUMBER -> value
    }
}

// Analiza spanish number.
private fun parseSpanishNumber(value: String): Int? {
    value.toIntOrNull()?.let { return it }

    val units = mapOf(
        "cero" to 0,
        "uno" to 1,
        "una" to 1,
        "dos" to 2,
        "tres" to 3,
        "cuatro" to 4,
        "cinco" to 5,
        "seis" to 6,
        "siete" to 7,
        "ocho" to 8,
        "nueve" to 9,
        "diez" to 10,
        "once" to 11,
        "doce" to 12,
        "trece" to 13,
        "catorce" to 14,
        "quince" to 15,
        "dieciseis" to 16,
        "diecisiete" to 17,
        "dieciocho" to 18,
        "diecinueve" to 19,
        "veinte" to 20,
        "veintiuno" to 21,
        "veintidos" to 22,
        "veintitres" to 23,
        "veinticuatro" to 24,
        "veinticinco" to 25,
        "veintiseis" to 26,
        "veintisiete" to 27,
        "veintiocho" to 28,
        "veintinueve" to 29,
        "treinta" to 30
    )

    units[value]?.let { return it }

    val compact = value.replace(" ", "")
    units[compact]?.let { return it }

    return null
}

// Ejecuta la lógica de levenshtein distance.
private fun levenshteinDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    val previous = IntArray(b.length + 1) { it }
    val current = IntArray(b.length + 1)

    for (i in a.indices) {
        current[0] = i + 1

        for (j in b.indices) {
            val insertCost = current[j] + 1
            val deleteCost = previous[j + 1] + 1
            val replaceCost = previous[j] + if (a[i] == b[j]) 0 else 1

            current[j + 1] = min(min(insertCost, deleteCost), replaceCost)
        }

        for (j in previous.indices) {
            previous[j] = current[j]
        }
    }

    return previous[b.length]
}
