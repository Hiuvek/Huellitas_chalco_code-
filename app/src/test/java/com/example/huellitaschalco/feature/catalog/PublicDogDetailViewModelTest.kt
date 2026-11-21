package com.example.huellitaschalco.feature.catalog

import com.example.huellitaschalco.feature.catalog.presentation.PublicDogDetailViewModel
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PublicDogDetailViewModelTest {
    private val dog =
        RegisteredDog(
            "selected",
            "s",
            "Luna",
            8,
            "Chico",
            "Hembra",
            "Descripción guardada",
            listOf(DogPhoto("p", "path", 0)),
            true,
            DogHealth(vaccination = HealthStatus.Yes),
        )
    private val shelter =
        PublicShelter(
            "s",
            "Refugio correcto",
            "Chalco",
            "Estado de México",
            "Zona pública",
            "10 a 17 h",
            true,
        )

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun TestScope.subscribe(model: PublicDogDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect() }
    }

    @Test
    fun requestsSelectedIdAndWaitsForItsShelter() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, dog) }
        val shelters = Shelters()
        val vm = PublicDogDetailViewModel("selected", dogs, shelters)
        subscribe(vm)
        runCurrent()
        assertEquals("selected", dogs.ids.single())
        assertEquals("s", shelters.ids.single())
        assertTrue(vm.state.value.loading)
        assertNull(vm.state.value.dog)
        shelters.value.value = PublicShelterState(false, shelter)
        runCurrent()
        assertEquals(dog, vm.state.value.dog)
        assertEquals(shelter, vm.state.value.shelter)
    }

    @Test
    fun unavailableDogNeverQueriesOrShowsRefuge() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, error = "No disponible") }
        val shelters = Shelters()
        val vm = PublicDogDetailViewModel("missing", dogs, shelters)
        subscribe(vm)
        runCurrent()
        assertEquals("No disponible", vm.state.value.error)
        assertTrue(shelters.ids.isEmpty())
        assertNull(vm.state.value.dog)
    }

    @Test
    fun inactiveRefugeHidesTheWholePublicRecord() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, dog) }
        val shelters =
            Shelters().apply { value.value = PublicShelterState(false, error = "Inactivo") }
        val vm = PublicDogDetailViewModel("selected", dogs, shelters)
        subscribe(vm)
        runCurrent()
        assertEquals("Inactivo", vm.state.value.error)
        assertNull(vm.state.value.dog)
    }

    @Test
    fun liveChangesUpdateDescriptionAndHealth() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, dog) }
        val shelters = Shelters().apply { value.value = PublicShelterState(false, shelter) }
        val vm = PublicDogDetailViewModel("selected", dogs, shelters)
        subscribe(vm)
        runCurrent()
        val edited =
            dog.copy(
                description = "Descripción corregida",
                health = DogHealth(sterilization = HealthStatus.No),
            )
        dogs.value.value = PublicDog(false, edited)
        runCurrent()
        assertEquals(edited, vm.state.value.dog)
    }

    @Test
    fun adoptionRemovesPreviouslyVisibleInformation() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, dog) }
        val shelters = Shelters().apply { value.value = PublicShelterState(false, shelter) }
        val vm = PublicDogDetailViewModel("selected", dogs, shelters)
        subscribe(vm)
        runCurrent()
        assertNotNull(vm.state.value.dog)
        dogs.value.value = PublicDog(false, error = "Adoptado")
        runCurrent()
        assertNull(vm.state.value.dog)
        assertNull(vm.state.value.shelter)
        assertEquals("Adoptado", vm.state.value.error)
    }

    @Test
    fun changingRefugeIdDoesNotReuseTheOldRefuge() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, dog) }
        val shelters = Shelters().apply { value.value = PublicShelterState(false, shelter) }
        val vm = PublicDogDetailViewModel("selected", dogs, shelters)
        subscribe(vm)
        runCurrent()
        shelters.value.value = PublicShelterState()
        dogs.value.value = PublicDog(false, dog.copy(shelterId = "new"))
        runCurrent()
        assertTrue(vm.state.value.loading)
        assertEquals("new", shelters.ids.last())
        assertNull(vm.state.value.shelter)
    }

    @Test
    fun loadingTimeoutOffersAnErrorWithoutInventingData() = runTest {
        val vm = PublicDogDetailViewModel("selected", Dogs(), Shelters())
        subscribe(vm)
        runCurrent()
        advanceTimeBy(12_001)
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.dog)
    }

    @Test
    fun retryStartsANewServerObservation() = runTest {
        val dogs = Dogs().apply { value.value = PublicDog(false, error = "Error") }
        val shelters = Shelters().apply { value.value = PublicShelterState(false, shelter) }
        val vm = PublicDogDetailViewModel("selected", dogs, shelters)
        subscribe(vm)
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf("selected", "selected"), dogs.ids)
        dogs.value.value = PublicDog(false, dog)
        runCurrent()
        assertEquals(dog, vm.state.value.dog)
        assertNull(vm.state.value.error)
    }

    private class Shelters : PublicShelterRepository {
        val value = MutableStateFlow(PublicShelterState())
        val ids = mutableListOf<String>()

        override fun observe(id: String): Flow<PublicShelterState> = flow {
            ids += id
            emitAll(value)
        }
    }

    private class Dogs : DogRepository {
        val value = MutableStateFlow(PublicDog())
        val ids = mutableListOf<String>()
        override val isConfigured = true
        override val usesEmulator = true

        override fun observePublic(id: String): Flow<PublicDog> = flow {
            ids += id
            emitAll(value)
        }

        override fun observe(shelterId: String) = flowOf(ShelterDogs(false))

        override suspend fun photo(path: String) = byteArrayOf()

        override suspend fun publish(
            publication: DogPublication,
            progress: (String) -> Unit,
        ): RegisteredDog = error("unused")

        override suspend fun updateDog(
            attempt: DogEditAttempt,
            progress: (String) -> Unit,
        ): RegisteredDog = error("unused")

        override suspend fun updateHealth(
            uid: String,
            shelterId: String,
            dogId: String,
            expected: DogHealth,
            desired: DogHealth,
        ) {}
    }
}
