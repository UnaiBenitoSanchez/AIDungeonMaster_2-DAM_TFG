import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.chaquo.python")
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.example.aidungeonmaster"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aidungeonmaster"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "4.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inyectar la API Key como constante de compilación
        buildConfigField(
            "String",
            "GROQ_API_KEY",
            "\"${localProps.getProperty("GROQ_API_KEY", "")}\""
        )

        // Gemini API Key para generación de imágenes con Imagen 3
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\""
        )

        // APIs de Cloudflare para la generación de imagenes
        buildConfigField(
            "String",
            "CLOUDFLARE_ACCOUNT_ID",
            "\"${localProps.getProperty("CLOUDFLARE_ACCOUNT_ID", "")}\""
        )

        buildConfigField(
            "String",
            "CLOUDFLARE_API_TOKEN",
            "\"${localProps.getProperty("CLOUDFLARE_API_TOKEN", "")}\""
        )

        // Inicio de sesión con Google
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )

        buildConfigField(
            "String",
            "PUSH_GATEWAY_URL",
            "\"${localProps.getProperty("PUSH_GATEWAY_URL", "")}\""
        )

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
    }
    sourceSets {
        getByName("main") {
            srcDir("src/main/python")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Accompanist
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // IA Generativa
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // GSON
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp para HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coil para cargar imágenes (opcional, para imageUrl)
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.compose.material:material-icons-extended")

    // --- ARSENAL PARA EL QR ---
    // CameraX (Librerías base y vista)
    val cameraxVersion = "1.3.1" // O la última versión estable
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit Barcode Scanning (Para detectar el QR)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // ML Kit Text Recognition (Para detectar supermercados en el mundo real)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Iconos extendidos de Material (para el candado, mochila, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // Para solucionar el error del ListenableFuture
    implementation("com.google.guava:guava:31.0.1-android")

    // Opcional: Si usas las funciones de extensión de Guava para Kotlin
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")

    // WorkManager (para RankingCheckWorker e InactivityWorker)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Localización (para SupermarketProximityWorker)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // --- REALIDAD AUMENTADA ---
    // ARCore: motor de AR de Google
    implementation("com.google.ar:core:1.43.0")
    // SceneView: wrapper Compose sobre ARCore + Filament (renderer 3D)
    implementation("io.github.sceneview:arsceneview:2.2.1")

    // Inicio de sesión con Google
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Notificaciones
    implementation("com.google.firebase:firebase-messaging-ktx")
}