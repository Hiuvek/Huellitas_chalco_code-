package com.example.huellitaschalco.feature.requests

import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.feature.application.data.FirebaseAdoptionApplicationRepository
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.google.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class AdoptionDeliverySdkTest {
    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuthRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var repo: FirebaseAdoptionApplicationRepository

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        app =
            FirebaseApp.initializeApp(
                ApplicationProvider.getApplicationContext<android.content.Context>(),
                FirebaseOptions.Builder()
                    .setProjectId("demo-huellitas-chalco")
                    .setApplicationId("1:1234567890:android:0123456789abcdef")
                    .setApiKey("demo-firebase-auth-emulator-key")
                    .build(),
                "hu14-${UUID.randomUUID()}",
            )
        val client =
            FirebaseAuth.getInstance(app).apply {
                useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9099)
            }
        auth = FirebaseAuthRepository(client, true)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        db.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 8180)
        repo = FirebaseAdoptionApplicationRepository(auth, db)
        auth.signUp("Adoptante Prueba", "hu14-${UUID.randomUUID()}@example.test", "PruebaHU05!")
    }

    @After
    fun cleanup() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun deliveryClosesPendingRequestsAndRetriesKeepTheSameAdoption(): Unit = runBlocking {
        auth.signIn("hu07-a@example.test", "PruebaHU07!")
        val uid = auth.user.value!!.uid
        val requestId = "12000000-0000-4000-8000-0000000000a0"
        val evaluations =
            com.example.huellitaschalco.feature.shelter.requests.data.FirebaseEvaluationRepository(
                auth,
                db,
            )
        evaluations.decide(
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationAttempt(
                uid,
                "hu07-a",
                requestId,
                "Enviada",
                com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationDecision
                    .Review,
            )
        )
        evaluations.decide(
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationAttempt(
                uid,
                "hu07-a",
                requestId,
                "En revisión",
                com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationDecision
                    .Approve,
            )
        )
        val client =
            com.example.huellitaschalco.core.firebase.EmulatorCallableFunctions(
                FirebaseAuth.getInstance(app),
                BuildConfig.FIREBASE_AUTH_EMULATOR_HOST,
            )
        val repository =
            com.example.huellitaschalco.feature.shelter.adoption.data.FirebaseAdoptionRepository(
                auth,
                db,
                client,
            )
        val date =
            java.text
                .SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("America/Mexico_City") }
                .format(java.util.Date())
        val input =
            com.example.huellitaschalco.feature.shelter.adoption.domain.AdoptionAttempt(
                uid,
                requestId,
                "hu07-a",
                "hu12-dog-a",
                date,
                true,
            )
        assertFalse(repository.record(input).alreadyExisting)
        val record =
            withTimeout(20_000) {
                repository.observeDog(uid, "hu07-a", "hu12-dog-a").first { !it.loading }
            }
        assertNull(record.error)
        assertEquals(requestId, record.adoption!!.requestId)
        assertEquals(
            "Adoptado",
            db.collection("perritos")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getString("estado"),
        )
        val other =
            db.collection("solicitudes")
                .document("12000000-0000-4000-8000-0000000000a1")
                .get(Source.SERVER)
                .await()
        assertEquals("No aprobada", other.getString("estado"))
        assertEquals(
            "El perrito fue adoptado por otra persona.",
            other.getString("motivoNoAprobacion"),
        )
        assertTrue(repository.record(input).alreadyExisting)
        assertEquals(
            record.adoption!!.createdAt,
            db.collection("adopciones")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getTimestamp("creadoEn")!!
                .toDate()
                .time,
        )
        auth.signIn("hu07-adopter@example.test", "PruebaHU07!")
        assertEquals(
            requestId,
            db.collection("adopciones")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getString("solicitudId"),
        )
        assertEquals(
            "Perrito solicitudes A",
            db.collection("perritos")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getString("nombre"),
        )
    }
}
