package com.example.huellitaschalco.feature.auth

import com.example.huellitaschalco.feature.auth.domain.*
import com.example.huellitaschalco.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidFormDoesNotCallFirebase() = runTest {
        val model = AuthViewModel(repository, true)
        model.submit()
        advanceUntilIdle()
        assertEquals(3, model.state.value.fieldErrors.size)
        assertEquals(0, repository.calls)
    }

    @Test
    fun loginTrimsEmailAndPreservesPasswordExactly() = runTest {
        val model = loginForm()
        model.submit()
        advanceUntilIdle()
        assertEquals("usuario@ejemplo.mx", repository.lastEmail)
        assertEquals(" 123456 ", repository.lastPassword)
        assertTrue(model.state.value.completed)
        assertEquals("", model.state.value.password)
    }

    @Test
    fun registrationPassesNameToRepository() = runTest {
        val model = AuthViewModel(repository, true)
        model.edit(AuthField.Name, " Usuario Chalco ")
        model.edit(AuthField.Email, " usuario@ejemplo.mx ")
        model.edit(AuthField.Password, "123456")
        model.submit()
        advanceUntilIdle()
        assertEquals("Usuario Chalco", repository.lastName)
        assertTrue(model.state.value.completed)
    }

    @Test
    fun connectionFailureKeepsCapturedFields() = runTest {
        repository.failure = AuthProblem.Network
        val model = loginForm()
        model.submit()
        advanceUntilIdle()
        assertEquals(" usuario@ejemplo.mx ", model.state.value.email)
        assertEquals(" 123456 ", model.state.value.password)
        assertEquals(AuthProblem.Network.message, model.state.value.error)
        assertFalse(model.state.value.completed)
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun duplicateEmailAppearsBesideEmailField() = runTest {
        repository.failure = AuthProblem.EmailInUse
        val model = loginForm()
        model.submit()
        advanceUntilIdle()
        assertEquals(AuthProblem.EmailInUse.message, model.state.value.fieldErrors[AuthField.Email])
        assertNull(model.state.value.error)
        assertEquals(" 123456 ", model.state.value.password)
    }

    @Test
    fun doubleTapStartsOnlyOneOperationAndNoSuccessBeforeCompletion() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val model = loginForm()
        model.submit()
        model.submit()
        runCurrent()
        assertEquals(1, repository.calls)
        assertTrue(model.state.value.isLoading)
        assertFalse(model.state.value.completed)
        model.edit(AuthField.Email, "otro@ejemplo.mx")
        assertEquals(" usuario@ejemplo.mx ", model.state.value.email)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.state.value.completed)
    }

    @Test
    fun failedOperationCanBeRetriedWithoutReenteringData() = runTest {
        repository.failure = AuthProblem.Network
        val model = loginForm()
        model.submit()
        advanceUntilIdle()
        repository.failure = null
        model.submit()
        advanceUntilIdle()
        assertEquals(2, repository.calls)
        assertTrue(model.state.value.completed)
    }

    @Test
    fun correctingFieldClearsItsError() = runTest {
        val model = AuthViewModel(repository, true)
        model.submit()
        model.edit(AuthField.Email, "nuevo@ejemplo.mx")
        assertFalse(AuthField.Email in model.state.value.fieldErrors)
        assertTrue(AuthField.Name in model.state.value.fieldErrors)
    }

    private fun loginForm() =
        AuthViewModel(repository, false).apply {
            edit(AuthField.Email, " usuario@ejemplo.mx ")
            edit(AuthField.Password, " 123456 ")
        }

    private class FakeAuthRepository : AuthRepository {
        override val user = MutableStateFlow<AuthUser?>(null)
        override val isConfigured = true
        override val usesEmulator = true
        var calls = 0
        var lastName = ""
        var lastEmail = ""
        var lastPassword = ""
        var failure: AuthProblem? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun signUp(name: String, email: String, password: String) {
            lastName = name
            signIn(email, password)
        }

        override suspend fun signIn(email: String, password: String) {
            calls++
            lastEmail = email
            lastPassword = password
            gate?.await()
            failure?.let { throw AuthFailure(it) }
            user.value = AuthUser("uid-prueba", email, lastName)
        }

        override fun signOut() {
            user.value = null
        }
    }
}
