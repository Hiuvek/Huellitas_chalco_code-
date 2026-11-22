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

class ScheduleReviewSdkTest {
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
                "hu15-${UUID.randomUUID()}",
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
        auth.signUp("Adoptante Prueba", "hu15-${UUID.randomUUID()}@example.test", "PruebaHU05!")
    }

    @After
    fun cleanup() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun schedulesExactlyOneReviewAndBoardIsLimitedToActiveRefuge(): Unit = runBlocking {
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
        val delivery =
            db.collection("adopciones")
                .document("hu12-dog-a")
                .get(Source.SERVER)
                .await()
                .getTimestamp("fechaEntrega")!!
                .toDate()
                .time
        val date =
            java.text
                .SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("America/Mexico_City") }
                .format(java.util.Date(delivery + 7 * 86_400_000L))
        val input =
            com.example.huellitaschalco.feature.shelter.followup.domain.ScheduleAttempt(
                UUID.randomUUID().toString(),
                uid,
                "hu07-a",
                "hu12-dog-a",
                date,
                "VIDEOLLAMADA",
            )
        assertFalse(repository.program(input).alreadyExisting)
        assertTrue(repository.program(input).alreadyExisting)
        val board = withTimeout(20_000) { repository.observe(uid, "hu07-a").first { !it.loading } }
        assertNull(board.error)
        assertTrue(board.adoptions.all { it.adoption.shelterId == "hu07-a" })
        val review = board.reviews.single { it.id == input.id }
        assertEquals("PROGRAMADA", review.status)
        assertEquals("VIDEOLLAMADA", review.type)
        assertNull(review.followUpId)
        assertTrue(review.createdAt > 0)
        assertEquals("hu12-dog-a", review.adoptionId)
        val before = review.createdAt
        repository.program(input)
        assertEquals(
            before,
            db.collection("revisiones")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getTimestamp("creadoEn")!!
                .toDate()
                .time,
        )
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        val blocked =
            withTimeout(20_000) {
                repository.observe(auth.user.value!!.uid, "hu07-a").first { !it.loading }
            }
        assertNotNull(blocked.error)
        assertTrue(blocked.reviews.isEmpty())
        assertTrue(blocked.adoptions.isEmpty())
        assertTrue(
            runCatching { repository.program(input.copy(uid = auth.user.value!!.uid)) }.isFailure
        )
    }
}
