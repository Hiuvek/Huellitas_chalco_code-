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
        compose.onNodeWithTag("dog-review").performClick()
        waitForTag("dog-publish")
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
        compose.onNodeWithText(name).performScrollTo().performClick()
        compose.onNodeWithText("12 meses · Mediano · Hembra").assertExists()
        assertTrue(source.exists())
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
