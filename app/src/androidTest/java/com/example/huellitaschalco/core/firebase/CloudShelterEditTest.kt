package com.example.huellitaschalco.core.firebase

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Modifica temporalmente el horario escolar y lo restaura con comprobación de conflictos. */
class CloudShelterEditTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cancelSavePublicReadConflictAndRestore(): Unit = runBlocking {
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("cloudShelterEdit") == "true" &&
                !BuildConfig.FIREBASE_AUTH_EMULATOR
        )
        val container = ApplicationProvider.getApplicationContext<HuellitasApplication>().container
        val app = FirebaseApp.getInstance()
        assertEquals("huellitas-chalco", app.options.projectId)
        val uid = requireNotNull(FirebaseAuth.getInstance(app).currentUser).uid
        val db = FirebaseFirestore.getInstance(app)
        val id = "refugio-prueba-chalco"
        assertEquals(
            true,
            db.collection("refugios")
                .document(id)
                .get(Source.SERVER)
                .await()
                .getBoolean("esDemostracion"),
        )
        assertEquals(
            true,
            db.collection("membresias")
                .document("${uid}__$id")
                .get(Source.SERVER)
                .await()
                .getBoolean("activo"),
        )
        val repo = container.publicShelterRepository
        val original =
            withTimeout(20_000) {
                    repo.observe(id).first { !it.loading && !it.stale && it.shelter != null }
                }
                .shelter!!
        val draft = ShelterEditDraft.from(original)
        val hours =
            "Horario temporal de verificación escolar ${UUID.randomUUID().toString().take(8)}"
        val edited = original.copy(hours = hours)
        var changed = false
        var publicApp: FirebaseApp? = null
        try {
            compose.setContent {
                HuellitasChalcoTheme {
                    HuellitasApp(
                        container.authRepository,
                        container.shelterAccessRepository,
                        UiDestination(UiScreen.ShelterProfile),
                    )
                }
            }
            waitTag("profile-edit-shelter")
            compose.onNodeWithTag("profile-edit-shelter").performScrollTo().performClick()
            waitTag("shelter-edit-hours")
            compose
                .onNodeWithTag("shelter-edit-hours")
                .performScrollTo()
                .performTextReplacement(hours)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Cancelar").performClick()
            compose.onNodeWithText("Descartar cambios").performClick()
            waitTag("profile-edit-shelter")
            assertEquals(
                original.hours,
                db.collection("refugios")
                    .document(id)
                    .get(Source.SERVER)
                    .await()
                    .getString("horarioPublico"),
            )
            compose.onNodeWithTag("profile-edit-shelter").performScrollTo().performClick()
            waitTag("shelter-edit-hours")
            compose.onNodeWithTag("shelter-edit-hours").assertTextContains(original.hours)
            compose
                .onNodeWithTag("shelter-edit-hours")
                .performScrollTo()
                .performTextReplacement(hours)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            changed = true
            compose.onNodeWithTag("shelter-edit-save").performClick()
            waitTag("shelter-edit-done")
            publicApp =
                FirebaseApp.initializeApp(
                    ApplicationProvider.getApplicationContext<android.content.Context>(),
                    app.options,
                    "shelter-public-${UUID.randomUUID()}",
                )
            val publicDb = FirebaseFirestore.getInstance(publicApp)
            assertEquals(
                hours,
                publicDb
                    .collection("refugios")
                    .document(id)
                    .get(Source.SERVER)
                    .await()
                    .getString("horarioPublico"),
            )
            val saved = db.collection("refugios").document(id).get(Source.SERVER).await()
            assertEquals(uid, saved.getString("actualizadoPorUid"))
            assertNotNull(saved.getTimestamp("actualizadoEn"))
            assertTrue(
                runCatching {
                        repo.update(uid, original, draft.copy(name = "Otra edición de prueba"))
                    }
                    .isFailure
            )
            repo.update(uid, edited, draft.copy(hours = hours))
            assertEquals(
                saved.getTimestamp("actualizadoEn"),
                db.collection("refugios")
                    .document(id)
                    .get(Source.SERVER)
                    .await()
                    .getTimestamp("actualizadoEn"),
            )
            publicDb.terminate().await()
            assertEquals(uid, FirebaseAuth.getInstance(app).currentUser?.uid)
            android.util.Log.i(
                "ShelterEditTest",
                "PASS cancelación, edición, lectura pública independiente, auditoría, conflicto e idempotencia",
            )
        } finally {
            if (changed) repo.update(uid, edited, draft)
            publicApp?.delete()
        }
        assertEquals(
            original.hours,
            db.collection("refugios")
                .document(id)
                .get(Source.SERVER)
                .await()
                .getString("horarioPublico"),
        )
        android.util.Log.i(
            "ShelterEditTest",
            "PASS horario original restaurado sin sobrescribir ediciones ajenas",
        )
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
