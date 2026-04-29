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

object FixedTextTranslator {
    private data class T(
        val en: String,
        val ca: String,
        val eu: String,
        val de: String,
        val fr: String
    ) {
        fun of(code: String): String = when (code) {
            "en" -> en
            "ca" -> ca
            "eu" -> eu
            "de" -> de
            "fr" -> fr
            else -> en
        }
    }

    private fun t(en: String, ca: String, eu: String, de: String, fr: String) = T(en, ca, eu, de, fr)

    private val exact = mapOf(
        "Español" to t("Spanish", "Espanyol", "Gaztelania", "Spanisch", "Espagnol"),
        "English" to t("English", "Angles", "Ingelesa", "Englisch", "Anglais"),
        "Català" to t("Catalan", "Catala", "Katalana", "Katalanisch", "Catalan"),
        "Euskera" to t("Basque", "Euskara", "Euskara", "Baskisch", "Basque"),
        "Deutsch" to t("German", "Alemany", "Alemana", "Deutsch", "Allemand"),
        "Français" to t("French", "Frances", "Frantsesa", "Franzosisch", "Francais"),
        "Idioma" to t("Language", "Idioma", "Hizkuntza", "Sprache", "Langue"),
        "Selecciona idioma" to t("Select language", "Selecciona idioma", "Aukeratu hizkuntza", "Sprache auswahlen", "Choisir la langue"),

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
        "Demuestra tu valor en combate ganando batallas." to t("Prove your worth in combat by winning battles.", "Demostra el teu valor en combat guanyant batalles.", "Erakutsi zure ausardia borrokan guduak irabaziz.", "Beweise deinen Wert im Kampf, indem du Schlachten gewinnst.", "Prouve ta valeur au combat en gagnant des batailles.")
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
        "Zombie" to t("Zombie", "Zombi", "Zonbia", "Zombie", "Zombie")
    )

    fun translate(context: Context, value: String): String {
        if (value.isBlank()) return value
        val language = AppLanguageManager.getSavedLanguage(context).code
        if (language == "es") return value
        exact[value]?.let { return it.of(language) }
        terms[value]?.let { return it.of(language) }
        translateDynamic(language, value)?.let { return it }
        return value
    }

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
                    else -> value
                }
            value.startsWith("Nv.") -> value.replace("Nv.", w("Lv."))
            value.startsWith("Hace ") && value.endsWith(" min") -> "${w("Ago")} ${value.removePrefix("Hace ")}"
            value.startsWith("Hace ") && value.endsWith(" h") -> "${w("Ago")} ${value.removePrefix("Hace ")}"
            value.startsWith("Hace ") && value.endsWith(" días") -> "${w("Ago")} ${value.removePrefix("Hace ").removeSuffix(" días")} ${w("days")}"
            value.startsWith("Desbloqueado el ") -> "${w("Unlocked on")} ${value.removePrefix("Desbloqueado el ")}"
            value.startsWith("Nivel ") -> "${w("Level")} ${value.removePrefix("Nivel ")}"
            value.contains(" · Nivel ") -> value.replace(" · Nivel ", " · ${w("Level")} ")
            value.startsWith("Última vez: ") -> value.replace("Última vez:", w("Last seen:"))
            value.startsWith("Última orden: ") -> value.replace("Última orden:", w("Last command:"))
            value.startsWith("Suma: ") -> value.replace("Suma:", w("Sum:"))
            value.startsWith("Media: ") -> value.replace("Media:", w("Average:"))
            value.contains("miembros") -> value.replace("miembros", w("members"))
            value.startsWith("⚡ CRÍTICO") -> value.replace("CRÍTICO", w("CRITICAL"))
            else -> null
        }
    }

    private fun word(language: String, key: String): String = when (key) {
        "Lv." -> when (language) { "en" -> "Lv."; "ca" -> "Nv."; "eu" -> "Maila "; "de" -> "St."; "fr" -> "Nv."; else -> "Nv." }
        "Ago" -> when (language) { "en" -> ""; "ca" -> "Fa"; "eu" -> "Duela"; "de" -> "Vor"; "fr" -> "Il y a"; else -> "Hace" }.ifBlank { "" }
        "days" -> when (language) { "en" -> "days ago"; "ca" -> "dies"; "eu" -> "egun"; "de" -> "Tagen"; "fr" -> "jours"; else -> "dias" }
        "Unlocked on" -> when (language) { "en" -> "Unlocked on"; "ca" -> "Desbloquejat el"; "eu" -> "Desblokeatua:"; "de" -> "Freigeschaltet am"; "fr" -> "Debloque le"; else -> "Desbloqueado el" }
        "Level" -> when (language) { "en" -> "Level"; "ca" -> "Nivell"; "eu" -> "Maila"; "de" -> "Stufe"; "fr" -> "Niveau"; else -> "Nivel" }
        "Last seen:" -> when (language) { "en" -> "Last seen:"; "ca" -> "Ultima vegada:"; "eu" -> "Azken aldia:"; "de" -> "Zuletzt:"; "fr" -> "Derniere fois :"; else -> "Ultima vez:" }
        "Last command:" -> when (language) { "en" -> "Last command:"; "ca" -> "Ultima ordre:"; "eu" -> "Azken agindua:"; "de" -> "Letzter Befehl:"; "fr" -> "Derniere commande :"; else -> "Ultima orden:" }
        "Sum:" -> when (language) { "en" -> "Sum:"; "ca" -> "Suma:"; "eu" -> "Batura:"; "de" -> "Summe:"; "fr" -> "Somme:"; else -> "Suma:" }
        "Average:" -> when (language) { "en" -> "Average:"; "ca" -> "Mitjana:"; "eu" -> "Batezbestekoa:"; "de" -> "Durchschnitt:"; "fr" -> "Moyenne:"; else -> "Media:" }
        "members" -> when (language) { "en" -> "members"; "ca" -> "membres"; "eu" -> "kide"; "de" -> "Mitglieder"; "fr" -> "membres"; else -> "miembros" }
        "CRITICAL" -> when (language) { "en" -> "CRITICAL"; "ca" -> "CRITIC"; "eu" -> "KRITIKOA"; "de" -> "KRITISCH"; "fr" -> "CRITIQUE"; else -> "CRITICO" }
        else -> key
    }
}

@Composable
fun localizedText(value: String): String =
    FixedTextTranslator.translate(LocalContext.current, value)

@Composable
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
