# AIDungeonMaster

AIDungeonMaster es una aplicación Android desarrollada en Kotlin que combina narrativa interactiva generada por inteligencia artificial, persistencia en la nube, progresión RPG, sistemas sociales, accesibilidad avanzada, procesamiento en Python, renderizado 3D, realidad aumentada y funcionalidades contextuales ligadas al entorno real.

## Descripción general

El proyecto implementa una experiencia de rol móvil en la que el usuario puede:

- registrarse, iniciar sesión o autenticarse con Google;
- crear, gestionar y eliminar personajes persistentes;
- generar retratos de personaje y monstruos mediante IA;
- iniciar aventuras dinámicas guiadas por un motor narrativo basado en IA;
- participar en combates por turnos;
- gestionar inventario, equipo, monedas, experiencia y progresión;
- registrar eventos en un diario narrativo enriquecido con procesamiento en Python;
- explorar localizaciones persistentes del mundo del juego;
- visualizar escenarios en 3D y realidad aumentada;
- escanear QR y usar OCR para activar contenido contextual;
- interactuar con un módulo social con amistades, perfiles, chats y gremios;
- participar en batallas cooperativas contra jefes de gremio;
- personalizar una sala personal del personaje;
- exportar fichas de personaje en PDF;
- usar opciones de accesibilidad como modo daltónico, tutorial guiado y control por voz.

La aplicación sigue una arquitectura modular inspirada en MVVM y se apoya en Firebase, servicios de IA, renderizado nativo/embebido y procesamiento auxiliar en Python.

---

## Características principales

### Núcleo jugable
- Creación, selección y borrado de personajes.
- Sistema de aventura narrativa dinámica guiada por IA.
- Combate por turnos con enemigos, botín y consecuencias persistentes.
- Gestión de inventario, equipo, rarezas, consumibles y encantamientos.
- Sistema de experiencia, nivel, estadísticas derivadas y progresión.
- Diario narrativo persistente con capítulos y reescritura épica.
- Bestiario, ranking y logros.
- Mapa del mundo persistente con localizaciones descubiertas.

### Funcionalidades avanzadas
- Generación de narrativa estructurada mediante API externa de IA.
- Generación de retratos y monstruos mediante IA.
- Procesamiento narrativo con Python embebido.
- Visualización 3D de localizaciones con Three.js en WebView.
- Mapa en realidad aumentada con ARCore + SceneView.
- Exportación de ficha de personaje en PDF.
- Música procedural para exploración y combate.
- Escaneo QR y OCR con ML Kit.
- Detección contextual de supermercados cercanos mediante geolocalización y OpenStreetMap/Overpass.
- Tienda contextual del mundo de juego vinculada a localización física.
- Notificaciones y tareas programadas con WorkManager.

### Módulo social
- Búsqueda de usuarios.
- Solicitudes de amistad.
- Lista de amigos.
- Perfiles sociales personalizables.
- Foto de perfil y biografía.
- Presencia online y último acceso.
- Chat privado entre jugadores.
- Sistema de gremios.
- Chat de gremio.
- Vista de miembros del gremio.
- Batallas cooperativas contra jefe de gremio.

### Accesibilidad y experiencia de usuario
- Tutorial guiado integrado en la interfaz.
- Modo daltónico con persistencia de preferencias.
- Control por voz.
- Síntesis de voz y feedback hablado.
- Overlay de asistencia de usabilidad.
- Interfaz multidioma.

---

## Stack tecnológico

### Lenguajes
- **Kotlin**: lenguaje principal de la aplicación. ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
- **Python**: procesamiento narrativo auxiliar mediante Chaquopy. ![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
- **JavaScript**: renderizado 3D con Three.js. ![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
- **HTML**: contenido embebido para WebView. ![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
- **JSON**: intercambio de datos entre cliente, APIs y almacenamiento. ![JSON](https://img.shields.io/badge/JSON-000000?style=for-the-badge&logo=json&logoColor=white)

### Frameworks y librerías
- **Jetpack Compose**: interfaz declarativa.
- **Navigation Compose**: navegación.
- **ViewModel + StateFlow**: gestión reactiva del estado.
- **Firebase Authentication**: autenticación.
- **Cloud Firestore**: persistencia NoSQL en la nube.
- **Google Identity / Credentials API**: inicio de sesión con Google.
- **Retrofit**: cliente de APIs REST.
- **OkHttp**: comunicaciones HTTP.
- **Gson**: serialización y deserialización JSON.
- **Chaquopy**: integración de Python en Android.
- **CameraX**: acceso a cámara.
- **ML Kit Barcode Scanning**: lectura de códigos QR.
- **ML Kit Text Recognition**: OCR.
- **WorkManager**: tareas en segundo plano.
- **Google Play Services Location**: localización.
- **ARCore**: realidad aumentada.
- **SceneView / Filament**: renderizado AR/3D.
- **Three.js**: representación 3D procedural en WebView.
- **Coil**: carga de imágenes.
- **Cloudflare Workers AI**: generación visual y traducción auxiliar de prompts.
- **Groq API**: generación narrativa estructurada.
- **AudioTrack**: síntesis de audio procedural.
- **PdfDocument**: exportación de fichas en PDF.
- **androidx.lifecycle:lifecycle-process**: ciclo de vida global de la aplicación.

---

## Arquitectura

El proyecto sigue una arquitectura modular basada en **MVVM**.

### Capas principales

#### 1. Capa de presentación
Compuesta por pantallas desarrolladas con Jetpack Compose.

Responsabilidades:
- representación del estado;
- interacción con el usuario;
- navegación entre módulos;
- consumo del estado expuesto por los ViewModel;
- overlays de tutorial, accesibilidad y asistencia.

#### 2. Capa de lógica de presentación
Implementada mediante `ViewModel`.

Responsabilidades:
- coordinación entre UI y datos;
- mantenimiento del estado reactivo;
- validación de entradas;
- control del flujo funcional de pantallas y subsistemas;
- sincronización entre narrativa, combate, inventario, mapa, diario y social.

#### 3. Capa de datos
Formada por modelos, repositorios y servicios.

Responsabilidades:
- acceso a Firebase Authentication;
- lectura y escritura en Firestore;
- consumo de APIs externas;
- encapsulación de operaciones persistentes;
- gestión de perfiles, amistades, chats, gremios y partidas.

#### 4. Capa de integración externa
Incluye servicios auxiliares y motores adicionales.

Responsabilidades:
- llamadas al motor narrativo;
- generación de imágenes;
- ejecución de scripts Python;
- integración con AR, cámara, localización y notificaciones;
- renderizado 3D en WebView;
- síntesis de música procedural;
- reconocimiento y síntesis de voz.

---

## Estructura del proyecto

```text
app/
 └── src/
      └── main/
           ├── java/com/example/aidungeonmaster/
           │    ├── data/
           │    │    ├── api/              # Servicios remotos y DTOs
           │    │    ├── auth/             # Login con Google
           │    │    ├── model/            # Modelos de dominio
           │    │    └── repository/       # Persistencia, social, juego, gremios
           │    ├── navigation/            # Rutas y flujo de navegación
           │    ├── python/                # Puente Kotlin-Python
           │    ├── ui/
           │    │    ├── accessibility/    # Voz, modo daltónico, overlay de ayuda
           │    │    ├── achievements/     # Logros
           │    │    ├── game/             # Juego, combate, inventario, mapa, AR, QR
           │    │    ├── home/             # Home, personajes, ficha, PDF
           │    │    ├── i18n/             # Traducción fija de interfaz
           │    │    ├── login/            # Inicio de sesión
           │    │    ├── register/         # Registro
           │    │    ├── settings/         # Idioma y ajustes
           │    │    ├── social/           # Amigos, perfiles, chats, gremios
           │    │    ├── theme/            # Tema visual
           │    │    └── tutorial/         # Tutorial guiado
           │    ├── utils/                 # Música, notificaciones, IA visual, utilidades
           │    ├── viewmodel/             # ViewModels
           │    └── workers/               # Workers de ranking, inactividad y proximidad
           ├── python/                     # Scripts Python de resumen y diario
           └── res/                        # Recursos visuales, strings e idiomas
```

---

## Módulos funcionales

### Autenticación
Gestiona:
- registro de usuarios;
- inicio y cierre de sesión;
- inicio de sesión con Google;
- asociación de datos del jugador con `auth.uid`;
- creación y actualización de perfil público.

Tecnologías:
- Firebase Authentication
- Firestore
- Credentials API
- Google Identity

### Personajes
Gestiona:
- creación de personajes;
- persistencia de estadísticas;
- raza, clase, subclase y atributos;
- tiradas tipo RPG;
- retrato generado;
- selección y borrado completo;
- personalización temática de aventura.

### Aventura narrativa
Gestiona:
- contexto actual de la partida;
- historial de conversación con la IA;
- envío de acciones del jugador;
- recepción de respuesta estructurada;
- reintentos automáticos ante respuestas inválidas;
- aplicación de cambios al estado del juego.

### Combate
Gestiona:
- aparición de enemigos;
- cálculo de daño y curación;
- uso de armas y consumibles;
- huida o derrota;
- recompensas;
- integración con inventario, diario, logros y progreso.

### Inventario y progresión
Gestiona:
- objetos;
- equipo por slots;
- rarezas;
- encantamientos;
- monedas;
- experiencia;
- nivel;
- bonificaciones y comparación de equipo.

### Diario narrativo
Gestiona:
- almacenamiento de eventos;
- resumen de sesiones;
- agrupación por capítulos;
- títulos automáticos de capítulo;
- reescritura estilizada/épica;
- agrupación de sucesos repetidos.

Tecnologías:
- Firestore
- Python + Chaquopy

### Mapa y exploración
Gestiona:
- localizaciones descubiertas;
- persistencia del mapa;
- representación 2D/3D del mundo;
- galería 3D de ubicaciones;
- mapa en AR.

### Sala personal
Gestiona:
- compra de decoraciones;
- colocación de objetos decorativos;
- persistencia de la sala;
- visita en modo lectura a salas de otros jugadores.

### Bestiario
Gestiona:
- registro de encuentros;
- registro de derrotas;
- notas de monstruos;
- regeneración de imagen de monstruo;
- fichas persistentes de criaturas.

### Social
Gestiona:
- perfiles públicos;
- amistad entre jugadores;
- solicitudes pendientes;
- lista de amigos;
- chats privados;
- presencia online y último acceso;
- personalización visual del perfil.

### Gremios
Gestiona:
- creación y búsqueda de gremios;
- membresía;
- resumen del gremio;
- chat de gremio;
- miembros;
- batallas cooperativas contra jefe final.

### Integración contextual
Gestiona:
- QR;
- OCR;
- cámara;
- localización;
- detección de supermercados cercanos;
- acceso a tienda contextual del juego.

### Accesibilidad
Gestiona:
- control por voz;
- síntesis de voz;
- tutorial guiado;
- modo daltónico;
- asistencia contextual de navegación.

---

## Flujo técnico principal

1. El usuario se autentica mediante Firebase o Google.
2. Crea o selecciona un personaje persistido en Firestore.
3. Inicia una aventura con una temática concreta.
4. La app construye el contexto narrativo y envía la acción del usuario al servicio de IA.
5. La respuesta estructurada se parsea y valida.
6. Se actualizan:
   - narrativa mostrada;
   - estadísticas;
   - combate;
   - inventario;
   - diario;
   - mapa;
   - progreso general.
7. Los cambios se sincronizan con Firestore.
8. Los subsistemas auxiliares enriquecen la experiencia con:
   - imágenes generadas;
   - resumen narrativo con Python;
   - AR y 3D;
   - accesibilidad;
   - notificaciones;
   - social y gremios;
   - eventos contextuales por localización real.

---

## Estado del proyecto

Proyecto académico de TFG orientado a la exploración de:
- desarrollo Android moderno;
- arquitectura modular;
- integración de IA generativa;
- persistencia cloud;
- interacción enriquecida mediante AR, 3D y visión artificial;
- accesibilidad;
- gamificación social;
- procesamiento híbrido Kotlin + Python.

---

## Futuras mejoras

- Movimiento del personaje por la localización 3D generada del mapa, permitiendo desplazarse e interactuar dentro del entorno renderizado.
- Carga completa de pueblos y asentamientos al entrar en ellos, con espacios navegables que incluyan tiendas, taberna, casas y otros puntos de interés.
- Competiciones entre gremios basadas en peleas de jefes, con clasificaciones y recompensas para los participantes.
- Sistema de mejoras de gremio que permita desbloquear capacidades colectivas, ventajas pasivas y mejoras progresivas para los miembros.
- Notificaciones en tiempo real completamente funcionales para eventos sociales, combates de gremio y alertas de juego.
- Control por voz con reconocimiento adaptado al idioma seleccionado en la aplicación, respondiendo también en ese mismo idioma.
- Detección y escaneo contextual de peluquerías y farmacias cercanas, ampliando los tipos de establecimientos reales vinculados al juego.

---

## Licencia

Este proyecto se publica **exclusivamente con fines académicos y de demostración**.
Si deseas utilizar este proyecto, **contacta con el autor** antes de hacerlo para pedir permiso.

---

## Autor

**Unai Benito Sánchez**  
Proyecto TFG DAM  
AIDungeonMaster

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![IA](https://img.shields.io/badge/IA%20Generativa-FF6F00?style=flat-square&logo=openai&logoColor=white)
