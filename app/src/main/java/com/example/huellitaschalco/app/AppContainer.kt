package com.example.huellitaschalco.app

import android.content.Context
import com.example.huellitaschalco.core.firebase.FirebaseProvider
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.auth.domain.AuthRepository

class AppContainer(context: Context) {
    private val firebase = FirebaseProvider(context.applicationContext)
    val authRepository: AuthRepository =
        FirebaseAuthRepository(firebase.auth, firebase.usesEmulator)
}
