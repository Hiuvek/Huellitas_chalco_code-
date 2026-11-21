package com.example.huellitaschalco.feature.shelter.dogs

import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogEditViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DogEditViewModelTest {
    private val original =
        RegisteredDog(
            "dog",
            "s",
            "Luna",
            24,
            "Mediano",
            "Hembra",
            "Perrita tranquila que disfruta jugar y pasear acompañada de su familia.",
            listOf(DogPhoto("old", "perritos/s/creator/dog/old.jpg", 0)),
            true,
            DogHealth(vaccination = HealthStatus.Yes),
        )

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun model(repo: Fake = Fake(), importer: Importer = Importer()) =
        DogEditViewModel(repo, importer, "u", original)

    @Test
    fun loadsOriginalAnswersAndKeepsRemotePhotoAndHealth() {
        val vm = model()
        assertEquals("24", vm.state.value.draft.form.age)
        assertEquals(original.health, vm.state.value.draft.form.health)
        assertEquals(original.photos[0], vm.state.value.draft.photos[0].remote)
        assertFalse(vm.state.value.dirty)
    }

    @Test
    fun requiresPhotoAndValidAnswersBeforeReview() {
        val vm = model()
        vm.edit(DogField.Name, "")
        vm.removePhoto("old")
        vm.review()
        assertFalse(vm.state.value.review)
        assertTrue(DogField.Photos in vm.state.value.errors)
        assertTrue(DogField.Name in vm.state.value.errors)
    }

    @Test
    fun newCoverAndRemovalKeepOrderWithoutAlteringOriginalReferences() = runTest {
        val vm = model()
        vm.addPhotos(listOf("new"))
        advanceUntilIdle()
        vm.cover("new")
        assertEquals("new", vm.state.value.draft.photos.first().id)
        val content = vm.state.value.draft.content("u", original)
        assertEquals("perritos/s/u/dog/new.jpg", content.photos[0].path)
        assertEquals(1, content.photos[1].order)
        assertEquals(original.photos[0].path, content.photos[1].path)
        vm.removePhoto("old")
        assertEquals(1, vm.state.value.draft.photos.size)
    }

    @Test
    fun failedImportPreservesRemoteAndAlreadyImportedPhotos() = runTest {
        val vm = model()
        vm.addPhotos(listOf("new", "bad"))
        advanceUntilIdle()
        assertEquals(listOf("old", "new"), vm.state.value.draft.photos.map { it.id })
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.importing)
    }

    @Test
    fun rejectsSeventhPhotoAndReleasesOnlyLocalFiles() = runTest {
        val importer = Importer()
        val vm = model(importer = importer)
        vm.addPhotos((1..7).map { "new$it" })
        advanceUntilIdle()
        assertEquals(6, vm.state.value.draft.photos.size)
        assertNotNull(vm.state.value.error)
        vm.releasePhotos()
        assertEquals(5, importer.removed.size)
        assertFalse("old" in importer.removed)
    }

    @Test
    fun saveRequiresReviewLocksInputsAndPreventsDoubleSubmit() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val vm = model(repo)
        vm.edit(DogField.Name, "Luna nueva")
        vm.save()
        runCurrent()
        assertEquals(0, repo.attempts.size)
        vm.review()
        vm.save()
        vm.save()
        runCurrent()
        assertEquals(1, repo.attempts.size)
        assertNull(vm.state.value.saved)
        vm.edit(DogField.Name, "No guardar")
        vm.removePhoto("old")
        assertEquals("Luna nueva", vm.state.value.draft.form.name)
        assertEquals(1, vm.state.value.draft.photos.size)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("Luna nueva", vm.state.value.saved!!.name)
        assertEquals(original.health, vm.state.value.saved!!.health)
    }

    @Test
    fun failedSaveRetriesExactlyTheSameDataAndPhotos() = runTest {
        val repo = Fake().apply { failure = IllegalStateException("offline") }
        val vm = model(repo)
        vm.edit(DogField.Name, "Luna nueva")
        vm.addPhotos(listOf("new"))
        advanceUntilIdle()
        vm.review()
        vm.save()
        advanceUntilIdle()
        val draft = vm.state.value.draft
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.saved)
        vm.edit(DogField.Name, "Ignorado")
        repo.failure = null
        vm.save()
        advanceUntilIdle()
        assertEquals(draft, vm.state.value.draft)
        assertEquals(repo.attempts[0], repo.attempts[1])
        assertNotNull(vm.state.value.saved)
    }

    @Test
    fun conflictingSaveNeedsExplicitReloadAndReleasesTemporaryPhotos() = runTest {
        val actual = original.copy(name = "Cambio de otro responsable")
        val repo = Fake().apply { failure = DogEditConflict(actual) }
        val importer = Importer()
        val vm = model(repo, importer)
        vm.edit(DogField.Name, "Mi cambio")
        vm.addPhotos(listOf("new"))
        advanceUntilIdle()
        vm.review()
        vm.save()
        advanceUntilIdle()
        assertEquals("Mi cambio", vm.state.value.draft.form.name)
        assertEquals(actual, vm.state.value.conflict)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.attempts.size)
        vm.reloadConflict()
        assertEquals("Cambio de otro responsable", vm.state.value.draft.form.name)
        assertFalse(vm.state.value.locked)
        assertEquals(listOf("new"), importer.removed)
    }

    @Test
    fun timeoutKeepsFrozenDraftForSafeRetry() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val vm = model(repo)
        vm.edit(DogField.Name, "Nuevo nombre")
        vm.review()
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.saving)
        assertTrue(vm.state.value.locked)
        assertEquals("Nuevo nombre", vm.state.value.draft.form.name)
        assertNull(vm.state.value.saved)
        assertNotNull(vm.state.value.error)
    }

    private class Importer : DogPhotoImporter {
        val removed = mutableListOf<String>()

        override suspend fun import(uri: String): LocalDogPhoto {
            if (uri == "bad") error("bad")
            return LocalDogPhoto(uri, "/test.jpg")
        }

        override fun remove(photo: LocalDogPhoto) {
            removed += photo.id
        }
    }

    private class Fake : DogRepository {
        override val isConfigured = true
        override val usesEmulator = true
        var gate: CompletableDeferred<Unit>? = null
        var failure: Exception? = null
        val attempts = mutableListOf<DogEditAttempt>()

        override suspend fun updateDog(
            attempt: DogEditAttempt,
            progress: (String) -> Unit,
        ): RegisteredDog {
            attempts += attempt
            gate?.await()
            failure?.let { throw it }
            return attempt.draft.content(attempt.uid, attempt.base).applyTo(attempt.base)
        }

        override suspend fun publish(
            publication: DogPublication,
            progress: (String) -> Unit,
        ): RegisteredDog = error("unused")

        override fun observe(shelterId: String) = flowOf(ShelterDogs(false))

        override fun observePublic(id: String) = flowOf(PublicDog(false))

        override suspend fun photo(path: String) = byteArrayOf()

        override suspend fun updateHealth(
            uid: String,
            shelterId: String,
            dogId: String,
            expected: DogHealth,
            desired: DogHealth,
        ) {}
    }
}
