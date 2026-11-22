package com.example.huellitaschalco.feature.requests

import com.example.huellitaschalco.feature.application.domain.ApplicationDraft
import com.example.huellitaschalco.feature.requests.domain.*
import com.example.huellitaschalco.feature.requests.presentation.RequestsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private class Repository : AdoptionRequestsRepository {
        val lists = MutableStateFlow(RequestListState())
        val details = MutableStateFlow(RequestDetailState())
        val calls = mutableListOf<String>()

        override fun observeOwn(uid: String) = lists.onStart { calls.add(uid) }

        override fun observeOwnDetail(uid: String, id: String) = details.onStart {
            calls.add("$uid/$id")
        }
    }

    private fun item(status: String) =
        AdoptionRequest("r", "u", "d", "s", "folio", status, ApplicationDraft(), 10, 20)

    @Test
    fun subscribesToExactOwnerAndRefreshesWithoutLosingServerErrors() = runTest {
        val repo = Repository()
        val vm = RequestsViewModel("owner", null, repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.list.collect() }
        runCurrent()
        assertEquals(listOf("owner"), repo.calls)
        repo.lists.value = RequestListState(false, listOf(item("Enviada")))
        runCurrent()
        assertEquals(1, vm.list.value.requests.size)
        repo.lists.value = RequestListState(false, error = "Permiso revocado")
        runCurrent()
        assertTrue(vm.list.value.requests.isEmpty())
        assertEquals("Permiso revocado", vm.list.value.error)
        vm.retry()
        runCurrent()
        assertEquals(listOf("owner", "owner"), repo.calls)
    }

    @Test
    fun detailUsesSelectedIdAndReactsToStateHistoryAndRevocation() = runTest {
        val repo = Repository()
        val vm = RequestsViewModel("owner", "selected", repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.detail.collect() }
        runCurrent()
        assertEquals(listOf("owner/selected"), repo.calls)
        val change = RequestHistory("h", "En revisión", 20, null)
        repo.details.value = RequestDetailState(false, item("En revisión"), listOf(change))
        runCurrent()
        assertEquals("En revisión", vm.detail.value.request!!.status)
        assertEquals(listOf(change), vm.detail.value.history)
        repo.details.value = RequestDetailState(false, error = "Sin acceso")
        runCurrent()
        assertNull(vm.detail.value.request)
        assertTrue(vm.detail.value.history.isEmpty())
    }

    @Test
    fun allFourStatusesExplainTheRelevantNextStep() {
        val messages =
            listOf("Enviada", "En revisión", "Aprobada", "No aprobada").map { item(it).nextStep }
        assertEquals(4, messages.toSet().size)
        assertTrue(messages[2].contains("carta"))
        assertTrue(messages[3].contains("motivo"))
    }
}
