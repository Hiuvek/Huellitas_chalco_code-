package com.example.huellitaschalco.feature.shelter.dogs

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.shelter.dogs.data.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.google.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class DogPublicationSdkTest {
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuthRepository
    private lateinit var storage: FirebaseStorage
    private lateinit var repository: FirebaseDogRepository
    private lateinit var importer: AndroidDogPhotoImporter
    private val files = mutableListOf<File>()
    private val photos = mutableListOf<LocalDogPhoto>()

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        app =
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("demo-huellitas-chalco")
                    .setStorageBucket("demo-huellitas-chalco.appspot.com")
                    .setApplicationId("1:1234567890:android:0123456789abcdef")
                    .setApiKey("demo-firebase-auth-emulator-key")
                    .build(),
                "hu08-${UUID.randomUUID()}",
            )
        val client = FirebaseAuth.getInstance(app)
        client.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9099)
        auth = FirebaseAuthRepository(client, true)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        db.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 8180)
        storage = FirebaseStorage.getInstance(app)
        storage.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9199)
        storage.maxUploadRetryTimeMillis = 5000
        repository = FirebaseDogRepository(auth, db, storage, true)
        importer = AndroidDogPhotoImporter(context)
        auth.signIn("hu07-a@example.test", "PruebaHU07!")
    }

    @After
    fun cleanup() {
        photos.forEach { if (::importer.isInitialized) importer.remove(it) }
        files.forEach { it.delete() }
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        if (::app.isInitialized) app.delete()
    }

    private fun image(): File {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "hu08-${UUID.randomUUID()}.png").also { files += it }
        val bitmap = Bitmap.createBitmap(80, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(160, 105, 70))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private suspend fun publication(): DogPublication {
        val photo = importer.import(Uri.fromFile(image()).toString()).also { photos += it }
        return DogPublication(
            UUID.randomUUID().toString(),
            auth.user.value!!.uid,
            "hu07-a",
            DogDraft(
                "Luna prueba SDK",
                "18",
                "Meses",
                "Mediano",
                "Hembra",
                "Perrita ficticia para probar la publicación, tranquila y acostumbrada a caminar acompañada.",
                listOf(photo),
            ),
        )
    }

    @Test
    fun publishesPhotoAndAtomicRecordThenRetryDoesNotDuplicate(): Unit = runBlocking {
        val input = publication()
        val result = repository.publish(input) {}
        assertEquals(input.id, result.id)
        val snapshot = db.collection("perritos").document(input.id).get(Source.SERVER).await()
        assertTrue(snapshot.getBoolean("publicado") == true)
        assertEquals(18L, snapshot.getLong("edadMeses"))
        assertEquals("Sin información", snapshot.getString("vacunacion"))
        assertNotNull(snapshot.getTimestamp("creadoEn"))
        assertEquals(
            "hu07-a",
            db.collection("perritos_control")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("refugioId"),
        )
        assertTrue(repository.photo(result.photos.first().path).isNotEmpty())
        assertEquals(result, repository.publish(input) {})
        assertEquals(
            snapshot.getTimestamp("creadoEn"),
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getTimestamp("creadoEn"),
        )
        assertTrue(
            repository.observe("hu07-a").let { flow ->
                kotlinx.coroutines
                    .withTimeout(10_000) { flow.first { !it.loading } }
                    .dogs
                    .any { it.id == input.id }
            }
        )
    }

    @Test
    fun failedSecondPhotoDoesNotPublishAndCanBeRetried(): Unit = runBlocking {
        val original = publication()
        val bad = image()
        bad.writeBytes(ByteArray(2 * 1024 * 1024 + 1))
        val input =
            original.copy(
                draft =
                    original.draft.copy(
                        photos =
                            original.draft.photos +
                                LocalDogPhoto(UUID.randomUUID().toString(), bad.path)
                    )
            )
        try {
            repository.publish(input) {}
            fail("No debe publicar una foto inválida")
        } catch (_: IllegalArgumentException) {}
        assertFalse(
            db.collection("perritos").document(input.id).get(Source.SERVER).await().exists()
        )
        bad.writeBytes(File(original.draft.photos.first().filePath).readBytes())
        assertEquals(2, repository.publish(input) {}.photos.size)
    }

    @Test
    fun otherShelterCannotBeUsedToPublish(): Unit = runBlocking {
        val input = publication().copy(shelterId = "hu07-b")
        try {
            repository.publish(input) {}
            fail("No debe publicar en otro refugio")
        } catch (_: com.google.firebase.storage.StorageException) {}
        assertFalse(
            db.collection("perritos").document(input.id).get(Source.SERVER).await().exists()
        )
    }

    @Test
    fun offlinePublicationFailsWithoutQueueAndThenCanRetry(): Unit = runBlocking {
        val input = publication()
        db.disableNetwork().await()
        try {
            repository.publish(input) {}
            fail("No debe confirmar sin servidor")
        } catch (_: FirebaseFirestoreException) {} finally {
            db.enableNetwork().await()
        }
        assertFalse(
            db.collection("perritos").document(input.id).get(Source.SERVER).await().exists()
        )
        assertEquals(input.id, repository.publish(input) {}.id)
    }

    @Test
    fun photoImporterRejectsNonImagesAndConvertsValidPngToJpeg(): Unit = runBlocking {
        val source = image()
        val photo = importer.import(Uri.fromFile(source).toString()).also { photos += it }
        val bytes = File(photo.filePath).readBytes()
        assertEquals(255, bytes[0].toInt() and 255)
        assertEquals(216, bytes[1].toInt() and 255)
        source.writeText("Este archivo no es una imagen")
        try {
            importer.import(Uri.fromFile(source).toString())
            fail("No debe aceptar texto como fotografía")
        } catch (_: IllegalArgumentException) {}
    }
}
