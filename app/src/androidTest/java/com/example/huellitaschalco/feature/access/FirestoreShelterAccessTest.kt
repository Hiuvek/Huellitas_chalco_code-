package com.example.huellitaschalco.feature.access

import androidx.test.core.app.ApplicationProvider
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.feature.access.data.FirestoreShelterAccessRepository
import com.example.huellitaschalco.feature.access.domain.*
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class FirestoreShelterAccessTest {
    private var app: FirebaseApp? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var auth: FirebaseAuthRepository
    private lateinit var access: FirestoreShelterAccessRepository
    private lateinit var db: FirebaseFirestore

    @Before
    fun setup() {
        assumeTrue(BuildConfig.FIREBASE_AUTH_EMULATOR)
        val local =
            FirebaseApp.initializeApp(
                ApplicationProvider.getApplicationContext(),
                FirebaseOptions.Builder()
                    .setProjectId("demo-huellitas-chalco")
                    .setApplicationId("1:1234567890:android:0123456789abcdef")
                    .setApiKey("demo-firebase-auth-emulator-key")
                    .build(),
                "hu07-${UUID.randomUUID()}",
            )
        app = local
        val client = FirebaseAuth.getInstance(local)
        client.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9099)
        auth = FirebaseAuthRepository(client, true)
        db = FirebaseFirestore.getInstance(local)
        db.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        db.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 8180)
        access = FirestoreShelterAccessRepository(auth, db, scope)
    }

    @After
    fun cleanup() {
        scope.cancel()
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        app?.delete()
    }

    @Test
    fun loginLoadsOnlyActiveShelterMembershipAndChangingAccountDropsIt() = runBlocking {
        withTimeout(30_000) {
            auth.signIn("hu07-a@example.test", "PruebaHU07!")
            val first =
                access.access.first { it is ShelterAccess.Ready && it.uid == auth.user.value?.uid }
                    as ShelterAccess.Ready
            assertEquals(listOf("hu07-a"), first.shelters.map { it.id })
            auth.signOut()
            access.access.first { it is ShelterAccess.SignedOut }
            auth.signIn("hu07-b@example.test", "PruebaHU07!")
            val second =
                access.access.first { it is ShelterAccess.Ready && it.uid == auth.user.value?.uid }
                    as ShelterAccess.Ready
            assertEquals(listOf("hu07-b"), second.shelters.map { it.id })
            assertTrue(first.sheltersFor(auth.user.value?.uid).isEmpty())
            auth.signOut()
            auth.signIn("hu07-adopter@example.test", "PruebaHU07!")
            val adopter =
                access.access.first { it is ShelterAccess.Ready && it.uid == auth.user.value?.uid }
                    as ShelterAccess.Ready
            assertTrue(adopter.shelters.isEmpty())
        }
    }

    @Test
    fun androidSdkCannotReadAnotherSheltersPrivateRecords() = runBlocking {
        withTimeout(30_000) {
            auth.signIn("hu07-a@example.test", "PruebaHU07!")
            assertEquals(
                "hu07-a",
                db.collection("seguimientos")
                    .document("hu07-a")
                    .get(Source.SERVER)
                    .await()
                    .getString("refugioId"),
            )
            try {
                db.collection("seguimientos").document("hu07-b").get(Source.SERVER).await()
                fail("No debe leer seguimientos ajenos")
            } catch (error: FirebaseFirestoreException) {
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
            }
        }
    }

    @Test
    fun offlineStateCannotKeepAuthorizationFromCache(): Unit = runBlocking {
        withTimeout(30_000) {
            auth.signIn("hu07-a@example.test", "PruebaHU07!")
            access.access.first { it is ShelterAccess.Ready && it.shelters.isNotEmpty() }
            db.disableNetwork().await()
            access.refresh()
            access.access.first { it is ShelterAccess.Checking }
            assertTrue(access.access.value.sheltersFor(auth.user.value?.uid).isEmpty())
            db.enableNetwork().await()
            access.access.first { it is ShelterAccess.Ready && it.shelters.isNotEmpty() }
        }
    }
}
