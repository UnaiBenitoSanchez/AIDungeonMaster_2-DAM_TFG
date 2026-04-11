# AIDungeonMaster

AIDungeonMaster es una aplicación Android desarrollada en Kotlin que combina narrativa interactiva generada por inteligencia artificial, persistencia en la nube, sistemas de progresión RPG y funcionalidades avanzadas como generación de imágenes, procesamiento en Python, renderizado 3D y realidad aumentada.

## Descripción general

El proyecto implementa una experiencia de rol móvil en la que el usuario puede:

- registrarse e iniciar sesión;
- crear y gestionar personajes;
- iniciar aventuras dinámicas guiadas por un motor narrativo basado en IA;
- participar en combates;
- gestionar inventario, progreso, ranking, logros y diario;
- explorar ubicaciones persistentes del mundo del juego;
- utilizar funciones contextuales como QR, OCR, 3D y AR.

La aplicación está diseñada sobre una arquitectura modular inspirada en MVVM, integrando servicios externos y almacenamiento remoto mediante Firebase.

---

## Características principales

### Núcleo jugable
- Creación y selección de personajes.
- Sistema de aventura narrativa dinámica.
- Combate por turnos con enemigos generados.
- Gestión de inventario, equipo, monedas y progresión.
- Diario narrativo persistente.
- Bestiario, ranking y logros.

### Funcionalidades avanzadas
- Generación de narrativa mediante API externa de IA.
- Generación de retratos y monstruos mediante IA.
- Procesamiento textual con Python embebido.
- Visualización 3D de ubicaciones con Three.js en WebView.
- Mapa en realidad aumentada con ARCore.
- Escaneo QR y OCR con ML Kit.
- Notificaciones y tareas programadas con WorkManager.

---

## Stack tecnológico

### Lenguajes
- **Kotlin**: lenguaje principal de la aplicación.
- **Python**: procesamiento narrativo auxiliar mediante Chaquopy.
- **JavaScript**: renderizado 3D con Three.js.
- **HTML**: contenido embebido en WebView.
- **JSON**: intercambio de datos entre cliente, APIs y almacenamiento.

### Frameworks y librerías
- **Jetpack Compose**: interfaz declarativa.
- **Navigation Compose**: navegación.
- **ViewModel + StateFlow**: gestión reactiva del estado.
- **Firebase Authentication**: autenticación.
- **Cloud Firestore**: persistencia NoSQL en la nube.
- **Retrofit**: cliente de APIs REST.
- **OkHttp**: comunicaciones HTTP.
- **Gson**: serialización y deserialización JSON.
- **Chaquopy**: integración de Python en Android.
- **CameraX**: acceso a cámara.
- **ML Kit**: QR y reconocimiento de texto.
- **WorkManager**: tareas en segundo plano.
- **Google Play Services Location**: localización.
- **ARCore**: realidad aumentada.
- **SceneView / Filament**: renderizado AR/3D.
- **Three.js**: representación 3D procedural en WebView.
- **Coil**: carga de imágenes.
- **Cloudflare Workers AI**: generación visual.
- **Groq API**: generación narrativa.

---

## Arquitectura

El proyecto sigue una arquitectura modular basada en **MVVM**.

### Capas principales

#### 1. Capa de presentación
Compuesta por pantallas desarrolladas con Jetpack Compose.

Responsabilidades:
- representación de estado;
- interacción con el usuario;
- navegación entre módulos;
- consumo de estado expuesto por los ViewModel.

#### 2. Capa de lógica de presentación
Implementada mediante `ViewModel`.

Responsabilidades:
- coordinación entre UI y datos;
- mantenimiento del estado reactivo;
- validación de entradas;
- gestión del flujo funcional de cada pantalla.

#### 3. Capa de datos
Formada por modelos, repositorios y servicios.

Responsabilidades:
- acceso a Firebase Authentication;
- lectura y escritura en Firestore;
- consumo de APIs externas;
- encapsulación de operaciones persistentes.

#### 4. Capa de integración externa
Incluye servicios auxiliares y motores adicionales.

Responsabilidades:
- llamadas al motor narrativo;
- generación de imágenes;
- ejecución de scripts Python;
- integración con AR, cámara y localización.

---

## Estructura del proyecto

La organización del repositorio se basa en paquetes funcionales y técnicos.

```text
app/
 └── src/
      └── main/
           ├── java/.../data/            # Modelos de dominio, DTOs, respuestas y servicios
           ├── java/.../repository/      # Acceso a datos y persistencia
           ├── java/.../navigation/      # Rutas y flujo de navegación
           ├── java/.../ui/              # Pantallas y componentes Compose
           ├── java/.../viewmodel/       # ViewModels y estado reactivo
           ├── java/.../utils/           # Utilidades auxiliares
           ├── java/.../workers/         # Tareas en segundo plano
           ├── java/.../python/          # Integración Kotlin-Python
           └── python/                   # Scripts Python para diario y procesamiento textual
```

> La estructura exacta puede variar ligeramente según ramas o reorganizaciones internas del proyecto.

---

## Módulos funcionales

### Autenticación
Gestiona:
- registro de usuarios;
- inicio y cierre de sesión;
- asociación de datos del jugador con `auth.uid`.

Tecnologías:
- Firebase Authentication
- Firestore

### Personajes
Gestiona:
- creación de personajes;
- persistencia de estadísticas;
- raza, clase, subclase y atributos;
- retrato generado;
- selección y borrado.

### Aventura narrativa
Gestiona:
- contexto actual de la partida;
- envío de acciones del jugador;
- recepción de respuesta estructurada desde IA;
- aplicación de cambios al estado del juego.

### Combate
Gestiona:
- aparición de enemigos;
- cálculo de daño y curación;
- recompensas;
- impacto en progreso, diario e inventario.

### Inventario y progresión
Gestiona:
- objetos;
- equipo;
- monedas;
- experiencia;
- nivel;
- bonificaciones.

### Diario narrativo
Gestiona:
- almacenamiento de eventos;
- resumen de sesiones;
- agrupación por capítulos;
- reescritura estilizada.

Tecnologías:
- Firestore
- Python + Chaquopy

### Mapa y exploración
Gestiona:
- localizaciones descubiertas;
- persistencia del mapa;
- visualización del mundo;
- representación 3D y AR.

### Integración contextual
Gestiona:
- QR;
- OCR;
- cámara;
- localización;
- eventos ligados al entorno real.

---

## Flujo técnico principal

1. El usuario se autentica mediante Firebase.
2. Crea o selecciona un personaje persistido en Firestore.
3. Inicia una aventura.
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
8. Los subsistemas auxiliares enriquecen la experiencia con imágenes, resumen narrativo, AR, 3D o notificaciones.

---

## Estado del proyecto

Proyecto académico de TFG orientado a exploración de:
- desarrollo Android moderno;
- arquitectura modular;
- integración de IA generativa;
- persistencia cloud;
- interacción enriquecida mediante AR, 3D y visión artificial.

---

## Futuras mejoras

- ampliación del sistema de clases, razas y subclases;
- mayor profundidad táctica del combate;
- moderación avanzada del contenido generado;
- modo multijugador o cooperativo;
- panel administrativo o backend dedicado;
- analítica de uso;
- sistema de guardado offline sincronizable;
- expansión del mapa y generación procedural avanzada.

---

## Autor

**Unai Benito Sánchez**  
Proyecto TFG DAM  
AIDungeonMaster

---

## Licencia

- Licencia académica/no comercial
