package com.example.huellitaschalco.feature.catalog

import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.feature.catalog.data.FirebaseCatalogRepository
import com.example.huellitaschalco.feature.catalog.domain.PublicCatalog
import com.google.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Usa fichas ficticias de seed:catalog y un FirebaseApp sin sesión. Nunca conecta a la nube. */
class FirebaseCatalogSdkTest {
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private lateinit var repo: FirebaseCatalogRepository
    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        app =
            FirebaseApp.initializeApp(
                ApplicationProvider.getApplicationContext<android.content.Context>(),
                FirebaseOptions.Builder()
                    .setProjectId("demo-huellitas-chalco")
                    .setStorageBucket("demo-huellitas-chalco.appspot.com")
                    .setApplicationId("1:1234567890:android:0123456789abcdef")
                    .setApiKey("demo-firebase-auth-emulator-key")
                    .build(),
                "hu02-${UUID.randomUUID()}",
            )
        FirebaseAuth.getInstance(app).useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9099)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        db.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 8180)
        repo = FirebaseCatalogRepository(db, 2_000)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun cleanup() {
        if (::scope.isInitialized) scope.cancel()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    private suspend fun ready(): PublicCatalog =
        withTimeout(10_000) { repo.observe().first { !it.loading }.also { assertNull(it.error) } }

    private fun observe() = repo.observe().stateIn(scope, SharingStarted.Eagerly, PublicCatalog())

    private suspend fun StateFlow<PublicCatalog>.awaitReady(
        predicate: (PublicCatalog) -> Boolean
    ): PublicCatalog =
        withTimeout(10_000) { first { !it.loading && it.error == null && predicate(it) } }

    private val available = "hu02-catalog-available"

    @Test
    fun guestReadsChalcoCatalogAndPublishedPhotoOnly(): Unit = runBlocking {
        assertNull(FirebaseAuth.getInstance(app).currentUser)
        val state = ready()
        val ids = state.dogs.map { it.dog.id }
        assertTrue(available in ids)
        assertTrue("hu02-catalog-process" in ids)
        for (key in listOf("adopted", "draft", "outside", "inactive")) assertFalse(
            "hu02-catalog-$key" in ids
        )
        val dog = state.dogs.first { it.dog.id == available }
        assertEquals("Refugio catálogo hu02-chalco", dog.shelterName)
        assertEquals("Disponible", dog.dog.status)
        val storage =
            FirebaseStorage.getInstance(app).apply {
                useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9199)
            }
        val bytes =
            storage.reference.child(dog.dog.photos.first().path).getBytes(2 * 1024 * 1024).await()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(bitmap)
        bitmap!!.recycle()
    }

    @Test
    fun adoptionRemovesDogFromAnAlreadyOpenCatalog(): Unit = runBlocking {
        val states = observe()
        states.awaitReady { it.dogs.any { d -> d.dog.id == available } }
        try {
            patch("perritos/$available", "estado", "Adoptado")
            states.awaitReady { it.dogs.none { d -> d.dog.id == available } }
        } finally {
            patch("perritos/$available", "estado", "Disponible")
        }
    }

    @Test
    fun deactivatingShelterRemovesItsDogsLive(): Unit = runBlocking {
        val states = observe()
        states.awaitReady { it.dogs.any { d -> d.dog.id == available } }
        try {
            patch("refugios/hu02-chalco", "activo", false)
            states.awaitReady { it.dogs.none { d -> d.dog.shelterId == "hu02-chalco" } }
        } finally {
            patch("refugios/hu02-chalco", "activo", true)
        }
    }

    @Test
    fun editedNameAppearsInExistingCatalog(): Unit = runBlocking {
        val states = observe()
        states.awaitReady { it.dogs.any { d -> d.dog.id == available } }
        try {
            patch("perritos/$available", "nombre", "Lunita editada HU02")
            states.awaitReady {
                it.dogs.any { d -> d.dog.id == available && d.dog.name == "Lunita editada HU02" }
            }
        } finally {
            patch("perritos/$available", "nombre", "Lunita HU02")
        }
    }

    @Test
    fun offlineCacheDoesNotPretendToBeCurrentAndCanRecover(): Unit = runBlocking {
        ready()
        db.disableNetwork().await()
        val states = observe()
        try {
            val failed = withTimeout(8_000) { states.first { it.error != null } }
            assertTrue(failed.dogs.isEmpty())
        } finally {
            db.enableNetwork().await()
        }
        states.awaitReady { it.dogs.any { d -> d.dog.id == available } }
    }

    @Test
    fun missingConfigurationReportsAnErrorInsteadOfDemoData(): Unit = runBlocking {
        val state = FirebaseCatalogRepository(null).observe().first()
        assertFalse(state.loading)
        assertNotNull(state.error)
        assertTrue(state.dogs.isEmpty())
    }

    private suspend fun patch(document: String, field: String, value: Any) =
        withContext(Dispatchers.IO) {
            check(BuildConfig.FIREBASE_AUTH_EMULATOR)
            val connection =
                URI(
                        "http://${BuildConfig.FIREBASE_AUTH_EMULATOR_HOST}:8180/v1/projects/demo-huellitas-chalco/databases/(default)/documents/$document?updateMask.fieldPaths=$field"
                    )
                    .toURL()
                    .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("Authorization", "Bearer owner")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.doOutput = true
                val typed =
                    if (value is Boolean) JSONObject().put("booleanValue", value)
                    else JSONObject().put("stringValue", value)
                connection.outputStream.use {
                    it.write(
                        JSONObject()
                            .put("fields", JSONObject().put(field, typed))
                            .toString()
                            .toByteArray()
                    )
                }
                check(connection.responseCode in 200..299) {
                    "No se pudo preparar la ficha del emulador: ${connection.responseCode}"
                }
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
            Unit
        }
}
