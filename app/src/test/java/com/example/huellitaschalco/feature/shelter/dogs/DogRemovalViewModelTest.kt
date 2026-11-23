package com.example.huellitaschalco.feature.shelter.dogs

import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogRemovalViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DogRemovalViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun cancellationNeverRemovesTheDog() = runTest {
        val repo = Repository()
        val vm = DogRemovalViewModel(repo, "u", "s", "dog")
        vm.confirm()
        vm.ask("Luna")
        vm.cancel()
        vm.confirm()
        runCurrent()
        assertEquals(0, repo.calls)
        assertFalse(vm.state.value.removed)
    }

    @Test
    fun doubleTapWaitsForServerAndOnlySendsOnce() = runTest {
        val repo = Repository().apply { action = { delay(100) } }
        val vm = DogRemovalViewModel(repo, "u", "s", "dog")
        vm.ask("Luna")
        vm.confirm()
        vm.confirm()
        runCurrent()
        assertEquals(1, repo.calls)
        assertTrue(vm.state.value.removing)
        assertFalse(vm.state.value.removed)
        advanceUntilIdle()
        assertTrue(vm.state.value.removed)
        assertFalse(vm.state.value.confirming)
    }

    @Test
    fun networkFailureKeepsConfirmationAndAllowsRetry() = runTest {
        val repo = Repository().apply { action = { throw Exception("sin red") } }
        val vm = DogRemovalViewModel(repo, "u", "s", "dog")
        vm.ask("Luna")
        vm.confirm()
        advanceUntilIdle()
        assertFalse(vm.state.value.removed)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.confirming)
        repo.action = {}
        vm.confirm()
        advanceUntilIdle()
        assertTrue(vm.state.value.removed)
        assertEquals(2, repo.calls)
    }

    @Test
    fun rejectedRemovalNeverShowsSuccess() = runTest {
        val repo =
            Repository().apply { action = { throw DogRemovalRejected("Adopción en proceso") } }
        val vm = DogRemovalViewModel(repo, "u", "s", "dog")
        vm.ask("Luna")
        vm.confirm()
        advanceUntilIdle()
        assertFalse(vm.state.value.removed)
        assertEquals("Adopción en proceso", vm.state.value.error)
    }

    private class Repository : DogRepository {
        override val isConfigured = true
        override val usesEmulator = true
        var calls = 0
        var action: suspend () -> Unit = {}

        override suspend fun remove(uid: String, shelterId: String, dogId: String) {
            calls++
            action()
        }

        override fun observe(shelterId: String) = flowOf(ShelterDogs(false))

        override fun observePublic(id: String) = flowOf(PublicDog(false))

        override suspend fun photo(path: String) = byteArrayOf()

        override suspend fun publish(
            publication: DogPublication,
            progress: (String) -> Unit,
        ): RegisteredDog = error("No se usa")

        override suspend fun updateDog(
            attempt: DogEditAttempt,
            progress: (String) -> Unit,
        ): RegisteredDog = error("No se usa")

        override suspend fun updateHealth(
            uid: String,
            shelterId: String,
            dogId: String,
            expected: DogHealth,
            desired: DogHealth,
        ) {}
    }
}
