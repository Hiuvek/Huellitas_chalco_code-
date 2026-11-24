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

// AI Logic utiliza la configuración local del proyecto Firebase.
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
        versionCode = 13
        versionName = "1.5.1"

        manifestPlaceholders["appLabel"] = "@string/app_name"
    }

    buildTypes {
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.kotlinx.coroutines.play.services)
}
