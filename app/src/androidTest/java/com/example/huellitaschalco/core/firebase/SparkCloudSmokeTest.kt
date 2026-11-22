package com.example.huellitaschalco.core.firebase

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.feature.shelter.dogs.data.FirebaseDogRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.google.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Activación explícita. Conserva la sesión del propietario; solo crea la ficha de prueba indicada.
 */
class SparkCloudSmokeTest {
    @Test
    fun realProjectSharesSmallPhotoWithIndependentPublicClient(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("sparkCloud") == "true" && !BuildConfig.FIREBASE_AUTH_EMULATOR)
        val id = requireNotNull(args.getString("sparkDogId"))
        require(id.startsWith("spark-smoke-") && id.matches(Regex("[A-Za-z0-9-]{1,100}")))
        val context = ApplicationProvider.getApplicationContext<HuellitasApplication>()
        val app = FirebaseApp.getInstance()
        assertEquals("huellitas-chalco", app.options.projectId)
        val auth = context.container.authRepository
        val uid =
            requireNotNull(FirebaseAuth.getInstance(app).currentUser) {
                    "Se necesita la sesión del responsable autorizada en la app normal."
                }
                .uid
        val db = FirebaseFirestore.getInstance(app)
        val shelter = "refugio-prueba-chalco"
        assertEquals(
            true,
            db.collection("membresias")
                .document("${uid}__$shelter")
                .get(Source.SERVER)
                .await()
                .getBoolean("activo"),
        )
        assertFalse(db.collection("perritos").document(id).get(Source.SERVER).await().exists())
        val file = File(context.cacheDir, "$id.jpg")
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(120, 150, 110))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
        bitmap.recycle()
        val photoId = UUID.randomUUID().toString()
        var publicApp: FirebaseApp? = null
        try {
            val repo = FirebaseDogRepository(auth, db, null, false)
            val dog =
                repo.publish(
                    DogPublication(
                        id,
                        uid,
                        shelter,
                        DogDraft(
                            "Verificación Spark temporal",
                            "12",
                            "Meses",
                            "Mediano",
                            "Macho",
                            "Ficha temporal de verificación técnica del proyecto escolar. No representa un perrito real y será retirada al terminar la prueba.",
                            listOf(LocalDogPhoto(photoId, file.path)),
                        ),
                    )
                ) {}
            // Otra instancia sin sesión ni caché compartida representa al cliente público.
            publicApp =
                FirebaseApp.initializeApp(context, app.options, "spark-public-${UUID.randomUUID()}")
            val publicDb = FirebaseFirestore.getInstance(publicApp)
            publicDb.firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            assertEquals(
                "Verificación Spark temporal",
                publicDb
                    .collection("perritos")
                    .document(id)
                    .get(Source.SERVER)
                    .await()
                    .getString("nombre"),
            )
            assertArrayEquals(
                file.readBytes(),
                FirestorePhotos(publicDb).read(dog.photos.single().path),
            )
            publicDb.terminate().await()
        } finally {
            publicApp?.delete()
            file.delete()
        }
    }
}
