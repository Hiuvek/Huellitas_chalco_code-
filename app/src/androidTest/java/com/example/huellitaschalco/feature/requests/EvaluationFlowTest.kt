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

class EvaluationFlowTest {
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
    fun responsibleConfirmsReviewThenRejectsWithSavedReason(): Unit = runBlocking {
        container.authRepository.signIn("hu07-b@example.test", "PruebaHU07!")
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        val id = "12000000-0000-4000-8000-0000000000b0"
        compose.setContent {
            HuellitasChalcoTheme {
                HuellitasApp(
                    container.authRepository,
                    container.shelterAccessRepository,
                    UiDestination(UiScreen.EvaluateRequest, recordId = id),
                )
            }
        }
        waitTag("evaluation-review")
        click("evaluation-review")
        compose.onNodeWithTag("evaluation-confirm").performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        waitTag("evaluation-reject")
        click("evaluation-reject")
        compose.onNodeWithTag("evaluation-confirm").performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.onNodeWithText("Escribe el motivo con 5 a 1000 caracteres.").assertExists()
        val reason = "No cumple las condiciones revisadas para este perrito de demostración."
        compose.onNodeWithTag("evaluation-reason").performTextInput(reason)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("evaluation-confirm").performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(20_000) {
            compose
                .onAllNodesWithText("Estado guardado: No aprobada")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("evaluation-reject").assertDoesNotExist()
        val db =
            com.google.firebase.firestore.FirebaseFirestore.getInstance(
                com.google.firebase.FirebaseApp.getInstance("huellitas-auth-emulator")
            )
        val record =
            db.collection("solicitudes")
                .document(id)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
        org.junit.Assert.assertEquals("No aprobada", record.getString("estado"))
        org.junit.Assert.assertEquals(reason, record.getString("motivoNoAprobacion"))
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
