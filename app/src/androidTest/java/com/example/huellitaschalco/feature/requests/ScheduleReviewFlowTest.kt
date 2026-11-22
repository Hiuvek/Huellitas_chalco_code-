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
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assume.assumeTrue

class ScheduleReviewFlowTest {
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
    fun responsibleSchedulesFromRealAdoptionAndOpensThePlannedReview(): Unit = runBlocking {
        container.authRepository.signIn("hu07-b@example.test", "PruebaHU07!")
        val db =
            com.google.firebase.firestore.FirebaseFirestore.getInstance(
                com.google.firebase.FirebaseApp.getInstance("huellitas-auth-emulator")
            )
        val delivery =
            db.collection("adopciones")
                .document("hu12-dog-b")
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
                .getTimestamp("fechaEntrega")!!
                .toDate()
                .time
        val date =
            java.text
                .SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("America/Mexico_City") }
                .format(java.util.Date(delivery + 8 * 86_400_000L))
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
                    UiDestination(
                        UiScreen.AdoptionForm,
                        recordId = "12000000-0000-4000-8000-0000000000b0",
                    ),
                )
            }
        }
        waitTag("adoption-schedule")
        click("adoption-schedule")
        waitTag("schedule-date")
        compose.onNodeWithTag("schedule-date").performScrollTo().performTextInput(date)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        click("schedule-LLAMADA")
        click("schedule-prepare")
        compose.onNodeWithTag("schedule-confirm").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
        waitTag("schedule-done")
        click("schedule-done")
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Abrir revisión").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Abrir revisión")[0].performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Fecha programada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Fecha programada").assertExists()
        compose.onNodeWithText("Llamada").assertExists()
        compose
            .onNodeWithText("Todavía no hay un resultado registrado para esta revisión.")
            .assertExists()
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
