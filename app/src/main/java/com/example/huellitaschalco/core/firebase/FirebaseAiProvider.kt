package com.example.huellitaschalco.core.firebase

import android.content.Context
import com.example.huellitaschalco.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** AI Logic no tiene emulador: solo las acciones explícitas de Gemini usan este proyecto real. */
class FirebaseAiProvider(private val context: Context) {
    val app: FirebaseApp? by lazy {
        if (
            BuildConfig.FIREBASE_AI_APP_ID.isBlank() ||
                BuildConfig.FIREBASE_AI_API_KEY.isBlank() ||
                BuildConfig.FIREBASE_AI_PROJECT.isBlank() ||
                BuildConfig.FIREBASE_AI_PROJECT.startsWith("demo-")
        )
            null
        else {
            val app =
                FirebaseApp.getApps(context).firstOrNull { it.name == "huellitas-gemini" }
                    ?: FirebaseApp.initializeApp(
                        context,
                        FirebaseOptions.Builder()
                            .setProjectId(BuildConfig.FIREBASE_AI_PROJECT)
                            .setApplicationId(BuildConfig.FIREBASE_AI_APP_ID)
                            .setApiKey(BuildConfig.FIREBASE_AI_API_KEY)
                            .build(),
                        "huellitas-gemini",
                    )
            FirebaseAppCheck.getInstance(app)
                .installAppCheckProviderFactory(
                    if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
                    else PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            app
        }
    }
}
