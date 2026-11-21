package com.example.huellitaschalco.feature.shelter.dogs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** El selector entrega una imagen ficticia; formulario, importador y Firebase son reales. */
class DogRegistrationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var source: File
    private val name = "Luna UI ${UUID.randomUUID().toString().take(6)}"
    private val container
        get() = ApplicationProvider.getApplicationContext<HuellitasApplication>().container

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        container.authRepository.signOut()
        container.authRepository.signIn("hu07-a@example.test", "PruebaHU07!")
        source = File(compose.activity.cacheDir, "hu08-ui-${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(120, 140, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(150, 100, 60))
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @After
    fun cleanup() {
        if (::source.isInitialized) source.delete()
        if (BuildConfig.FIREBASE_AUTH_EMULATOR) container.authRepository.signOut()
    }

    @Test
    fun validatesThenReviewsPublishesAndShowsSavedRecord() {
        val registry =
            object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    dispatchResult(
                        requestCode,
                        Activity.RESULT_OK,
                        Intent().setData(Uri.fromFile(source)),
                    )
                }
            }
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                HuellitasChalcoTheme {
                    HuellitasApp(
                        container.authRepository,
                        container.shelterAccessRepository,
                        UiDestination(UiScreen.DogForm),
                    )
                }
            }
        }
        waitForTag("dog-name")
        compose.onNodeWithTag("dog-review").performClick()
        compose.onNodeWithText("Escribe un nombre de 1 a 60 caracteres.").assertExists()
        compose.onNodeWithTag("dog-publish").assertDoesNotExist()
        compose.onNodeWithTag("dog-add-photos").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText("1 de 6 fotografías.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("dog-name").performScrollTo().performTextInput(name)
        compose.onNodeWithTag("dog-age").performScrollTo().performTextInput("12")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Mediano").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("Mediano").assertIsSelected()
        compose.onNodeWithText("Hembra").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("Hembra").assertIsSelected()
        compose
            .onNodeWithTag("dog-description")
            .performScrollTo()
            .performTextInput(
                "Perrita ficticia que disfruta caminar y jugar, preparada para comprobar el registro local."
            )
        Espresso.closeSoftKeyboard()
        choose("register-health-Vaccination-Yes")
        choose("register-health-Deworming-No")
        compose.onNodeWithTag("register-health-Sterilization-Unknown").assertIsSelected()
        compose.onNodeWithTag("dog-review").performClick()
        waitForTag("dog-publish")
        compose.onNodeWithText("Vacunación: Sí").assertExists()
        compose.onNodeWithText("Desparasitación: No").assertExists()
        compose.onNodeWithText(name).assertExists()
        compose.onNodeWithText("Volver a editar").performClick()
        compose.onNodeWithTag("dog-name").assertTextContains(name)
        compose.onNodeWithTag("dog-review").performClick()
        compose.onNodeWithTag("dog-publish").performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("¡Perrito publicado!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ver en Mis perritos").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(name).performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(15_000) {
            compose
                .onAllNodesWithText("Folio:", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("12 meses · Mediano · Hembra").assertExists()
        choose("health-Vaccination-No")
        choose("health-Deworming-Yes")
        choose("health-Sterilization-Yes")
        compose.onNodeWithTag("health-save").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(15_000) {
            compose
                .onAllNodesWithText("Salud guardada correctamente.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("Ver ficha pública").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Vacunación: No").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Desparasitación: Sí").assertExists()
        compose.onNodeWithText("Esterilización: Sí").assertExists()
        compose.onNodeWithContentDescription("Regresar").performClick()
        waitForTag("dog-edit")
        clickTag("dog-edit")
        waitForTag("edit-name")
        compose.onNodeWithTag("edit-name").assertTextContains(name)
        compose
            .onNodeWithTag("edit-name")
            .performScrollTo()
            .performTextReplacement("Cambio descartado")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Cancelar edición").performClick()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithTag("edit-name").assertTextContains("Cambio descartado")
        compose.onNodeWithText("Cancelar edición").performClick()
        compose.onNodeWithText("Salir", substring = false).performClick()
        waitForTag("dog-edit")
        clickTag("dog-edit")
        waitForTag("edit-name")
        compose.onNodeWithTag("edit-name").assertTextContains(name)
        compose.onNodeWithTag("edit-name").performScrollTo().performTextClearance()
        Espresso.closeSoftKeyboard()
        clickTag("edit-remove-0")
        clickTag("edit-review")
        compose.onNodeWithText("Escribe un nombre de 1 a 60 caracteres.").assertExists()
        compose.onNodeWithText("Agrega entre 1 y 6 fotografías distintas.").assertExists()
        compose.onNodeWithTag("edit-save").assertDoesNotExist()
        clickTag("edit-add-photos")
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText("1 de 6 fotografías.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val editedName = "$name editada"
        compose.onNodeWithTag("edit-name").performScrollTo().performTextReplacement(editedName)
        compose.onNodeWithTag("edit-age").performScrollTo().performTextReplacement("18")
        compose
            .onNodeWithTag("edit-description")
            .performScrollTo()
            .performTextReplacement(
                "Descripción actualizada de esta perrita ficticia, juguetona y acostumbrada a pasear acompañada."
            )
        Espresso.closeSoftKeyboard()
        clickTag("edit-review")
        waitForTag("edit-save")
        compose.onNodeWithText(editedName).assertExists()
        compose.onNodeWithText("18 meses · Mediano · Hembra").assertExists()
        compose.onNodeWithText("Volver a editar").performClick()
        compose.onNodeWithTag("edit-name").assertTextContains(editedName)
        clickTag("edit-review")
        clickTag("edit-save")
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("¡Cambios guardados!").fetchSemanticsNodes().isNotEmpty()
        }
        clickTag("edit-done")
        waitForTag("dog-edit")
        compose.onNodeWithText("18 meses · Mediano · Hembra").assertExists()
        compose.onNodeWithText("Ver ficha pública").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(editedName).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Vacunación: No").assertExists()
        compose.onNodeWithText("Desparasitación: Sí").assertExists()
        compose.onNodeWithText("Esterilización: Sí").assertExists()
        assertTrue(source.exists())
    }

    private fun clickTag(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (tag !in listOf("edit-review", "edit-save", "edit-done")) node.performScrollTo()
        node.performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun choose(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithTag(tag).assertIsSelected()
    }

    private fun waitForTag(tag: String) {
        try {
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: Throwable) {
            throw AssertionError("No apareció $tag:\n" + compose.onRoot().printToString(), error)
        }
    }
}
