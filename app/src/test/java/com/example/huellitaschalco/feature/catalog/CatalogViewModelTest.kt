package com.example.huellitaschalco.feature.catalog

import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.catalog.presentation.CatalogViewModel
import com.example.huellitaschalco.feature.shelter.dogs.domain.RegisteredDog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val puppy =
        CatalogDog(
            RegisteredDog(
                "a",
                "s",
                "Óscar",
                11,
                "Chico",
                "Macho",
                "Descripción de prueba",
                emptyList(),
                true,
            ),
            "Refugio Esperanza",
        )
    private val adult =
        CatalogDog(
            RegisteredDog(
                "b",
                "s2",
                "Luna",
                12,
                "Grande",
                "Hembra",
                "Descripción de prueba",
                emptyList(),
                true,
            ),
            "Huellitas de Chalco",
        )

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchesNameOrShelterIgnoringAccentsCaseAndOuterSpaces() {
        assertTrue(CatalogFilters(query = " OSCAR ").matches(puppy))
        assertTrue(CatalogFilters(query = "ESPERANZA").matches(puppy))
        assertFalse(CatalogFilters(query = "Luna").matches(puppy))
    }

    @Test
    fun ageBoundaryAndCombinedFiltersMatchExactly() {
        assertTrue(CatalogFilters(size = "Chico", sex = "Macho", age = "Cachorro").matches(puppy))
        assertFalse(CatalogFilters(age = "Adulto").matches(puppy))
        assertTrue(CatalogFilters(age = "Adulto").matches(adult))
        assertFalse(CatalogFilters(age = "Cachorro").matches(adult))
        assertFalse(CatalogFilters(query = "Luna", size = "Chico").matches(adult))
    }

    @Test
    fun loadingDoesNotInventRecordsAndEmptyRemainsDistinctFromError() = runTest {
        val repo = Fake()
        val vm = CatalogViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertTrue(vm.state.value.catalog.loading)
        assertTrue(vm.state.value.visible.isEmpty())
        repo.value.value = PublicCatalog(false)
        runCurrent()
        assertFalse(vm.state.value.catalog.loading)
        assertNull(vm.state.value.catalog.error)
        assertTrue(vm.state.value.visible.isEmpty())
        repo.value.value = PublicCatalog(false, error = "Sin conexión")
        runCurrent()
        assertEquals("Sin conexión", vm.state.value.catalog.error)
    }

    @Test
    fun searchFiltersClearAndLiveDataStayConsistent() = runTest {
        val repo = Fake().apply { value.value = PublicCatalog(false, listOf(puppy, adult)) }
        val vm = CatalogViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        vm.search("LUNA")
        runCurrent()
        assertEquals(listOf(adult), vm.state.value.visible)
        vm.size("Chico")
        runCurrent()
        assertTrue(vm.state.value.visible.isEmpty())
        vm.clear()
        vm.sex("Macho")
        vm.age("Cachorro")
        runCurrent()
        assertEquals(listOf(puppy), vm.state.value.visible)
        repo.value.value = PublicCatalog(false, listOf(adult))
        runCurrent()
        assertTrue(vm.state.value.visible.isEmpty())
        vm.clear()
        runCurrent()
        assertEquals(listOf(adult), vm.state.value.visible)
    }

    @Test
    fun invalidFiltersAreIgnoredAndSearchLengthIsBounded() = runTest {
        val vm = CatalogViewModel(Fake())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.size("Gigante")
        vm.sex("Otro")
        vm.age("Cualquiera")
        runCurrent()
        assertEquals(CatalogFilters(), vm.state.value.filters)
        vm.search("x".repeat(300))
        runCurrent()
        assertEquals(100, vm.state.value.filters.query.length)
    }

    @Test
    fun retryReopensRepositoryWithoutLosingFilters() = runTest {
        val repo = Fake()
        val vm = CatalogViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.search("Luna")
        runCurrent()
        assertEquals(1, repo.subscriptions)
        vm.retry()
        runCurrent()
        assertEquals(2, repo.subscriptions)
        assertEquals("Luna", vm.state.value.filters.query)
        repo.value.value = PublicCatalog(false, listOf(adult))
        runCurrent()
        assertEquals(listOf(adult), vm.state.value.visible)
    }

    private class Fake : CatalogRepository {
        val value = MutableStateFlow(PublicCatalog())
        var subscriptions = 0

        override fun observe(): Flow<PublicCatalog> = flow {
            subscriptions++
            emitAll(value)
        }
    }
}
