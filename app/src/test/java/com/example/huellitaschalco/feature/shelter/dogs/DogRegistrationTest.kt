package com.example.huellitaschalco.feature.shelter.dogs

import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogRegistrationViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DogRegistrationTest {
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun valid() =
        DogDraft(
            "Luna",
            "2",
            "Años",
            "Mediano",
            "Hembra",
            "Es una perrita tranquila que disfruta jugar y caminar acompañada.",
            listOf(LocalDogPhoto("foto", "/test.jpg")),
        )

    @Test
    fun requiredFieldsAndPhotoAreValidated() {
        assertEquals(
            setOf(
                DogField.Name,
                DogField.Age,
                DogField.Size,
                DogField.Sex,
                DogField.Description,
                DogField.Photos,
            ),
            DogDraft().errors().keys,
        )
        assertTrue(valid().errors().isEmpty())
    }

    @Test
    fun ageIsAnIntegerWithUnitsAndBounds() {
        assertEquals(24, valid().ageMonths())
        for (age in listOf("-1", "1.5", "361", "texto", "999999999999")) assertTrue(
            DogField.Age in valid().copy(age = age, ageUnit = "Meses").errors()
        )
        assertTrue(valid().copy(age = "0", ageUnit = "Meses").errors().isEmpty())
        assertTrue(valid().copy(age = "30").errors().isEmpty())
        assertTrue(DogField.Age in valid().copy(age = "31").errors())
    }

    @Test
    fun trimmedDescriptionAndPhotoLimitsAreEnforced() {
        assertTrue(DogField.Description in valid().copy(description = " ".repeat(45)).errors())
        assertTrue(DogField.Description in valid().copy(description = "a".repeat(2001)).errors())
        assertTrue(DogField.Name in valid().copy(name = "a".repeat(61)).errors())
        assertTrue(
            DogField.Photos in
                valid().copy(photos = List(2) { LocalDogPhoto("same", "/test") }).errors()
        )
        assertTrue(
            valid()
                .copy(photos = List(6) { LocalDogPhoto("photo-$it", "/test") })
                .errors()
                .isEmpty()
        )
        assertTrue(
            DogField.Photos in
                valid().copy(photos = List(7) { LocalDogPhoto("photo-$it", "/test") }).errors()
        )
    }

    @Test
    fun incompleteFormNeverPublishes() = runTest {
        val repo = FakeRepository()
        val model = DogRegistrationViewModel(repo, Importer(), "u", "s")
        model.review()
        model.publish()
        advanceUntilIdle()
        assertFalse(model.state.value.review)
        assertEquals(0, repo.attempts.size)
    }

    @Test
    fun failedImportKeepsAlreadySelectedPhotos() = runTest {
        val model = DogRegistrationViewModel(FakeRepository(), Importer(), "u", "s")
        model.addPhotos(listOf("good", "bad"))
        advanceUntilIdle()
        assertEquals(1, model.state.value.draft.photos.size)
        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.importing)
    }

    @Test
    fun duplicateTapWaitsForCommitAndLocksTheCapturedData() = runTest {
        val repo = FakeRepository()
        repo.gate = CompletableDeferred()
        val model = form(repo)
        model.publish()
        model.publish()
        runCurrent()
        assertEquals(1, repo.attempts.size)
        assertNull(model.state.value.published)
        model.edit(DogField.Name, "Otro nombre")
        assertEquals("Luna", model.state.value.draft.name)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("Luna", model.state.value.published?.name)
        model.publish()
        assertEquals(1, repo.attempts.size)
    }

    @Test
    fun retryPreservesIdentifierAnswersAndPhotographs() = runTest {
        val repo = FakeRepository()
        repo.fail = true
        val model = form(repo)
        val original = model.state.value.draft
        model.publish()
        advanceUntilIdle()
        assertEquals(original, model.state.value.draft)
        assertNull(model.state.value.published)
        repo.fail = false
        model.publish()
        advanceUntilIdle()
        assertEquals(2, repo.attempts.size)
        assertEquals(repo.attempts[0], repo.attempts[1])
        assertNotNull(model.state.value.published)
    }

    @Test
    fun successCanStartANewIndependentRegistration() = runTest {
        val repo = FakeRepository()
        val model = form(repo)
        model.publish()
        advanceUntilIdle()
        model.reset()
        assertEquals(DogDraft(), model.state.value.draft)
        assertNull(model.state.value.published)
    }

    private suspend fun form(repo: FakeRepository): DogRegistrationViewModel {
        val model = DogRegistrationViewModel(repo, Importer(), "u", "s")
        model.edit(DogField.Name, "Luna")
        model.edit(DogField.Age, "2")
        model.edit(DogField.AgeUnit, "Años")
        model.edit(DogField.Size, "Mediano")
        model.edit(DogField.Sex, "Hembra")
        model.edit(DogField.Description, valid().description)
        model.addPhotos(listOf("good"))
        yield()
        model.review()
        return model
    }

    private class Importer : DogPhotoImporter {
        override suspend fun import(uri: String): LocalDogPhoto {
            if (uri == "bad") error("Unreadable")
            return LocalDogPhoto(uri, "/test.jpg")
        }

        override fun remove(photo: LocalDogPhoto) {}
    }

    private class FakeRepository : DogRepository {
        override val isConfigured = true
        override val usesEmulator = true
        val attempts = mutableListOf<DogPublication>()
        var fail = false
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun publish(
            publication: DogPublication,
            progress: (String) -> Unit,
        ): RegisteredDog {
            attempts += publication
            gate?.await()
            if (fail) error("Network")
            return with(publication) {
                RegisteredDog(
                    id,
                    shelterId,
                    draft.name,
                    draft.ageMonths()!!,
                    draft.size,
                    draft.sex,
                    draft.description,
                    emptyList(),
                    true,
                )
            }
        }

        override suspend fun updateDog(
            attempt: DogEditAttempt,
            progress: (String) -> Unit,
        ): RegisteredDog = error("unused")

        override fun observe(shelterId: String) = flowOf(ShelterDogs(false))

        override fun observePublic(id: String) = flowOf(PublicDog(false))

        override suspend fun updateHealth(
            uid: String,
            shelterId: String,
            dogId: String,
            expected: DogHealth,
            desired: DogHealth,
        ) {}

        override suspend fun photo(path: String) = byteArrayOf()
    }
}
