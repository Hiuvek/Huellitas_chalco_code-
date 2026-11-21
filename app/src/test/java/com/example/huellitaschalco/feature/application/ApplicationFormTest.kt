package com.example.huellitaschalco.feature.application

import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.application.presentation.ApplicationFormViewModel
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationFormTest {
    private val user = AuthUser("uid", "prueba@example.test", "Nombre Prueba")
    private val valid =
        ApplicationDraft(
            "Nombre Prueba",
            "prueba@example.test",
            "5500000001",
            "CASA",
            "PROPIA",
            true,
            "3",
            "DE_4_A_8",
            false,
            "",
            "Quiero darle cuidados y compañía en un hogar seguro durante toda su vida.",
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
    fun validationIsCompleteAndGroupedByStep() {
        assertEquals(10, ApplicationDraft().errors().size)
        assertEquals(
            setOf(ApplicationField.Name, ApplicationField.Phone, ApplicationField.Email),
            ApplicationDraft().errors(0).keys,
        )
        assertTrue(valid.errors().isEmpty())
        assertTrue(valid.copy(pets = true, petsDetails = "").errors().isEmpty())
        for (people in listOf("0", "21", "1.5", "x")) assertTrue(
            ApplicationField.People in valid.copy(people = people).errors()
        )
        assertTrue(ApplicationField.Reasons in valid.copy(reasons = " ".repeat(100)).errors())
        assertTrue(ApplicationField.Email in valid.copy(email = "x y@example.test").errors())
        assertTrue(ApplicationField.Phone in valid.copy(phone = "+525500000001").errors())
    }

    @Test
    fun contactAndReasonsAreTrimmedAndNoPetsSuppressesHiddenDetails() {
        val fields =
            valid
                .copy(
                    name = " Nombre Prueba ",
                    reasons = " ${valid.reasons} ",
                    petsDetails = "Texto oculto",
                )
                .fields()
        assertEquals("Nombre Prueba", (fields["contacto"] as Map<*, *>)["nombre"])
        assertEquals(3L, (fields["hogar"] as Map<*, *>)["personas"])
        assertNull((fields["otrasMascotas"] as Map<*, *>)["detalle"])
        assertEquals(valid.reasons, fields["motivos"])
    }

    @Test
    fun uniquenessUsesOwnerDogAndTheOriginalNullSeparator() {
        assertEquals(64, applicationKey("uid", "dog").length)
        assertNotEquals(applicationKey("uid", "dog"), applicationKey("other", "dog"))
        assertNotEquals(applicationKey("a", "bc"), applicationKey("ab", "c"))
        assertEquals(
            "59b271ae1bbcb1d31d41929817f4b16fb439eb4f31520b5ad1d5ce98920a7138",
            applicationKey("a", "b"),
        )
    }

    @Test
    fun cannotSkipValidationOrSendBeforeReview() = runTest {
        val repo = Fake()
        val model = ApplicationFormViewModel(repo, user, "dog", "s")
        model.next()
        model.send()
        runCurrent()
        assertEquals(0, model.state.value.step)
        assertNotNull(model.state.value.errors[ApplicationField.Phone])
        assertTrue(repo.attempts.isEmpty())
    }

    @Test
    fun backAndEditPreserveEveryPreviousAnswer() {
        val model = filled(Fake())
        assertEquals(4, model.state.value.step)
        model.editStep(0)
        assertEquals(valid.reasons, model.state.value.draft.reasons)
        model.edit(ApplicationField.Phone, "5500000002")
        model.next()
        model.next()
        model.next()
        model.next()
        assertEquals(4, model.state.value.step)
        assertEquals("5500000002", model.state.value.draft.phone)
        assertEquals("CASA", model.state.value.draft.housing)
    }

    @Test
    fun duplicateTapWaitsForTheServerAndLocksAllAnswers() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val model = filled(repo)
        model.send()
        model.send()
        runCurrent()
        assertEquals(1, repo.attempts.size)
        assertTrue(model.state.value.sending)
        assertNull(model.state.value.receipt)
        model.edit(ApplicationField.Name, "Cambio ignorado")
        model.editStep(0)
        assertEquals("Nombre Prueba", model.state.value.draft.name)
        assertEquals(4, model.state.value.step)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertNotNull(model.state.value.receipt)
        model.send()
        assertEquals(1, repo.attempts.size)
    }

    @Test
    fun failurePreservesPayloadAndRetryUsesExactlyTheSameIdentifier() = runTest {
        val repo = Fake().apply { error = IllegalStateException("offline") }
        val model = filled(repo)
        model.send()
        advanceUntilIdle()
        assertNotNull(model.state.value.error)
        assertNull(model.state.value.receipt)
        assertEquals(valid, model.state.value.draft)
        repo.error = null
        model.send()
        advanceUntilIdle()
        assertEquals(repo.attempts[0], repo.attempts[1])
        assertNotNull(model.state.value.receipt)
    }

    @Test
    fun alreadyExistingRequestIsNotReportedAsANewOne() = runTest {
        val repo = Fake().apply { existing = true }
        val model = filled(repo)
        model.send()
        advanceUntilIdle()
        assertTrue(model.state.value.receipt!!.alreadyExisting)
    }

    @Test
    fun availabilityFailureNeverConfirmsOrDiscardsAnswers() = runTest {
        val repo = Fake().apply { error = ApplicationUnavailable("En proceso") }
        val model = filled(repo)
        model.send()
        advanceUntilIdle()
        assertEquals("En proceso", model.state.value.error)
        assertEquals(valid, model.state.value.draft)
        assertNull(model.state.value.receipt)
    }

    @Test
    fun timeoutRemainsUnconfirmedAndKeepsTheFrozenDraft() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred() }
        val model = filled(repo)
        model.send()
        advanceUntilIdle()
        assertFalse(model.state.value.sending)
        assertTrue(model.state.value.locked)
        assertNull(model.state.value.receipt)
        assertEquals(valid, model.state.value.draft)
    }

    @Test
    fun delayedIdentityCompletesUntouchedFieldsWithoutReplacingManualAnswersOrAnotherOwner() {
        val model = ApplicationFormViewModel(Fake(), user.copy(displayName = ""), "dog", "s")
        model.receiveIdentity(user)
        assertEquals(user.displayName, model.state.value.draft.name)
        model.edit(ApplicationField.Name, "Nombre capturado")
        model.edit(ApplicationField.Email, "contacto@example.test")
        model.receiveIdentity(user.copy(displayName = "Nombre de perfil"))
        model.receiveIdentity(AuthUser("other", "otra@example.test", "Otra cuenta"))
        assertEquals("Nombre capturado", model.state.value.draft.name)
        assertEquals("contacto@example.test", model.state.value.draft.email)
    }

    private fun filled(repo: Fake): ApplicationFormViewModel {
        val model = ApplicationFormViewModel(repo, user, "dog", "s")
        model.edit(ApplicationField.Phone, valid.phone)
        model.next()
        model.edit(ApplicationField.Housing, valid.housing)
        model.edit(ApplicationField.Tenure, valid.tenure)
        model.edit(ApplicationField.Yard, "Sí")
        model.edit(ApplicationField.People, valid.people)
        model.edit(ApplicationField.Hours, valid.hours)
        model.next()
        model.edit(ApplicationField.Pets, "No")
        model.next()
        model.edit(ApplicationField.Reasons, valid.reasons)
        model.next()
        return model
    }

    private class Fake : AdoptionApplicationRepository {
        var gate: CompletableDeferred<Unit>? = null
        var error: Exception? = null
        var existing = false
        val attempts = mutableListOf<ApplicationAttempt>()

        override suspend fun submit(attempt: ApplicationAttempt): ApplicationReceipt {
            attempts += attempt
            gate?.await()
            error?.let { throw it }
            return ApplicationReceipt(
                attempt.id,
                attempt.folio,
                attempt.dogId,
                attempt.shelterId,
                "Enviada",
                existing,
            )
        }
    }
}
