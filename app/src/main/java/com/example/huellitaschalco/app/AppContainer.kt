package com.example.huellitaschalco.app

import android.content.Context
import com.example.huellitaschalco.core.firebase.FirebaseProvider
import com.example.huellitaschalco.feature.access.data.FirestoreShelterAccessRepository
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firebase = FirebaseProvider(context.applicationContext)
    val authRepository: AuthRepository =
        FirebaseAuthRepository(firebase.auth, firebase.usesEmulator)
    val shelterAccessRepository =
        FirestoreShelterAccessRepository(authRepository, firebase.firestore, scope)
}
