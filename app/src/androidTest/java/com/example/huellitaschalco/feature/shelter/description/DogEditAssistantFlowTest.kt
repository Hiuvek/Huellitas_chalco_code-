package com.example.huellitaschalco.feature.shelter.description

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.description.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogEditRoute
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import java.io.ByteArrayOutputStream
import org.junit.*
import org.junit.Assert.*

/**
 * Comprueba la integración del asistente con la edición; no llama al servicio real ni guarda
 * fichas.
 */
class DogEditAssistantFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun usesEditedFactsAndRequiresAcceptanceWithoutSaving() {
        val container = ApplicationProvider.getApplicationContext<HuellitasApplication>().container
        var calls = 0
        var saves = 0
        val original = "Luna busca un hogar responsable en Chalco. Esta es una ficha de prueba."
        val proposal =
            "Luna es una Hembra de 18 meses, tamaño Mediano, que busca un hogar responsable en Chalco."
        val accepted = "$proposal Conoce los requisitos del refugio antes de adoptar."
        val generator =
            object : DescriptionGenerator {
                override val isConfigured = true

                override suspend fun generate(input: DescriptionInput): String {
                    calls++
                    assertEquals(18, input.ageMonths)
                    assertEquals(original, input.notes)
                    if (calls == 1) throw Exception("Fallo controlado")
                    return proposal
                }
            }
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val bytes =
            ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
                it.toByteArray()
            }
        bitmap.recycle()
        val dogs =
            object : DogRepository by container.dogRepository {
                override suspend fun photo(path: String) = bytes

                override suspend fun updateDog(
                    attempt: DogEditAttempt,
                    progress: (String) -> Unit,
                ): RegisteredDog {
                    saves++
                    error("La propuesta no debe guardar la ficha")
                }
            }
        val dog =
            RegisteredDog(
                "edit-ai-fixture",
                "ai-refuge",
                "Luna",
                12,
                "Mediano",
                "Hembra",
                original,
                listOf(DogPhoto("photo", "ai-photo", 0)),
                true,
            )
        compose.setContent {
            HuellitasChalcoTheme {
                DogEditRoute(
                    UiNavigator(
                        listOf(UiDestination(UiScreen.ShelterDogDetail, recordId = dog.id).encode())
                    ),
                    "ai-user",
                    dog,
                    1,
                    dogs,
                    container.dogPhotoImporter,
                    generator,
                ) {}
            }
        }
        compose.onNodeWithTag("edit-age").performScrollTo().performTextReplacement("18")
        Espresso.closeSoftKeyboard()
        click("edit-ai")
        click("ai-generate")
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText("No pudimos obtener una propuesta de Gemini.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        click("ai-discard")
        compose.onNodeWithTag("edit-description").assertTextContains(original)
        click("edit-ai")
        click("ai-generate")
        waitProposal()
        click("ai-discard")
        compose.onNodeWithTag("edit-description").assertTextContains(original)
        click("edit-ai")
        click("ai-generate")
        waitProposal()
        compose.onNodeWithTag("ai-proposal").performTextReplacement(accepted)
        Espresso.closeSoftKeyboard()
        click("ai-accept")
        compose.onNodeWithTag("edit-description").assertTextContains(accepted)
        assertEquals(0, saves)
        assertEquals(3, calls)
    }

    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.performClick()
    }

    private fun waitProposal() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("ai-proposal").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
