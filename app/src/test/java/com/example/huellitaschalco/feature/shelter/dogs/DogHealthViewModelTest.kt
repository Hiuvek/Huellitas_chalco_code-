package com.example.huellitaschalco.feature.shelter.dogs

import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogHealthViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DogHealthViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private val yes = DogHealth(vaccination = HealthStatus.Yes)
    private val other = DogHealth(deworming = HealthStatus.No)

    @Test
    fun unknownIsDifferentFromNoAndLegacyMissingIsUnknown() {
        assertEquals(HealthStatus.Unknown, DogDraft().health.vaccination)
        assertEquals(HealthStatus.Unknown, HealthStatus.fromStored(null))
        assertEquals(HealthStatus.No, HealthStatus.fromStored("No"))
        assertEquals(3, DogHealth().stored().size)
    }

    @Test
    fun saveRequiresChangesAndDuplicateTapWaitsForAcknowledgement() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val vm = model(repo)
        vm.save()
        runCurrent()
        assertEquals(0, repo.calls)
        vm.edit(HealthField.Vaccination, HealthStatus.Yes)
        vm.save()
        vm.save()
        runCurrent()
        assertEquals(1, repo.calls)
        assertFalse(vm.state.value.saved)
        vm.edit(HealthField.Vaccination, HealthStatus.No)
        assertEquals(yes, vm.state.value.draft)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.dirty)
    }

    @Test
    fun failurePreservesInputsAndRetryConfirmsSuccess() = runTest {
        val repo = Fake().apply { failure = IllegalStateException("offline") }
        val vm = model(repo)
        vm.edit(HealthField.Vaccination, HealthStatus.Yes)
        vm.save()
        advanceUntilIdle()
        assertEquals(yes, vm.state.value.draft)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.saved)
        repo.failure = null
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        assertEquals(2, repo.calls)
    }

    @Test
    fun conflictingUpdateNeedsExplicitReloadAndNeverOverwritesInputs() = runTest {
        val repo = Fake().apply { failure = HealthConflict(other) }
        val vm = model(repo)
        vm.edit(HealthField.Vaccination, HealthStatus.Yes)
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.conflict)
        assertEquals(yes, vm.state.value.draft)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.calls)
        vm.reload()
        assertEquals(other, vm.state.value.draft)
        assertFalse(vm.state.value.conflict)
    }

    @Test
    fun incomingStatePreservesDirtyFormAndReloadUsesLatest() {
        val vm = model(Fake())
        vm.edit(HealthField.Vaccination, HealthStatus.Yes)
        vm.receive(other)
        assertEquals(yes, vm.state.value.draft)
        vm.reload()
        assertEquals(other, vm.state.value.draft)
        vm.receive(yes)
        assertEquals(yes, vm.state.value.draft)
    }

    @Test
    fun newerServerUpdateIsNotLostWhenSaveAcknowledgementArrives() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val vm = model(repo)
        vm.edit(HealthField.Vaccination, HealthStatus.Yes)
        vm.save()
        runCurrent()
        vm.receive(yes)
        vm.receive(other)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(other, vm.state.value.draft)
        assertEquals(other, vm.state.value.baseline)
    }

    @Test
    fun timeoutKeepsDraftAndDoesNotReportSuccess() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val vm = model(repo)
        vm.edit(HealthField.Vaccination, HealthStatus.Yes)
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.saving)
        assertFalse(vm.state.value.saved)
        assertEquals(yes, vm.state.value.draft)
        assertNotNull(vm.state.value.error)
    }

    private fun model(repo: Fake) = DogHealthViewModel(repo, "u", "s", "d", DogHealth())

    private class Fake : DogRepository {
        override val isConfigured = true
        override val usesEmulator = true
        var calls = 0
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun updateHealth(
            uid: String,
            shelterId: String,
            dogId: String,
            expected: DogHealth,
            desired: DogHealth,
        ) {
            calls++
            gate?.await()
            failure?.let { throw it }
        }

        override suspend fun publish(
            publication: DogPublication,
            progress: (String) -> Unit,
        ): RegisteredDog = error("unused")

        override suspend fun updateDog(
            attempt: DogEditAttempt,
            progress: (String) -> Unit,
        ): RegisteredDog = error("unused")

        override fun observe(shelterId: String) = flowOf(ShelterDogs(false))

        override fun observePublic(id: String) = flowOf(PublicDog(false))

        override suspend fun photo(path: String) = byteArrayOf()
    }
}
