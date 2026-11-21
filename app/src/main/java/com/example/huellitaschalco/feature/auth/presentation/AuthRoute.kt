package com.example.huellitaschalco.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.auth.domain.*

@Composable
fun AuthRoute(nav: UiNavigator, destination: UiDestination, repository: AuthRepository) {
    val signUp = destination.screen == UiScreen.SignUp
    val shelter = destination.screen == UiScreen.ShelterLogin
    val model: AuthViewModel =
        viewModel(
            key = if (signUp) "register" else if (shelter) "shelter-login" else "login",
            factory = AuthViewModel.factory(repository, signUp),
        )
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) {
        if (state.completed) {
            val applicationDog = destination.recordId.takeIf { !shelter }
            nav.root(if (shelter) UiScreen.ShelterDogs else UiScreen.Catalog)
            if (applicationDog != null) {
                nav.open(UiScreen.Requirements, recordId = applicationDog)
                nav.open(UiScreen.ApplicationContact, recordId = applicationDog)
            }
            model.clear()
        }
    }
    HScaffold(
        nav,
        if (signUp) "Crear cuenta" else if (shelter) "Acceso para refugios" else "Iniciar sesión",
        bottom = {
            HButton(
                if (state.isLoading) "Espera un momento…"
                else if (signUp) "Crear cuenta"
                else if (shelter) "Iniciar sesión como refugio" else "Iniciar sesión",
                model::submit,
                Modifier.testTag("auth-submit"),
                enabled = !state.isLoading && repository.isConfigured,
            )
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!shelter)
                TextButton(
                    onClick = {
                        nav.open(
                            if (signUp) UiScreen.Login else UiScreen.SignUp,
                            recordId = destination.recordId,
                        )
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (signUp) "¿Ya tienes cuenta? Iniciar sesión"
                        else "¿No tienes cuenta? Crear cuenta"
                    )
                }
        },
    ) {
        HBody(
            if (signUp) "Crea tu cuenta para enviar solicitudes de adopción."
            else if (shelter)
                "Solo las cuentas con una membresía activa pueden administrar su refugio."
            else "Ingresa para enviar solicitudes y consultar su estado.",
            muted = true,
        )
        if (!repository.isConfigured)
            HBanner(
                "El acceso todavía no está disponible en esta instalación.",
                HSymbol.Info,
                error = true,
            )
        if (repository.usesEmulator)
            HBanner(
                "Entorno de prueba local. Estas cuentas no se crean en el proyecto real.",
                HSymbol.Info,
            )
        if (signUp)
            HInput(
                label = "Nombre completo",
                value = state.name,
                onValueChange = { model.edit(AuthField.Name, it) },
                modifier = Modifier.testTag("auth-name"),
                error = state.fieldErrors[AuthField.Name],
                enabled = !state.isLoading,
            )
        HInput(
            label = if (shelter) "Correo del refugio" else "Correo electrónico",
            value = state.email,
            onValueChange = { model.edit(AuthField.Email, it) },
            modifier = Modifier.testTag("auth-email"),
            error = state.fieldErrors[AuthField.Email],
            keyboard = KeyboardType.Email,
            enabled = !state.isLoading,
        )
        HInput(
            label = "Contraseña",
            value = state.password,
            onValueChange = { model.edit(AuthField.Password, it) },
            modifier = Modifier.testTag("auth-password"),
            error = state.fieldErrors[AuthField.Password],
            password = true,
            helper = if (signUp) "Mínimo 6 caracteres." else null,
            enabled = !state.isLoading,
        )
        state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
        Spacer(Modifier.height(20.dp))
    }
}
