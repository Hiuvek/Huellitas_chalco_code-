package com.example.huellitaschalco.feature.shelter.adoption

import com.example.huellitaschalco.feature.shelter.adoption.domain.*
import com.example.huellitaschalco.feature.shelter.adoption.presentation.AdoptionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private class Repository : AdoptionRepository {
        val calls = mutableListOf<AdoptionAttempt>()
        var action: suspend () -> Unit = {}

        override fun observeDog(uid: String, shelterId: String, dogId: String) =
            flowOf(AdoptionState(false))

        override suspend fun record(attempt: AdoptionAttempt): AdoptionReceipt {
            calls.add(attempt)
            action()
            return AdoptionReceipt("dog", "r", "dog", "s", attempt.date, false)
        }
    }

    @Test
    fun datesAreStrictAndLetterIsRequired() {
        val valid = AdoptionAttempt("u", "r", "s", "dog", "2026-01-01", true)
        assertNull(valid.error())
        for (date in listOf("2026-02-30", "2999-01-01", "01/01/2026", "")) assertNotNull(
            valid.copy(date = date).error()
        )
        assertNotNull(valid.copy(signedLetter = false).error())
    }

    @Test
    fun noConfirmationOrInvalidFormNeverSends() = runTest {
        val repo = Repository()
        val vm = AdoptionViewModel("u", "s", "r", "dog", repo)
        vm.submit()
        vm.prepare()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
        assertNotNull(vm.form.value.error)
        vm.date("2026-01-01")
        vm.signed(true)
        vm.prepare()
        assertTrue(vm.form.value.confirming)
        vm.cancel()
        vm.submit()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun doubleTapWaitsForServerBeforeConfirming() = runTest {
        val repo = Repository().apply { action = { delay(100) } }
        val vm = AdoptionViewModel("u", "s", "r", "dog", repo)
        vm.date("2026-01-01")
        vm.signed(true)
        vm.prepare()
        vm.submit()
        vm.submit()
        runCurrent()
        assertEquals(1, repo.calls.size)
        assertNull(vm.form.value.receipt)
        advanceTimeBy(101)
        runCurrent()
        assertEquals("dog", vm.form.value.receipt!!.id)
    }

    @Test
    fun failureFreezesOriginalDateAndKeepsAnExactRetry() = runTest {
        val repo = Repository().apply { action = { throw Exception("sin red") } }
        val vm = AdoptionViewModel("u", "s", "r", "dog", repo)
        vm.date("2026-01-01")
        vm.signed(true)
        vm.prepare()
        vm.submit()
        runCurrent()
        vm.date("2026-02-01")
        vm.signed(false)
        assertEquals("2026-01-01", vm.form.value.date)
        assertTrue(vm.form.value.signed)
        assertNull(vm.form.value.receipt)
        assertNotNull(vm.form.value.error)
        repo.action = {}
        vm.submit()
        runCurrent()
        assertEquals(repo.calls[0], repo.calls[1])
    }

    @Test
    fun timeoutDoesNotPretendTheDeliveryWasRecorded() = runTest {
        val repo = Repository().apply { action = { awaitCancellation() } }
        val vm = AdoptionViewModel("u", "s", "r", "dog", repo)
        vm.date("2026-01-01")
        vm.signed(true)
        vm.prepare()
        vm.submit()
        advanceTimeBy(30_001)
        runCurrent()
        assertNull(vm.form.value.receipt)
        assertFalse(vm.form.value.sending)
        assertTrue(vm.form.value.locked)
    }

    @Test
    fun definitiveRejectionAllowsCorrectingTheForm() = runTest {
        val repo =
            Repository().apply {
                action = { throw AdoptionRejected("Fecha rechazada por el servidor") }
            }
        val vm = AdoptionViewModel("u", "s", "r", "dog", repo)
        vm.date("2026-01-01")
        vm.signed(true)
        vm.prepare()
        vm.submit()
        runCurrent()
        assertFalse(vm.form.value.locked)
        assertNull(vm.form.value.receipt)
        vm.cancel()
        vm.date("2026-02-01")
        assertEquals("2026-02-01", vm.form.value.date)
    }
}
