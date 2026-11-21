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
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class ReceivedRequestsSdkTest {
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
                "hu12-${UUID.randomUUID()}",
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
        auth.signUp("Adoptante Prueba", "hu12-${UUID.randomUUID()}@example.test", "PruebaHU05!")
    }

    @After
    fun cleanup() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun responsibleSeesOwnRefugeRequestsAndCompleteAnswers(): Unit = runBlocking {
        auth.signIn("hu07-a@example.test", "PruebaHU07!")
        val uid = auth.user.value!!.uid
        val reads = FirebaseAdoptionRequestsRepository(auth, db)
        val state =
            withTimeout(20_000) { reads.observeReceived(uid, "hu07-a").first { !it.loading } }
        assertNull(state.error)
        assertTrue(state.requests.isNotEmpty())
        assertTrue(state.requests.all { it.shelterId == "hu07-a" })
        val selected = state.requests.first { it.id == "12000000-0000-4000-8000-0000000000a0" }
        assertEquals("Solicitante demo 1", selected.answers.name)
        assertEquals("Perrito solicitudes A", selected.dogName)
        val detail =
            withTimeout(20_000) {
                reads.observeReceivedDetail(uid, "hu07-a", selected.id).first { !it.loading }
            }
        assertNull(detail.error)
        assertEquals(selected, detail.request)
        assertEquals("Enviada", detail.history.single().status)
        assertEquals("5500000010", detail.request!!.answers.phone)
    }

    @Test
    fun differentRefugeAndAdopterCannotQueryAdministrativeScope(): Unit = runBlocking {
        for (email in listOf("hu07-b@example.test", "hu07-adopter@example.test")) {
            auth.signIn(email, "PruebaHU07!")
            val uid = auth.user.value!!.uid
            val reads = FirebaseAdoptionRequestsRepository(auth, db)
            val list =
                withTimeout(20_000) { reads.observeReceived(uid, "hu07-a").first { !it.loading } }
            assertNotNull(list.error)
            assertTrue(list.requests.isEmpty())
            val detail =
                withTimeout(20_000) {
                    reads
                        .observeReceivedDetail(
                            uid,
                            "hu07-a",
                            "12000000-0000-4000-8000-0000000000a0",
                        )
                        .first { !it.loading }
                }
            assertNotNull(detail.error)
            assertNull(detail.request)
            assertTrue(detail.history.isEmpty())
        }
    }

    @Test
    fun responsibleBReadsOnlyBAndMissingIdHasNoExampleAnswers(): Unit = runBlocking {
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        val uid = auth.user.value!!.uid
        val reads = FirebaseAdoptionRequestsRepository(auth, db)
        val state =
            withTimeout(20_000) { reads.observeReceived(uid, "hu07-b").first { !it.loading } }
        assertNull(state.error)
        assertTrue(state.requests.all { it.shelterId == "hu07-b" })
        assertTrue(state.requests.any { it.id == "12000000-0000-4000-8000-0000000000b0" })
        val missing =
            withTimeout(20_000) {
                reads.observeReceivedDetail(uid, "hu07-b", "missing").first { !it.loading }
            }
        assertNotNull(missing.error)
        assertNull(missing.request)
    }
}
