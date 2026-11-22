package com.example.huellitaschalco.feature.shelter.followup

import com.example.huellitaschalco.core.date.civilDateMillis
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.followup.domain.*
import com.example.huellitaschalco.feature.shelter.followup.presentation.RecordFollowUpViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecordFollowUpTest {
    private val delivery = civilDateMillis("2026-11-20")!!
    private val review =
        ScheduledReview("review", "dog", "dog", "s", delivery, "LLAMADA", "PROGRAMADA", null, 1)
    private val base =
        FollowUpAttempt(
            "u",
            "s",
            "review",
            "2026-11-21",
            "BUENA",
            "Se adapta bien a las rutinas del hogar.",
            null,
            "CERRAR",
            "",
            "",
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
        val calls = mutableListOf<FollowUpAttempt>()
        var action: suspend () -> Unit = {}

        override fun observe(uid: String, shelterId: String) = flowOf(FollowUpBoard(false))

        override suspend fun program(attempt: ScheduleAttempt): ReviewReceipt = error("unused")

        override suspend fun photo(path: String): ByteArray = error("unused")

        override suspend fun record(attempt: FollowUpAttempt): FollowUpReceipt {
            calls.add(attempt)
            action()
            return FollowUpReceipt(
                attempt.reviewId,
                attempt.reviewId,
                attempt.date,
                if (attempt.disposition == "PROGRAMAR") nextReviewId(attempt.reviewId) else null,
                false,
            )
        }
    }

    private val importer =
        object : DogPhotoImporter {
            override suspend fun import(uri: String) = LocalDogPhoto("photo", uri)

            override fun remove(photo: LocalDogPhoto) {}
        }

    private fun vm(repo: Repository) =
        RecordFollowUpViewModel("u", "s", review, delivery, repo, importer).apply {
            edit {
                it.copy(
                    date = base.date,
                    adaptation = base.adaptation,
                    notes = base.notes,
                    disposition = base.disposition,
                )
            }
        }

    @Test
    fun rejectsInvalidDatesNotesAdaptationAndMissingDecision() {
        assertNull(base.error(delivery, civilDateMillis("2026-11-22")!!))
        for (input in
            listOf(
                base.copy(date = "2026-02-30"),
                base.copy(date = "2026-11-19"),
                base.copy(date = "2099-01-01"),
                base.copy(notes = "corto"),
                base.copy(adaptation = "OTRO"),
                base.copy(disposition = ""),
            )) assertNotNull(input.error(delivery))
        assertNotNull(
            base
                .copy(disposition = "PROGRAMAR", nextDate = base.date, nextType = "VISITA")
                .error(delivery)
        )
        assertNull(
            base
                .copy(disposition = "PROGRAMAR", nextDate = "2026-12-01", nextType = "VISITA")
                .error(delivery)
        )
    }

    @Test
    fun stableNextIdIsBoundedAndDoesNotCollideWithParent() {
        assertEquals(64, nextReviewId("r").length)
        assertEquals(nextReviewId("r"), nextReviewId("r"))
        assertNotEquals(nextReviewId("r"), nextReviewId("r2"))
        assertNotEquals(nextReviewId("r"), nextReviewId(nextReviewId("r")))
    }

    @Test
    fun requiresConfirmationAndDoesNotSubmitAfterCancel() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        vm.submit()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
        vm.prepare()
        vm.cancel()
        vm.submit()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun ambiguousFailureKeepsSameEvidenceAndOnlyOneInFlight() = runTest {
        val repo = Repository().apply { action = { throw Exception("red") } }
        val vm = vm(repo)
        vm.addPhoto("/private/photo.jpg")
        runCurrent()
        vm.prepare()
        vm.submit()
        vm.submit()
        runCurrent()
        assertEquals(1, repo.calls.size)
        assertNull(vm.state.value.receipt)
        assertTrue(vm.state.value.locked)
        vm.edit { it.copy(notes = "No debe sustituir el intento") }
        vm.removePhoto()
        assertEquals(base.notes, vm.state.value.notes)
        assertNotNull(vm.state.value.photo)
        repo.action = {}
        vm.submit()
        runCurrent()
        assertEquals(repo.calls[0], repo.calls[1])
        assertNotNull(vm.state.value.receipt)
    }

    @Test
    fun definitiveRejectionKeepsValuesAndAllowsCorrectionWithoutFalseSuccess() = runTest {
        val repo = Repository().apply { action = { throw FollowUpRejected("rechazado") } }
        val vm = vm(repo)
        vm.prepare()
        vm.submit()
        runCurrent()
        assertFalse(vm.state.value.locked)
        assertNull(vm.state.value.receipt)
        assertEquals(base.notes, vm.state.value.notes)
        vm.edit { it.copy(adaptation = "REGULAR") }
        assertEquals("REGULAR", vm.state.value.adaptation)
    }
}
