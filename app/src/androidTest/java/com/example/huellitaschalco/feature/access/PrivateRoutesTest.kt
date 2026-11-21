package com.example.huellitaschalco.feature.access

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.huellitaschalco.app.HuellitasApp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.access.domain.*
import com.example.huellitaschalco.feature.auth.domain.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

internal class TestAuth(user: AuthUser?) : AuthRepository {
    override val user = MutableStateFlow(user)
    override val isConfigured = true
    override val usesEmulator = true

    override suspend fun signUp(name: String, email: String, password: String) = Unit

    override suspend fun signIn(email: String, password: String) = Unit

    override fun signOut() {
        user.value = null
    }
}

internal class TestAccess(initial: ShelterAccess) : ShelterAccessRepository {
    override val access = MutableStateFlow(initial)

    override fun refresh() = Unit
}

@RunWith(Parameterized::class)
class PrivateRoutesTest(private val screen: UiScreen) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun visitorAndAdopterCannotBypassGuardThroughPreviewRoutes() {
        val auth = TestAuth(null)
        val access = TestAccess(ShelterAccess.SignedOut)
        compose.setContent {
            HuellitasChalcoTheme { HuellitasApp(auth, access, UiDestination(screen)) }
        }
        compose.onNodeWithText("Esta sección es solo para refugios autorizados").assertExists()
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
        compose.runOnIdle {
            auth.user.value = AuthUser("adopter", "adopter@example.test", "Adoptante")
            access.access.value = ShelterAccess.Ready("adopter", emptyList())
        }
        compose.onNodeWithText("Esta sección es solo para refugios autorizados").assertExists()
        compose.onNodeWithTag("shelter-authorized").assertDoesNotExist()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): List<Array<UiScreen>> =
            UiScreen.entries.filter { it.requiresShelterAccess }.map { arrayOf(it) }
    }
}
