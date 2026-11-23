package com.example.huellitaschalco.core.firebase

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.example.huellitaschalco.ui.theme.HuellitasChalcoTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Solo se activa explícitamente y solo retira la ficha nueva que crea esta prueba. */
class CloudDogRemovalTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cancelThenConfirmRemovalFromTheRealShelterUi(): Unit = runBlocking {
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("cloudDogRemoval") == "true" &&
                !BuildConfig.FIREBASE_AUTH_EMULATOR
        )
        val context = ApplicationProvider.getApplicationContext<HuellitasApplication>()
        val container = context.container
        val app = FirebaseApp.getInstance()
        assertEquals("huellitas-chalco", app.options.projectId)
        val uid = requireNotNull(FirebaseAuth.getInstance(app).currentUser).uid
        val db = FirebaseFirestore.getInstance(app)
        val shelter = "refugio-prueba-chalco"
        assertEquals(
            true,
            db.collection("refugios")
                .document(shelter)
                .get(Source.SERVER)
                .await()
                .getBoolean("esDemostracion"),
        )
        assertEquals(
            true,
            db.collection("membresias")
                .document("${uid}__$shelter")
                .get(Source.SERVER)
                .await()
                .getBoolean("activo"),
        )
        val id = "spark-remove-${UUID.randomUUID()}"
        val file = File(context.cacheDir, "$id.jpg")
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(77, 101, 80))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
        bitmap.recycle()
        var publicApp: FirebaseApp? = null
        try {
            val repo = container.dogRepository
            repo.publish(
                DogPublication(
                    id,
                    uid,
                    shelter,
                    DogDraft(
                        "Prueba eliminación nube",
                        "12",
                        "Meses",
                        "Mediano",
                        "Macho",
                        "Ficha ficticia escolar creada exclusivamente para comprobar la eliminación de la lista en Firebase real. No corresponde a un animal real.",
                        listOf(LocalDogPhoto(UUID.randomUUID().toString(), file.path)),
                    ),
                )
            ) {}
            compose.setContent {
                HuellitasChalcoTheme {
                    HuellitasApp(
                        container.authRepository,
                        container.shelterAccessRepository,
                        UiDestination(UiScreen.ShelterDogDetail, recordId = id),
                    )
                }
            }
            compose.waitUntil(30_000) {
                compose.onAllNodesWithTag("dog-remove").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("dog-remove").performScrollTo().performClick()
            compose.onNodeWithTag("dog-remove-cancel").performClick()
            assertEquals(
                true,
                db.collection("perritos")
                    .document(id)
                    .get(Source.SERVER)
                    .await()
                    .getBoolean("publicado"),
            )
            compose.onNodeWithTag("dog-remove").performScrollTo().performClick()
            compose.onNodeWithTag("dog-remove-confirm").performClick()
            compose.waitUntil(30_000) {
                compose
                    .onAllNodesWithText("Administra tus perritos")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithText("Prueba eliminación nube").assertDoesNotExist()
            val saved = db.collection("perritos").document(id).get(Source.SERVER).await()
            assertEquals(true, saved.getBoolean("eliminado"))
            assertEquals(false, saved.getBoolean("publicado"))
            assertEquals(uid, saved.getString("eliminadoPorUid"))
            assertNotNull(saved.getTimestamp("eliminadoEn"))
            assertTrue(
                db.collection("perritos_control").document(id).get(Source.SERVER).await().exists()
            )
            repo.remove(uid, shelter, id)
            assertEquals(
                saved.getTimestamp("eliminadoEn"),
                db.collection("perritos")
                    .document(id)
                    .get(Source.SERVER)
                    .await()
                    .getTimestamp("eliminadoEn"),
            )
            publicApp =
                FirebaseApp.initializeApp(
                    context,
                    app.options,
                    "removal-public-${UUID.randomUUID()}",
                )
            val publicDb = FirebaseFirestore.getInstance(publicApp)
            assertTrue(
                runCatching {
                        publicDb.collection("perritos").document(id).get(Source.SERVER).await()
                    }
                    .isFailure
            )
            publicDb.terminate().await()
            assertEquals(uid, FirebaseAuth.getInstance(app).currentUser?.uid)
            android.util.Log.i(
                "DogRemovalTest",
                "PASS cancelación, retiro confirmado, lista actualizada, reintento y acceso público bloqueado; ficha=$id",
            )
        } finally {
            publicApp?.delete()
            file.delete()
        }
    }
}
