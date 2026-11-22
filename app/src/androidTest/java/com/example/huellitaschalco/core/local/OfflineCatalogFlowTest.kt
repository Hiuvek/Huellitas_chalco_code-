package com.example.huellitaschalco.core.local

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class OfflineCatalogFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val container
        get() = ApplicationProvider.getApplicationContext<HuellitasApplication>().container

    private lateinit var db: FirebaseFirestore
    private var uid: String? = null
    private var dogId: String? = null

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        container.authRepository.signOut()
        db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("huellitas-auth-emulator"))
        db.enableNetwork().await()
        container.authRepository.signOut()
        container.authRepository.signUp(
            "Adoptante Room",
            "hu17-ui-${UUID.randomUUID()}@example.test",
            "PruebaHU05!",
        )
        uid = container.authRepository.user.value!!.uid
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    @After
    fun cleanup(): Unit = runBlocking {
        if (::db.isInitialized) db.enableNetwork().await()
        if (uid != null && dogId != null) container.draftStore.remove(uid!!, "SOLICITUD", dogId!!)
        if (BuildConfig.FIREBASE_AUTH_EMULATOR) container.authRepository.signOut()
    }

    @Test
    fun savedCatalogFiltersDetailAndApplicationDraftRemainUsableWithoutFirestoreConnection(): Unit =
        runBlocking {
            val live =
                withTimeout(25_000) {
                    container.catalogRepository.observe().first {
                        !it.loading && !it.stale && it.error == null && it.dogs.isNotEmpty()
                    }
                }
            val item = live.dogs.first { it.dog.status == "Disponible" }
            dogId = item.dog.id
            db.disableNetwork().await()
            compose.setContent {
                HuellitasChalcoTheme {
                    HuellitasApp(
                        container.authRepository,
                        container.shelterAccessRepository,
                        UiDestination(UiScreen.Catalog),
                    )
                }
            }
            waitTag("catalog-refresh")
            compose.onNodeWithTag("catalog-search").performTextInput(item.dog.name)
            Espresso.closeSoftKeyboard()
            waitTag("catalog-dog-${item.dog.id}")
            click("catalog-dog-${item.dog.id}")
            waitTag("detail-requirements")
            click("detail-requirements")
            waitTag("requirements-start")
            click("requirements-start")
            waitTag("application-Phone")
            compose
                .onNodeWithTag("application-Phone")
                .performScrollTo()
                .performTextInput("5512345678")
            Espresso.closeSoftKeyboard()
            compose
                .onNodeWithText("Consulta de la última copia guardada.", substring = true)
                .assertExists()
            withTimeout(15_000) {
                while (
                    container.draftStore.get(uid!!, "SOLICITUD", item.dog.id)?.let {
                        DraftCodecs.application(it).draft.phone
                    } != "5512345678"
                ) delay(50)
            }
            assertEquals(
                "5512345678",
                DraftCodecs.application(container.draftStore.get(uid!!, "SOLICITUD", item.dog.id)!!)
                    .draft
                    .phone,
            )
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
}
