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

    @Test
    fun healthPersistsPubliclyAndRetryIsIdempotent(): Unit = runBlocking {
        val input =
            publication().let {
                it.copy(
                    draft =
                        it.draft.copy(
                            health =
                                DogHealth(HealthStatus.Yes, HealthStatus.No, HealthStatus.Unknown)
                        )
                )
            }
        val published = repository.publish(input) {}
        assertEquals(input.draft.health, published.health)
        val ref = db.collection("perritos").document(input.id)
        val before = ref.get(Source.SERVER).await()
        val desired = DogHealth(HealthStatus.No, HealthStatus.Yes, HealthStatus.Yes)
        repository.updateHealth(
            input.userUid,
            input.shelterId,
            input.id,
            input.draft.health,
            desired,
        )
        val after = ref.get(Source.SERVER).await()
        assertEquals("No", after.getString("vacunacion"))
        assertEquals("Sí", after.getString("desparasitacion"))
        assertEquals("Sí", after.getString("esterilizacion"))
        assertEquals(
            before.data!!.filterKeys {
                it !in setOf("vacunacion", "desparasitacion", "esterilizacion", "actualizadoEn")
            },
            after.data!!.filterKeys {
                it !in setOf("vacunacion", "desparasitacion", "esterilizacion", "actualizadoEn")
            },
        )
        repository.updateHealth(
            input.userUid,
            input.shelterId,
            input.id,
            input.draft.health,
            desired,
        )
        assertEquals(
            after.getTimestamp("actualizadoEn"),
            ref.get(Source.SERVER).await().getTimestamp("actualizadoEn"),
        )
        auth.signOut()
        val public =
            kotlinx.coroutines.withTimeout(10_000) {
                repository.observePublic(input.id).first { !it.loading }
            }
        assertNull(public.error)
        assertEquals(desired, public.dog!!.health)
    }

    @Test
    fun staleHealthDoesNotOverwriteAnotherResponsible(): Unit = runBlocking {
        val input = publication()
        repository.publish(input) {}
        val actual = DogHealth(vaccination = HealthStatus.Yes)
        repository.updateHealth(input.userUid, input.shelterId, input.id, DogHealth(), actual)
        try {
            repository.updateHealth(
                input.userUid,
                input.shelterId,
                input.id,
                DogHealth(),
                DogHealth(deworming = HealthStatus.No),
            )
            fail("Debe rechazar el estado obsoleto")
        } catch (conflict: HealthConflict) {
            assertEquals(actual, conflict.actual)
        }
        assertEquals(
            "Sí",
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("vacunacion"),
        )
    }

    @Test
    fun offlineHealthIsNotQueuedAndCanRetry(): Unit = runBlocking {
        val input = publication()
        repository.publish(input) {}
        val desired = DogHealth(sterilization = HealthStatus.No)
        db.disableNetwork().await()
        try {
            repository.updateHealth(input.userUid, input.shelterId, input.id, DogHealth(), desired)
            fail("No debe confirmar sin servidor")
        } catch (_: FirebaseFirestoreException) {} finally {
            db.enableNetwork().await()
        }
        assertEquals(
            "Sin información",
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("esterilizacion"),
        )
        repository.updateHealth(input.userUid, input.shelterId, input.id, DogHealth(), desired)
        assertEquals(
            "No",
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("esterilizacion"),
        )
    }

    @Test
    fun anotherShelterCannotEditPublishedHealth(): Unit = runBlocking {
        val input = publication()
        repository.publish(input) {}
        auth.signOut()
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        try {
            repository.updateHealth(
                auth.user.value!!.uid,
                input.shelterId,
                input.id,
                DogHealth(),
                DogHealth(vaccination = HealthStatus.Yes),
            )
            fail("Otro refugio no debe editar salud")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }

    @Test
    fun editsFieldsAndPhotosPubliclyWithoutChangingIdentityHealthOrControl(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        val before = db.collection("perritos").document(input.id).get(Source.SERVER).await()
        val control =
            db.collection("perritos_control").document(input.id).get(Source.SERVER).await().data
        val local = importer.import(Uri.fromFile(image()).toString()).also { photos += it }
        val draft =
            DogEditDraft.from(original).let {
                it.copy(
                    form =
                        it.form.copy(
                            name = "Luna actualizada",
                            age = "3",
                            ageUnit = "Años",
                            size = "Grande",
                            sex = "Hembra",
                            description =
                                "Perrita ficticia con sus características actualizadas para la prueba de edición.",
                        ),
                    photos = listOf(EditableDogPhoto(local = local)) + it.photos,
                )
            }
        val attempt = DogEditAttempt(input.userUid, original, draft)
        val edited = repository.updateDog(attempt) {}
        assertEquals("Luna actualizada", edited.name)
        assertEquals(36, edited.ageMonths)
        assertEquals(2, edited.photos.size)
        assertEquals(original.photos[0].path, edited.photos[1].path)
        assertEquals(1, edited.photos[1].order)
        assertTrue(repository.photo(edited.photos[0].path).isNotEmpty())
        val after = db.collection("perritos").document(input.id).get(Source.SERVER).await()
        assertEquals(before.getTimestamp("creadoEn"), after.getTimestamp("creadoEn"))
        for (key in
            listOf(
                "refugioId",
                "estado",
                "publicado",
                "vacunacion",
                "desparasitacion",
                "esterilizacion",
                "esDemostracion",
            )) assertEquals(before.get(key), after.get(key))
        assertEquals(
            control,
            db.collection("perritos_control").document(input.id).get(Source.SERVER).await().data,
        )
        assertEquals(edited, repository.updateDog(attempt) {})
        assertEquals(
            after.getTimestamp("actualizadoEn"),
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getTimestamp("actualizadoEn"),
        )
        val shortened =
            repository.updateDog(
                DogEditAttempt(
                    input.userUid,
                    edited,
                    DogEditDraft.from(edited)
                        .copy(photos = listOf(EditableDogPhoto(remote = edited.photos[0]))),
                )
            ) {}
        assertEquals(1, shortened.photos.size)
        auth.signOut()
        val public =
            kotlinx.coroutines.withTimeout(10_000) {
                repository.observePublic(input.id).first { !it.loading }
            }
        assertEquals("Luna actualizada", public.dog!!.name)
        assertEquals(shortened.photos, public.dog!!.photos)
        try {
            repository.photo(original.photos[0].path)
            fail("No debe leer públicamente una foto retirada")
        } catch (_: com.google.firebase.storage.StorageException) {}
    }

    @Test
    fun editDoesNotLoseAConcurrentHealthUpdate(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        val health = DogHealth(HealthStatus.Yes, HealthStatus.No, HealthStatus.Yes)
        repository.updateHealth(input.userUid, input.shelterId, input.id, original.health, health)
        val draft =
            DogEditDraft.from(original).let {
                it.copy(form = it.form.copy(name = "Nombre actualizado"))
            }
        val result = repository.updateDog(DogEditAttempt(input.userUid, original, draft)) {}
        assertEquals(health, result.health)
        assertEquals(
            "Sí",
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("esterilizacion"),
        )
    }

    @Test
    fun staleDogEditReportsConflictAndCannotOverwrite(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        fun change(name: String) =
            DogEditDraft.from(original).let { it.copy(form = it.form.copy(name = name)) }
        val actual =
            repository.updateDog(
                DogEditAttempt(input.userUid, original, change("Cambio primero"))
            ) {}
        try {
            repository.updateDog(
                DogEditAttempt(input.userUid, original, change("Cambio obsoleto"))
            ) {}
            fail("Debe detectar conflicto")
        } catch (conflict: DogEditConflict) {
            assertEquals(actual, conflict.actual)
        }
        assertEquals(
            "Cambio primero",
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("nombre"),
        )
    }

    @Test
    fun failedPhotoEditKeepsOriginalAndRetriesSameFiles(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        val first = importer.import(Uri.fromFile(image()).toString()).also { photos += it }
        val broken = image().also { it.writeBytes(ByteArray(2 * 1024 * 1024 + 1)) }
        val draft =
            DogEditDraft.from(original).let {
                it.copy(
                    form = it.form.copy(name = "Cambio con fotos"),
                    photos =
                        it.photos +
                            listOf(
                                EditableDogPhoto(local = first),
                                EditableDogPhoto(
                                    local = LocalDogPhoto(UUID.randomUUID().toString(), broken.path)
                                ),
                            ),
                )
            }
        val attempt = DogEditAttempt(input.userUid, original, draft)
        try {
            repository.updateDog(attempt) {}
            fail("No debe guardar una carga parcial")
        } catch (_: IllegalArgumentException) {}
        assertEquals(
            original.name,
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("nombre"),
        )
        broken.writeBytes(File(first.filePath).readBytes())
        assertEquals(3, repository.updateDog(attempt) {}.photos.size)
    }

    @Test
    fun offlineDogEditDoesNotQueueChangesAndCanRetry(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        val draft =
            DogEditDraft.from(original).let {
                it.copy(form = it.form.copy(name = "Cambio tras conexión"))
            }
        val attempt = DogEditAttempt(input.userUid, original, draft)
        db.disableNetwork().await()
        try {
            repository.updateDog(attempt) {}
            fail("No debe confirmar sin servidor")
        } catch (_: FirebaseFirestoreException) {} finally {
            db.enableNetwork().await()
        }
        assertEquals(
            original.name,
            db.collection("perritos")
                .document(input.id)
                .get(Source.SERVER)
                .await()
                .getString("nombre"),
        )
        assertEquals("Cambio tras conexión", repository.updateDog(attempt) {}.name)
    }

    @Test
    fun otherShelterCannotEditDogFields(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        val draft =
            DogEditDraft.from(original).let { it.copy(form = it.form.copy(name = "Cambio ajeno")) }
        auth.signOut()
        auth.signIn("hu07-b@example.test", "PruebaHU07!")
        try {
            repository.updateDog(DogEditAttempt(auth.user.value!!.uid, original, draft)) {}
            fail("Otro refugio no debe editar")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }

    @Test
    fun emptyPhotoEditIsRejectedBeforeWriting(): Unit = runBlocking {
        val input = publication()
        val original = repository.publish(input) {}
        try {
            repository.updateDog(
                DogEditAttempt(
                    input.userUid,
                    original,
                    DogEditDraft.from(original).copy(photos = emptyList()),
                )
            ) {}
            fail("Debe conservar una fotografía")
        } catch (_: IllegalArgumentException) {}
        assertEquals(
            original.photos.size,
            (db.collection("perritos").document(input.id).get(Source.SERVER).await().get("fotos")
                    as List<*>)
                .size,
        )
    }
}
