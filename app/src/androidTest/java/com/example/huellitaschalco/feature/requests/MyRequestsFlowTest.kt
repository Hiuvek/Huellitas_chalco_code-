package com.example.huellitaschalco.feature.requests

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assume.assumeTrue

class MyRequestsFlowTest {
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

    private fun show() {
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(
                    container.authRepository,
                    container.shelterAccessRepository,
                    UiDestination(UiScreen.MyRequests),
                )
            }
        }
    }

    @Test
    fun guestCannotSeePrivateExampleAnswers() {
        show()
        waitTag("requests-login")
        compose.onNodeWithText("HC-2026-0148", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Tus respuestas").assertDoesNotExist()
    }

    @Test
    fun ownerOpensSavedAnswersAndLogoutRemovesTheDetail(): Unit = runBlocking {
        val email = "hu06-ui-${UUID.randomUUID()}@example.test"
        container.authRepository.signUp("Consulta UI", email, "PruebaHU05!")
        val attempt =
            ApplicationAttempt(
                UUID.randomUUID().toString(),
                container.authRepository.user.value!!.uid,
                "hu02-catalog-available",
                "hu02-chalco",
                ApplicationDraft(
                    "Consulta UI",
                    email,
                    "5500000060",
                    "CASA",
                    "PROPIA",
                    true,
                    "2",
                    "DE_4_A_8",
                    false,
                    "",
                    "Quiero cuidar a Lunita con responsabilidad y darle un hogar seguro toda su vida.",
                ),
            )
        container.adoptionApplicationRepository.submit(attempt)
        show()
        waitTag("request-${attempt.id}")
        compose.onNodeWithTag("request-${attempt.id}").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        waitTag("request-answers")
        compose.onNodeWithText("Historial").assertExists()
        compose.onNodeWithText(attempt.folio, substring = true).assertExists()
        compose.onNodeWithTag("request-answers").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("5500000060").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(attempt.draft.reasons).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { container.authRepository.signOut() }
        waitTag("requests-login")
        compose.onNodeWithText("5500000060").assertDoesNotExist()
        compose.onNodeWithText(attempt.folio, substring = true).assertDoesNotExist()
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
