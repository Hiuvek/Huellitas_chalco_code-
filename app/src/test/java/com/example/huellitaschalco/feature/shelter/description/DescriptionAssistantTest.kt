package com.example.huellitaschalco.feature.shelter.description

import com.example.huellitaschalco.feature.shelter.description.domain.*
import com.example.huellitaschalco.feature.shelter.description.presentation.DescriptionAssistantViewModel
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DescriptionAssistantTest {
    private val input = DescriptionInput("Luna", 12, "Mediano", "Hembra", "")
    private val valid =
        "Luna es una hembra de 12 meses y tamaño Mediano que busca un hogar responsable en Chalco."

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private class Generator : DescriptionGenerator {
        override var isConfigured = true
        var calls = 0
        var action: suspend (DescriptionInput) -> String = {
            "Luna es una hembra de 12 meses y tamaño Mediano que busca un hogar responsable en Chalco."
        }

        override suspend fun generate(input: DescriptionInput): String {
            calls++
            return action(input)
        }
    }

    @Test
    fun inputsRequireBasicFactsAndExcludePhotosAndMedicalStatus() {
        val draft = DogDraft("Luna", "1", "Años", "Mediano", "Hembra", "Notas proporcionadas")
        assertEquals(12, DescriptionInput.fromDraft(draft)!!.ageMonths)
        assertEquals(
            DescriptionInput.fromDraft(draft),
            DescriptionInput.fromDraft(
                draft.copy(
                    health = DogHealth(HealthStatus.Yes, HealthStatus.Yes, HealthStatus.Yes),
                    photos = listOf(LocalDogPhoto("p", "/private/photo.jpg")),
                )
            ),
        )
        assertNull(DescriptionInput.fromDraft(draft.copy(age = "")))
        assertNull(DescriptionInput.fromDraft(draft.copy(size = "")))
    }

    @Test
    fun rejectsUnsupportedMedicalClaimsWrongAgeAndUnprovidedNumbers() {
        assertEquals(valid, DescriptionPolicy.validate(input, valid))
        for (text in
            listOf(
                "$valid Está vacunada.",
                "$valid Es juguetona y convive con niños.",
                valid.replace("12 meses", "24 meses"),
                "$valid Tiene 4 hermanos.",
                valid.replace("Luna", "Otra"),
                valid.replace("Mediano", "Grande"),
            )) assertTrue(runCatching { DescriptionPolicy.validate(input, text) }.isFailure)
    }

    @Test
    fun noAutomaticCallAndOnlyOneRequestWhileGenerating() = runTest {
        val gate = CompletableDeferred<String>()
        val generator = Generator().apply { action = { gate.await() } }
        val vm = DescriptionAssistantViewModel(generator)
        vm.open(input)
        runCurrent()
        assertEquals(0, generator.calls)
        vm.generate()
        vm.generate()
        runCurrent()
        assertEquals(1, generator.calls)
        assertNull(vm.selected())
        gate.complete(valid)
        runCurrent()
        assertEquals(valid, vm.state.value.proposal)
    }

    @Test
    fun editAndDiscardNeverChangeSourceUntilExplicitSelection() = runTest {
        val vm = DescriptionAssistantViewModel(Generator())
        vm.open(input)
        vm.generate()
        runCurrent()
        val edited = "$valid Conoce más sobre su proceso de adopción."
        vm.edit(edited)
        assertEquals(edited, vm.selected())
        assertEquals("", input.notes)
        vm.close()
        assertNull(vm.state.value.proposal)
        assertNull(vm.selected())
    }

    @Test
    fun networkTimeoutAndMissingConfigurationKeepManualAlternative() = runTest {
        val generator = Generator().apply { action = { throw Exception("servicio") } }
        val vm = DescriptionAssistantViewModel(generator)
        vm.open(input)
        vm.generate()
        runCurrent()
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.proposal)
        generator.action = {
            delay(40_000)
            valid
        }
        vm.generate()
        advanceTimeBy(30_001)
        runCurrent()
        assertFalse(vm.state.value.generating)
        assertNotNull(vm.state.value.error)
        generator.isConfigured = false
        val before = generator.calls
        vm.generate()
        runCurrent()
        assertEquals(before, generator.calls)
    }

    @Test
    fun closingCancelsGenerationAndLateResultsCannotReopenProposal() = runTest {
        val generator =
            Generator().apply {
                action = {
                    delay(10_000)
                    valid
                }
            }
        val vm = DescriptionAssistantViewModel(generator)
        vm.open(input)
        vm.generate()
        runCurrent()
        vm.close()
        advanceUntilIdle()
        assertNull(vm.state.value.proposal)
        assertNull(vm.state.value.input)
    }
}
