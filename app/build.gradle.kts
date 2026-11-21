plugins {
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

android {
    namespace = "com.example.huellitaschalco"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.huellitaschalco"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "@string/app_name"
        buildConfigField("boolean", "FIREBASE_AUTH_EMULATOR", "false")
        buildConfigField("String", "FIREBASE_AUTH_EMULATOR_HOST", "\"$authEmulatorHost\"")
    }

    buildTypes {
        debug {
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

dependencies {
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
