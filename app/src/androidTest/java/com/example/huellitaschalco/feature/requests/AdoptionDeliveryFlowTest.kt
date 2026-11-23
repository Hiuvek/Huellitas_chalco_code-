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

class AdoptionDeliveryFlowTest {
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
    fun approvedRequestRequiresDateLetterAndConfirmationBeforeDelivery(): Unit = runBlocking {
        container.authRepository.signIn("hu07-b@example.test", "PruebaHU07!")
        val uid = container.authRepository.user.value!!.uid
        val id = "12000000-0000-4000-8000-0000000000b0"
        container.evaluationRepository.decide(
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationAttempt(
                uid,
                "hu07-b",
                id,
                "Enviada",
                com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationDecision
                    .Review,
            )
        )
        container.evaluationRepository.decide(
            com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationAttempt(
                uid,
                "hu07-b",
                id,
                "En revisión",
                com.example.huellitaschalco.feature.shelter.requests.domain.EvaluationDecision
                    .Approve,
            )
        )
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
                    UiDestination(UiScreen.EvaluateRequest, recordId = id),
                )
            }
        }
        waitTag("adoption-open")
        click("adoption-open")
        waitTag("adoption-prepare")
        compose.onNodeWithTag("adoption-prepare").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
        compose
            .onNodeWithText("Confirma que se realizó la entrega y se firmó la carta.")
            .assertExists()
        val date =
            java.text
                .SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("America/Mexico_City") }
                .format(java.util.Date())
        compose.onNodeWithTag("adoption-date").assertTextContains(date)
        val yesterday =
            java.text
                .SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("America/Mexico_City") }
                .format(java.util.Date(System.currentTimeMillis() - 86_400_000L))
        compose.onNodeWithTag("adoption-date").performScrollTo().performTextReplacement(yesterday)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("adoption-signed").performScrollTo().performClick()
        compose.onNodeWithTag("adoption-prepare").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
        compose.onNodeWithTag("adoption-confirm").assertExists()
        compose.onNodeWithTag("adoption-confirm").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Adopción registrada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("adoption-prepare").assertDoesNotExist()
        compose.onNodeWithTag("adoption-close-pending").assertDoesNotExist()
        compose
            .onNodeWithText(
                "Cierre completado. No quedan otras solicitudes pendientes para este perrito."
            )
            .assertExists()
        val db =
            com.google.firebase.firestore.FirebaseFirestore.getInstance(
                com.google.firebase.FirebaseApp.getInstance("huellitas-auth-emulator")
            )
        val record =
            db.collection("adopciones")
                .document("hu12-dog-b")
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
        org.junit.Assert.assertEquals(id, record.getString("solicitudId"))
        org.junit.Assert.assertTrue(record.getBoolean("cartaFirmada") == true)
        org.junit.Assert.assertEquals(
            com.example.huellitaschalco.core.date.civilDateMillis(yesterday),
            record.getTimestamp("fechaEntrega")!!.toDate().time,
        )
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
