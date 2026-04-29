package com.example.aidungeonmaster.ui.accessibility

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.navigation.NavHostController
import com.example.aidungeonmaster.data.model.Character
import com.example.aidungeonmaster.data.model.Guild
import com.example.aidungeonmaster.navigation.Screen
import com.example.aidungeonmaster.ui.theme.ColorBlindType
import com.example.aidungeonmaster.viewmodel.GameViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.Normalizer
import java.util.Locale

/**
 * Traduce lenguaje natural sencillo a acciones de navegación o acciones de juego.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun executeVoiceCommand(
    rawCommand: String,
    navController: NavHostController,
    currentRoute: String?,
    currentArguments: Bundle?,
    characters: List<Character>,
    myGuilds: List<Guild>,
    gameViewModel: GameViewModel,
    currentColorBlindType: ColorBlindType,
    onColorBlindChanged: (ColorBlindType) -> Unit,
    onOpenUsabilityOptions: () -> Unit,
    onStartVoiceControl: () -> Unit,
    onStopVoiceControl: () -> Unit,
    onRelaunchTutorial: () -> Unit,
    onLogout: () -> Unit
): String {
    val command = rawCommand.normalizedForVoice()
    if (command.isBlank()) return "No he entendido la orden."

    fun navigate(route: String, feedback: String): String {
        navController.navigate(route) { launchSingleTop = true }
        return feedback
    }

    fun currentGameContext(): CharacterContext? {
        val userId = Uri.decode(currentArguments?.getString("userId").orEmpty())
        val characterName = Uri.decode(currentArguments?.getString("characterName").orEmpty())
        return if (userId.isNotBlank() && characterName.isNotBlank()) {
            CharacterContext(userId = userId, characterName = characterName)
        } else {
            null
        }
    }

    fun matchedCharacterContext(): CharacterContext? {
        val current = currentGameContext()
        val matched = characters.bestVoiceMatch(command)
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        return when {
            matched != null && uid.isNotBlank() -> CharacterContext(uid, matched.name, matched)
            current != null -> current
            characters.size == 1 && uid.isNotBlank() -> CharacterContext(uid, characters.first().name, characters.first())
            else -> null
        }
    }

    fun myJoinedGuild(): Guild? {
        return myGuilds.firstOrNull { it.joined } ?: myGuilds.firstOrNull()
    }

    if (command.containsAny(
            "activa control por voz",
            "activar control por voz",
            "enciende control por voz",
            "enciende el control por voz",
            "activa la voz",
            "activar la voz",
            "enciende la voz",
            "activa comandos de voz"
        )
    ) {
        onStartVoiceControl()
        return "El control por voz ya está activo."
    }

    if (command.containsAny(
            "desactiva control por voz",
            "desactiva el control por voz",
            "apaga control por voz",
            "apaga el control por voz",
            "para control por voz",
            "deten control por voz",
            "detén control por voz",
            "desactivar control por voz",
            "desactiva la voz",
            "desactivar la voz",
            "desactiva voz",
            "apaga la voz",
            "apagar la voz",
            "quita la voz",
            "quitar la voz",
            "desactiva comandos de voz",
            "apaga comandos de voz",
            "deja de escuchar",
            "deja de oir",
            "deja de oír",
            "apaga el microfono",
            "apaga el micrófono",
            "desactiva el microfono",
            "desactiva el micrófono"
        )
    ) {
        onStopVoiceControl()
        return "Control por voz desactivado."
    }

    if (command.containsAny(
            "cerrar sesion",
            "cerrar sesión",
            "cierra sesion",
            "cierra sesión",
            "salir de mi cuenta",
            "logout"
        )
    ) {
        onLogout()
        return "Cerrando sesión."
    }

    if (command.containsAny(
            "relanzar tutorial",
            "repetir tutorial",
            "reiniciar tutorial",
            "ver tutorial",
            "abrir tutorial"
        )
    ) {
        onRelaunchTutorial()
        return "Relanzando tutorial."
    }

    if (command.containsAny(
            "desactiva modo daltonico",
            "desactiva el modo daltonico",
            "desactiva modo daltónico",
            "desactiva el modo daltónico",
            "desactivar modo daltonico",
            "desactivar el modo daltonico",
            "desactivar modo daltónico",
            "desactivar el modo daltónico",
            "quita modo daltonico",
            "quita el modo daltonico",
            "quita modo daltónico",
            "quita el modo daltónico",
            "quita el daltonico",
            "quita el daltónico",
            "apaga modo daltonico",
            "apaga el modo daltonico",
            "apaga modo daltónico",
            "apaga el modo daltónico",
            "sin filtro de color",
            "sin modo daltonico",
            "sin modo daltónico",
            "colores normales",
            "modo normal",
            "vision normal",
            "visión normal"
        )
    ) {
        onColorBlindChanged(ColorBlindType.NONE)
        return "Modo daltónico desactivado."
    }

    command.requestedColorBlindType()?.let { requestedType ->
        if (requestedType != currentColorBlindType) {
            onColorBlindChanged(requestedType)
        }
        return "Modo daltónico cambiado a ${requestedType.displayName}."
    }

    if (command.containsAny(
            "modo daltonico",
            "modo daltónico",
            "activar modo daltonico",
            "activar modo daltónico",
            "activa modo daltonico",
            "activa modo daltónico"
        )
    ) {
        return "Di el tipo: protanopía, deuteranopía, tritanopía o acromatopsia."
    }

    if (command.containsAny("ayuda", "que puedo decir", "comandos", "ordenes disponibles")) {
        return "Puedes decir: desactiva la voz, protanopía, deuteranopía, tritanopía, acromatopsia, colores normales, crear personaje, sube fuerza, abre gremios, abre chat del gremio, cambia color de acento a morado, selecciona aventura cyberpunk, descargar ficha en PDF, repetir tutorial, siguiente página del tutorial, página anterior del tutorial, ir a la página 3 del tutorial o cerrar sesión."
    }

    if (command.containsAny(
            "opciones de usabilidad",
            "ajustes de usabilidad",
            "opciones de accesibilidad",
            "ajustes de accesibilidad",
            "abre accesibilidad",
            "abrir accesibilidad",
            "abre opciones de accesibilidad",
            "abrir opciones de accesibilidad",
            "abre opciones de usabilidad",
            "abrir opciones de usabilidad"
        )
    ) {
        onOpenUsabilityOptions()
        return "Abriendo opciones de usabilidad."
    }

    TutorialVoiceRegistry.tryHandle(rawCommand)?.let { feedback ->
        return feedback
    }

    if (command.containsAny("volver atras", "vuelve atras", "atras", "retrocede", "pantalla anterior")) {
        val didPop = navController.popBackStack()
        return if (didPop) "Volviendo atrás." else "No hay una pantalla anterior a la que volver."
    }

    VoiceFormRegistry.tryHandle(rawCommand)?.let { feedback ->
        return feedback
    }

    if (command.containsAny(
            "ir a registro",
            "ve a registro",
            "abre registro",
            "abrir registro",
            "pantalla de registro",
            "registrarme",
            "crear cuenta",
            "crear una cuenta"
        )
    ) {
        return navigate(Screen.Register.route, "Abriendo registro.")
    }

    if (command.containsAny(
            "ir a login",
            "ir a inicio de sesion",
            "ir a iniciar sesion",
            "abre login",
            "abrir login",
            "pantalla de login",
            "iniciar sesion",
            "inicio de sesion"
        )
    ) {
        return navigate(Screen.Login.route, "Abriendo inicio de sesión.")
    }

    if (command.containsAny(
            "crear personaje",
            "crear un personaje",
            "nuevo personaje",
            "nuevo heroe",
            "nuevo héroe",
            "crear heroe",
            "crear héroe",
            "crear aventurero",
            "nuevo aventurero"
        )
    ) {
        return navigate(
            Screen.Home.createRoute(openCreateCharacter = true),
            "Abriendo creación de personaje."
        )
    }

    if (command.containsAny("inicio", "pantalla principal", "home", "mis personajes", "tus personajes")) {
        return navigate(Screen.Home.route, "Abriendo pantalla principal.")
    }

    if (command.containsAny("mi perfil", "abre perfil", "abrir perfil", "perfil de jugador")) {
        return navigate(Screen.MyProfile.route, "Abriendo perfil.")
    }

    if (command.containsAny("ranking", "clasificacion", "clasificación")) {
        return navigate(Screen.Ranking.route, "Abriendo ranking mundial.")
    }

    if (command.containsAny("logros", "recompensas")) {
        return navigate(Screen.Achievements.route, "Abriendo logros.")
    }

    if (command.containsAny("lista de amigos", "mis amigos", "amigos")) {
        return navigate(Screen.FriendsList.route, "Abriendo lista de amigos.")
    }

    if (command.containsAny("solicitudes de amistad", "peticiones de amistad", "solicitudes")) {
        return navigate(Screen.FriendRequests.route, "Abriendo solicitudes de amistad.")
    }

    if (command.containsAny("buscar usuarios", "buscar jugador", "buscar jugadores", "busca usuarios")) {
        return navigate(Screen.UserSearch.route, "Abriendo búsqueda de usuarios.")
    }

    if (command.containsAny("crear gremio", "crear un gremio", "nuevo gremio", "crear clan", "crear un clan")) {
        return navigate(Screen.Guilds.createRoute(openCreate = true), "Abriendo creación de gremio.")
    }

    if (
        command.containsAny("gremios", "gremio", "guilds", "clanes", "clan") &&
        !command.containsAny(
            "mi gremio",
            "chat del gremio",
            "miembros del gremio",
            "jefe final del gremio",
            "raid del gremio",
            "batalla del gremio"
        )
    ) {
        return navigate(Screen.Guilds.createRoute(), "Abriendo gremios.")
    }

    if (command.containsAny(
            "mi gremio",
            "abre mi gremio",
            "abrir mi gremio",
            "ir a mi gremio",
            "entra en mi gremio"
        )
    ) {
        val guild = myJoinedGuild()
            ?: return "No he encontrado tu gremio todavía. Di abre gremios para cargar la lista."
        return navigate(Screen.GuildDetails.createRoute(guild.id), "Abriendo tu gremio.")
    }

    if (command.containsAny(
            "abre chat del gremio",
            "abrir chat del gremio",
            "chat de mi gremio",
            "abre el chat de mi gremio",
            "ir al chat del gremio"
        )
    ) {
        val guild = myJoinedGuild()
            ?: return "No he encontrado tu gremio todavía. Di abre gremios para cargar la lista."
        return navigate(
            Screen.GuildDetails.createRoute(guild.id, tab = "chat"),
            "Abriendo chat de tu gremio."
        )
    }

    if (command.containsAny(
            "abre miembros del gremio",
            "abrir miembros del gremio",
            "miembros de mi gremio",
            "ver miembros del gremio",
            "ir a miembros del gremio"
        )
    ) {
        val guild = myJoinedGuild()
            ?: return "No he encontrado tu gremio todavía. Di abre gremios para cargar la lista."
        return navigate(
            Screen.GuildDetails.createRoute(guild.id, tab = "miembros"),
            "Abriendo miembros de tu gremio."
        )
    }

    if (command.containsAny(
            "abre jefe final del gremio",
            "abrir jefe final del gremio",
            "jefe final de mi gremio",
            "raid del gremio",
            "batalla del gremio"
        )
    ) {
        val guild = myJoinedGuild()
            ?: return "No he encontrado tu gremio todavía. Di abre gremios para cargar la lista."
        return navigate(
            Screen.GuildDetails.createRoute(guild.id, tab = "jefe_final"),
            "Abriendo jefe final de tu gremio."
        )
    }

    if (command.containsAny("inventario", "mochila", "objetos")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre inventario de Aria."
        return navigate(Screen.Inventory.createRoute(context.charId), "Abriendo inventario de ${context.characterName}.")
    }

    if (command.containsAny("diario", "journal")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre diario de Aria."
        return navigate(Screen.Journal.createRoute(context.charId), "Abriendo diario de ${context.characterName}.")
    }

    if (command.containsAny("bestiario", "monstruos", "criaturas")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre bestiario de Aria."
        return navigate("bestiary/${Uri.encode(context.charId)}", "Abriendo bestiario de ${context.characterName}.")
    }

    if (command.containsAny("qr", "codigo qr", "escaner", "escáner")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre el escáner QR de Aria."
        return navigate("qr_scanner/${Uri.encode(context.charId)}", "Abriendo escáner QR de ${context.characterName}.")
    }

    if (command.containsAny("mapa", "abrir mapa", "abre mapa", "ir al mapa", "realidad aumentada")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre mapa de Aria."

        return navigate(
            Screen.ARMap.createRoute(context.charId),
            "Abriendo mapa de ${context.characterName}."
        )
    }

    if (command.containsAny("galeria", "galería", "ubicaciones", "lugares 3d", "3d")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre galería de Aria."
        return navigate(
            Screen.LocationsGallery.createRoute(context.charId, context.characterName),
            "Abriendo galería de ubicaciones de ${context.characterName}."
        )
    }

    if (command.containsAny("sala", "habitacion", "habitación", "fortaleza", "cuarto")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre sala de Aria."
        return navigate(
            Screen.PersonalRoom.createRoute(context.charId, context.characterName),
            "Abriendo sala personal de ${context.characterName}."
        )
    }

    if (command.containsAny("ficha", "hoja de personaje", "datos del personaje")) {
        val context = matchedCharacterContext()
            ?: return "Necesito saber de qué personaje. Di, por ejemplo, abre ficha de Aria."
        return navigate(
            Screen.CharacterSheet.createRoute(context.userId, context.characterName),
            "Abriendo ficha de ${context.characterName}."
        )
    }

    val isOpenGameCommand =
        command.startsWithAny(
            "ir a partida de",
            "ve a partida de",
            "abre la partida de",
            "abrir la partida de",
            "continua la partida de",
            "continúa la partida de",
            "abrir partida de",
            "abre partida de",
            "ir a aventura de",
            "abre aventura de"
        ) || command.containsAny(
            "partida",
            "aventura",
            "continuar partida",
            "continua partida",
            "continúa partida",
            "abre personaje",
            "abrir personaje"
        )

    if (isOpenGameCommand) {
        val context = matchedCharacterContext()
            ?: return "No he encontrado ese personaje. Prueba diciendo: abre la partida de, y el nombre del personaje."

        val character = context.character

        return if (character?.gameTheme.isNullOrBlank()) {
            navigate(
                Screen.GameSetup.createRoute(context.userId, context.characterName),
                "Abriendo configuración de partida para ${context.characterName}."
            )
        } else {
            navigate(
                Screen.GamePlay.createRoute(
                    context.userId,
                    context.characterName,
                    character?.gameTheme.orEmpty()
                ),
                "Abriendo partida de ${context.characterName}."
            )
        }
    }

    val route = currentRoute.orEmpty()
    if (route == Screen.GamePlay.route || route.contains("game_play")) {
        gameViewModel.sendCustomAction(rawCommand.trim())
        return "Ejecutando acción: ${rawCommand.trim()}."
    }

    return "No he reconocido esa orden. Di ayuda para escuchar ejemplos de comandos."
}

private data class CharacterContext(
    val userId: String,
    val characterName: String,
    val character: Character? = null
) {
    val charId: String = "${userId}_${characterName}"
}

private fun List<Character>.bestVoiceMatch(command: String): Character? {
    return this
        .filter { character ->
            val normalizedName = character.name.normalizedForVoice()
            normalizedName.isNotBlank() && command.contains(normalizedName)
        }
        .maxByOrNull { it.name.length }
}

private fun String.containsAny(vararg candidates: String): Boolean {
    return candidates.any { candidate -> contains(candidate.normalizedForVoice()) }
}

private fun String.normalizedForVoice(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")

    return withoutAccents
        .lowercase(Locale("es", "ES"))
        .replace(Regex("[^a-z0-9ñ_ ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}


private fun String.requestedColorBlindType(): ColorBlindType? {
    return when {
        containsAny(
            "protanopia",
            "protanopía",
            "modo protanopia",
            "modo protanopía",
            "activa protanopia",
            "activa protanopía",
            "activar protanopia",
            "activar protanopía",
            "pon protanopia",
            "pon protanopía"
        ) -> ColorBlindType.PROTANOPIA

        containsAny(
            "deuteranopia",
            "deuteranopía",
            "modo deuteranopia",
            "modo deuteranopía",
            "activa deuteranopia",
            "activa deuteranopía",
            "activar deuteranopia",
            "activar deuteranopía",
            "pon deuteranopia",
            "pon deuteranopía"
        ) -> ColorBlindType.DEUTERANOPIA

        containsAny(
            "tritanopia",
            "tritanopía",
            "modo tritanopia",
            "modo tritanopía",
            "activa tritanopia",
            "activa tritanopía",
            "activar tritanopia",
            "activar tritanopía",
            "pon tritanopia",
            "pon tritanopía"
        ) -> ColorBlindType.TRITANOPIA

        containsAny(
            "acromatopsia",
            "modo acromatopsia",
            "activa acromatopsia",
            "activar acromatopsia",
            "pon acromatopsia",
            "escala de grises",
            "blanco y negro",
            "monocromatico",
            "monocromático"
        ) -> ColorBlindType.ACHROMATOPSIA

        else -> null
    }
}

private fun String.startsWithAny(vararg prefixes: String): Boolean {
    return prefixes.any { this.startsWith(it.normalizedForVoice()) }
}
