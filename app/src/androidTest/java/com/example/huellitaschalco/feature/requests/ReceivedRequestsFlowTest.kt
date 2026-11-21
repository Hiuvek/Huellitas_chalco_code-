package com.example.huellitaschalco.feature.requests

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assume.assumeTrue

class ReceivedRequestsFlowTest {
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

    @Test
    fun responsibleOpensStoredAnswersAndFiltersHaveRealCounts(): Unit = runBlocking {
        container.authRepository.signIn("hu07-a@example.test", "PruebaHU07!")
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
                    UiDestination(UiScreen.ShelterRequests),
                )
            }
        }
        val tag = "received-12000000-0000-4000-8000-0000000000a0"
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("No aprobada (0)").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("No hay solicitudes con este estado.").assertExists()
        compose.onNodeWithText("Todas (2)").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("5500000010").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("5500000010").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                "Quiero darle cuidados y compañía en un hogar seguro durante toda su vida."
            )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Historial").performScrollTo().assertIsDisplayed()
    }
}
