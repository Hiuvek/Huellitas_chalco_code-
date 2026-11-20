package com.example.huellitaschalco.feature.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.auth.domain.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Integración con el SDK Android y Firebase Auth Emulator; nunca crea usuarios de producción. */
@RunWith(AndroidJUnit4::class)
class FirebaseAuthRepositoryTest {
    private var app: FirebaseApp? = null
    private lateinit var client: FirebaseAuth
    private lateinit var repository: FirebaseAuthRepository
    private val email
        get() = "integration-${UUID.randomUUID()}@example.test"

    @Before
    fun setUp() {
        assumeTrue(
            "Requiere -PfirebaseAuthEmulator=true y el emulador local",
            BuildConfig.FIREBASE_AUTH_EMULATOR,
        )
        app =
            FirebaseApp.initializeApp(
                ApplicationProvider.getApplicationContext(),
                FirebaseOptions.Builder()
                    .setProjectId("demo-huellitas-chalco")
                    .setApplicationId("1:1234567890:android:0123456789abcdef")
                    .setApiKey("demo-firebase-auth-emulator-key")
                    .build(),
                "integration-${UUID.randomUUID()}",
            )
        client = FirebaseAuth.getInstance(requireNotNull(app))
        client.useEmulator(BuildConfig.FIREBASE_AUTH_EMULATOR_HOST, 9099)
        repository = FirebaseAuthRepository(client, true)
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.signOut()
        app?.delete()
    }

    @Test
    fun registerLoginSignOutAndRestoreSession() = runBlocking {
        withTimeout(30_000) {
            val address = email
            repository.signUp("Usuario de prueba", address, "Password123!")
            val created = requireNotNull(repository.user.value)
            assertEquals("Usuario de prueba", created.displayName)
            assertEquals(address, created.email)
            repository.signOut()
            assertNull(repository.user.value)
            repository.signIn(address, "Password123!")
            assertEquals(created.uid, repository.user.value?.uid)
            val restored = FirebaseAuthRepository(client, true)
            assertEquals(created, restored.user.value)
        }
    }

    @Test
    fun duplicateEmailIsReported() = runBlocking {
        withTimeout(30_000) {
            val address = email
            repository.signUp("Primera cuenta", address, "Password123!")
            repository.signOut()
            try {
                repository.signUp("Segunda cuenta", address, "Password123!")
                fail("No debe duplicar una cuenta")
            } catch (error: AuthFailure) {
                assertEquals(AuthProblem.EmailInUse, error.problem)
            }
        }
    }

    @Test
    fun invalidPasswordDoesNotStartSession() = runBlocking {
        withTimeout(30_000) {
            val address = email
            repository.signUp("Usuario de prueba", address, "Password123!")
            repository.signOut()
            try {
                repository.signIn(address, "OtraPassword123!")
                fail("No debe iniciar sesión")
            } catch (error: AuthFailure) {
                assertEquals(AuthProblem.InvalidCredentials, error.problem)
                assertNull(repository.user.value)
            }
        }
    }
}
