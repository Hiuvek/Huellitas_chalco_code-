package com.example.huellitaschalco.feature.shelter.requests

import com.example.huellitaschalco.feature.application.domain.ApplicationDraft
import com.example.huellitaschalco.feature.requests.domain.AdoptionRequest
import com.example.huellitaschalco.feature.shelter.requests.domain.*
import com.example.huellitaschalco.feature.shelter.requests.presentation.EvaluationViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class EvaluationViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun item(status: String = "En revisión") =
        AdoptionRequest("r", "owner", "dog", "s", "folio", status, ApplicationDraft(), 1, 1)

    private class Repository : EvaluationRepository {
        val calls = mutableListOf<EvaluationAttempt>()
        var action: suspend () -> Unit = {}

        override suspend fun decide(attempt: EvaluationAttempt) {
            calls.add(attempt)
            action()
        }
    }

    @Test
    fun cannotSkipReviewOrReopenAFinalDecision() {
        assertFalse(EvaluationDecision.Approve.allowedFrom("Enviada"))
        assertFalse(EvaluationDecision.Review.allowedFrom("No aprobada"))
        assertTrue(EvaluationDecision.Reject.allowedFrom("En revisión"))
    }

    @Test
    fun rejectionRequiresReasonAndNeverSendsInvalidInput() = runTest {
        val repo = Repository()
        val vm = EvaluationViewModel("u", "s", "r", repo)
        vm.select(item(), EvaluationDecision.Reject)
        vm.confirm()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
        assertNotNull(vm.state.value.error)
        vm.reason("No cumple las condiciones.")
        vm.confirm()
        runCurrent()
        assertEquals("No aprobada", vm.state.value.savedStatus)
        assertNull(vm.state.value.attempt)
    }

    @Test
    fun cancellationDoesNotPersistAndWrongScopeCannotBeSelected() = runTest {
        val repo = Repository()
        val vm = EvaluationViewModel("u", "s", "r", repo)
        vm.select(item().copy(shelterId = "other"), EvaluationDecision.Approve)
        assertNull(vm.state.value.attempt)
        vm.select(item(), EvaluationDecision.Approve)
        vm.cancel()
        vm.confirm()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun doubleTapSendsOnceAndFailureRetainsDecisionForExactRetry() = runTest {
        val repo =
            Repository().apply {
                action = {
                    delay(100)
                    throw EvaluationConflict("Cambió el estado")
                }
            }
        val vm = EvaluationViewModel("u", "s", "r", repo)
        vm.select(item(), EvaluationDecision.Reject)
        vm.reason("Motivo conservado")
        vm.confirm()
        vm.confirm()
        runCurrent()
        assertEquals(1, repo.calls.size)
        vm.reason("No debe cambiar")
        advanceTimeBy(101)
        runCurrent()
        assertEquals("Motivo conservado", vm.state.value.attempt!!.reason)
        assertEquals("Cambió el estado", vm.state.value.error)
        repo.action = {}
        vm.confirm()
        runCurrent()
        assertEquals(repo.calls[0], repo.calls[1])
        assertEquals("No aprobada", vm.state.value.savedStatus)
    }

    @Test
    fun timeoutDoesNotClaimSuccessAndPreservesAttempt() = runTest {
        val repo = Repository().apply { action = { awaitCancellation() } }
        val vm = EvaluationViewModel("u", "s", "r", repo)
        vm.select(item(), EvaluationDecision.Approve)
        vm.confirm()
        advanceTimeBy(30_001)
        runCurrent()
        assertNull(vm.state.value.savedStatus)
        assertFalse(vm.state.value.sending)
        assertNotNull(vm.state.value.attempt)
        assertNotNull(vm.state.value.error)
    }
}
