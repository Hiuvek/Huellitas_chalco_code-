package com.example.huellitaschalco.nucleo.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

class ProveedorFirebase(context: Context) {
    val usesEmulator = false
    val auth: FirebaseAuth? =
        FirebaseApp.initializeApp(context)?.let { FirebaseAuth.getInstance(it) }

    val firestore: FirebaseFirestore? =
        auth?.app?.let { app ->
            FirebaseFirestore.getInstance(app).apply {
                firestoreSettings =
                    FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .build()
            }
        }

    val functions: OperacionesInvocables? =
        auth?.let { client -> firestore?.let { OperacionesSpark(it, client) } }
}
