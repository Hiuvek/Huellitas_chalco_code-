package com.example.huellitaschalco.feature.requests

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.*
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
import com.example.huellitaschalco.feature.shelter.followup.domain.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class FollowUpFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val container
        get() = ApplicationProvider.getApplicationContext<HuellitasApplication>().container

    private val id = UUID.randomUUID().toString()
    private lateinit var source: File
    private lateinit var uid: String
    private val notes = "Se adapta a los paseos y convive tranquila con su familia."

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        container.authRepository.signOut()
        container.authRepository.signIn("hu07-b@example.test", "PruebaHU07!")
        uid = container.authRepository.user.value!!.uid
        container.followUpRepository.program(
            ScheduleAttempt(id, uid, "hu07-b", "hu12-dog-b", "2026-12-01", "LLAMADA")
        )
        source = File(compose.activity.cacheDir, "hu16-${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(120, 140, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(150, 100, 60))
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    @After
    fun cleanup() {
        if (::source.isInitialized) source.delete()
        if (BuildConfig.FIREBASE_AUTH_EMULATOR) container.authRepository.signOut()
    }

    @Test
    fun capturesPhotoAndResultProgramsNextAndShowsRealHistory(): Unit = runBlocking {
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
                        UiDestination(UiScreen.FollowUpForm, recordId = id),
                    )
                }
            }
        }
        waitTag("followup-prepare")
        click("followup-prepare")
        compose.onNodeWithTag("followup-confirm").assertDoesNotExist()
        input("followup-date", "2026-11-21")
        click("adaptation-BUENA")
        input("followup-notes", notes)
        click("followup-photo")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Quitar fotografía").fetchSemanticsNodes().isNotEmpty()
        }
        click("followup-PROGRAMAR")
        input("followup-next-date", "2026-12-02")
        click("followup-next-VISITA")
        click("followup-prepare")
        compose.onNodeWithTag("followup-confirm").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Seguimiento registrado").fetchSemanticsNodes().isNotEmpty()
        }
        val board =
            withTimeout(20_000) {
                container.followUpRepository.observe(uid, "hu07-b").first {
                    !it.loading && it.results.any { r -> r.id == id }
                }
            }
        val result = board.results.single { it.id == id }
        assertEquals(notes, result.notes)
        assertNotNull(result.photo)
        assertEquals("REALIZADA", board.reviews.single { it.id == id }.status)
        assertEquals("VISITA", board.reviews.single { it.id == nextReviewId(id) }.type)
        assertTrue(container.followUpRepository.photo(result.photo!!).size > 4)
        waitTag("followup-done")
        click("followup-done")
        waitTag("followups-Historial")
        click("followups-Historial")
        waitTag("result-$id")
        compose.onNodeWithTag("result-$id").performScrollTo().assertExists()
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
    }

    private fun input(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(text)
        Espresso.closeSoftKeyboard()
    }
}
