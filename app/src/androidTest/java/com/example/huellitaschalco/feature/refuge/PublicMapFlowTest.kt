package com.example.huellitaschalco.feature.refuge

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.refuge.maps.PublicMapPanel
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class PublicMapFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val container
        get() = ApplicationProvider.getApplicationContext<HuellitasApplication>().container

    private var monitor: Instrumentation.ActivityMonitor? = null

    @Before
    fun setup() {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        container.authRepository.signOut()
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    @After
    fun cleanup() {
        monitor?.let { InstrumentationRegistry.getInstrumentation().removeMonitor(it) }
        if (BuildConfig.FIREBASE_AUTH_EMULATOR) container.authRepository.signOut()
    }

    @Test
    fun failedOpeningKeepsZoneAndAllowsRetryWithoutInventingAnAddress() {
        val shelter =
            PublicShelter(
                "map-test",
                "Refugio",
                "Chalco",
                "Estado de México",
                "Centro de Chalco",
                "10 a 16 h",
                true,
            )
        var attempts = 0
        var opened: String? = null
        val launcher = PublicMapLauncher { url ->
            attempts++
            if (attempts == 1) throw android.content.ActivityNotFoundException()
            opened = url
        }
        compose.setContent {
            HuellitasChalcoTheme {
                HScaffold(UiNavigator(), "Ubicación") { PublicMapPanel(shelter, launcher) }
            }
        }
        click()
        compose.onNodeWithText("No se pudo abrir Google Maps.", substring = true).assertExists()
        compose.onNodeWithText("Centro de Chalco, Chalco, Estado de México").assertExists()
        click()
        assertEquals(2, attempts)
        assertEquals(shelter.googleMapsUrl(), opened)
        compose
            .onNodeWithText("No se pudo abrir Google Maps.", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun realPublicRefugeLaunchesGoogleMapsIntentAndRetainsInformation(): Unit = runBlocking {
        val live =
            withTimeout(25_000) {
                container.catalogRepository.observe().first {
                    !it.loading && !it.stale && it.error == null && it.dogs.isNotEmpty()
                }
            }
        val dog = live.dogs.first().dog
        val filter =
            IntentFilter(Intent.ACTION_VIEW).apply {
                addDataScheme("https")
                addDataAuthority("www.google.com", null)
                addDataPath("/maps/search/", PatternMatcher.PATTERN_LITERAL)
            }
        monitor =
            InstrumentationRegistry.getInstrumentation()
                .addMonitor(filter, Instrumentation.ActivityResult(Activity.RESULT_OK, null), true)
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(
                    container.authRepository,
                    container.shelterAccessRepository,
                    UiDestination(UiScreen.RefugeInformation, recordId = dog.id),
                )
            }
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("public-map-open").fetchSemanticsNodes().isNotEmpty()
        }
        click()
        compose.waitForIdle()
        assertEquals(1, monitor!!.hits)
        compose.onNodeWithTag("refuge-requirements").assertExists()
        compose.onNodeWithText("Ubicación pública aproximada").assertExists()
    }

    private fun click() {
        compose.onNodeWithTag("public-map-open").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
    }
}
