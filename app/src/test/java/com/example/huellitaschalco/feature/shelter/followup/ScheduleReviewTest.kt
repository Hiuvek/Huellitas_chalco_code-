package com.example.huellitaschalco.feature.shelter.followup

import com.example.huellitaschalco.core.date.civilDateMillis
import com.example.huellitaschalco.feature.shelter.adoption.domain.Adoption
import com.example.huellitaschalco.feature.shelter.followup.domain.*
import com.example.huellitaschalco.feature.shelter.followup.presentation.ScheduleReviewViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleReviewTest {
    private val adopted =
        AdoptedDog(
            Adoption(
                "dog",
                "r",
                "dog",
                "owner",
                "s",
                civilDateMillis("2026-11-21")!!,
                true,
                "staff",
                1,
            ),
            "Luna",
            "Persona",
        )

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private class Repository : FollowUpRepository {
        val calls = mutableListOf<ScheduleAttempt>()
        var action: suspend () -> Unit = {}

        override suspend fun record(attempt: FollowUpAttempt): FollowUpReceipt = error("unused")

        override suspend fun photo(path: String): ByteArray = error("unused")

        override fun observe(uid: String, shelterId: String) = flowOf(FollowUpBoard(false))

        override suspend fun program(attempt: ScheduleAttempt): ReviewReceipt {
            calls.add(attempt)
            action()
            return ReviewReceipt(attempt.id, attempt.adoptionId, attempt.date, attempt.type, false)
        }
    }

    @Test
    fun civilDatesAndTypesRespectDeliveryIncludingSameDay() {
        val input = ScheduleAttempt("id", "u", "s", "dog", "2026-11-21", "VISITA")
        assertNull(input.error(adopted.adoption.deliveryAt))
        assertNotNull(input.copy(date = "2026-11-20").error(adopted.adoption.deliveryAt))
        assertNotNull(input.copy(date = "2026-02-30").error(1))
        assertNotNull(input.copy(type = "OTRO").error(1))
    }

    @Test
    fun invalidScheduleAndCancelledConfirmationDoNotSend() = runTest {
        val repo = Repository()
        val vm = ScheduleReviewViewModel("u", "s", adopted, repo)
        vm.prepare()
        vm.submit()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
        vm.date("2026-12-01")
        vm.type("LLAMADA")
        vm.prepare()
        vm.cancel()
        vm.submit()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun ambiguousFailureKeepsStableIdDateAndTypeForRetry() = runTest {
        val repo = Repository().apply { action = { throw Exception("red") } }
        val vm = ScheduleReviewViewModel("u", "s", adopted, repo)
        vm.date("2026-12-01")
        vm.type("VIDEOLLAMADA")
        vm.prepare()
        vm.submit()
        vm.submit()
        runCurrent()
        assertEquals(1, repo.calls.size)
        assertNull(vm.state.value.receipt)
        vm.date("2026-12-02")
        assertEquals("2026-12-01", vm.state.value.date)
        repo.action = {}
        vm.submit()
        runCurrent()
        assertEquals(repo.calls[0], repo.calls[1])
        assertNotNull(vm.state.value.receipt)
    }

    @Test
    fun definitiveRejectionAllowsCorrectingThePlannedDay() = runTest {
        val repo = Repository().apply { action = { throw ReviewRejected("Fecha inválida") } }
        val vm = ScheduleReviewViewModel("u", "s", adopted, repo)
        vm.date("2026-12-01")
        vm.type("VISITA")
        vm.prepare()
        vm.submit()
        runCurrent()
        assertFalse(vm.state.value.locked)
        vm.cancel()
        vm.date("2026-12-02")
        assertEquals("2026-12-02", vm.state.value.date)
    }

    @Test
    fun aNewSchedulingIntentGetsANewIdAfterThePreviousAcknowledgement() = runTest {
        val repo = Repository()
        val vm = ScheduleReviewViewModel("u", "s", adopted, repo)
        vm.date("2026-12-01")
        vm.type("VISITA")
        vm.prepare()
        vm.submit()
        runCurrent()
        vm.newSchedule()
        assertNull(vm.state.value.receipt)
        assertEquals("", vm.state.value.date)
        vm.date("2026-12-02")
        vm.type("LLAMADA")
        vm.prepare()
        vm.submit()
        runCurrent()
        assertEquals(2, repo.calls.size)
        assertNotEquals(repo.calls[0].id, repo.calls[1].id)
    }
}
