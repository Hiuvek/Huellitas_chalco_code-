import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// La configuración de la cuenta Firebase se agrega localmente, no se inventa para compilar.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val authEmulatorEnabled = providers.gradleProperty("firebaseAuthEmulator").orNull == "true"
val authEmulatorHost = providers.gradleProperty("firebaseAuthEmulatorHost").getOrElse("10.0.2.2")
require(authEmulatorHost in setOf("10.0.2.2", "127.0.0.1", "localhost")) {
    "Usa 10.0.2.2 para el emulador Android o 127.0.0.1 con adb reverse para un dispositivo físico."
}

// El proyecto demo se inicializa por código y no usa la configuración de la app real.
tasks.matching { it.name == "processDebugGoogleServices" }.configureEach {
    enabled = !authEmulatorEnabled
}

// AI Logic usa el proyecto real incluso si Auth/Firestore/Storage se prueban en emuladores.
// Solo se lee la configuración local existente; no se guarda en Git.
val aiConfig = file("google-services.json").takeIf { it.isFile }?.let { JsonSlurper().parse(it) as Map<*, *> }
val aiProject = (aiConfig?.get("project_info") as? Map<*, *>)?.get("project_id") as? String ?: ""
val aiClient = (aiConfig?.get("client") as? List<*>)?.filterIsInstance<Map<*, *>>()?.firstOrNull {
    ((it["client_info"] as? Map<*, *>)?.get("android_client_info") as? Map<*, *>)?.get("package_name") == "com.example.huellitaschalco"
}
val aiAppId = (aiClient?.get("client_info") as? Map<*, *>)?.get("mobilesdk_app_id") as? String ?: ""
val aiApiKey = ((aiClient?.get("api_key") as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("current_key") as? String ?: ""
fun quotedConfig(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val aiModel = providers.gradleProperty("geminiModel").getOrElse("gemini-3.5-flash-lite")

android {
    namespace = "com.example.huellitaschalco"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.huellitaschalco"
        buildConfigField("boolean", "SHOW_PREVIEW_GALLERY", "false")
        buildConfigField("String", "FIREBASE_AI_PROJECT", quotedConfig(aiProject))
        buildConfigField("String", "FIREBASE_AI_APP_ID", quotedConfig(aiAppId))
        buildConfigField("String", "FIREBASE_AI_API_KEY", quotedConfig(aiApiKey))
        buildConfigField("String", "GEMINI_MODEL", quotedConfig(aiModel))
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "@string/app_name"
        buildConfigField("boolean", "FIREBASE_AUTH_EMULATOR", "false")
        buildConfigField("String", "FIREBASE_AUTH_EMULATOR_HOST", "\"$authEmulatorHost\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SHOW_PREVIEW_GALLERY", (providers.gradleProperty("showPreviewGallery").orNull == "true").toString())
            if (authEmulatorEnabled) {
                applicationIdSuffix = ".local"
                manifestPlaceholders["appLabel"] = "Huellitas Chalco (local)"
            }
            buildConfigField("boolean", "FIREBASE_AUTH_EMULATOR", authEmulatorEnabled.toString())
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
