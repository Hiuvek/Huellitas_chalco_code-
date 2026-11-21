package com.example.huellitaschalco.feature.access

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.huellitaschalco.app.HuellitasApp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.access.domain.*
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import org.junit.Rule
import org.junit.Test

class ShelterGateTest {
    @get:Rule val compose = createComposeRule()
    private val a = Shelter("a", "Refugio A", "Centro de Chalco", "10 a 17 h")
    private val b = Shelter("b", "Refugio B", "Chalco", "9 a 15 h")

    @Test
    fun roleChangesRevocationAndAccountSwitchRemovePrivateContent() {
        val auth = TestAuth(AuthUser("a", "a@example.test", "Responsable A"))
        val access = TestAccess(ShelterAccess.Checking("a"))
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(auth, access, UiDestination(UiScreen.ShelterProfile))
            }
        }
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
        compose.runOnIdle { access.access.value = ShelterAccess.Ready("a", listOf(a)) }
        compose.onNodeWithTag("shelter-authorized").assertExists()
        compose.onAllNodesWithText("a@example.test").onFirst().assertExists()
        compose.runOnIdle { access.access.value = ShelterAccess.Ready("a", emptyList()) }
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
        compose.runOnIdle {
            access.access.value = ShelterAccess.Ready("a", listOf(a))
            auth.user.value = AuthUser("b", "b@example.test", "B")
        }
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
        compose.runOnIdle { access.access.value = ShelterAccess.Ready("b", listOf(b)) }
        compose.onNodeWithTag("shelter-authorized").assertExists()
        compose.runOnIdle { auth.signOut() }
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
    }

    @Test
    fun selectionOnlyOffersSheltersFromValidatedMemberships() {
        val auth = TestAuth(AuthUser("a", "a@example.test", "Responsable A"))
        val access = TestAccess(ShelterAccess.Ready("a", listOf(a, b)))
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(auth, access, UiDestination(UiScreen.ShelterProfile))
            }
        }
        compose.onNodeWithTag("shelter-selector").performClick()
        compose.onNodeWithText("Refugio B").performClick()
        compose.onNodeWithTag("shelter-selector").assertTextContains("Refugio B")
        compose.runOnIdle { access.access.value = ShelterAccess.Ready("a", listOf(a)) }
        compose.onNodeWithTag("shelter-selector").assertDoesNotExist()
        compose.onAllNodesWithText("Refugio A").onFirst().assertExists()
    }

    @Test
    fun errorNeverUsesThePreviousAuthorizationAndAllowsRetry() {
        val auth = TestAuth(AuthUser("a", "a@example.test", "Responsable A"))
        val access = TestAccess(ShelterAccess.Ready("a", listOf(a)))
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(auth, access, UiDestination(UiScreen.ShelterProfile))
            }
        }
        compose.onNodeWithTag("shelter-authorized").assertExists()
        compose.runOnIdle { access.access.value = ShelterAccess.Unavailable("a") }
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").assertExists()
    }
}
