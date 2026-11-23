package com.example.huellitaschalco.feature.refuge

import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.refuge.presentation.ShelterEditViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShelterEditingTest {
    private val info =
        PublicShelter(
            "s",
            "Refugio",
            "Chalco",
            "Estado de México",
            "Centro",
            "10 a 17 h",
            true,
            AdoptionPolicy(listOf("Requisito"), listOf("Paso")),
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
    fun normalizeListsAndPreserveProtectedFields() {
        val draft =
            ShelterEditDraft.from(info)
                .copy(
                    name = " Nuevo ",
                    requirements = " Uno \n\n Dos ",
                    latitude = "19,25",
                    longitude = "-98,9",
                )
        assertNull(draft.error())
        val result = draft.applyTo(info)
        assertEquals(listOf("Uno", "Dos"), result.policy!!.requirements)
        assertEquals("Nuevo", result.name)
        assertEquals(info.id, result.id)
        assertEquals(info.municipality, result.municipality)
        assertEquals(info.demonstration, result.demonstration)
    }

    @Test
    fun rejectBlankTextAndInvalidLists() {
        val d = ShelterEditDraft.from(info)
        for (bad in
            listOf(
                d.copy(name = " "),
                d.copy(zone = ""),
                d.copy(hours = ""),
                d.copy(requirements = "\n "),
                d.copy(steps = "x".repeat(301)),
                d.copy(requirements = List(11) { "Requisito" }.joinToString("\n")),
            )) assertNotNull(bad.error())
    }

    @Test
    fun coordinatesRequirePairAndValidRange() {
        val d = ShelterEditDraft.from(info)
        assertNull(d.error())
        for ((lat, lon) in
            listOf("19" to "", "NaN" to "0", "91" to "0", "0" to "-181")) assertNotNull(
            d.copy(latitude = lat, longitude = lon).error()
        )
    }

    @Test
    fun openingDoesNotRoundExistingCoordinates() {
        val original = info.copy(location = PublicCoordinates.checked(19.123456789, -98.987654321))
        assertEquals(original, ShelterEditDraft.from(original).applyTo(original))
    }

    @Test
    fun editingAndInvalidSaveNeverWrite() = runTest {
        val repo = Repository()
        val vm = ShelterEditViewModel("u", repo)
        vm.load(info)
        vm.edit { it.copy(name = "") }
        vm.save()
        runCurrent()
        assertEquals(0, repo.calls)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun doubleTapWaitsForServer() = runTest {
        val repo = Repository().apply { action = { delay(100) } }
        val vm = ShelterEditViewModel("u", repo)
        vm.load(info)
        vm.edit { it.copy(hours = "Nuevo horario") }
        vm.save()
        vm.save()
        runCurrent()
        assertEquals(1, repo.calls)
        assertFalse(vm.state.value.saved)
        assertTrue(vm.state.value.saving)
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun failureKeepsEditsAndAllowsRetry() = runTest {
        val repo = Repository().apply { action = { throw Exception("sin red") } }
        val vm = ShelterEditViewModel("u", repo)
        vm.load(info)
        vm.edit { it.copy(hours = "Nuevo horario") }
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.saved)
        assertEquals("Nuevo horario", vm.state.value.draft.hours)
        assertNotNull(vm.state.value.error)
        repo.action = {}
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun conflictKeepsEditsAndRequiresExplicitReload() = runTest {
        val repo = Repository().apply { action = { throw ShelterEditConflict() } }
        val vm = ShelterEditViewModel("u", repo)
        vm.load(info)
        vm.edit { it.copy(hours = "Mi edición") }
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.conflict)
        assertEquals("Mi edición", vm.state.value.draft.hours)
        vm.save()
        runCurrent()
        assertEquals(1, repo.calls)
        val updated = info.copy(hours = "Otro responsable")
        vm.load(updated)
        assertEquals("Mi edición", vm.state.value.draft.hours)
        vm.reload(updated)
        assertEquals("Otro responsable", vm.state.value.draft.hours)
        assertFalse(vm.state.value.conflict)
        repo.action = {}
        vm.edit { it.copy(hours = "Edición revisada") }
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
    }

    private class Repository : PublicShelterRepository {
        var calls = 0
        var action: suspend () -> Unit = {}

        override fun observe(id: String) = flowOf(PublicShelterState(false))

        override suspend fun update(uid: String, expected: PublicShelter, draft: ShelterEditDraft) {
            calls++
            action()
        }
    }
}
