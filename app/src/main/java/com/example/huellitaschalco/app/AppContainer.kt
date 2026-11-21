package com.example.huellitaschalco.app

import android.content.Context
import com.example.huellitaschalco.core.firebase.FirebaseProvider
import com.example.huellitaschalco.feature.access.data.FirestoreShelterAccessRepository
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.shelter.dogs.data.AndroidDogPhotoImporter
import com.example.huellitaschalco.feature.shelter.dogs.data.FirebaseDogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firebase = FirebaseProvider(context.applicationContext)
    val authRepository: AuthRepository =
        FirebaseAuthRepository(firebase.auth, firebase.usesEmulator)
    val catalogRepository =
        com.example.huellitaschalco.feature.catalog.data.FirebaseCatalogRepository(
            firebase.firestore
        )
    val publicShelterRepository =
        com.example.huellitaschalco.feature.refuge.data.FirebasePublicShelterRepository(
            firebase.firestore
        )
    val adoptionApplicationRepository =
        com.example.huellitaschalco.feature.application.data.FirebaseAdoptionApplicationRepository(
            authRepository,
            firebase.firestore,
        )
    val adoptionRequestsRepository =
        com.example.huellitaschalco.feature.requests.data.FirebaseAdoptionRequestsRepository(
            authRepository,
            firebase.firestore,
        )
    val evaluationRepository =
        com.example.huellitaschalco.feature.shelter.requests.data.FirebaseEvaluationRepository(
            authRepository,
            firebase.firestore,
        )
    val adoptionRepository =
        com.example.huellitaschalco.feature.shelter.adoption.data.FirebaseAdoptionRepository(
            authRepository,
            firebase.firestore,
            firebase.functions,
        )
    val dogPhotoImporter = AndroidDogPhotoImporter(context.applicationContext)
    val dogRepository =
        FirebaseDogRepository(
            authRepository,
            firebase.firestore,
            firebase.storage,
            firebase.usesEmulator,
        )
    val shelterAccessRepository =
        FirestoreShelterAccessRepository(authRepository, firebase.firestore, scope)
}
