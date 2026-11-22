package com.example.huellitaschalco.core.firebase

import android.graphics.Bitmap
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.feature.application.data.FirebaseAdoptionApplicationRepository
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.shelter.adoption.data.FirebaseAdoptionRepository
import com.example.huellitaschalco.feature.shelter.adoption.domain.AdoptionAttempt
import com.example.huellitaschalco.feature.shelter.dogs.data.FirebaseDogRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.followup.data.FirebaseFollowUpRepository
import com.example.huellitaschalco.feature.shelter.followup.domain.*
import com.example.huellitaschalco.feature.shelter.requests.data.FirebaseEvaluationRepository
import com.example.huellitaschalco.feature.shelter.requests.domain.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Prueba optativa real: usa los repositorios Android y no cierra la sesión del propietario. */
class SparkCloudAdoptionTest {
    @Test
    fun realAdoptionFromApplicationToFollowUp(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("sparkAdoption") == "true" && !BuildConfig.FIREBASE_AUTH_EMULATOR)
        val dogId = requireNotNull(args.getString("sparkDogId"))
        require(dogId.matches(Regex("spark-e2e-[a-z0-9-]{1,70}")))
        val context = ApplicationProvider.getApplicationContext<HuellitasApplication>()
        val ownerApp = FirebaseApp.getInstance()
        assertEquals("huellitas-chalco", ownerApp.options.projectId)
        val ownerClient = FirebaseAuth.getInstance(ownerApp)
        val ownerUid = requireNotNull(ownerClient.currentUser).uid
        val ownerAuth = context.container.authRepository
        val db = FirebaseFirestore.getInstance(ownerApp)
        val shelter = "refugio-prueba-chalco"
        assertEquals(
            true,
            db.collection("refugios")
                .document(shelter)
                .get(Source.SERVER)
                .await()
                .getBoolean("esDemostracion"),
        )
        assertEquals(
            true,
            db.collection("membresias")
                .document("${ownerUid}__$shelter")
                .get(Source.SERVER)
                .await()
                .getBoolean("activo"),
        )
        assertFalse(db.collection("perritos").document(dogId).get(Source.SERVER).await().exists())
        val photo = File(context.cacheDir, "$dogId.jpg")
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(120, 150, 110))
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
        bitmap.recycle()
        var adopterApp: FirebaseApp? = null
        var adopterDb: FirebaseFirestore? = null
        try {
            val dogs = FirebaseDogRepository(ownerAuth, db, null, false)
            dogs.publish(
                DogPublication(
                    dogId,
                    ownerUid,
                    shelter,
                    DogDraft(
                        "Prueba adopción nube",
                        "12",
                        "Meses",
                        "Mediano",
                        "Macho",
                        "Demostración escolar ficticia del proceso completo en Firebase real. La solicitud, entrega, carta y seguimiento de este registro son simulados; no representan una adopción real.",
                        listOf(LocalDogPhoto(UUID.randomUUID().toString(), photo.path)),
                    ),
                )
            ) {}
            Log.i("SparkAdoption", "PASS publicación de ficha de demostración")
            adopterApp =
                FirebaseApp.initializeApp(
                    context,
                    ownerApp.options,
                    "spark-adopter-${UUID.randomUUID()}",
                )
            val adopterClient = FirebaseAuth.getInstance(adopterApp)
            val adopterAuth = FirebaseAuthRepository(adopterClient)
            adopterDb = FirebaseFirestore.getInstance(adopterApp)
            adopterDb.firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            val email = "$dogId@example.test"
            adopterAuth.signUp(
                "Adoptante demostración escolar",
                email,
                UUID.randomUUID().toString() + "Aa1!",
            )
            val adopterUid = requireNotNull(adopterClient.currentUser).uid
            assertEquals(
                "Disponible",
                adopterDb
                    .collection("perritos")
                    .document(dogId)
                    .get(Source.SERVER)
                    .await()
                    .getString("estado"),
            )
            val applications = FirebaseAdoptionApplicationRepository(adopterAuth, adopterDb)
            val requestId = UUID.randomUUID().toString()
            val input =
                ApplicationAttempt(
                    requestId,
                    adopterUid,
                    dogId,
                    shelter,
                    ApplicationDraft(
                        "Adoptante demostración escolar",
                        email,
                        "5500000001",
                        "CASA",
                        "PROPIA",
                        true,
                        "3",
                        "DE_4_A_8",
                        false,
                        "",
                        "Demostración escolar ficticia para verificar la solicitud y el seguimiento de una adopción en la nube.",
                    ),
                )
            assertFalse(applications.submit(input).alreadyExisting)
            assertFalse(applications.submit(input).alreadyExisting)
            assertEquals(
                "Enviada",
                db.collection("solicitudes")
                    .document(requestId)
                    .get(Source.SERVER)
                    .await()
                    .getString("estado"),
            )
            Log.i("SparkAdoption", "PASS solicitud compartida y reintento sin duplicados")
            val evaluations = FirebaseEvaluationRepository(ownerAuth, db)
            evaluations.decide(
                EvaluationAttempt(
                    ownerUid,
                    shelter,
                    requestId,
                    "Enviada",
                    EvaluationDecision.Review,
                )
            )
            evaluations.decide(
                EvaluationAttempt(
                    ownerUid,
                    shelter,
                    requestId,
                    "En revisión",
                    EvaluationDecision.Approve,
                )
            )
            assertEquals(
                "Aprobada",
                adopterDb
                    .collection("solicitudes")
                    .document(requestId)
                    .get(Source.SERVER)
                    .await()
                    .getString("estado"),
            )
            Log.i("SparkAdoption", "PASS evaluación visible al adoptante independiente")
            val operations = SparkOperations(db, ownerClient)
            val deliveries = FirebaseAdoptionRepository(ownerAuth, db, operations)
            val today =
                SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                    .apply { timeZone = TimeZone.getTimeZone("America/Mexico_City") }
                    .format(Date())
            val delivery = AdoptionAttempt(ownerUid, requestId, shelter, dogId, today, true)
            assertFalse(deliveries.record(delivery).alreadyExisting)
            assertTrue(deliveries.record(delivery).alreadyExisting)
            assertEquals(
                "Adoptado",
                adopterDb
                    .collection("perritos")
                    .document(dogId)
                    .get(Source.SERVER)
                    .await()
                    .getString("estado"),
            )
            Log.i("SparkAdoption", "PASS entrega simulada y reintento sin duplicados")
            val followups = FirebaseFollowUpRepository(ownerAuth, db, operations)
            val reviewId = UUID.randomUUID().toString()
            followups.program(
                ScheduleAttempt(reviewId, ownerUid, shelter, dogId, today, "VIDEOLLAMADA")
            )
            val result =
                FollowUpAttempt(
                    ownerUid,
                    shelter,
                    reviewId,
                    today,
                    "BUENA",
                    "Resultado ficticio de prueba escolar: se comprueba la persistencia del seguimiento en la nube.",
                    LocalDogPhoto(UUID.randomUUID().toString(), photo.path),
                    "CERRAR",
                    "",
                    "",
                )
            assertFalse(followups.record(result).alreadyExisting)
            assertTrue(followups.record(result).alreadyExisting)
            assertEquals(
                "REALIZADA",
                db.collection("revisiones")
                    .document(reviewId)
                    .get(Source.SERVER)
                    .await()
                    .getString("estado"),
            )
            val saved = db.collection("seguimientos").document(reviewId).get(Source.SERVER).await()
            assertTrue(saved.exists())
            assertArrayEquals(
                photo.readBytes(),
                followups.photo(requireNotNull(result.photoPath())),
            )
            assertTrue(
                runCatching {
                        adopterDb
                            .collection("seguimientos")
                            .document(reviewId)
                            .get(Source.SERVER)
                            .await()
                    }
                    .isFailure
            )
            Log.i(
                "SparkAdoption",
                "PASS seguimiento con fotografía privada; acceso de adoptante rechazado",
            )
            assertEquals(ownerUid, ownerClient.currentUser?.uid)
            Log.i(
                "SparkAdoption",
                "RESULT dog=$dogId request=$requestId review=$reviewId; sesión original conservada",
            )
        } finally {
            adopterDb?.terminate()?.await()
            adopterApp?.delete()
            photo.delete()
        }
    }
}
