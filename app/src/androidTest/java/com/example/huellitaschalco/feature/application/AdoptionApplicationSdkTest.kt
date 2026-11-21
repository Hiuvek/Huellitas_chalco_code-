package com.example.huellitaschalco.feature.application

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
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class AdoptionApplicationSdkTest {
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
    fun newAuthenticatedAccountPersistsRequestUniqueKeyAndHistory(): Unit = runBlocking {
        val input = attempt()
        val result = repo.submit(input)
        assertEquals(input.id, result.id)
        assertFalse(result.alreadyExisting)
        assertFalse(
            db.collection("usuarios").document(input.uid).get(Source.SERVER).await().exists()
        )
        val request = db.collection("solicitudes").document(result.id).get(Source.SERVER).await()
        assertEquals(input.uid, request.getString("usuarioUid"))
        assertEquals("Enviada", request.getString("estado"))
        assertNotNull(request.getTimestamp("creadoEn"))
        assertEquals("5500000001", (request.get("contacto") as Map<*, *>)["telefono"])
        assertEquals(
            result.id,
            db.collection("solicitudes_claves")
                .document(applicationKey(input.uid, input.dogId))
                .get(Source.SERVER)
                .await()
                .getString("solicitudId"),
        )
        val history =
            db.collection("historial_solicitudes")
                .document("${result.id}-1")
                .get(Source.SERVER)
                .await()
        assertEquals("Enviada", history.getString("estadoNuevo"))
        assertEquals(input.uid, history.getString("actorUid"))
        assertNotNull(history.getTimestamp("creadoEn"))
    }

    @Test
    fun retryKeepsOneRequestOneHistoryAndOriginalTimestamp(): Unit = runBlocking {
        val input = attempt()
        val first = repo.submit(input)
        val before = db.collection("solicitudes").document(first.id).get(Source.SERVER).await()
        assertEquals(first, repo.submit(input))
        assertEquals(
            before.getTimestamp("creadoEn"),
            db.collection("solicitudes")
                .document(first.id)
                .get(Source.SERVER)
                .await()
                .getTimestamp("creadoEn"),
        )
        assertEquals(
            1,
            db.collection("solicitudes")
                .whereEqualTo("usuarioUid", input.uid)
                .get(Source.SERVER)
                .await()
                .size(),
        )
        assertEquals(
            1,
            db.collection("historial_solicitudes")
                .whereEqualTo("usuarioUid", input.uid)
                .get(Source.SERVER)
                .await()
                .size(),
        )
    }

    @Test
    fun twoConcurrentAttemptsConvergeToOneRequest(): Unit = runBlocking {
        val first = attempt()
        val second = attempt()
        val results = coroutineScope {
            listOf(async { repo.submit(first) }, async { repo.submit(second) }).awaitAll()
        }
        assertEquals(results[0].id, results[1].id)
        assertEquals(1, results.count { it.alreadyExisting })
        assertEquals(
            1,
            db.collection("solicitudes")
                .whereEqualTo("usuarioUid", first.uid)
                .get(Source.SERVER)
                .await()
                .size(),
        )
        assertEquals(
            1,
            db.collection("historial_solicitudes")
                .whereEqualTo("usuarioUid", first.uid)
                .get(Source.SERVER)
                .await()
                .size(),
        )
    }

    @Test
    fun offlineSubmissionDoesNotQueueWritesAndCanRetry(): Unit = runBlocking {
        val input = attempt()
        db.disableNetwork().await()
        try {
            repo.submit(input)
            fail("No debe confirmar sin servidor")
        } catch (_: FirebaseFirestoreException) {} finally {
            db.enableNetwork().await()
        }
        assertFalse(
            db.collection("solicitudes").document(input.id).get(Source.SERVER).await().exists()
        )
        assertFalse(
            db.collection("solicitudes_claves")
                .document(applicationKey(input.uid, input.dogId))
                .get(Source.SERVER)
                .await()
                .exists()
        )
        assertEquals(input.id, repo.submit(input).id)
    }

    @Test
    fun anotherAccountOrRefugeCannotReadPrivateResponses(): Unit = runBlocking {
        val input = attempt()
        repo.submit(input)
        auth.signOut()
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        for ((collection, id) in
            listOf(
                "solicitudes" to input.id,
                "historial_solicitudes" to "${input.id}-1",
                "solicitudes_claves" to applicationKey(input.uid, input.dogId),
            )) {
            try {
                db.collection(collection).document(id).get(Source.SERVER).await()
                fail("No debe leer datos ajenos")
            } catch (error: FirebaseFirestoreException) {
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
            }
        }
    }

    @Test
    fun inProcessDogRejectsNewRequestsWithoutPartialDocuments(): Unit = runBlocking {
        val input = attempt("hu02-catalog-process")
        try {
            repo.submit(input)
            fail("No debe admitir solicitudes")
        } catch (_: ApplicationUnavailable) {}
        assertFalse(
            db.collection("solicitudes").document(input.id).get(Source.SERVER).await().exists()
        )
        assertFalse(
            db.collection("solicitudes_claves")
                .document(applicationKey(input.uid, input.dogId))
                .get(Source.SERVER)
                .await()
                .exists()
        )
    }

    @Test
    fun switchingAccountsCannotSubmitAnOldOwnersDraft(): Unit = runBlocking {
        val input = attempt()
        auth.signOut()
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        try {
            repo.submit(input)
            fail("No debe enviar con otra cuenta")
        } catch (_: IllegalArgumentException) {}
        assertFalse(
            db.collection("solicitudes").document(input.id).get(Source.SERVER).await().exists()
        )
    }

    @Test
    fun newAttemptReturnsExistingRequestWithoutOverwritingItsAnswers(): Unit = runBlocking {
        val input = attempt()
        val first = repo.submit(input)
        val next = attempt().copy(draft = input.draft.copy(phone = "5500000002"))
        val result = repo.submit(next)
        assertEquals(first.id, result.id)
        assertTrue(result.alreadyExisting)
        val data = db.collection("solicitudes").document(first.id).get(Source.SERVER).await()
        assertEquals("5500000001", (data.get("contacto") as Map<*, *>)["telefono"])
    }
}
