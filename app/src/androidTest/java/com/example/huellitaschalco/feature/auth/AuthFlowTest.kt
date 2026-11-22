package com.example.huellitaschalco.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.MainActivity
import com.example.huellitaschalco.app.HuellitasApplication
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Recorre la interfaz completa contra el emulador local usando el SDK de Firebase. */
class AuthFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun requireLocalEmulator() {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        compose.waitForIdle()
        if (
            (compose.activity.application as HuellitasApplication)
                .container
                .authRepository
                .user
                .value != null
        ) {
            compose.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
            compose.onNodeWithContentDescription("Ver todas las pantallas").assertDoesNotExist()
            compose.onNodeWithText("Datos de ejemplo").assertDoesNotExist()
            compose.onNodeWithText("Actualizado hoy a las 9:30").assertDoesNotExist()
            compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
            compose.onAllNodesWithText("Cerrar sesión").onLast().performClick()
            waitForText("Crear cuenta")
        }
    }

    @After
    fun closeTestSession() {
        if (BuildConfig.FIREBASE_AUTH_EMULATOR)
            compose.runOnIdle {
                (compose.activity.application as HuellitasApplication)
                    .container
                    .authRepository
                    .signOut()
            }
    }

    @Test
    fun registerRestoreLogoutAndLoginFromTheUi() {
        val email = "ui-${UUID.randomUUID()}@example.test"
        compose
            .onNodeWithText("Crear cuenta", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag("auth-name").performTextInput("Prueba HU01")
        compose.onNodeWithTag("auth-email").performTextInput(email)
        compose.onNodeWithTag("auth-password").performTextInput("Password123!")
        compose.onNodeWithTag("auth-submit").performClick()
        waitForText("Hola, Prueba")
        compose.activityRule.scenario.recreate()
        waitForText("Hola, Prueba")
        compose.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Ver todas las pantallas").assertDoesNotExist()
        compose.onNodeWithText("Datos de ejemplo").assertDoesNotExist()
        compose.onNodeWithText("Actualizado hoy a las 9:30").assertDoesNotExist()
        compose.onAllNodesWithText(email).onFirst().assertExists()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.onAllNodesWithText("Cerrar sesión").onLast().performClick()
        waitForText("Iniciar sesión")
        compose.runOnIdle {
            val app = compose.activity.application as HuellitasApplication
            assertNull(app.container.authRepository.user.value)
        }
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("auth-email").performTextInput(email)
        compose.onNodeWithTag("auth-password").performTextInput("Incorrecta123!")
        compose.onNodeWithTag("auth-submit").performClick()
        waitForText("El correo o la contraseña no coinciden. Revisa tus datos e intenta de nuevo.")
        compose.onNodeWithTag("auth-email").assertTextContains(email)
        compose.onNodeWithTag("auth-password").performTextReplacement("Password123!")
        compose.onNodeWithTag("auth-submit").performClick()
        waitForText("Hola, Prueba")
        compose.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Ver todas las pantallas").assertDoesNotExist()
        compose.onNodeWithText("Datos de ejemplo").assertDoesNotExist()
        compose.onNodeWithText("Actualizado hoy a las 9:30").assertDoesNotExist()
        compose.onAllNodesWithText(email).onFirst().assertExists()
        compose.runOnIdle {
            (compose.activity.application as HuellitasApplication)
                .container
                .authRepository
                .signOut()
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
