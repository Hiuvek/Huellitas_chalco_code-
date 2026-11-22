package com.example.huellitaschalco.feature.shelter.description

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.description.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.DogRegistrationRoute
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import org.junit.*
import org.junit.Assert.*

/** Generador controlado para comprobar la interacción; la prueba SDK real es independiente. */
class DescriptionAssistantFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun errorDiscardEditAndAcceptPreserveManualDraftAndNeverPublish() {
        val container = ApplicationProvider.getApplicationContext<HuellitasApplication>().container
        var calls = 0
        var publications = 0
        val original = "Observaciones verificadas por el refugio para esta ficha de prueba."
        val proposal =
            "Luna es una hembra de 12 meses y tamaño Mediano que busca un hogar responsable en Chalco."
        val edited = "$proposal Conoce los requisitos del refugio antes de adoptar."
        val generator =
            object : DescriptionGenerator {
                override val isConfigured = true

                override suspend fun generate(input: DescriptionInput): String {
                    calls++
                    assertEquals(original, input.notes)
                    assertEquals(12, input.ageMonths)
                    if (calls == 1) throw Exception("red de prueba")
                    return proposal
                }
            }
        val dogs =
            object : DogRepository by container.dogRepository {
                override suspend fun publish(
                    publication: DogPublication,
                    progress: (String) -> Unit,
                ): RegisteredDog {
                    publications++
                    error("No debe publicar desde Gemini")
                }
            }
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        compose.setContent {
            HuellitasChalcoTheme {
                DogRegistrationRoute(
                    UiNavigator(listOf(UiDestination(UiScreen.DogForm).encode())),
                    "test-ai-uid",
                    "test-ai-shelter",
                    dogs,
                    container.dogPhotoImporter,
                    generator = generator,
                )
            }
        }
        input("dog-name", "Luna")
        input("dog-age", "12")
        compose.onNodeWithText("Mediano").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("Hembra").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        input("dog-description", original)
        click("dog-ai")
        click("ai-generate")
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText("No pudimos obtener una propuesta de Gemini.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        click("ai-discard")
        compose.onNodeWithTag("dog-description").assertTextContains(original)
        click("dog-ai")
        click("ai-generate")
        waitTag("ai-proposal")
        compose.onNodeWithTag("ai-proposal").performTextReplacement(edited)
        Espresso.closeSoftKeyboard()
        click("ai-discard")
        compose.onNodeWithTag("dog-description").assertTextContains(original)
        click("dog-ai")
        click("ai-generate")
        waitTag("ai-proposal")
        compose.onNodeWithTag("ai-proposal").performTextReplacement(edited)
        Espresso.closeSoftKeyboard()
        // Los botones de AlertDialog están fuera del contenido desplazable.
        compose.onNodeWithTag("ai-accept").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("dog-description").assertTextContains(edited)
        assertEquals(3, calls)
        assertEquals(0, publications)
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(tag: String) {
        if (tag == "ai-discard")
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick) { it() }
        else
            compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(
                SemanticsActions.OnClick
            ) {
                it()
            }
    }

    private fun input(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(text)
        Espresso.closeSoftKeyboard()
    }
}
