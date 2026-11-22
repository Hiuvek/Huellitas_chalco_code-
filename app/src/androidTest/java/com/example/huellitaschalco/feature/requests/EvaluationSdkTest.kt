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

class EvaluationSdkTest {
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
                "hu13-${UUID.randomUUID()}",
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
        auth.signUp("Adoptante Prueba", "hu13-${UUID.randomUUID()}@example.test", "PruebaHU05!")
    }

    @After
    fun cleanup() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun simultaneousApprovalsOnlyPersistOneAndRetryKeepsOneHistory(): Unit = runBlocking {
        auth.signIn("hu07-a@example.test", "PruebaHU07!")
        val uid = auth.user.value!!.uid
        val evaluations =
            com.example.huellitaschalco.feature.shelter.requests.data.FirebaseEvaluationRepository(
                auth,
                db,
            )
        val ids =
            listOf("12000000-0000-4000-8000-0000000000a0", "12000000-0000-4000-8000-0000000000a1")
        val review =
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationDecision.Review
        val approve =
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationDecision.Approve
        for (id in ids) evaluations.decide(
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationAttempt(
                uid,
                "hu07-a",
                id,
                "Enviada",
                review,
            )
        )
        val inputs = ids.map {
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationAttempt(
                uid,
                "hu07-a",
                it,
                "En revisión",
                approve,
            )
        }
        val results =
            inputs.map { input -> async { runCatching { evaluations.decide(input) } } }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
        val winner =
            db.collection("perritos_control")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getString("solicitudAprobadaId")
        assertTrue(winner in ids)
        assertEquals(
            "En proceso",
            db.collection("perritos")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getString("estado"),
        )
        for (id in ids) assertEquals(
            if (id == winner) "Aprobada" else "En revisión",
            db.collection("solicitudes").document(id).get(Source.SERVER).await().getString("estado"),
        )
        val confirmed = inputs.single { it.requestId == winner }
        val history = db.collection("historial_solicitudes").document(confirmed.historyId)
        val before = history.get(Source.SERVER).await().getTimestamp("creadoEn")
        evaluations.decide(confirmed)
        assertEquals(before, history.get(Source.SERVER).await().getTimestamp("creadoEn"))
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        assertTrue(
            runCatching { evaluations.decide(confirmed.copy(uid = auth.user.value!!.uid)) }
                .isFailure
        )
    }
}
