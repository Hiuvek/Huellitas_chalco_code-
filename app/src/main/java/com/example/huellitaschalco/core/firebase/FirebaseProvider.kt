package com.example.huellitaschalco.core.firebase

import android.content.Context
import com.example.huellitaschalco.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

/** Un proyecto demo y una app separada aíslan las pruebas del proyecto real. */
class FirebaseProvider(context: Context) {
    val usesEmulator = BuildConfig.DEBUG && BuildConfig.FIREBASE_AUTH_EMULATOR
    val auth: FirebaseAuth? =
        if (usesEmulator) {
            val app =
                FirebaseApp.getApps(context).firstOrNull { it.name == EMULATOR_APP }
                    ?: FirebaseApp.initializeApp(
                        context,
                        FirebaseOptions.Builder()
                            .setProjectId("demo-huellitas-chalco")
                            .setApplicationId("1:1234567890:android:0123456789abcdef")
                            .setApiKey("demo-firebase-auth-emulator-key")
                            .build(),
                        EMULATOR_APP,
                    )
            FirebaseAuth.getInstance(app).apply {
                useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9099)
            }
        } else {
            FirebaseApp.initializeApp(context)?.let { FirebaseAuth.getInstance(it) }
        }

    private companion object {
        const val EMULATOR_APP = "huellitas-auth-emulator"
    }
}
