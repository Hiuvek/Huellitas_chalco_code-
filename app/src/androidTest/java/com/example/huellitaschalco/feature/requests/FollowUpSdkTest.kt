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

class FollowUpSdkTest {
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
                "hu16-${UUID.randomUUID()}",
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
        auth.signUp("Adoptante Prueba", "hu16-${UUID.randomUUID()}@example.test", "PruebaHU05!")
    }

    @After
    fun cleanup() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun recordsClosedResultWithIdempotenceAndPrivateReadIsolation(): Unit = runBlocking {
        auth.signIn("hu07-a@example.test", "PruebaHU07!")
        val uid = auth.user.value!!.uid
        val functions =
            com.example.huellitaschalco.core.firebase.SparkOperations(
                db,
                FirebaseAuth.getInstance(app),
            )
        val repository =
            com.example.huellitaschalco.feature.shelter.followup.data.FirebaseFollowUpRepository(
                auth,
                db,
                functions,
            )
        val id = UUID.randomUUID().toString()
        repository.program(
            com.example.huellitaschalco.feature.shelter.followup.domain.ScheduleAttempt(
                id,
                uid,
                "hu07-a",
                "hu12-dog-a",
                "2026-12-01",
                "VIDEOLLAMADA",
            )
        )
        val input =
            com.example.huellitaschalco.feature.shelter.followup.domain.FollowUpAttempt(
                uid,
                "hu07-a",
                id,
                java.text
                    .SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("America/Mexico_City") }
                    .format(
                        db.collection("adopciones")
                            .document("hu12-dog-a")
                            .get(Source.SERVER)
                            .await()
                            .getTimestamp("fechaEntrega")!!
                            .toDate()
                    ),
                "REGULAR",
                "Se recomienda acompañar la adaptación con paseos cortos.",
                null,
                "CERRAR",
                "",
                "",
            )
        assertFalse(repository.record(input).alreadyExisting)
        assertTrue(repository.record(input).alreadyExisting)
        val board =
            withTimeout(20_000) {
                repository.observe(uid, "hu07-a").first {
                    !it.loading && it.results.any { r -> r.id == id }
                }
            }
        assertNull(board.error)
        val result = board.results.single { it.id == id }
        assertEquals(input.notes, result.notes)
        assertEquals(uid, result.actorUid)
        assertTrue(result.createdAt > 0)
        assertEquals("REALIZADA", board.reviews.single { it.id == id }.status)
        assertFalse(
            board.reviews.any {
                it.id ==
                    com.example.huellitaschalco.feature.shelter.followup.domain.nextReviewId(id)
            }
        )
        assertTrue(
            runCatching {
                    repository.record(
                        input.copy(notes = "Otro resultado no debe sustituir al original.")
                    )
                }
                .isFailure
        )
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        assertTrue(
            runCatching { repository.record(input.copy(uid = auth.user.value!!.uid)) }.isFailure
        )
        val blocked =
            withTimeout(20_000) {
                repository.observe(auth.user.value!!.uid, "hu07-a").first { !it.loading }
            }
        assertNotNull(blocked.error)
        assertTrue(blocked.results.isEmpty())
        auth.signOut()
        assertTrue(
            withTimeout(10_000) { repository.observe(uid, "hu07-a").first { !it.loading } }
                .results
                .isEmpty()
        )
    }
}
