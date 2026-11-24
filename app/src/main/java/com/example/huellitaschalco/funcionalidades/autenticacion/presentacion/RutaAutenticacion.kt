package com.example.huellitaschalco.funcionalidades.autenticacion.presentacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun RutaAutenticacion(nav: NavegadorInterfaz, destination: DestinoInterfaz, repository: RepositorioAutenticacion) {
    val signUp = destination.screen == PantallaInterfaz.SignUp
    val shelter = destination.screen == PantallaInterfaz.ShelterLogin
    val model: ModeloVistaAutenticacion =
        viewModel(
            key = if (signUp) "register" else if (shelter) "shelter-login" else "login",
            factory = ModeloVistaAutenticacion.factory(repository, signUp),
        )
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) {
        if (state.completed) {
            val applicationDog = destination.recordId.takeIf { !shelter }
            nav.root(if (shelter) PantallaInterfaz.ShelterDogs else PantallaInterfaz.Catalog)
            if (applicationDog != null) {
                nav.open(PantallaInterfaz.Requirements, recordId = applicationDog)
                nav.open(PantallaInterfaz.ApplicationContact, recordId = applicationDog)
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
                            if (signUp) PantallaInterfaz.Login else PantallaInterfaz.SignUp,
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
        HHeading(
            if (signUp) "Empieza tu solicitud con una cuenta"
            else if (shelter) "Administra tu refugio" else "Continúa con tu cuenta"
        )
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
                SimboloHuellitas.Info,
                error = true,
            )
        if (signUp)
            HInput(
                label = "Nombre completo",
                value = state.name,
                onValueChange = { model.edit(CampoAutenticacion.Name, it) },
                modifier = Modifier.testTag("auth-name"),
                error = state.fieldErrors[CampoAutenticacion.Name],
                enabled = !state.isLoading,
            )
        HInput(
            label = if (shelter) "Correo del refugio" else "Correo electrónico",
            value = state.email,
            onValueChange = { model.edit(CampoAutenticacion.Email, it) },
            modifier = Modifier.testTag("auth-email"),
            error = state.fieldErrors[CampoAutenticacion.Email],
            keyboard = KeyboardType.Email,
            enabled = !state.isLoading,
        )
        HInput(
            label = "Contraseña",
            value = state.password,
            onValueChange = { model.edit(CampoAutenticacion.Password, it) },
            modifier = Modifier.testTag("auth-password"),
            error = state.fieldErrors[CampoAutenticacion.Password],
            password = true,
            helper = if (signUp) "Mínimo 6 caracteres." else null,
            enabled = !state.isLoading,
        )
        state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
        Spacer(Modifier.height(20.dp))
    }
}
