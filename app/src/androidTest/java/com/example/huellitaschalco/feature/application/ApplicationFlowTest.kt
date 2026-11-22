package com.example.huellitaschalco.feature.application

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class ApplicationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val container
        get() = ApplicationProvider.getApplicationContext<HuellitasApplication>().container

    @Before
    fun setup() {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        container.authRepository.signOut()
    }

    @After
    fun cleanup() {
        if (BuildConfig.FIREBASE_AUTH_EMULATOR) container.authRepository.signOut()
    }

    private fun show(destination: UiDestination) {
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(
                    container.authRepository,
                    container.shelterAccessRepository,
                    destination,
                )
            }
        }
    }

    @Test
    fun guestRegistersReturnsToSelectedDogReviewsAndSendsRealAnswers(): Unit = runBlocking {
        show(UiDestination(UiScreen.Requirements, recordId = "hu02-catalog-available"))
        waitTag("requirements-start")
        clickTag("requirements-start")
        waitTag("application-signup")
        clickTag("application-signup")
        waitTag("auth-name")
        val email = "hu05-ui-${UUID.randomUUID()}@example.test"
        compose.onNodeWithTag("auth-name").performTextInput("Solicitud UI")
        compose.onNodeWithTag("auth-email").performTextInput(email)
        compose.onNodeWithTag("auth-password").performTextInput("PruebaHU05!")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("auth-submit").performClick()
        waitTag("application-Name")
        compose.onNodeWithTag("application-Name").assertTextContains("Solicitud UI")
        next()
        compose.onNodeWithText("El teléfono debe tener 10 dígitos.").assertExists()
        compose.onNodeWithTag("application-Phone").performScrollTo().performTextInput("5500000001")
        Espresso.closeSoftKeyboard()
        next()
        choose("application-Housing-CASA")
        choose("application-Tenure-PROPIA")
        choose("application-Yard-Sí")
        compose.onNodeWithTag("application-People").performScrollTo().performTextInput("3")
        Espresso.closeSoftKeyboard()
        choose("application-Hours-DE_4_A_8")
        next()
        choose("application-Pets-Sí")
        compose
            .onNodeWithTag("application-PetsDetails")
            .performScrollTo()
            .performTextInput("Un gato adulto.")
        Espresso.closeSoftKeyboard()
        next()
        val reasons =
            "Tengo un hogar seguro y quiero darle cuidados y compañía a Lunita durante toda su vida."
        compose.onNodeWithTag("application-Reasons").performScrollTo().performTextInput(reasons)
        Espresso.closeSoftKeyboard()
        next()
        compose.onNodeWithText("Revisa tus respuestas").assertExists()
        compose.onNodeWithText("Editar contacto").performScrollTo().performClick()
        compose
            .onNodeWithTag("application-Phone")
            .performScrollTo()
            .performTextReplacement("5500000002")
        Espresso.closeSoftKeyboard()
        repeat(4) { next() }
        compose.onNodeWithText(reasons).assertExists()
        next()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("¡Solicitud enviada!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Enviada").assertExists()
        val uid = container.authRepository.user.value!!.uid
        val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("huellitas-auth-emulator"))
        val records =
            db.collection("solicitudes").whereEqualTo("usuarioUid", uid).get(Source.SERVER).await()
        assertEquals(1, records.size())
        val data = records.documents.single()
        assertEquals("hu02-catalog-available", data.getString("perritoId"))
        assertEquals("5500000002", (data.get("contacto") as Map<*, *>)["telefono"])
        assertEquals("Un gato adulto.", (data.get("otrasMascotas") as Map<*, *>)["detalle"])
        assertEquals(reasons, data.getString("motivos"))
        assertNotNull(data.getTimestamp("creadoEn"))
    }

    @Test
    fun inProcessDogShowsRequirementsButCannotStartAnotherRequest() {
        show(UiDestination(UiScreen.Requirements, recordId = "hu02-catalog-process"))
        waitTag("requirements-start")
        compose.onNodeWithTag("requirements-start").assertIsNotEnabled()
        compose
            .onNodeWithText(
                "Este perrito está en proceso de adopción. No admite nuevas solicitudes."
            )
            .assertExists()
        compose.onNodeWithTag("application-login").assertDoesNotExist()
    }

    @Test
    fun signingOutAndChangingAccountDoesNotExposeThePreviousContactDraft(): Unit = runBlocking {
        container.authRepository.signUp(
            "Cuenta Uno",
            "hu05-one-${UUID.randomUUID()}@example.test",
            "PruebaHU05!",
        )
        show(UiDestination(UiScreen.ApplicationContact, recordId = "hu02-catalog-available"))
        waitTag("application-Name")
        compose.onNodeWithTag("application-Phone").performScrollTo().performTextInput("5500000009")
        Espresso.closeSoftKeyboard()
        compose.runOnIdle { container.authRepository.signOut() }
        waitTag("application-login")
        compose.onNodeWithTag("application-Phone").assertDoesNotExist()
        container.authRepository.signUp(
            "Cuenta Dos",
            "hu05-two-${UUID.randomUUID()}@example.test",
            "PruebaHU05!",
        )
        waitTag("application-Name")
        compose.onNodeWithTag("application-Name").assertTextContains("Cuenta Dos")
        assertEquals(
            "",
            compose
                .onNodeWithTag("application-Phone")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
    }

    private fun next() {
        compose.onNodeWithTag("application-next").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
    }

    private fun choose(tag: String) {
        clickTag(tag)
        compose.onNodeWithTag(tag).assertIsSelected()
    }

    private fun clickTag(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
