package com.example.huellitaschalco.feature.access

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.feature.access.domain.ShelterAccess
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Solo lectura: usa la sesión existente y exige activación explícita; no solicita contraseñas. */
class CloudShelterAccessTest {
    @Test
    fun existingSessionResolvesAuthorizedMembershipInRealProject(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("hu07CloudRole") == "true" && !BuildConfig.FIREBASE_AUTH_EMULATOR)
        assertEquals("huellitas-chalco", FirebaseApp.getInstance().options.projectId)
        val container = ApplicationProvider.getApplicationContext<HuellitasApplication>().container
        val user =
            requireNotNull(container.authRepository.user.value) {
                "Inicia sesión en la app con la cuenta autorizada antes de ejecutar esta prueba."
            }
        assertEquals(args.getString("expectedUid"), user.uid)
        val refuge = requireNotNull(args.getString("expectedShelterId"))
        container.shelterAccessRepository.refresh()
        withTimeout(30_000) {
            val access =
                container.shelterAccessRepository.access.first {
                    it is ShelterAccess.Ready && it.uid == user.uid
                } as ShelterAccess.Ready
            assertTrue(access.shelters.any { it.id == refuge })
            val db = FirebaseFirestore.getInstance()
            val membership =
                db.collection("membresias")
                    .document("${user.uid}__$refuge")
                    .get(Source.SERVER)
                    .await()
            assertEquals(true, membership.getBoolean("activo"))
            db.collection("seguimientos")
                .whereEqualTo("refugioId", refuge)
                .get(Source.SERVER)
                .await()
            try {
                db.collection("seguimientos")
                    .whereEqualTo("refugioId", "refugio-ajeno-no-autorizado")
                    .get(Source.SERVER)
                    .await()
                fail("No debe consultar un refugio ajeno")
            } catch (error: FirebaseFirestoreException) {
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
            }
        }
    }
}
