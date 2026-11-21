package com.example.huellitaschalco.core.firebase

import android.content.Context
import com.example.huellitaschalco.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.storage.FirebaseStorage

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
                            .setStorageBucket("demo-huellitas-chalco.appspot.com")
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

    val firestore: FirebaseFirestore? =
        auth?.app?.let { app ->
            FirebaseFirestore.getInstance(app).apply {
                firestoreSettings =
                    FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .build()
                if (usesEmulator) useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 8180)
            }
        }

    val storage: FirebaseStorage? =
        auth
            ?.app
            ?.takeIf { !it.options.storageBucket.isNullOrBlank() }
            ?.let { app ->
                FirebaseStorage.getInstance(app).apply {
                    if (usesEmulator) useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9199)
                    maxUploadRetryTimeMillis = 20_000
                    maxOperationRetryTimeMillis = 15_000
                }
            }

    private companion object {
        const val EMULATOR_APP = "huellitas-auth-emulator"
    }
}
