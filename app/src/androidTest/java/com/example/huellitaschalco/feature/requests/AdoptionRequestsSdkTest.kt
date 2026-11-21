package com.example.huellitaschalco.feature.requests

import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.feature.application.data.FirebaseAdoptionApplicationRepository
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.requests.data.FirebaseAdoptionRequestsRepository
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

class AdoptionRequestsSdkTest {
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
                "hu05-${UUID.randomUUID()}",
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
        auth.signUp("Adoptante Prueba", "hu05-${UUID.randomUUID()}@example.test", "PruebaHU05!")
    }

    @After
    fun cleanup() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    private fun attempt(dog: String = "hu02-catalog-available") =
        ApplicationAttempt(
            UUID.randomUUID().toString(),
            auth.user.value!!.uid,
            dog,
            "hu02-chalco",
            ApplicationDraft(
                "Adoptante Prueba",
                auth.user.value!!.email,
                "5500000001",
                "CASA",
                "PROPIA",
                true,
                "3",
                "DE_4_A_8",
                false,
                "",
                "Quiero darle compañía y cuidados en un hogar seguro durante toda su vida.",
            ),
        )

    @Test
    fun listAndDetailExposeOnlyActualOwnerAnswersAndServerHistory(): Unit = runBlocking {
        val input = attempt()
        repo.submit(input)
        val reads = FirebaseAdoptionRequestsRepository(auth, db)
        val list = withTimeout(20_000) { reads.observeOwn(input.uid).first { !it.loading } }
        assertNull(list.error)
        assertEquals(1, list.requests.size)
        val record = list.requests.single()
        assertEquals(input.id, record.id)
        assertEquals("Lunita HU02", record.dogName)
        assertEquals(input.draft, record.answers)
        assertTrue(record.createdAt > 0)
        val detail =
            withTimeout(20_000) {
                reads.observeOwnDetail(input.uid, input.id).first { !it.loading }
            }
        assertNull(detail.error)
        assertEquals(record, detail.request)
        assertEquals("Enviada", detail.history.single().status)
        assertEquals(record.createdAt, detail.history.single().timestamp)
    }

    @Test
    fun anotherOwnerHasAnEmptyListAndCannotOpenThePreviousOwnersId(): Unit = runBlocking {
        val input = attempt()
        repo.submit(input)
        auth.signOut()
        auth.signUp("Otra cuenta", "hu06-other-${UUID.randomUUID()}@example.test", "PruebaHU05!")
        val reads = FirebaseAdoptionRequestsRepository(auth, db)
        val uid = auth.user.value!!.uid
        val list = withTimeout(20_000) { reads.observeOwn(uid).first { !it.loading } }
        assertNull(list.error)
        assertTrue(list.requests.isEmpty())
        val detail =
            withTimeout(20_000) { reads.observeOwnDetail(uid, input.id).first { !it.loading } }
        assertNull(detail.request)
        assertTrue(detail.history.isEmpty())
        assertNotNull(detail.error)
        assertNotNull(reads.observeOwn(input.uid).first().error)
    }

    @Test
    fun signingOutImmediatelyRemovesTheOpenPrivateDetail(): Unit = runBlocking {
        val input = attempt()
        repo.submit(input)
        val values =
            mutableListOf<com.example.huellitaschalco.feature.requests.domain.RequestDetailState>()
        val job = launch {
            FirebaseAdoptionRequestsRepository(auth, db)
                .observeOwnDetail(input.uid, input.id)
                .collect { values.add(it) }
        }
        withTimeout(20_000) { while (values.none { it.request != null }) delay(20) }
        auth.signOut()
        withTimeout(5_000) { while (values.last().error == null) delay(20) }
        assertNull(values.last().request)
        assertTrue(values.last().history.isEmpty())
        job.cancelAndJoin()
    }

    @Test
    fun disconnectionDoesNotPresentCachedPrivateAnswersAsConfirmed(): Unit = runBlocking {
        val input = attempt()
        repo.submit(input)
        db.disableNetwork().await()
        try {
            val state =
                withTimeout(8_000) {
                    FirebaseAdoptionRequestsRepository(auth, db, 500)
                        .observeOwnDetail(input.uid, input.id)
                        .first { !it.loading }
                }
            assertNotNull(state.error)
            assertNull(state.request)
            assertTrue(state.history.isEmpty())
        } finally {
            db.enableNetwork().await()
        }
        val recovered =
            withTimeout(20_000) {
                FirebaseAdoptionRequestsRepository(auth, db)
                    .observeOwnDetail(input.uid, input.id)
                    .first { !it.loading }
            }
        assertNull(recovered.error)
        assertEquals(input.id, recovered.request!!.id)
    }
}
