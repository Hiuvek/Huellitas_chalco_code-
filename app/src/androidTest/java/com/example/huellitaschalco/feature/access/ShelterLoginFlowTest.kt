package com.example.huellitaschalco.feature.access

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.MainActivity
import com.example.huellitaschalco.app.HuellitasApplication
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class ShelterLoginFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun logout() {
        if (BuildConfig.FIREBASE_AUTH_EMULATOR)
            compose.runOnIdle {
                (compose.activity.application as HuellitasApplication)
                    .container
                    .authRepository
                    .signOut()
            }
    }

    @Test
    fun adopterIsRejectedAndAuthorizedAccountCanLoginAndLogout() {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        compose.onNodeWithText("Acceso para refugios").performScrollTo().performClick()
        login("hu07-adopter@example.test")
        waitForText("Esta sección es solo para refugios autorizados")
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
        compose.onNodeWithText("Usar otra cuenta").performClick()
        login("hu07-a@example.test")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("shelter-authorized").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
        compose.onNodeWithText("hu07-a@example.test").assertExists()
        compose.onAllNodesWithText("Refugio de prueba A").onFirst().assertExists()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.onAllNodesWithText("Cerrar sesión").onLast().performClick()
        waitForText("Crear cuenta")
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
    }

    private fun login(email: String) {
        compose.onNodeWithTag("auth-email").performTextInput(email)
        compose.onNodeWithTag("auth-password").performTextInput("PruebaHU07!")
        compose.onNodeWithTag("auth-submit").performClick()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
