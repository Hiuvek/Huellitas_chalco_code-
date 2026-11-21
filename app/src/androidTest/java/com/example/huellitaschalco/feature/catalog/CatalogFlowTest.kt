package com.example.huellitaschalco.feature.catalog

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.catalog.presentation.CatalogRoute
import com.example.huellitaschalco.feature.shelter.dogs.domain.RegisteredDog
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assume.assumeTrue

class CatalogFlowTest {
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
    fun guestSearchesFiltersAndOpensTheSelectedRealRecord() {
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(
                    container.authRepository,
                    container.shelterAccessRepository,
                    UiDestination(UiScreen.Catalog),
                )
            }
        }
        waitTag("catalog-search")
        compose.onNodeWithTag("catalog-search").performTextInput("HU02")
        Espresso.closeSoftKeyboard()
        waitTag("catalog-dog-hu02-catalog-available")
        compose.onNodeWithTag("catalog-dog-hu02-catalog-process").assertExists()
        for (key in listOf("adopted", "draft", "outside", "inactive")) compose
            .onNodeWithTag("catalog-dog-hu02-catalog-$key")
            .assertDoesNotExist()
        clickTag("catalog-filters")
        clickTag("catalog-sex-Macho")
        compose.onNodeWithTag("catalog-sex-Macho").assertIsSelected()
        compose.onNodeWithText("Ver catálogo").performClick()
        compose.onNodeWithTag("catalog-dog-hu02-catalog-available").assertDoesNotExist()
        compose.onNodeWithTag("catalog-dog-hu02-catalog-process").assertExists()
        compose.onNodeWithTag("catalog-search").performScrollTo().performTextReplacement(" oscar ")
        Espresso.closeSoftKeyboard()
        clickTag("catalog-dog-hu02-catalog-process")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Óscar HU02").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("24 meses · Grande · Macho").assertExists()
        compose.onNodeWithText("En proceso").assertExists()
        compose.onNodeWithContentDescription("Regresar").performClick()
        waitTag("catalog-search")
        compose.onNodeWithTag("catalog-search").assertTextContains(" oscar ")
        compose.onNodeWithTag("catalog-dog-hu02-catalog-process").assertExists()
    }

    @Test
    fun loadingErrorRetryEmptyAndNoSearchResultsAreDistinct() {
        val repo = Fake()
        val nav = UiNavigator(listOf(UiDestination(UiScreen.Catalog).encode()))
        compose.setContent {
            HuellitasChalcoTheme { CatalogRoute(nav, null, repo, container.dogRepository) }
        }
        waitTag("catalog-loading")
        compose.onNodeWithTag("catalog-empty").assertDoesNotExist()
        compose.runOnIdle {
            repo.value.value = PublicCatalog(false, error = "Sin conexión de prueba")
        }
        waitTag("catalog-error")
        clickTag("catalog-retry")
        compose.waitUntil { repo.subscriptions >= 2 }
        compose.runOnIdle { repo.value.value = PublicCatalog(false) }
        waitTag("catalog-empty")
        val dog =
            RegisteredDog(
                "test-dog",
                "s",
                "Luna",
                5,
                "Chico",
                "Hembra",
                "Perrita de demostración",
                emptyList(),
                true,
            )
        compose.runOnIdle {
            repo.value.value = PublicCatalog(false, listOf(CatalogDog(dog, "Refugio prueba")))
        }
        waitTag("catalog-dog-test-dog")
        compose.onNodeWithTag("catalog-search").performScrollTo().performTextInput("no existe")
        Espresso.closeSoftKeyboard()
        waitTag("catalog-no-results")
        compose.onNodeWithTag("catalog-empty").assertDoesNotExist()
        clickTag("catalog-clear")
        waitTag("catalog-dog-test-dog")
        compose.runOnIdle { repo.value.value = PublicCatalog(false) }
        waitTag("catalog-empty")
        compose.onNodeWithTag("catalog-dog-test-dog").assertDoesNotExist()
    }

    private fun clickTag(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private class Fake : CatalogRepository {
        val value = MutableStateFlow(PublicCatalog())
        var subscriptions = 0

        override fun observe(): Flow<PublicCatalog> = flow {
            subscriptions++
            emitAll(value)
        }
    }
}
