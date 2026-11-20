package com.example.huellitaschalco.feature.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.feature.auth.data.FirebaseAuthRepository
import com.example.huellitaschalco.feature.auth.domain.AuthFailure
import com.example.huellitaschalco.feature.auth.domain.AuthProblem
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Prueba opcional: crea UNA cuenta ficticia en el proyecto indicado expresamente al ejecutarla. */
@RunWith(AndroidJUnit4::class)
class CloudAuthSmokeTest {
    @Test
    fun realProjectRegisterLoginAndRestore() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(
            "La prueba en la nube requiere autorización explícita al ejecutarla",
            args.getString("cloudSmoke") == "true",
        )
        val options =
            FirebaseOptions.Builder()
                .setProjectId(requireNotNull(args.getString("firebaseProjectId")))
                .setApplicationId(requireNotNull(args.getString("firebaseAppId")))
                .setApiKey(requireNotNull(args.getString("firebaseApiKey")))
                .build()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val appName = "cloud-hu01-smoke"
        var app = FirebaseApp.initializeApp(context, options, appName)
        var client = FirebaseAuth.getInstance(app)
        try {
            withTimeout(60_000) {
                val repository = FirebaseAuthRepository(client)
                val email = "hu01-${UUID.randomUUID()}@example.test"
                val password = "PruebaHU01-${UUID.randomUUID()}!"
                repository.signUp("Prueba HU-01", email, password)
                val user = requireNotNull(repository.user.value)
                assertEquals("Prueba HU-01", user.displayName)
                assertEquals(email, user.email)
                repository.signOut()
                assertNull(repository.user.value)
                try {
                    repository.signIn(email, "Incorrecta123!")
                    fail("No debe aceptar una contraseña incorrecta")
                } catch (error: AuthFailure) {
                    assertEquals(AuthProblem.InvalidCredentials, error.problem)
                    assertNull(repository.user.value)
                }
                repository.signIn(email, password)
                assertEquals(user, repository.user.value)
                app.delete()
                app = FirebaseApp.initializeApp(context, options, appName)
                client = FirebaseAuth.getInstance(app)
                assertEquals(user, FirebaseAuthRepository(client).user.value)
            }
        } finally {
            client.signOut()
            app.delete()
        }
    }
}
