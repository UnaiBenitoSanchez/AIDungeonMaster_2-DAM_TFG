package com.example.aidungeonmaster.ui.i18n

import android.content.Context
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.aidungeonmaster.ui.settings.AppLanguageManager
import androidx.compose.material3.Text as MaterialText

// Clase que encapsula la lógica de fixed text translator.
object FixedTextTranslator {
    // Clase que encapsula la lógica de t.
    private data class T(
        val en: String,
        val ca: String,
        val eu: String,
        val de: String,
        val fr: String,
        val gl: String = ""
    ) {
        fun of(code: String): String = when (code) {
            "en" -> en
            "ca" -> ca
            "eu" -> eu
            "de" -> de
            "fr" -> fr
            "gl" -> gl
            else -> en
        }
    }

    private fun t(en: String, ca: String, eu: String, de: String, fr: String, gl: String = "") = T(en, ca, eu, de, fr, gl)

    private val exact = mapOf(
        "Español" to t("Spanish", "Espanyol", "Gaztelania", "Spanisch", "Espagnol"),
        "English" to t("English", "Angles", "Ingelesa", "Englisch", "Anglais"),
        "Català" to t("Catalan", "Catala", "Katalana", "Katalanisch", "Catalan"),
        "Euskera" to t("Basque", "Euskara", "Euskara", "Baskisch", "Basque"),
        "Deutsch" to t("German", "Alemany", "Alemana", "Deutsch", "Allemand"),
        "Français" to t("French", "Frances", "Frantsesa", "Franzosisch", "Francais"),
        "Idioma" to t("Language", "Idioma", "Hizkuntza", "Sprache", "Langue"),
        "Selecciona idioma" to t("Select language", "Selecciona idioma", "Aukeratu hizkuntza", "Sprache auswahlen", "Choisir la langue"),
        "Galego" to t("Galician", "Gallec", "Galiziera", "Galicisch", "Galicien", "Galego"),


        "Continuar" to t("Continue", "Continuar", "Jarraitu", "Weiter", "Continuer"),
        "Saltar" to t("Skip", "Saltar", "Saltatu", "Uberspringen", "Passer"),
        "Atrás" to t("Back", "Enrere", "Atzera", "Zuruck", "Retour"),
        "Volver" to t("Back", "Tornar", "Itzuli", "Zuruck", "Retour"),
        "Cerrar" to t("Close", "Tancar", "Itxi", "Schliessen", "Fermer"),
        "Cancelar" to t("Cancel", "Cancel-lar", "Utzi", "Abbrechen", "Annuler"),
        "Crear" to t("Create", "Crear", "Sortu", "Erstellen", "Creer"),
        "Eliminar" to t("Delete", "Eliminar", "Ezabatu", "Loschen", "Supprimer"),
        "Guardar" to t("Save", "Desar", "Gorde", "Speichern", "Enregistrer"),
        "Aplicar" to t("Apply", "Aplicar", "Aplikatu", "Anwenden", "Appliquer"),
        "Ver" to t("View", "Veure", "Ikusi", "Ansehen", "Voir"),
        "Chat" to t("Chat", "Xat", "Txata", "Chat", "Chat"),
        "Enviar" to t("Send", "Enviar", "Bidali", "Senden", "Envoyer"),
        "Sin datos" to t("No data", "Sense dades", "Daturik gabe", "Keine Daten", "Aucune donnee"),
        "Sin descripción." to t("No description.", "Sense descripcio.", "Deskribapenik gabe.", "Keine Beschreibung.", "Aucune description."),
        "Sin biografía todavía." to t("No bio yet.", "Encara sense biografia.", "Oraindik biografiarik ez.", "Noch keine Biografie.", "Pas encore de biographie."),
        "Seleccionado" to t("Selected", "Seleccionat", "Hautatua", "Ausgewahlt", "Selectionne"),

        "Tus personajes" to t("Your characters", "Els teus personatges", "Zure pertsonaiak", "Deine Charaktere", "Tes personnages"),
        "Tus Personajes" to t("Your characters", "Els teus personatges", "Zure pertsonaiak", "Deine Charaktere", "Tes personnages"),
        "¡Bienvenido a AI Dungeon Master!" to t("Welcome to AI Dungeon Master!", "Benvingut a AI Dungeon Master!", "Ongi etorri AI Dungeon Master-era!", "Willkommen bei AI Dungeon Master!", "Bienvenue dans AI Dungeon Master !"),
        "Soy Enzo, tu guía de aventura. Te enseñaré rápidamente las zonas principales de la app para que puedas empezar sin perderte." to t("I am Enzo, your adventure guide. I will quickly show you the main areas of the app so you can start without getting lost.", "Soc l'Enzo, el teu guia d'aventura. T'ensenyare rapidament les zones principals de l'app perque puguis comencar sense perdre't.", "Enzo naiz, zure abentura-gida. Aplikazioaren gune nagusiak azkar erakutsiko dizkizut, galdu gabe hasteko.", "Ich bin Enzo, dein Abenteuerfuhrer. Ich zeige dir kurz die wichtigsten Bereiche der App, damit du direkt starten kannst.", "Je suis Enzo, ton guide d'aventure. Je vais te montrer rapidement les zones principales de l'application pour commencer sans te perdre."),
        "Accesibilidad" to t("Accessibility", "Accessibilitat", "Irisgarritasuna", "Barrierefreiheit", "Accessibilite"),
        "Pulsa aquí para abrir las opciones de accesibilidad, como el modo daltónico y el control por voz." to t("Tap here to open accessibility options such as color-blind mode and voice control.", "Prem aqui per obrir les opcions d'accessibilitat, com el mode daltonic i el control per veu.", "Sakatu hemen irisgarritasun aukerak irekitzeko, hala nola daltoniko modua eta ahots-kontrola.", "Tippe hier, um Barrierefreiheitsoptionen wie Farbenblindheitsmodus und Sprachsteuerung zu offnen.", "Appuie ici pour ouvrir les options d'accessibilite, comme le mode daltonien et le controle vocal."),
        "Repetir tutorial" to t("Repeat tutorial", "Repetir tutorial", "Tutoriala errepikatu", "Tutorial wiederholen", "Recommencer le tutoriel"),
        "Ranking mundial" to t("Global ranking", "Ranquing mundial", "Munduko sailkapena", "Weltrangliste", "Classement mondial"),
        "Logros" to t("Achievements", "Assoliments", "Lorpenak", "Erfolge", "Succes"),
        "Cerrar sesión" to t("Log out", "Tancar sessio", "Saioa itxi", "Abmelden", "Deconnexion"),
        "Tarjeta del personaje" to t("Character card", "Targeta del personatge", "Pertsonaiaren txartela", "Charakterkarte", "Carte du personnage"),
        "Sala personal" to t("Personal room", "Sala personal", "Gela pertsonala", "Personlicher Raum", "Salle personnelle"),
        "Ficha RPG" to t("RPG sheet", "Fitxa RPG", "RPG fitxa", "RPG-Bogen", "Fiche RPG"),
        "Eliminar personaje" to t("Delete character", "Eliminar personatge", "Pertsonaia ezabatu", "Charakter loschen", "Supprimer le personnage"),
        "Social" to t("Social", "Social", "Soziala", "Sozial", "Social"),
        "Zona social" to t("Social area", "Zona social", "Gune soziala", "Sozialbereich", "Zone sociale"),
        "Mi perfil" to t("My profile", "El meu perfil", "Nire profila", "Mein Profil", "Mon profil"),
        "Buscar usuarios" to t("Search users", "Buscar usuaris", "Bilatu erabiltzaileak", "Benutzer suchen", "Rechercher des utilisateurs"),
        "Solicitudes de amistad" to t("Friend requests", "Sol-licituds d'amistat", "Lagun-eskaerak", "Freundschaftsanfragen", "Demandes d'ami"),
        "Lista de amigos" to t("Friends list", "Llista d'amics", "Lagunen zerrenda", "Freundesliste", "Liste d'amis"),
        "Gremios" to t("Guilds", "Gremis", "Gremioak", "Gilden", "Guildes"),
        "¿Eliminar personaje?" to t("Delete character?", "Eliminar personatge?", "Pertsonaia ezabatu?", "Charakter loschen?", "Supprimer le personnage ?"),
        "Personaje caído. Elimínalo para limpiar sus registros." to t("Fallen character. Delete it to clean up its records.", "Personatge caigut. Elimina'l per netejar-ne els registres.", "Pertsonaia eroria. Ezabatu erregistroak garbitzeko.", "Gefallener Charakter. Losche ihn, um seine Eintrage zu bereinigen.", "Personnage tombe. Supprime-le pour nettoyer ses donnees."),
        "Abrir fortaleza" to t("Open fortress", "Obrir fortalesa", "Ireki gotorlekua", "Festung offnen", "Ouvrir la forteresse"),
        "Ver ficha RPG" to t("View RPG sheet", "Veure fitxa RPG", "Ikusi RPG fitxa", "RPG-Bogen ansehen", "Voir la fiche RPG"),
        "Borrar" to t("Delete", "Esborrar", "Ezabatu", "Loschen", "Effacer"),
        "Hace un momento" to t("Just now", "Ara mateix", "Duela une bat", "Gerade eben", "A l'instant"),
        "Ayer" to t("Yesterday", "Ahir", "Atzo", "Gestern", "Hier"),

        "Nuevo Aventurero ⚔️" to t("New Adventurer ⚔️", "Nou aventurer ⚔️", "Abenturazale berria ⚔️", "Neuer Abenteurer ⚔️", "Nouvel aventurier ⚔️"),
        "Nombre del héroe" to t("Hero name", "Nom de l'heroi", "Heroiaren izena", "Heldenname", "Nom du heros"),
        "Raza" to t("Race", "Raca", "Arraza", "Volk", "Race"),
        "Clase" to t("Class", "Classe", "Klasea", "Klasse", "Classe"),
        "Subclase" to t("Subclass", "Subclasse", "Azpiklasea", "Unterklasse", "Sous-classe"),
        "Selecciona una clase primero" to t("Select a class first", "Selecciona primer una classe", "Aukeratu klase bat lehenik", "Wahle zuerst eine Klasse", "Choisis d'abord une classe"),
        "Apariencia Física 🎨" to t("Physical Appearance 🎨", "Aparenca fisica 🎨", "Itxura fisikoa 🎨", "Aussehen 🎨", "Apparence physique 🎨"),
        "Describe cómo se ve tu personaje. La IA generará su retrato." to t("Describe how your character looks. The AI will generate their portrait.", "Descriu com es veu el teu personatge. La IA en generara el retrat.", "Deskribatu zure pertsonaiaren itxura. IAk erretratua sortuko du.", "Beschreibe das Aussehen deiner Figur. Die KI erstellt ihr Portrat.", "Decris l'apparence de ton personnage. L'IA generera son portrait."),
        "Ej: Joven con cicatriz en el ojo, pelo largo plateado, armadura dorada..." to t("Ex: Young person with an eye scar, long silver hair, golden armor...", "Ex: Jove amb cicatriu a l'ull, cabell llarg platejat, armadura daurada...", "Adib: gaztea begian orbainarekin, ile zilar luzea, urrezko armadura...", "Bsp.: Jung, Narbe am Auge, langes silbernes Haar, goldene Rustung...", "Ex : jeune avec une cicatrice a l'oeil, longs cheveux argentes, armure doree..."),
        "Pintando el retrato..." to t("Painting the portrait...", "Pintant el retrat...", "Erretratua margotzen...", "Portrat wird gemalt...", "Peinture du portrait..."),
        "🔄 Regenerar Retrato" to t("🔄 Regenerate Portrait", "🔄 Regenerar retrat", "🔄 Erretratua birsortu", "🔄 Portrat neu erstellen", "🔄 Regenerer le portrait"),
        "🖼️ Generar Retrato" to t("🖼️ Generate Portrait", "🖼️ Generar retrat", "🖼️ Erretratua sortu", "🖼️ Portrat erstellen", "🖼️ Generer le portrait"),
        "Generando retrato..." to t("Generating portrait...", "Generant retrat...", "Erretratua sortzen...", "Portrat wird erstellt...", "Generation du portrait..."),
        "Puede tardar entre 30 y 90 segundos. Por favor espera." to t("This may take 30 to 90 seconds. Please wait.", "Pot trigar entre 30 i 90 segons. Espera, si us plau.", "30 eta 90 segundo artean iraun dezake. Itxaron mesedez.", "Das kann 30 bis 90 Sekunden dauern. Bitte warten.", "Cela peut prendre entre 30 et 90 secondes. Merci de patienter."),
        "Atributos ⚔️" to t("Attributes ⚔️", "Atributs ⚔️", "Atributuak ⚔️", "Attribute ⚔️", "Attributs ⚔️"),
        "Sistema D&D — 4d6 descarta el menor" to t("D&D system - 4d6 drop the lowest", "Sistema D&D - 4d6 descarta el menor", "D&D sistema - 4d6 txikiena baztertuta", "D&D-System - 4W6, niedrigsten verwerfen", "Systeme D&D - 4d6, on retire le plus bas"),
        "Tira 4 dados de 6, descarta el resultado más bajo y suma los 3 restantes." to t("Roll 4 six-sided dice, drop the lowest result and add the other 3.", "Tira 4 daus de 6, descarta el resultat mes baix i suma els 3 restants.", "Bota sei aldeko 4 dado, baztertu emaitza txikiena eta batu beste 3ak.", "Wirf 4 W6, streiche den niedrigsten Wurf und addiere die restlichen 3.", "Lance 4 des a 6 faces, retire le plus bas et additionne les 3 autres."),
        "🎲 Tirar Dados" to t("🎲 Roll Dice", "🎲 Tirar daus", "🎲 Dadoak bota", "🎲 Wurfeln", "🎲 Lancer les des"),
        "↩ Manual" to t("↩ Manual", "↩ Manual", "↩ Eskuz", "↩ Manuell", "↩ Manuel"),
        "Crear Personaje" to t("Create Character", "Crear personatge", "Sortu pertsonaia", "Charakter erstellen", "Creer le personnage"),
        "Guardando..." to t("Saving...", "Desant...", "Gordetzen...", "Speichern...", "Enregistrement..."),
        "Fuerza" to t("Strength", "Forca", "Indarra", "Starke", "Force"),
        "Destreza" to t("Dexterity", "Destresa", "Trebetasuna", "Geschicklichkeit", "Dexterite"),
        "Constitución" to t("Constitution", "Constitucio", "Gorpuzkera", "Konstitution", "Constitution"),
        "Inteligencia" to t("Intelligence", "Intel-ligencia", "Adimena", "Intelligenz", "Intelligence"),
        "Sabiduría" to t("Wisdom", "Saviesa", "Jakinduria", "Weisheit", "Sagesse"),
        "Carisma" to t("Charisma", "Carisma", "Karisma", "Charisma", "Charisme"),

        "Preparando aventura para" to t("Preparing adventure for", "Preparant aventura per a", "Abentura prestatzen honentzat:", "Abenteuer wird vorbereitet fur", "Preparation de l'aventure pour"),
        "Selecciona la temática de tu partida:" to t("Select your game's theme:", "Selecciona la tematica de la partida:", "Aukeratu partidaren gaia:", "Wahle das Thema deiner Partie:", "Choisis le theme de ta partie :"),
        "Nota: Esta será la historia permanente de este héroe." to t("Note: This will be this hero's permanent story.", "Nota: Aquesta sera la historia permanent d'aquest heroi.", "Oharra: hau heroi honen istorio iraunkorra izango da.", "Hinweis: Dies wird die dauerhafte Geschichte dieses Helden.", "Note : ce sera l'histoire permanente de ce heros."),
        "Fantasía Épica" to t("Epic Fantasy", "Fantasia epica", "Fantasia epikoa", "Epische Fantasy", "Fantasy epique"),
        "Terror Gótico" to t("Gothic Horror", "Terror gotic", "Izu gotikoa", "Gotischer Horror", "Horreur gothique"),
        "Cyberpunk" to t("Cyberpunk", "Cyberpunk", "Cyberpunk", "Cyberpunk", "Cyberpunk"),
        "Misterio" to t("Mystery", "Misteri", "Misterioa", "Mysterium", "Mystere"),

        "Tu aventura comienza aquí" to t("Your adventure begins here", "La teva aventura comenca aqui", "Zure abentura hemen hasten da", "Dein Abenteuer beginnt hier", "Ton aventure commence ici"),
        "Iniciar Sesión" to t("Sign in", "Iniciar sessio", "Saioa hasi", "Anmelden", "Connexion"),
        "Correo Electrónico" to t("Email", "Correu electronic", "Posta elektronikoa", "E-Mail", "Adresse e-mail"),
        "Contraseña" to t("Password", "Contrasenya", "Pasahitza", "Passwort", "Mot de passe"),
        "Confirmar Contraseña" to t("Confirm password", "Confirmar contrasenya", "Berretsi pasahitza", "Passwort bestatigen", "Confirmer le mot de passe"),
        "Nombre de usuario" to t("Username", "Nom d'usuari", "Erabiltzaile-izena", "Benutzername", "Nom d'utilisateur"),
        "ENTRAR A LA AVENTURA" to t("ENTER THE ADVENTURE", "ENTRAR A L'AVENTURA", "ABENTURAN SARTU", "INS ABENTEUER", "ENTRER DANS L'AVENTURE"),
        "CONTINUAR CON GOOGLE" to t("CONTINUE WITH GOOGLE", "CONTINUAR AMB GOOGLE", "JARRAITU GOOGLE-REKIN", "MIT GOOGLE FORTFAHREN", "CONTINUER AVEC GOOGLE"),
        "¿Nuevo aventurero? Crea tu cuenta" to t("New adventurer? Create your account", "Nou aventurer? Crea el teu compte", "Abenturazale berria? Sortu kontua", "Neuer Abenteurer? Konto erstellen", "Nouvel aventurier ? Cree ton compte"),
        "Registrarse" to t("Register", "Registrar-se", "Erregistratu", "Registrieren", "S'inscrire"),
        "Crear cuenta" to t("Create account", "Crear compte", "Sortu kontua", "Konto erstellen", "Creer un compte"),
        "¿Ya tienes cuenta? Inicia sesión" to t("Already have an account? Sign in", "Ja tens compte? Inicia sessio", "Baduzu kontua? Hasi saioa", "Du hast schon ein Konto? Anmelden", "Tu as deja un compte ? Connecte-toi"),
        "Introduce un nombre visible." to t("Enter a display name.", "Introdueix un nom visible.", "Sartu bistaratzeko izena.", "Gib einen Anzeigenamen ein.", "Saisis un nom affiche."),
        "El nombre de usuario debe tener al menos 3 caracteres." to t("The username must be at least 3 characters long.", "El nom d'usuari ha de tenir almenys 3 caracters.", "Erabiltzaile-izenak gutxienez 3 karaktere izan behar ditu.", "Der Benutzername muss mindestens 3 Zeichen lang sein.", "Le nom d'utilisateur doit contenir au moins 3 caracteres."),
        "Las contraseñas no coinciden." to t("Passwords do not match.", "Les contrasenyes no coincideixen.", "Pasahitzak ez datoz bat.", "Die Passwörter stimmen nicht überein.", "Les mots de passe ne correspondent pas."),
        "¡Revisa tu correo! Te hemos enviado un email de verificación." to t("Check your email! We sent you a verification email.", "Revisa el correu! T'hem enviat un email de verificacio.", "Begiratu posta! Egiaztapen-mezu bat bidali dizugu.", "Prufe deine E-Mails! Wir haben dir eine Bestatigungs-E-Mail gesendet.", "Verifie ton e-mail ! Nous t'avons envoye un e-mail de verification."),

        "Mis gremios" to t("My guilds", "Els meus gremis", "Nire gremioak", "Meine Gilden", "Mes guildes"),
        "Todavía no perteneces a ningún gremio." to t("You do not belong to any guild yet.", "Encara no pertanys a cap gremi.", "Oraindik ez zara gremio bateko kide.", "Du gehorst noch keiner Gilde an.", "Tu n'appartiens encore a aucune guilde."),
        "Buscar gremios" to t("Search guilds", "Buscar gremis", "Bilatu gremioak", "Gilden suchen", "Rechercher des guildes"),
        "Solo puedes pertenecer a un gremio a la vez" to t("You can only belong to one guild at a time", "Nomes pots pertanyer a un gremi alhora", "Aldi berean gremio bakarreko kide izan zaitezke", "Du kannst nur einer Gilde gleichzeitig angehoren", "Tu ne peux appartenir qu'a une guilde a la fois"),
        "Crear gremio" to t("Create guild", "Crear gremi", "Sortu gremioa", "Gilde erstellen", "Creer une guilde"),
        "Nombre" to t("Name", "Nom", "Izena", "Name", "Nom"),
        "Descripción" to t("Description", "Descripcio", "Deskribapena", "Beschreibung", "Description"),
        "Color de acento" to t("Accent color", "Color d'accent", "Azentu kolorea", "Akzentfarbe", "Couleur d'accent"),
        "Color de banner" to t("Banner color", "Color del baner", "Banner kolorea", "Bannerfarbe", "Couleur de banniere"),
        "Tu gremio" to t("Your guild", "El teu gremi", "Zure gremioa", "Deine Gilde", "Ta guilde"),
        "Completo" to t("Full", "Complet", "Beteta", "Voll", "Complet"),
        "Unirme" to t("Join", "Unir-me", "Bat egin", "Beitreten", "Rejoindre"),

        "Mi perfil" to t("My profile", "El meu perfil", "Nire profila", "Mein Profil", "Mon profil"),
        "Perfil del amigo" to t("Friend profile", "Perfil de l'amic", "Lagunaren profila", "Freundesprofil", "Profil de l'ami"),
        "Cargando perfil..." to t("Loading profile...", "Carregant perfil...", "Profila kargatzen...", "Profil wird geladen...", "Chargement du profil..."),
        "Nombre visible" to t("Display name", "Nom visible", "Bistaratzeko izena", "Anzeigename", "Nom affiche"),
        "Biografía" to t("Bio", "Biografia", "Biografia", "Biografie", "Biographie"),
        "Elegir foto" to t("Choose photo", "Triar foto", "Aukeratu argazkia", "Foto auswahlen", "Choisir une photo"),
        "Colores del perfil" to t("Profile colors", "Colors del perfil", "Profilaren koloreak", "Profilfarben", "Couleurs du profil"),
        "Guardar perfil" to t("Save profile", "Desar perfil", "Gorde profila", "Profil speichern", "Enregistrer le profil"),
        "Abrir chat privado" to t("Open private chat", "Obrir xat privat", "Ireki txat pribatua", "Privaten Chat offnen", "Ouvrir le chat prive"),
        "Salas de personajes" to t("Character rooms", "Sales de personatges", "Pertsonaien gelak", "Charakterraume", "Salles des personnages"),
        "Este amigo todavía no tiene personajes visibles." to t("This friend does not have visible characters yet.", "Aquest amic encara no te personatges visibles.", "Lagun honek oraindik ez du pertsonaia ikusgarririk.", "Dieser Freund hat noch keine sichtbaren Charaktere.", "Cet ami n'a pas encore de personnages visibles."),
        "Visitar sala" to t("Visit room", "Visitar sala", "Bisita gela", "Raum besuchen", "Visiter la salle"),
        "En línea" to t("Online", "En linia", "Linean", "Online", "En ligne"),

        "Mis amigos" to t("My friends", "Els meus amics", "Nire lagunak", "Meine Freunde", "Mes amis"),
        "Todavía no tienes amigos." to t("You do not have friends yet.", "Encara no tens amics.", "Oraindik ez duzu lagunik.", "Du hast noch keine Freunde.", "Tu n'as pas encore d'amis."),
        "Buscar aventureros" to t("Search adventurers", "Buscar aventurers", "Bilatu abenturazaleak", "Abenteurer suchen", "Rechercher des aventuriers"),
        "Buscar por nombre o usuario" to t("Search by name or username", "Buscar per nom o usuari", "Bilatu izenez edo erabiltzailez", "Nach Name oder Benutzer suchen", "Rechercher par nom ou utilisateur"),
        "Ej: unai_gm" to t("Ex: unai_gm", "Ex: unai_gm", "Adib: unai_gm", "Bsp.: unai_gm", "Ex : unai_gm"),
        "Enviar solicitud" to t("Send request", "Enviar sol-licitud", "Bidali eskaera", "Anfrage senden", "Envoyer la demande"),
        "Escribe un mensaje..." to t("Write a message...", "Escriu un missatge...", "Idatzi mezu bat...", "Nachricht schreiben...", "Ecris un message..."),

        "Opciones de accesibilidad" to t("Accessibility options", "Opcions d'accessibilitat", "Irisgarritasun aukerak", "Barrierefreiheitsoptionen", "Options d'accessibilite"),
        "Opciones de usabilidad" to t("Usability options", "Opcions d'usabilitat", "Erabilgarritasun aukerak", "Bedienoptionen", "Options d'utilisabilite"),
        "Configura ayudas visuales y control por voz." to t("Configure visual aids and voice control.", "Configura ajudes visuals i control per veu.", "Konfiguratu ikusmen-laguntzak eta ahots-kontrola.", "Konfiguriere visuelle Hilfen und Sprachsteuerung.", "Configure les aides visuelles et le controle vocal."),
        "Configura ayudas visuales y manejo por voz de la aplicación." to t("Configure visual aids and voice handling for the app.", "Configura ajudes visuals i maneig per veu de l'aplicacio.", "Konfiguratu aplikazioaren ikusmen-laguntzak eta ahots bidezko erabilera.", "Konfiguriere visuelle Hilfen und Sprachbedienung der App.", "Configure les aides visuelles et le controle vocal de l'application."),
        "Modo daltónico" to t("Color-blind mode", "Mode daltonic", "Daltoniko modua", "Farbenblindheitsmodus", "Mode daltonien"),
        "Aplicar filtro" to t("Apply filter", "Aplicar filtre", "Aplikatu iragazkia", "Filter anwenden", "Appliquer le filtre"),
        "Control por voz" to t("Voice control", "Control per veu", "Ahots-kontrola", "Sprachsteuerung", "Controle vocal"),
        "Permite navegar y dictar acciones mediante órdenes habladas." to t("Allows navigation and action dictation through spoken commands.", "Permet navegar i dictar accions amb ordres parlades.", "Ahots-aginduen bidez nabigatzeko eta ekintzak diktatzeko aukera ematen du.", "Ermoglicht Navigation und Aktionsdiktat per Sprachbefehl.", "Permet de naviguer et de dicter des actions avec des commandes vocales."),
        "Sin filtro" to t("No filter", "Sense filtre", "Iragazkirik gabe", "Kein Filter", "Sans filtre"),
        "Visión de color estándar" to t("Standard color vision", "Visio de color estandard", "Kolore ikusmen estandarra", "Standard-Farbsehen", "Vision des couleurs standard"),
        "Protanopía" to t("Protanopia", "Protanopia", "Protanopia", "Protanopie", "Protanopie"),
        "Dificultad para distinguir tonos rojos" to t("Difficulty distinguishing red tones", "Dificultat per distingir tons vermells", "Tonu gorriak bereizteko zailtasuna", "Schwierigkeit, Rottone zu unterscheiden", "Difficulte a distinguer les tons rouges"),
        "Deuteranopía" to t("Deuteranopia", "Deuteranopia", "Deuteranopia", "Deuteranopie", "Deuteranopie"),
        "Dificultad para distinguir tonos verdes" to t("Difficulty distinguishing green tones", "Dificultat per distingir tons verds", "Tonu berdeak bereizteko zailtasuna", "Schwierigkeit, Gruntone zu unterscheiden", "Difficulte a distinguer les tons verts"),
        "Tritanopía" to t("Tritanopia", "Tritanopia", "Tritanopia", "Tritanopie", "Tritanopie"),
        "Dificultad para distinguir tonos azules" to t("Difficulty distinguishing blue tones", "Dificultat per distingir tons blaus", "Tonu urdinak bereizteko zailtasuna", "Schwierigkeit, Blautone zu unterscheiden", "Difficulte a distinguer les tons bleus"),
        "Acromatopsia" to t("Achromatopsia", "Acromatopsia", "Akromatopsia", "Achromatopsie", "Achromatopsie"),
        "Visión en escala de grises (monocromática)" to t("Grayscale vision (monochromatic)", "Visio en escala de grisos (monocromatica)", "Gris eskalako ikusmena (monokromatikoa)", "Graustufen-Sehen (monochromatisch)", "Vision en niveaux de gris (monochromatique)"),
        "Ejemplos de órdenes" to t("Command examples", "Exemples d'ordres", "Agindu adibideak", "Befehlsbeispiele", "Exemples de commandes"),
        "• Abre lista de amigos" to t("• Open friends list", "• Obre la llista d'amics", "• Ireki lagunen zerrenda", "• Freundesliste offnen", "• Ouvre la liste d'amis"),
        "• Crear gremio" to t("• Create guild", "• Crear gremi", "• Sortu gremioa", "• Gilde erstellen", "• Creer une guilde"),
        "• Desactiva la voz" to t("• Disable voice", "• Desactiva la veu", "• Desaktibatu ahotsa", "• Stimme deaktivieren", "• Desactive la voix"),
        "• Activa protanopía" to t("• Enable protanopia", "• Activa protanopia", "• Aktibatu protanopia", "• Protanopie aktivieren", "• Active la protanopie"),
        "• Colores normales" to t("• Normal colors", "• Colors normals", "• Kolore normalak", "• Normale Farben", "• Couleurs normales"),
        "• Abre la partida de A" to t("• Open A's game", "• Obre la partida d'A", "• Ireki A-ren partida", "• Spiel von A offnen", "• Ouvre la partie de A"),
        "• Abre inventario de A" to t("• Open A's inventory", "• Obre l'inventari d'A", "• Ireki A-ren inbentarioa", "• Inventar von A offnen", "• Ouvre l'inventaire de A"),
        "• Vuelve atrás" to t("• Go back", "• Torna enrere", "• Itzuli atzera", "• Zuruckgehen", "• Reviens en arriere"),
        "• Dentro de una aventura: Ataco con mi espada" to t("• Inside an adventure: I attack with my sword", "• Dins d'una aventura: Ataco amb la meva espasa", "• Abentura batean: nire ezpatarekin erasotzen dut", "• In einem Abenteuer: Ich greife mit meinem Schwert an", "• Dans une aventure : j'attaque avec mon epee"),

        "Overview" to t("Overview", "Resum", "Laburpena", "Übersicht", "Aperçu", "Resumo"),
        "Featured members" to t("Featured members", "Membres destacats", "Kide nabarmenak", "Vorgestellte Mitglieder", "Membres en vedette", "Membros destacados"),
        "Miembro" to t("Member", "Membre", "Kidea", "Mitglied", "Membre", "Membro"),
        "Líder" to t("Leader", "Líder", "Liderra", "Anführer", "Chef", "Líder"),
        "Miembros" to t("Members", "Membres", "Kideak", "Mitglieder", "Membres", "Membros"),
        "Desde aquí puedes hablar con el gremio, revisar sus miembros y abrir chats privados con ellos." to t("From here you can talk with the guild, review its members and open private chats with them.", "Des d'aquí pots parlar amb el gremi, revisar-ne els membres i obrir xats privats amb ells.", "Hemendik gremioarekin hitz egin, kideak ikusi eta haiekin txat pribatuak ireki ditzakezu.", "Von hier aus kannst du mit der Gilde sprechen, ihre Mitglieder prüfen und private Chats öffnen.", "D'ici, tu peux parler avec la guilde, consulter ses membres et ouvrir des chats privés avec eux.", "Desde aquí podes falar co gremio, revisar os seus membros e abrir chats privados con eles."),
        "Stats Totales" to t("Total stats", "Estadístiques totals", "Estatistika guztira", "Gesamtwerte", "Stats totales", "Estatísticas totais"),
        "HP Máximo" to t("Max HP", "HP màxim", "Gehieneko HP", "Max. HP", "PV max", "HP máximo"),
        "Fuerza" to t("Strength", "Força", "Indarra", "Stärke", "Force", "Forza"),
        "Combate" to t("Combat", "Combat", "Borroka", "Kampf", "Combat", "Combate"),
        "Exploración" to t("Exploration", "Exploració", "Esplorazioa", "Erkundung", "Exploration", "Exploración"),
        "Progresión" to t("Progression", "Progressió", "Aurrerapena", "Fortschritt", "Progression", "Progresión"),
        "Los mejores héroes de todos los reinos" to t("The best heroes from all realms", "Els millors herois de tots els regnes", "Erreinu guztietako heroi onenak", "Die besten Helden aller Reiche", "Les meilleurs héros de tous les royaumes", "Os mellores heroes de todos os reinos"),
        "FICHA DE AVENTURERO" to t("ADVENTURER SHEET", "FITXA D'AVENTURER", "ABENTURAZALEAREN FITXA", "ABENTEURERBOGEN", "FICHE D'AVENTURIER", "FICHA DE AVENTUREIRO"),
        "Atributos" to t("Attributes", "Atributs", "Atributuak", "Attribute", "Attributs", "Atributos"),
        "Combate y partida" to t("Combat and game", "Combat i partida", "Borroka eta partida", "Kampf und Spiel", "Combat et partie", "Combate e partida"),
        "Competencia" to t("Proficiency", "Competència", "Gaitasuna", "Übung", "Maîtrise", "Competencia"),
        "Iniciativa" to t("Initiative", "Iniciativa", "Ekimena", "Initiative", "Initiative", "Iniciativa"),
        "Última partida" to t("Last game", "Última partida", "Azken partida", "Letztes Spiel", "Dernière partie", "Última partida"),
        "Rasgos físicos" to t("Physical traits", "Trets físics", "Ezaugarri fisikoak", "Körperliche Merkmale", "Traits physiques", "Trazos físicos"),
        "Descargar ficha en PDF" to t("Download sheet as PDF", "Descarregar fitxa en PDF", "Deskargatu fitxa PDF gisa", "Bogen als PDF herunterladen", "Télécharger la fiche en PDF", "Descargar ficha en PDF"),
        "Sala" to t("Room", "Sala", "Gela", "Raum", "Salle", "Sala"),
        "Aventura" to t("Adventure", "Aventura", "Abentura", "Abenteuer", "Aventure", "Aventura"),
        "Bestiario" to t("Bestiary", "Bestiari", "Bestiarioa", "Bestiarium", "Bestiaire", "Bestiario"),
        "Inventario" to t("Inventory", "Inventari", "Inbentarioa", "Inventar", "Inventaire", "Inventario"),
        "Diario" to t("Journal", "Diari", "Egunkaria", "Tagebuch", "Journal", "Diario"),
        "Mapa" to t("Map", "Mapa", "Mapa", "Karte", "Carte", "Mapa"),
        "¿Qué quieres hacer?" to t("What do you want to do?", "Què vols fer?", "Zer egin nahi duzu?", "Was möchtest du tun?", "Que veux-tu faire ?", "Que queres facer?"),
        "Observar el entorno" to t("Look around", "Observar l'entorn", "Ingurua behatu", "Umgebung beobachten", "Observer les environs", "Observar a contorna"),
        "Descansar" to t("Rest", "Descansar", "Atseden hartu", "Ausruhen", "Se reposer", "Descansar"),
        "Preparando aventura para" to t("Preparing adventure for", "Preparant aventura per a", "Abentura prestatzen honentzat", "Abenteuer wird vorbereitet für", "Préparation de l'aventure pour", "Preparando aventura para"),
        "Error de conexión" to t("Connection error", "Error de connexió", "Konexio-errorea", "Verbindungsfehler", "Erreur de connexion", "Erro de conexión"),

        "Permiso de notificaciones" to t("Notification permission", "Permis de notificacions", "Jakinarazpen baimena", "Benachrichtigungsberechtigung", "Autorisation de notifications"),
        "Permiso de cámara" to t("Camera permission", "Permis de camera", "Kamera baimena", "Kameraberechtigung", "Autorisation de camera"),
        "Permiso de ubicación" to t("Location permission", "Permis d'ubicacio", "Kokapen baimena", "Standortberechtigung", "Autorisation de localisation"),
        "Ubicación en segundo plano" to t("Background location", "Ubicacio en segon pla", "Atzeko planoko kokapena", "Standort im Hintergrund", "Localisation en arriere-plan"),
        "Permiso" to t("Permission", "Permis", "Baimena", "Berechtigung", "Autorisation"),
        "Se usa para avisos de inactividad, ranking y eventos del juego." to t("Used for inactivity, ranking and game event alerts.", "S'usa per avisos d'inactivitat, ranquing i esdeveniments del joc.", "Jarduera-ezaren, rankingaren eta joko-gertaeren abisuetarako erabiltzen da.", "Wird fur Inaktivitat, Rangliste und Spielereignisse genutzt.", "Utilisee pour les alertes d'inactivite, de classement et d'evenements du jeu."),
        "Se usa para escanear QR, reconocer texto y activar funciones contextuales." to t("Used to scan QR codes, recognize text and enable contextual features.", "S'usa per escanejar QR, reconeixer text i activar funcions contextuals.", "QR kodeak eskaneatzeko, testua ezagutzeko eta funtzio testuingurukoak aktibatzeko erabiltzen da.", "Wird zum Scannen von QR-Codes, Texterkennung und Kontextfunktionen genutzt.", "Utilisee pour scanner les QR, reconnaitre du texte et activer des fonctions contextuelles."),
        "Se usa para funciones contextuales, detección de lugares cercanos y exploración del mundo." to t("Used for contextual features, nearby place detection and world exploration.", "S'usa per funcions contextuals, deteccio de llocs propers i exploracio del mon.", "Funtzio testuingurukoetarako, inguruko lekuak hautemateko eta mundua esploratzeko erabiltzen da.", "Wird fur Kontextfunktionen, nahe Orte und Welterkundung genutzt.", "Utilisee pour les fonctions contextuelles, la detection de lieux proches et l'exploration du monde."),
        "La aplicación necesita este permiso para funcionar correctamente." to t("The app needs this permission to work properly.", "L'aplicacio necessita aquest permis per funcionar correctament.", "Aplikazioak baimen hau behar du ondo funtzionatzeko.", "Die App braucht diese Berechtigung, um richtig zu funktionieren.", "L'application a besoin de cette autorisation pour fonctionner correctement."),

        "🏆 LOGRO DESBLOQUEADO" to t("🏆 ACHIEVEMENT UNLOCKED", "🏆 ASSOLIMENT DESBLOQUEJAT", "🏆 LORPENA DESBLOKEATUTA", "🏆 ERFOLG FREIGESCHALTET", "🏆 SUCCES DEBLOQUE"),
        "✅ MISIÓN COMPLETADA" to t("✅ QUEST COMPLETED", "✅ MISSIO COMPLETADA", "✅ MISIOA OSATUTA", "✅ QUEST ABGESCHLOSSEN", "✅ MISSION TERMINEE"),
        "Recompensa conseguida" to t("Reward obtained", "Recompensa aconseguida", "Saria lortuta", "Belohnung erhalten", "Recompense obtenue"),
        "Aceptar Misión" to t("Accept Quest", "Acceptar missio", "Onartu misioa", "Quest annehmen", "Accepter la mission"),
        "Salón de la Fama" to t("Hall of Fame", "Salo de la fama", "Ospearen aretoa", "Ruhmeshalle", "Temple de la renommee"),
        "Misiones" to t("Quests", "Missions", "Misioak", "Quests", "Missions"),
        "Progreso" to t("Progress", "Progres", "Aurrerapena", "Fortschritt", "Progression"),

        "VICTORIA" to t("VICTORY", "VICTORIA", "GARAIPENA", "SIEG", "VICTOIRE"),
        "DERROTA" to t("DEFEAT", "DERROTA", "PORROTA", "NIEDERLAGE", "DEFAITE"),
        "HUIDA EXITOSA" to t("SUCCESSFUL ESCAPE", "FUGIDA AMB EXIT", "IHES ARRAKASTATSUA", "ERFOLGREICHE FLUCHT", "FUITE REUSSIE"),
        "Has derrotado al enemigo." to t("You defeated the enemy.", "Has derrotat l'enemic.", "Etsaia garaitu duzu.", "Du hast den Gegner besiegt.", "Tu as vaincu l'ennemi."),
        "Has caído en combate." to t("You fell in combat.", "Has caigut en combat.", "Borrokan erori zara.", "Du bist im Kampf gefallen.", "Tu es tombe au combat."),
        "Has escapado del combate con vida." to t("You escaped the fight alive.", "Has escapat del combat amb vida.", "Borrokatik bizirik ihes egin duzu.", "Du bist lebend entkommen.", "Tu as echappe au combat vivant."),
        "TU TURNO" to t("YOUR TURN", "EL TEU TORN", "ZURE TXANDA", "DEIN ZUG", "TON TOUR"),
        "ENEMIGO" to t("ENEMY", "ENEMIC", "ETSAIA", "GEGNER", "ENNEMI"),
        "TIRANDO…" to t("ROLLING...", "TIRANT...", "BOTATZEN...", "WURFELT...", "LANCEMENT..."),
        "HUIDA" to t("ESCAPE", "FUGIDA", "IHESA", "FLUCHT", "FUITE"),
        "💀 FALLO CRÍTICO" to t("💀 CRITICAL FAILURE", "💀 FALLADA CRITICA", "💀 HUTS KRITIKOA", "💀 KRITISCHER FEHLSCHLAG", "💀 ECHEC CRITIQUE"),

        "Primera Sangre" to t("First Blood", "Primera sang", "Lehen odola", "Erstes Blut", "Premier sang"),
        "Gana tu primer combate." to t("Win your first combat.", "Guanya el teu primer combat.", "Irabazi zure lehen borroka.", "Gewinne deinen ersten Kampf.", "Gagne ton premier combat."),
        "Guerrero Veterano" to t("Veteran Warrior", "Guerrer veterà", "Gerlaria beteranoa", "Veteranenkrieger", "Guerrier veteran"),
        "Gana 10 combates." to t("Win 10 combats.", "Guanya 10 combats.", "Irabazi 10 borroka.", "Gewinne 10 Kampfe.", "Gagne 10 combats."),
        "Golpe Crítico" to t("Critical Strike", "Cop critic", "Kolpe kritikoa", "Kritischer Treffer", "Coup critique"),
        "Consigue un golpe crítico en combate." to t("Land a critical hit in combat.", "Aconsegueix un cop critic en combat.", "Lortu kolpe kritiko bat borrokan.", "Erziele einen kritischen Treffer im Kampf.", "Obtiens un coup critique en combat."),
        "Explorador Novato" to t("Novice Explorer", "Explorador novell", "Esploratzaile hasiberria", "Anfanger-Entdecker", "Explorateur novice"),
        "Descubre tu primera ubicación en el mapa." to t("Discover your first map location.", "Descobreix la teva primera ubicacio al mapa.", "Aurkitu zure lehen kokapena mapan.", "Entdecke deinen ersten Ort auf der Karte.", "Decouvre ton premier lieu sur la carte."),
        "¡Subiste de Nivel!" to t("Level Up!", "Has pujat de nivell!", "Mailaz igo zara!", "Stufenaufstieg!", "Niveau superieur !"),
        "Sube de nivel por primera vez." to t("Level up for the first time.", "Puja de nivell per primer cop.", "Igo mailaz lehen aldiz.", "Steige zum ersten Mal auf.", "Monte de niveau pour la premiere fois."),
        "Primeros Pasos" to t("First Steps", "Primers passos", "Lehen urratsak", "Erste Schritte", "Premiers pas"),
        "Comienza tu aventura enviando tus primeras acciones al Dungeon Master." to t("Begin your adventure by sending your first actions to the Dungeon Master.", "Comenca l'aventura enviant les primeres accions al Dungeon Master.", "Hasi abentura Dungeon Masterrari lehen ekintzak bidaliz.", "Beginne dein Abenteuer mit deinen ersten Aktionen an den Dungeon Master.", "Commence ton aventure en envoyant tes premieres actions au Dungeon Master."),
        "El Camino del Guerrero" to t("The Warrior's Path", "El cami del guerrer", "Gerlarien bidea", "Der Weg des Kriegers", "La voie du guerrier"),
        "Demuestra tu valor en combate ganando batallas." to t("Prove your worth in combat by winning battles.", "Demostra el teu valor en combat guanyant batalles.", "Erakutsi zure ausardia borrokan guduak irabaziz.", "Beweise deinen Wert im Kampf, indem du Schlachten gewinnst.", "Prouve ta valeur au combat en gagnant des batailles."),
        "No hay aventureros todavía." to t("No adventurers yet.", "Encara no hi ha aventurers.", "Oraindik ez dago abenturazalerik.", "Noch keine Abenteurer.", "Il n'y a pas encore d'aventuriers."),
        "⚔️ Ranking Mundial" to t("⚔️ Global Ranking", "⚔️ Ranquing mundial", "⚔️ Munduko sailkapena", "⚔️ Weltrangliste", "⚔️ Classement mondial"),
        "Convocando a los héroes..." to t("Summoning the heroes...", "Convocant els heroes...", "Heroiak deitzen...", "Die Helden werden gerufen...", "Convocation des heros..."),
        "Nadie ha reclamado el trono aún.\n¡Sé el primero en la historia!" to t("No one has claimed the throne yet.\nBe the first in history!", "Ningu no ha reclamat el tron encara.\nSigues el primer de la historia!", "Inork ez du oraindik tronua aldarrikatu.\nIzan zaitez historiako lehena!", "Noch hat niemand den Thron beansprucht.\nSei der Erste der Geschichte!", "Personne n'a encore reclame le trone.\nSois le premier dans l'histoire !"),
        "✨ Hall of Fame" to t("✨ Hall of Fame", "✨ Salo de la fama", "✨ Ospearen aretoa", "✨ Ruhmeshalle", "✨ Temple de la renommee"),
        "Logro secreto — ¡Descúbrelo!" to t("Secret achievement — Discover it!", "Assoliment secret — Descobreix-lo!", "Lorpen sekretua — Deskubritu ezazu!", "Geheimer Erfolg — Entdecke ihn!", "Succes secret — Decouvre-le !"),
        "⚡ En Progreso" to t("⚡ In Progress", "⚡ En progres", "⚡ Martxan", "⚡ In Bearbeitung", "⚡ En cours"),
        "📋 Disponibles" to t("📋 Available", "📋 Disponibles", "📋 Erabilgarri", "📋 Verfugbar", "📋 Disponibles"),
        "✅ Completadas" to t("✅ Completed", "✅ Completades", "✅ Osatuta", "✅ Abgeschlossen", "✅ Terminees"),
        "CREAR AVENTURERO" to t("CREATE ADVENTURER", "CREAR AVENTURER", "ABENTURAZALEA SORTU", "ABENTEURER ERSTELLEN", "CREER AVENTURIER"),
        "Forja tu leyenda" to t("Forge your legend", "Forja la teva llegenda", "Landu zure kondaira", "Schmiede deine Legende", "Forge ta legende"),
        "Hoja del Aventurero" to t("Adventurer sheet", "Full de l'aventurer", "Abenturazalearen orria", "Abenteurerbogen", "Feuille de l'aventurier"),
        "Ej: Unai García" to t("Ex: Unai Garcia", "Ex: Unai Garcia", "Adib: Unai Garcia", "Bsp.: Unai Garcia", "Ex : Unai Garcia"),
        "Será el identificador para buscarte." to t("This will be the identifier others use to find you.", "Sera l'identificador per buscar-te.", "Zu bilatzeko erabiliko den identifikatzailea izango da.", "Dies ist die Kennung, mit der man dich suchen kann.", "Ce sera l'identifiant pour te rechercher."),
        "Pergamino Mágico (Email)" to t("Magic scroll (Email)", "Pergami magic (Email)", "Pergamino magikoa (Emaila)", "Magische Schriftrolle (E-Mail)", "Parchemin magique (email)"),
        "Sello Secreto (Contraseña)" to t("Secret seal (Password)", "Segell secret (Contrasenya)", "Zigilu sekretua (Pasahitza)", "Geheimes Siegel (Passwort)", "Sceau secret (Mot de passe)"),
        "Confirmar Sello" to t("Confirm seal", "Confirmar segell", "Berretsi zigilua", "Siegel bestatigen", "Confirmer le sceau"),
        "FORJAR DESTINO" to t("FORGE DESTINY", "FORJAR DESTI", "PATUA LANDU", "SCHICKSAL SCHMIEDEN", "FORGER LE DESTIN"),
        "← Volver al portal" to t("← Back to the portal", "← Tornar al portal", "← Atarira itzuli", "← Zuruck zum Portal", "← Retour au portail"),
        "Al crear una cuenta, aceptas los términos del pacto mágico y las reglas de la taberna" to t("By creating an account, you accept the terms of the magic pact and the tavern rules", "En crear un compte, acceptes els termes del pacte magic i les regles de la taverna", "Kontu bat sortzean, itun magikoaren baldintzak eta tabernako arauak onartzen dituzu", "Mit dem Erstellen eines Kontos akzeptierst du die Bedingungen des magischen Pakts und die Regeln der Taverne", "En creant un compte, tu acceptes les termes du pacte magique et les regles de la taverne"),
        "Asigna los bonos raciales: +2 y +1 a los atributos que prefieras." to t("Assign the racial bonuses: +2 and +1 to the attributes you prefer.", "Assigna els bonus racials: +2 i +1 als atributs que prefereixis.", "Esleitu arraza-bonuak: +2 eta +1 nahiago dituzun atributuei.", "Verteile die Volksboni: +2 und +1 auf die Attribute deiner Wahl.", "Attribue les bonus raciaux : +2 et +1 aux attributs de ton choix."),
        "Escribe al menos 2 caracteres." to t("Type at least 2 characters.", "Escriu almenys 2 caracters.", "Idatzi gutxienez 2 karaktere.", "Gib mindestens 2 Zeichen ein.", "Ecris au moins 2 caracteres."),
        "No se encontraron usuarios." to t("No users found.", "No s'han trobat usuaris.", "Ez da erabiltzailerik aurkitu.", "Keine Benutzer gefunden.", "Aucun utilisateur trouve."),
        "No hay solicitudes pendientes." to t("There are no pending requests.", "No hi ha sollicituds pendents.", "Ez dago zain dagoen eskaerarik.", "Es gibt keine ausstehenden Anfragen.", "Il n'y a aucune demande en attente."),
        "No tienes solicitudes pendientes." to t("You have no pending requests.", "No tens sollicituds pendents.", "Ez duzu zain dagoen eskaerarik.", "Du hast keine ausstehenden Anfragen.", "Tu n'as aucune demande en attente."),
        "Aceptar" to t("Accept", "Acceptar", "Onartu", "Annehmen", "Accepter"),
        "Rechazar" to t("Reject", "Rebutjar", "Baztertu", "Ablehnen", "Refuser"),
        "Cargando gremio..." to t("Loading guild...", "Carregant gremi...", "Gremioa kargatzen...", "Gilde wird geladen...", "Chargement de la guilde..."),
        "Explorar" to t("Explore", "Explorar", "Arakatu", "Erkunden", "Explorer"),
        "Sin descripción todavía." to t("No description yet.", "Encara no hi ha descripcio.", "Oraindik ez dago deskribapenik.", "Noch keine Beschreibung.", "Pas encore de description."),
        "Mi gremio" to t("My guild", "El meu gremi", "Nire gremioa", "Meine Gilde", "Ma guilde"),
        "No tienes personajes disponibles." to t("You have no available characters.", "No tens personatges disponibles.", "Ez duzu pertsonaia erabilgarririk.", "Du hast keine verfugbaren Charaktere.", "Tu n'as pas de personnages disponibles."),
        "Gremio completo" to t("Guild full", "Gremi complet", "Gremioa beteta", "Gilde voll", "Guilde complete"),
        "Unirme al gremio" to t("Join guild", "Unir-me al gremi", "Gremioan sartu", "Gilde beitreten", "Rejoindre la guilde"),
        "Abrir chat" to t("Open chat", "Obrir xat", "Ireki txata", "Chat offnen", "Ouvrir le chat"),
        "Ver miembros" to t("View members", "Veure membres", "Ikusi kideak", "Mitglieder ansehen", "Voir les membres"),
        "Resumen" to t("Summary", "Resum", "Laburpena", "Zusammenfassung", "Resume"),
        "Jefe final" to t("Final boss", "Cap final", "Azken nagusia", "Endboss", "Boss final"),
        "Vista general" to t("Overview", "Vista general", "Ikuspegi orokorra", "Uberblick", "Vue d'ensemble"),
        "Integrantes destacados" to t("Featured members", "Membres destacats", "Kide nabarmenak", "Ausgewahlte Mitglieder", "Membres en vedette"),
        "No hay integrantes visibles todavía." to t("There are no visible members yet.", "Encara no hi ha membres visibles.", "Oraindik ez dago kide ikusgarririk.", "Es gibt noch keine sichtbaren Mitglieder.", "Il n'y a pas encore de membres visibles."),
        "Chat del gremio" to t("Guild chat", "Xat del gremi", "Gremioko txata", "Gildenchat", "Chat de guilde"),
        "Debes unirte al gremio para leer y escribir en el chat." to t("You must join the guild to read and write in the chat.", "Has d'unir-te al gremi per llegir i escriure al xat.", "Gremioan sartu behar duzu txatean irakurri eta idazteko.", "Du musst der Gilde beitreten, um im Chat zu lesen und zu schreiben.", "Tu dois rejoindre la guilde pour lire et ecrire dans le chat."),
        "Todavía no hay mensajes. Rompe el hielo." to t("There are no messages yet. Break the ice.", "Encara no hi ha missatges. Trenca el gel.", "Oraindik ez dago mezurik. Hautsi izotza.", "Es gibt noch keine Nachrichten. Brich das Eis.", "Il n'y a pas encore de messages. Brise la glace."),
        "Escribe al gremio..." to t("Write to the guild...", "Escriu al gremi...", "Idatzi gremioari...", "An die Gilde schreiben...", "Ecrire a la guilde..."),
        "Miembros del gremio" to t("Guild members", "Membres del gremi", "Gremioko kideak", "Gildenmitglieder", "Membres de la guilde"),
        "Pulsa sobre un miembro para abrir chat privado con él. Si eres líder, también puedes transferir el liderazgo." to t("Tap a member to open a private chat. If you are the leader, you can also transfer leadership.", "Prem sobre un membre per obrir-hi un xat privat. Si ets el lider, tambe pots transferir el lideratge.", "Sakatu kide bat txat pribatu bat irekitzeko. Liderra bazara, lidergoa ere transferi dezakezu.", "Tippe auf ein Mitglied, um einen privaten Chat zu offnen. Wenn du der Anfuhrer bist, kannst du auch die Fuhrung ubertragen.", "Appuie sur un membre pour ouvrir un chat prive. Si tu es le chef, tu peux aussi transferer le leadership."),
        "No hay miembros para mostrar." to t("There are no members to show.", "No hi ha membres per mostrar.", "Ez dago erakusteko kiderik.", "Es gibt keine Mitglieder zum Anzeigen.", "Il n'y a aucun membre a afficher."),
        "Jefe final del gremio" to t("Guild final boss", "Cap final del gremi", "Gremioko azken nagusia", "Gilden-Endboss", "Boss final de la guilde"),
        "Debes unirte al gremio para participar." to t("You must join the guild to participate.", "Has d'unir-te al gremi per participar.", "Gremioan sartu behar duzu parte hartzeko.", "Du musst der Gilde beitreten, um teilzunehmen.", "Tu dois rejoindre la guilde pour participer."),
        "Batalla en curso" to t("Battle in progress", "Batalla en curs", "Borroka martxan", "Kampf lauft", "Bataille en cours"),
        "Batalla terminada" to t("Battle finished", "Batalla acabada", "Borroka amaituta", "Kampf beendet", "Bataille terminee"),
        "Sala de espera" to t("Waiting room", "Sala d'espera", "Itxaron-gela", "Warteraum", "Salle d'attente"),
        "Turno del jefe" to t("Boss turn", "Torn del cap", "Nagusiaren txanda", "Zug des Bosses", "Tour du boss"),
        "Es tu turno" to t("It is your turn", "Es el teu torn", "Zure txanda da", "Du bist am Zug", "C'est ton tour"),
        "Sin turno activo" to t("No active turn", "Sense torn actiu", "Txanda aktiborik ez", "Kein aktiver Zug", "Aucun tour actif"),
        "Turno de otro miembro" to t("Another member's turn", "Torn d'un altre membre", "Beste kide baten txanda", "Zug eines anderen Mitglieds", "Tour d'un autre membre"),
        "Victoria del gremio" to t("Guild victory", "Victoria del gremi", "Gremioaren garaipena", "Sieg der Gilde", "Victoire de la guilde"),
        "El jefe ha ganado" to t("The boss has won", "El cap ha guanyat", "Nagusiak irabazi du", "Der Boss hat gewonnen", "Le boss a gagne"),
        "1) Elige personaje. 2) Pulsa listo. 3) El líder inicia la pelea cuando todos estén listos." to t("1) Choose a character. 2) Tap ready. 3) The leader starts the battle when everyone is ready.", "1) Tria personatge. 2) Prem llest. 3) El lider inicia la batalla quan tothom esta llest.", "1) Aukeratu pertsonaia. 2) Sakatu prest. 3) Liderrak borroka hasten du denak prest daudenean.", "1) Charakter wahlen. 2) Bereit drucken. 3) Der Anfuhrer startet den Kampf, wenn alle bereit sind.", "1) Choisis un personnage. 2) Appuie sur pret. 3) Le chef lance le combat quand tout le monde est pret."),
        "Participantes" to t("Participants", "Participants", "Parte-hartzaileak", "Teilnehmer", "Participants"),
        "Aún no hay nadie en la sala." to t("There is nobody in the room yet.", "Encara no hi ha ningu a la sala.", "Oraindik ez dago inor gelan.", "Es ist noch niemand im Raum.", "Il n'y a encore personne dans la salle."),
        "Listo" to t("Ready", "Llesta", "Prest", "Bereit", "Pret"),
        "Esperando" to t("Waiting", "Esperant", "Zain", "Wartet", "En attente"),
        "No listo" to t("Not ready", "No llest", "Ez prest", "Nicht bereit", "Pas pret"),
        "La pelea anterior ha terminado. Todos han vuelto a pulsar Listo y el líder ya puede iniciar otra." to t("The previous battle has ended. Everyone has pressed Ready again and the leader can now start another one.", "La batalla anterior ha acabat. Tothom ha tornat a premer Llest i el lider ja en pot iniciar una altra.", "Aurreko borroka amaitu da. Denek berriro Prest sakatu dute eta liderrak beste bat has dezake.", "Der vorherige Kampf ist beendet. Alle haben erneut auf Bereit gedruckt und der Anfuhrer kann nun einen weiteren starten.", "Le combat precedent est termine. Tout le monde a de nouveau appuye sur Pret et le chef peut en lancer un autre."),
        "La pelea anterior ha terminado. Cada participante debe pulsar Listo otra vez para restaurar su estado." to t("The previous battle has ended. Each participant must press Ready again to restore their status.", "La batalla anterior ha acabat. Cada participant ha de tornar a premer Llest per restaurar el seu estat.", "Aurreko borroka amaitu da. Parte-hartzaile bakoitzak berriro Prest sakatu behar du bere egoera berrezartzeko.", "Der vorherige Kampf ist beendet. Jeder Teilnehmer muss erneut auf Bereit drucken, um seinen Status wiederherzustellen.", "Le combat precedent est termine. Chaque participant doit appuyer de nouveau sur Pret pour restaurer son etat."),
        "Todos los participantes están listos. El líder puede iniciar la pelea." to t("All participants are ready. The leader can start the battle.", "Tots els participants estan llestos. El lider pot iniciar la batalla.", "Parte-hartzaile guztiak prest daude. Liderrak borroka has dezake.", "Alle Teilnehmer sind bereit. Der Anfuhrer kann den Kampf starten.", "Tous les participants sont prets. Le chef peut lancer le combat."),
        "Todavía no están todos listos." to t("Not everyone is ready yet.", "Encara no estan tots llestos.", "Oraindik ez daude denak prest.", "Noch sind nicht alle bereit.", "Tout le monde n'est pas encore pret."),
        "Empezar otra pelea" to t("Start another battle", "Comencar una altra batalla", "Beste borroka bat hasi", "Einen weiteren Kampf starten", "Commencer un autre combat"),
        "Empezar pelea" to t("Start battle", "Comencar batalla", "Borroka hasi", "Kampf starten", "Commencer le combat"),
        "Entrar en pelea" to t("Enter battle", "Entrar a la batalla", "Borrokan sartu", "Kampf betreten", "Entrer dans le combat"),
        "Salir de sala" to t("Leave room", "Sortir de la sala", "Gelatik irten", "Raum verlassen", "Quitter la salle"),
        "Reiniciar" to t("Reset", "Reiniciar", "Berrezarri", "Zurucksetzen", "Reinitialiser"),
        "Registro de batalla" to t("Battle log", "Registre de batalla", "Borroka-erregistroa", "Kampfprotokoll", "Journal de bataille"),
        "Salir" to t("Leave", "Sortir", "Irten", "Verlassen", "Quitter"),
        "Completado" to t("Completed", "Completat", "Osatuta", "Abgeschlossen", "Termine"),
        "Al Filo de la Muerte" to t("On the Edge of Death", "Al llindar de la mort", "Heriotzaren ertzean", "Am Rand des Todes", "Au bord de la mort"),
        "Sobrevive a un combate con 1 HP." to t("Survive a combat with 1 HP.", "Sobreviu a un combat amb 1 HP.", "Biziraun 1 HPrekin borroka batean.", "Uberlebe einen Kampf mit 1 HP.", "Survis a un combat avec 1 PV."),
        "Descubre 5 ubicaciones distintas." to t("Discover 5 different locations.", "Descobreix 5 ubicacions diferents.", "Aurkitu 5 kokapen desberdin.", "Entdecke 5 verschiedene Orte.", "Decouvre 5 lieux differents."),
        "Cartógrafo Real" to t("Royal Cartographer", "Cartograf reial", "Errege-kartografoa", "Koniglicher Kartograf", "Cartographe royal"),
        "Descubre 10 ubicaciones distintas." to t("Discover 10 different locations.", "Descobreix 10 ubicacions diferents.", "Aurkitu 10 kokapen desberdin.", "Entdecke 10 verschiedene Orte.", "Decouvre 10 lieux differents."),
        "Leyenda de los Caminos" to t("Legend of the Roads", "Llegenda dels camins", "Bideen kondaira", "Legende der Wege", "Legende des chemins"),
        "Descubre 20 ubicaciones distintas." to t("Discover 20 different locations.", "Descobreix 20 ubicacions diferents.", "Aurkitu 20 kokapen desberdin.", "Entdecke 20 verschiedene Orte.", "Decouvre 20 lieux differents."),
        "Héroe Prometedor" to t("Promising Hero", "Heroi prometedor", "Heroi itxaropentsua", "Vielversprechender Held", "Heros prometteur"),
        "Alcanza el nivel 3." to t("Reach level 3.", "Arriba al nivell 3.", "Iritsi 3. mailara.", "Erreiche Stufe 3.", "Atteins le niveau 3."),
        "Aventurero Consagrado" to t("Accomplished Adventurer", "Aventurer consagrat", "Abenturazale ospetsua", "Erfahrener Abenteurer", "Aventurier consacre"),
        "Alcanza el nivel 5." to t("Reach level 5.", "Arriba al nivell 5.", "Iritsi 5. mailara.", "Erreiche Stufe 5.", "Atteins le niveau 5."),
        "Héroe de Leyenda" to t("Legendary Hero", "Heroi de llegenda", "Kondairazko heroia", "Legendarer Held", "Heros legendaire"),
        "Alcanza el nivel 10." to t("Reach level 10.", "Arriba al nivell 10.", "Iritsi 10. mailara.", "Erreiche Stufe 10.", "Atteins le niveau 10."),
        "Nombre de Leyenda" to t("Legendary Name", "Nom de llegenda", "Kondairazko izena", "Legendarer Name", "Nom legendaire"),
        "Alcanza el nivel 15." to t("Reach level 15.", "Arriba al nivell 15.", "Iritsi 15. mailara.", "Erreiche Stufe 15.", "Atteins le niveau 15."),
        "Mochila Llena" to t("Backpack Full", "Motxilla plena", "Motxila betea", "Voller Rucksack", "Sac a dos plein"),
        "Encuentra tu primer objeto." to t("Find your first item.", "Troba el teu primer objecte.", "Aurkitu zure lehen objektua.", "Finde deinen ersten Gegenstand.", "Trouve ton premier objet."),
        "Comerciante Astuto" to t("Cunning Merchant", "Comerciant astut", "Merkatari azkarra", "Listiger Handler", "Marchand astucieux"),
        "Acumula 5 objetos en el inventario." to t("Accumulate 5 items in the inventory.", "Acumula 5 objectes a l'inventari.", "Metatu 5 objektu inbentarioan.", "Sammle 5 Gegenstande im Inventar.", "Accumule 5 objets dans l'inventaire."),
        "El Nacimiento de un Héroe" to t("The Birth of a Hero", "El naixement d'un heroi", "Heroi baten jaiotza", "Die Geburt eines Helden", "La naissance d'un heros"),
        "Crea tu primer personaje." to t("Create your first character.", "Crea el teu primer personatge.", "Sortu zure lehen pertsonaia.", "Erstelle deinen ersten Charakter.", "Cree ton premier personnage."),
        "Ficha Preparada" to t("Sheet Ready", "Fitxa preparada", "Fitxa prest", "Bogen bereit", "Fiche prete"),
        "Abre la ficha RPG visual de un personaje." to t("Open a character's visual RPG sheet.", "Obre la fitxa RPG visual d'un personatge.", "Ireki pertsonaia baten RPG fitxa bisuala.", "Offne den visuellen RPG-Bogen eines Charakters.", "Ouvre la fiche RPG visuelle d'un personnage."),
        "Aventurero Documentado" to t("Documented Adventurer", "Aventurer documentat", "Abenturazale dokumentatua", "Dokumentierter Abenteurer", "Aventurier documente"),
        "Exporta una ficha RPG en PDF." to t("Export an RPG sheet as PDF.", "Exporta una fitxa RPG en PDF.", "Esportatu RPG fitxa bat PDF gisa.", "Exportiere einen RPG-Bogen als PDF.", "Exporte une fiche RPG en PDF."),
        "Primer Contacto" to t("First Contact", "Primer contacte", "Lehen kontaktua", "Erster Kontakt", "Premier contact"),
        "Abre la zona social por primera vez." to t("Open the social area for the first time.", "Obre la zona social per primera vegada.", "Ireki gune soziala lehen aldiz.", "Offne den Sozialbereich zum ersten Mal.", "Ouvre la zone sociale pour la premiere fois."),
        "Mirada Competitiva" to t("Competitive Look", "Mirada competitiva", "Ikuspegi lehiakorra", "Wettbewerbsblick", "Regard competitif"),
        "Consulta el ranking mundial." to t("Check the global ranking.", "Consulta el ranquing mundial.", "Kontsultatu munduko sailkapena.", "Sieh dir die Weltrangliste an.", "Consulte le classement mondial."),
        "Cazador de Logros" to t("Achievement Hunter", "Cacador d'assoliments", "Lorpen-ehiztaria", "Erfolgsjager", "Chasseur de succes"),
        "Visita el Salón de la Fama." to t("Visit the Hall of Fame.", "Visita el Salo de la fama.", "Bisitatu Ospearen aretoa.", "Besuche die Ruhmeshalle.", "Visite le Temple de la renommee."),
        "Código Misterioso" to t("Mysterious Code", "Codi misterios", "Kode misteriotsua", "Geheimnisvoller Code", "Code mysterieux"),
        "Escanea un código QR en el mundo real." to t("Scan a QR code in the real world.", "Escaneja un codi QR al mon real.", "Eskaneatu QR kode bat mundu errealean.", "Scanne einen QR-Code in der realen Welt.", "Scanne un code QR dans le monde reel."),
        "Envía 5 acciones al DM" to t("Send 5 actions to the DM", "Envia 5 accions al DM", "Bidali 5 ekintza DMari", "Sende 5 Aktionen an den DM", "Envoie 5 actions au DM"),
        "Gana 3 combates" to t("Win 3 combats", "Guanya 3 combats", "Irabazi 3 borroka", "Gewinne 3 Kampfe", "Gagne 3 combats"),
        "Tierras Desconocidas" to t("Unknown Lands", "Terres desconegudes", "Lur ezezagunak", "Unbekannte Lander", "Terres inconnues"),
        "Explora el mundo y descubre nuevos lugares." to t("Explore the world and discover new places.", "Explora el mon i descobreix nous llocs.", "Esploratu mundua eta aurkitu toki berriak.", "Erkunde die Welt und entdecke neue Orte.", "Explore le monde et decouvre de nouveaux lieux."),
        "Descubre 3 ubicaciones" to t("Discover 3 locations", "Descobreix 3 ubicacions", "Aurkitu 3 kokapen", "Entdecke 3 Orte", "Decouvre 3 lieux"),
        "Cazador de Tesoros" to t("Treasure Hunter", "Cacador de tresors", "Altxorraren ehiztaria", "Schatzjager", "Chasseur de tresors"),
        "Reúne objetos valiosos en tus aventuras." to t("Gather valuable items in your adventures.", "Reuneix objectes valuosos a les teves aventures.", "Bildu objektu baliotsuak zure abenturetan.", "Sammle wertvolle Gegenstande in deinen Abenteuern.", "Rassemble des objets precieux dans tes aventures."),
        "Encuentra 3 objetos" to t("Find 3 items", "Troba 3 objectes", "Aurkitu 3 objektu", "Finde 3 Gegenstande", "Trouve 3 objets"),
        "El Héroe Veterano" to t("The Veteran Hero", "L'heroi vetera", "Heroi beteranoa", "Der Veteranenheld", "Le heros veteran"),
        "Una misión épica para verdaderos aventureros." to t("An epic quest for true adventurers.", "Una missio epica per a veritables aventurers.", "Benetako abenturazaleentzako misio epikoa.", "Eine epische Quest fur wahre Abenteurer.", "Une mission epique pour les vrais aventuriers."),
        "Gana 5 combates" to t("Win 5 combats", "Guanya 5 combats", "Irabazi 5 borroka", "Gewinne 5 Kampfe", "Gagne 5 combats"),
        "Descubre 5 ubicaciones" to t("Discover 5 locations", "Descobreix 5 ubicacions", "Aurkitu 5 kokapen", "Entdecke 5 Orte", "Decouvre 5 lieux"),
        "Alcanza el nivel 5" to t("Reach level 5", "Arriba al nivell 5", "Iritsi 5. mailara", "Erreiche Stufe 5", "Atteins le niveau 5"),

        )

    private val terms = mapOf(
        "Humano" to t("Human", "Huma", "Gizakia", "Mensch", "Humain"),
        "Humanos" to t("Humans", "Humans", "Gizakiak", "Menschen", "Humains"),
        "Guerrero" to t("Fighter", "Guerrer", "Gerlaria", "Krieger", "Guerrier"),
        "Artífice" to t("Artificer", "Artificier", "Artifiziala", "Artifizient", "Artificier"),
        "Bardo" to t("Bard", "Bard", "Bardoa", "Barde", "Barde"),
        "Bárbaro" to t("Barbarian", "Barbar", "Barbaroa", "Barbar", "Barbare"),
        "Brujo" to t("Warlock", "Bruixot", "Sorgina", "Hexenmeister", "Sorcier"),
        "Clérigo" to t("Cleric", "Clergue", "Apaiza", "Kleriker", "Clerc"),
        "Druida" to t("Druid", "Druida", "Druida", "Druide", "Druide"),
        "Explorador" to t("Ranger", "Explorador", "Esploratzailea", "Waldlaufer", "Rodeur"),
        "Hechicero" to t("Sorcerer", "Fetiller", "Aztia", "Zauberer", "Ensorceleur"),
        "Mago" to t("Wizard", "Mag", "Magoa", "Magier", "Magicien"),
        "Monje" to t("Monk", "Monjo", "Fraidea", "Monch", "Moine"),
        "Paladín" to t("Paladin", "Paladi", "Paladina", "Paladin", "Paladin"),
        "Pícaro" to t("Rogue", "Murri", "Pikaroa", "Schurke", "Roublard"),
        "Elfos" to t("Elves", "Elfs", "Elfak", "Elfen", "Elfes"),
        "Enanos" to t("Dwarves", "Nans", "Nanoak", "Zwerge", "Nains"),
        "Gnomos" to t("Gnomes", "Gnoms", "Gnomoak", "Gnome", "Gnomes"),
        "Orcos" to t("Orcs", "Orcs", "Orkoak", "Orks", "Orcs"),
        "Semielfos" to t("Half-elves", "Semielfs", "Erdi-elfoak", "Halbelfen", "Demi-elfes"),
        "Semiorcos" to t("Half-orcs", "Semiorcs", "Erdi-orkoak", "Halborks", "Demi-orcs"),
        "Vampiro" to t("Vampire", "Vampir", "Banpiroa", "Vampir", "Vampire"),
        "Zombie" to t("Zombie", "Zombi", "Zonbia", "Zombie", "Zombie"),
        "Aarakocras" to t("Aarakocra", "Aarakocra", "Aarakocra", "Aarakocra", "Aarakocra"),
        "Aasimar" to t("Aasimar", "Aasimar", "Aasimar", "Aasimar", "Aasimar"),
        "Cambiantes" to t("Changelings", "Canviants", "Aldakor", "Gestaltwandler", "Changeformes"),
        "Centauro" to t("Centaur", "Centaure", "Zentauro", "Zentaur", "Centaure"),
        "Chico pollo" to t("Birdfolk", "Home ocell", "Hegazti-gizona", "Vogelmensch", "Homme-oiseau"),
        "Chico Slime" to t("Slimefolk", "Slime humà", "Slime gizona", "Schleimmensch", "Humanoïde slime"),
        "Deidad" to t("Deity", "Deïtat", "Jainkotasuna", "Gottheit", "Divinité"),
        "Demonio" to t("Demon", "Dimoni", "Deabrua", "Dämon", "Démon"),
        "Dracónidos" to t("Dragonborn", "Dracònids", "Drakonoideak", "Drachenblütige", "Drakéides"),
        "Elemental" to t("Elemental", "Elemental", "Elementala", "Elementarwesen", "Élémentaire"),
        "Elfo oscuro" to t("Dark elf", "Elf fosc", "Iratxo iluna", "Dunkelelf", "Elfe noir"),
        "Espectro" to t("Specter", "Espectre", "Espektroa", "Gespenst", "Spectre"),
        "Espíritu" to t("Spirit", "Esperit", "Izpiritua", "Geist", "Esprit"),
        "Etergénito" to t("Etherborn", "Etergènit", "Eter-sortua", "Äthergeborener", "Éthéroné"),
        "Firbolgs" to t("Firbolgs", "Firbolgs", "Firbolgak", "Firbolgs", "Firbolgs"),
        "Forjados" to t("Warforged", "Forjats", "Forjatuak", "Kriegsgeschmiedete", "Forgeliers"),
        "Genasi" to t("Genasi", "Genasi", "Genasi", "Genasi", "Genasi"),
        "Gith" to t("Gith", "Gith", "Gith", "Gith", "Gith"),
        "Goblins" to t("Goblins", "Goblins", "Goblinak", "Goblins", "Gobelins"),
        "Golem" to t("Golem", "Gòlem", "Golema", "Golem", "Golem"),
        "Goliats" to t("Goliaths", "Goliats", "Goliathak", "Goliaths", "Goliaths"),
        "Grungs" to t("Grungs", "Grungs", "Grungak", "Grungs", "Grungs"),
        "Híbridos Simic" to t("Simic hybrids", "Híbrids Simic", "Simic hibridoak", "Simic-Hybride", "Hybrides Simic"),
        "Hobgoblins" to t("Hobgoblins", "Hobgoblins", "Hobgoblinak", "Hobgoblins", "Hobgobelins"),
        "Hombre lobo" to t("Werewolf", "Home llop", "Otso-gizona", "Werwolf", "Loup-garou"),
        "Hombres lagarto" to t("Lizardfolk", "Homes sargantana", "Musker-gizonak", "Echsenmenschen", "Hommes-lézards"),
        "Huecos" to t("Hollows", "Buits", "Hutsak", "Hohle", "Creux"),
        "Ilusión" to t("Illusion", "Il·lusió", "Ilusioa", "Illusion", "Illusion"),
        "Kalashtar" to t("Kalashtar", "Kalashtar", "Kalashtar", "Kalashtar", "Kalashtar"),
        "Kenkus" to t("Kenkus", "Kenkus", "Kenkua", "Kenkus", "Kenkus"),
        "Kobolds" to t("Kobolds", "Kobolds", "Koboldak", "Kobolde", "Kobolds"),
        "Locathah" to t("Locathah", "Locathah", "Locathah", "Locathah", "Locathah"),
        "Loxodon" to t("Loxodon", "Loxodon", "Loxodon", "Loxodon", "Loxodon"),
        "Medianos" to t("Halflings", "Medians", "Erdi-tamainakoak", "Halblinge", "Halfelins"),
        "Minotauros" to t("Minotaurs", "Minotaures", "Minotauroak", "Minotauren", "Minotaures"),
        "Mutadores" to t("Shifters", "Mutadors", "Aldatzaileak", "Wandler", "Métamorphes"),
        "Orcos de Eberron" to t("Eberron orcs", "Orcs d'Eberron", "Eberroneko orkoak", "Orks aus Eberron", "Orcs d'Eberron"),
        "Osgos" to t("Bugbears", "Ossos", "Bugbear-ak", "Bugbears", "Bugbears"),
        "Polimorfo" to t("Shapeshifter", "Polimorf", "Polimorfoa", "Gestaltwandler", "Métamorphe"),
        "Quimera" to t("Chimera", "Quimera", "Kimera", "Chimäre", "Chimère"),
        "Rápido" to t("Swiftfolk", "Ràpid", "Azkarra", "Schnellling", "Rapide"),
        "Sátiro" to t("Satyr", "Sàtir", "Satyroa", "Satyr", "Satyre"),
        "Tabaxis" to t("Tabaxi", "Tabaxi", "Tabaxi", "Tabaxi", "Tabaxi"),
        "Tiflin" to t("Tiefling", "Tiflin", "Tiefling", "Tiefling", "Tieffelin"),
        "Tortogas" to t("Tortles", "Tortugues", "Tortleak", "Schildkrötenmenschen", "Tortues humanoïdes"),
        "Trasgo" to t("Goblin", "Trasgo", "Iratxoa", "Goblin", "Gobelin"),
        "Tritones" to t("Tritons", "Tritons", "Tritoiak", "Tritonen", "Tritons"),
        "Vedalken" to t("Vedalken", "Vedalken", "Vedalken", "Vedalken", "Vedalken"),
        "Verdan" to t("Verdan", "Verdan", "Verdan", "Verdan", "Verdan"),
        "Yuan-Ti Purasangres" to t("Yuan-ti purebloods", "Yuan-Ti de sang pura", "Odol garbiko Yuan-Tiak", "Yuan-Ti-Reinblütige", "Yuan-Ti sang-pur"),
        "Caballero de la Muerte" to t("Death knight", "Cavaller de la Mort", "Heriotzaren zalduna", "Todesritter", "Chevalier de la Mort"),
        "Chamán" to t("Shaman", "Xaman", "Xamana", "Schamane", "Chaman"),
        "Corsario" to t("Corsair", "Corsari", "Korsarioa", "Freibeuter", "Corsaire"),
        "Exorcista" to t("Exorcist", "Exorcista", "Exorzista", "Exorzist", "Exorciste"),
        "Tú" to t("You", "Tu", "Zu", "Du", "Toi"),

        )

    // Ejecuta la lógica de translate.
    fun translate(context: Context, value: String): String {
        if (value.isBlank()) return value
        val language = AppLanguageManager.getSavedLanguage(context).code
        if (language == "es") return value
        exact[value]?.let { return resolveMappedTranslation(language, value, it) }
        terms[value]?.let { return resolveMappedTranslation(language, value, it) }
        translateDynamic(language, value)?.let { return finalizeTranslation(language, it) }
        val composite = replaceKnownTerms(language, value)
        return finalizeTranslation(language, composite)
    }

    // Ejecuta la lógica de resolve mapped translation.
    private fun resolveMappedTranslation(language: String, original: String, translation: T): String {
        val mapped = if (language == "gl" && translation.gl.isBlank()) original else translation.of(language)
        return finalizeTranslation(language, mapped)
    }

    // Ejecuta la lógica de finalize translation.
    private fun finalizeTranslation(language: String, value: String): String =
        if (language == "gl") autoTranslateToGalician(value) else value


    // Traduce dynamic.
    private fun translateDynamic(language: String, value: String): String? {
        fun e(key: String) = exact[key]?.of(language).orEmpty()
        fun w(key: String) = word(language, key)

        return when {
            value.startsWith("Preparando aventura para ") ->
                "${e("Preparando aventura para")} ${value.removePrefix("Preparando aventura para ")}"
            value.startsWith("¿Estás seguro de que quieres borrar a ") ->
                when (language) {
                    "en" -> "Are you sure you want to delete ${value.substringAfter("borrar a ").substringBefore("?")}? This action cannot be undone."
                    "ca" -> "Segur que vols esborrar ${value.substringAfter("borrar a ").substringBefore("?")}? Aquesta accio no es pot desfer."
                    "eu" -> "Ziur ${value.substringAfter("borrar a ").substringBefore("?")} ezabatu nahi duzula? Ekintza hau ezin da desegin."
                    "de" -> "Mochtest du ${value.substringAfter("borrar a ").substringBefore("?")} wirklich loschen? Diese Aktion kann nicht ruckgangig gemacht werden."
                    "fr" -> "Veux-tu vraiment supprimer ${value.substringAfter("borrar a ").substringBefore("?")} ? Cette action est irreversible."
                    "gl" -> "Seguro que queres borrar a ${value.substringAfter("borrar a ").substringBefore("?")}? Esta acción non se pode desfacer."
                    else -> value
                }
            value.startsWith("Aventura: ") ->
                "${e("Aventura")}: ${replaceKnownTerms(language, value.removePrefix("Aventura: "))}"
            value.startsWith("Nv.") ->
                value.replace("Nv.", w("Lv."))
            value.startsWith("Hace ") && value.endsWith(" min") ->
                "${w("Ago")} ${value.removePrefix("Hace ")}".trim()
            value.startsWith("Hace ") && value.endsWith(" h") ->
                "${w("Ago")} ${value.removePrefix("Hace ")}".trim()
            value.startsWith("Hace ") && value.endsWith(" días") ->
                "${w("Ago")} ${value.removePrefix("Hace ").removeSuffix(" días")} ${w("days")}".trim()
            value.startsWith("Desbloqueado el ") ->
                "${w("Unlocked on")} ${value.removePrefix("Desbloqueado el ")}"
            value.matches(Regex("""\d+/\d+ desbloqueados""")) ->
                when (language) {
                    "en" -> value.replace(" desbloqueados", " unlocked")
                    "ca" -> value.replace(" desbloqueados", " desbloquejats")
                    "eu" -> value.replace(" desbloqueados", " desblokeatuta")
                    "de" -> value.replace(" desbloqueados", " freigeschaltet")
                    "fr" -> value.replace(" desbloqueados", " debloques")
                    "gl" -> value.replace(" desbloqueados", " desbloqueados")
                    else -> value
                }
            value.endsWith("% completado") ->
                when (language) {
                    "en" -> value.replace("% completado", "% completed")
                    "ca" -> value.replace("% completado", "% completat")
                    "eu" -> value.replace("% completado", "% osatuta")
                    "de" -> value.replace("% completado", "% abgeschlossen")
                    "fr" -> value.replace("% completado", "% termine")
                    "gl" -> value.replace("% completado", "% completado")
                    else -> value
                }
            value.startsWith("Nivel ") ->
                "${w("Level")} ${value.removePrefix("Nivel ")}"
            value.contains(" · Nivel ") ->
                value.replace(" · Nivel ", " · ${w("Level")} ")
            value.startsWith("Líder: ") ->
                value.replace("Líder:", e("Líder") + ":")
            value.startsWith("Estado: ") ->
                when (language) {
                    "en" -> "Status: ${translateDynamic(language, value.removePrefix("Estado: ")) ?: replaceKnownTerms(language, value.removePrefix("Estado: "))}"
                    "ca" -> "Estat: ${translateDynamic(language, value.removePrefix("Estado: ")) ?: replaceKnownTerms(language, value.removePrefix("Estado: "))}"
                    "eu" -> "Egoera: ${translateDynamic(language, value.removePrefix("Estado: ")) ?: replaceKnownTerms(language, value.removePrefix("Estado: "))}"
                    "de" -> "Status: ${translateDynamic(language, value.removePrefix("Estado: ")) ?: replaceKnownTerms(language, value.removePrefix("Estado: "))}"
                    "fr" -> "Etat : ${translateDynamic(language, value.removePrefix("Estado: ")) ?: replaceKnownTerms(language, value.removePrefix("Estado: "))}"
                    "gl" -> "Estado: ${translateDynamic(language, value.removePrefix("Estado: ")) ?: replaceKnownTerms(language, value.removePrefix("Estado: "))}"
                    else -> value
                }
            value.startsWith("Ronda ") ->
                when (language) {
                    "en" -> "Round ${value.removePrefix("Ronda ")}"
                    "ca" -> "Ronda ${value.removePrefix("Ronda ")}"
                    "eu" -> "Txanda ${value.removePrefix("Ronda ")}"
                    "de" -> "Runde ${value.removePrefix("Ronda ")}"
                    "fr" -> "Manche ${value.removePrefix("Ronda ")}"
                    "gl" -> "Rolda ${value.removePrefix("Ronda ")}"
                    else -> value
                }
            value.startsWith("Jefe: ") ->
                when (language) {
                    "en" -> "Boss: ${value.removePrefix("Jefe: ")}"
                    "ca" -> "Cap: ${value.removePrefix("Jefe: ")}"
                    "eu" -> "Nagusia: ${value.removePrefix("Jefe: ")}"
                    "de" -> "Boss: ${value.removePrefix("Jefe: ")}"
                    "fr" -> "Boss : ${value.removePrefix("Jefe: ")}"
                    "gl" -> "Xefe: ${value.removePrefix("Jefe: ")}"
                    else -> value
                }
            value.startsWith("Última vez: ") ->
                value.replace("Última vez:", w("Last seen:"))
            value.startsWith("Última orden: ") ->
                value.replace("Última orden:", w("Last command:"))
            value.startsWith("Suma: ") ->
                value.replace("Suma:", w("Sum:"))
            value.startsWith("Media: ") ->
                value.replace("Media:", w("Average:"))
            value.contains("miembros") ->
                value.replace("miembros", w("members"))
            value.contains(" (Tú)") ->
                when (language) {
                    "en" -> value.replace(" (Tú)", " (You)")
                    "ca" -> value.replace(" (Tú)", " (Tu)")
                    "eu" -> value.replace(" (Tú)", " (Zu)")
                    "de" -> value.replace(" (Tú)", " (Du)")
                    "fr" -> value.replace(" (Tú)", " (Toi)")
                    "gl" -> value.replace(" (Tú)", " (Ti)")
                    else -> value
                }
            value.startsWith("⚡ CRÍTICO") ->
                value.replace("CRÍTICO", w("CRITICAL"))
            else -> null
        }
    }


    // Ejecuta la lógica de replace known terms.
    private fun replaceKnownTerms(language: String, value: String): String {
        var translated = value

        val entries = terms.entries
            .sortedByDescending { it.key.length }

        entries.forEach { (original, translation) ->
            val replacement = if (language == "gl" && translation.gl.isBlank()) original else translation.of(language)
            translated = translated.replace(original, replacement)
        }

        return if (language == "gl") autoTranslateToGalician(translated) else translated
    }

    // Ejecuta la lógica de auto translate to galician.
    private fun autoTranslateToGalician(value: String): String {
        val phraseReplacements = linkedMapOf(
            "¡Bienvenido a AI Dungeon Master!" to "Benvido a AI Dungeon Master!",
            "Tus personajes" to "Os teus personaxes",
            "Tus Personajes" to "Os teus personaxes",
            "Más opciones" to "Máis opcións",
            "Ver tutorial" to "Ver titorial",
            "Selecciona idioma" to "Selecciona idioma",
            "Mis gremios" to "Os meus gremios",
            "Mi perfil" to "O meu perfil",
            "Mis amigos" to "Os meus amigos",
            "Opciones de accesibilidad" to "Opcións de accesibilidade",
            "Opciones de usabilidad" to "Opcións de usabilidade",
            "Configura ayudas visuales y control por voz." to "Configura axudas visuais e control por voz.",
            "Configura ayudas visuales y manejo por voz de la aplicación." to "Configura axudas visuais e manexo por voz da aplicación.",
            "Modo visita: puedes recorrer la sala, pero no modificar la decoración." to "Modo visita: podes percorrer a sala, pero non modificar a decoración.",
            "Usa el joystick para caminar por la sala y pisa una baldosa para decorarla." to "Usa o joystick para camiñar pola sala e pisa unha baldosa para decorala.",
            "Sala personal" to "Sala persoal",
            "Ocultar decoración" to "Ocultar decoración",
            "Búsqueda" to "Busca",
            "búsqueda" to "busca",
            "Buscar aventureros" to "Buscar aventureiros",
            "Buscar por nombre o usuario" to "Buscar por nome ou usuario",
            "Nombre visible" to "Nome visible",
            "Elegir foto" to "Escoller foto",
            "Guardar perfil" to "Gardar perfil",
            "Abrir chat privado" to "Abrir chat privado",
            "Salas de personajes" to "Salas de personaxes",
            "En línea" to "En liña",
            "Jefe final" to "Xefe final",
            "Cargando gremio..." to "Cargando gremio...",
            "Cargando sala del jefe..." to "Cargando sala do xefe...",
            "Estado de la sala" to "Estado da sala",
            "Esperando que comience la batalla..." to "Agardando a que comece a batalla...",
            "Todavía no te has unido a la pelea con un personaje." to "Aínda non te uniches á loita cun personaxe.",
            "Todavía no tienes amigos." to "Aínda non tes amigos.",
            "Todavía no perteneces a ningún gremio." to "Aínda non pertences a ningún gremio.",
            "Todavía no has descubierto monstruos." to "Aínda non descubriches monstros.",
            "No se pudo guardar el PDF." to "Non se puido gardar o PDF.",
            "No se pudo cargar la ficha del personaje." to "Non se puido cargar a ficha do personaxe.",
            "Ficha PDF guardada correctamente." to "Ficha PDF gardada correctamente.",
            "Descargar ficha en PDF" to "Descargar ficha en PDF",
            "Guardar ficha" to "Gardar ficha",
            "Personaje derrotado" to "Personaxe derrotado",
            "Crea gremio" to "Crea gremio",
            "Crear gremio" to "Crear gremio",
            "Buscar gremios" to "Buscar gremios",
            "Buscar usuarios" to "Buscar usuarios",
            "Solicitudes de amistad" to "Solicitudes de amizade",
            "Lista de amigos" to "Lista de amigos",
            "Zona social" to "Zona social"
        )

        val wordReplacements = linkedMapOf(
            "personajes" to "personaxes",
            "Personajes" to "Personaxes",
            "personaje" to "personaxe",
            "Personaje" to "Personaxe",
            "descripción" to "descrición",
            "Descripción" to "Descrición",
            "amistad" to "amizade",
            "Amistad" to "Amizade",
            "guardar" to "gardar",
            "Guardar" to "Gardar",
            "guardado" to "gardado",
            "Guardado" to "Gardado",
            "guardada" to "gardada",
            "Guardada" to "Gardada",
            "línea" to "liña",
            "Línea" to "Liña",
            "daño" to "dano",
            "Daño" to "Dano",
            "contraseña" to "contrasinal",
            "Contraseña" to "Contrasinal",
            "nombre" to "nome",
            "Nombre" to "Nome",
            "miembros" to "membros",
            "Miembros" to "Membros",
            "registro" to "rexistro",
            "Registro" to "Rexistro",
            "registros" to "rexistros",
            "Registros" to "Rexistros",
            "configuración" to "configuración",
            "batalla" to "batalla",
            "gremio" to "gremio",
            "jefe" to "xefe",
            "Jefe" to "Xefe",
            "sala" to "sala",
            "pelea" to "loita",
            "Pelear" to "Loitar",
            "mundo" to "mundo",
            "ficha" to "ficha",
            "última" to "última",
            "Última" to "Última",
            "correo" to "correo",
            "botín" to "botín",
            "debilidad" to "debilidade",
            "Debilidad" to "Debilidade",
            "debilidades" to "debilidades",
            "Debilidades" to "Debilidades",
            "resistencia" to "resistencia",
            "resistencias" to "resistencias"
        )

        var translated = value
        phraseReplacements.forEach { (from, to) ->
            translated = translated.replace(from, to)
        }
        wordReplacements.forEach { (from, to) ->
            translated = translated.replace(from, to)
        }
        translated = translated
            .replace("No se pudo", "Non se puido")
            .replace("No hay", "Non hai")
            .replace("Todavía", "Aínda")
            .replace("Todavia", "Aínda")
            .replace("Más", "Máis")
            .replace(" mas ", " máis ")
            .replace("Última orden", "Última orde")
            .replace("Última vez", "Última vez")
            .replace("Seleccionado", "Seleccionado")

        return translated
    }

    // Ejecuta la lógica de word.
    private fun word(language: String, key: String): String = when (key) {
        "Lv." -> when (language) { "en" -> "Lv."; "ca" -> "Nv."; "eu" -> "Maila "; "de" -> "St."; "fr" -> "Nv."; "gl" -> "Nv."; else -> "Nv." }
        "Ago" -> when (language) { "en" -> ""; "ca" -> "Fa"; "eu" -> "Duela"; "de" -> "Vor"; "fr" -> "Il y a"; "gl" -> "Hai"; else -> "Hace" }.ifBlank { "" }
        "days" -> when (language) { "en" -> "days ago"; "ca" -> "dies"; "eu" -> "egun"; "de" -> "Tagen"; "fr" -> "jours"; "gl" -> "días"; else -> "dias" }
        "Unlocked on" -> when (language) { "en" -> "Unlocked on"; "ca" -> "Desbloquejat el"; "eu" -> "Desblokeatua:"; "de" -> "Freigeschaltet am"; "fr" -> "Debloque le"; "gl" -> "Desbloqueado o"; else -> "Desbloqueado el" }
        "Level" -> when (language) { "en" -> "Level"; "ca" -> "Nivell"; "eu" -> "Maila"; "de" -> "Stufe"; "fr" -> "Niveau"; "gl" -> "Nivel"; else -> "Nivel" }
        "Last seen:" -> when (language) { "en" -> "Last seen:"; "ca" -> "Ultima vegada:"; "eu" -> "Azken aldia:"; "de" -> "Zuletzt:"; "fr" -> "Derniere fois :"; "gl" -> "Última vez:"; else -> "Ultima vez:" }
        "Last command:" -> when (language) { "en" -> "Last command:"; "ca" -> "Ultima ordre:"; "eu" -> "Azken agindua:"; "de" -> "Letzter Befehl:"; "fr" -> "Derniere commande :"; "gl" -> "Última orde:"; else -> "Ultima orden:" }
        "Sum:" -> when (language) { "en" -> "Sum:"; "ca" -> "Suma:"; "eu" -> "Batura:"; "de" -> "Summe:"; "fr" -> "Somme:"; "gl" -> "Suma:"; else -> "Suma:" }
        "Average:" -> when (language) { "en" -> "Average:"; "ca" -> "Mitjana:"; "eu" -> "Batezbestekoa:"; "de" -> "Durchschnitt:"; "fr" -> "Moyenne:"; "gl" -> "Media:"; else -> "Media:" }
        "members" -> when (language) { "en" -> "members"; "ca" -> "membres"; "eu" -> "kide"; "de" -> "Mitglieder"; "fr" -> "membres"; "gl" -> "membros"; else -> "miembros" }
        "CRITICAL" -> when (language) { "en" -> "CRITICAL"; "ca" -> "CRITIC"; "eu" -> "KRITIKOA"; "de" -> "KRITISCH"; "fr" -> "CRITIQUE"; "gl" -> "CRÍTICO"; else -> "CRITICO" }
        else -> key
    }
}

@Composable
// Ejecuta la lógica de localized text.
fun localizedText(value: String): String =
    FixedTextTranslator.translate(LocalContext.current, value)

@Composable
// Ejecuta la lógica de text.
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = localizedText(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
// Ejecuta la lógica de text.
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style
    )
}
