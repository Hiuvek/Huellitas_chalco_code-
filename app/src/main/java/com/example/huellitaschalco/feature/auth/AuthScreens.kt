package com.example.huellitaschalco.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*

@Composable
fun AuthScreen(nav: UiNavigator, destination: UiDestination) {
    val signUp = destination.screen == UiScreen.SignUp
    val shelter = destination.screen == UiScreen.ShelterLogin
    val error = destination.variant == UiVariant.Error
    val title =
        when {
            signUp -> "Crear cuenta"
            shelter -> "Acceso para refugios"
            else -> "Iniciar sesión"
        }
    HScaffold(
        nav,
        title,
        bottom = {
            HButton(
                if (signUp) "Crear cuenta"
                else if (shelter) "Iniciar sesión como refugio" else "Iniciar sesión",
                { nav.root(if (shelter) UiScreen.ShelterDogs else UiScreen.Catalog) },
                icon = if (shelter) HSymbol.Shield else null,
            )
            if (!shelter)
                HTextAction(
                    if (signUp) "¿Ya tienes cuenta? Iniciar sesión"
                    else "¿No tienes cuenta? Crear cuenta",
                    { nav.open(if (signUp) UiScreen.Login else UiScreen.SignUp) },
                )
        },
    ) {
        if (shelter) {
            HTag("Cuenta autorizada")
            Text(
                "Administra tus perritos y solicitudes",
                style = MaterialTheme.typography.headlineMedium,
            )
            HBody(
                "Solo las cuentas de refugio autorizadas pueden entrar a esta sección.",
                muted = true,
            )
        } else
            HBody(
                if (signUp) "Crea tu cuenta para enviar solicitudes de adopción."
                else "Ingresa para enviar solicitudes y consultar su estado.",
                muted = true,
            )
        if (signUp)
            HInput(
                "Nombre completo",
                error =
                    if (destination.variant == UiVariant.FieldError) "Escribe tu nombre." else null,
            )
        HInput(
            if (shelter) "Correo del refugio" else "Correo electrónico",
            if (signUp) "" else if (shelter) "refugio1@demo.mx" else "usuario1@demo.mx",
            keyboard = KeyboardType.Email,
        )
        HInput(
            "Contraseña",
            if (signUp) "" else "huellitas",
            password = true,
            helper = "Mínimo 6 caracteres.",
        )
        if (error)
            HBanner(
                "No pudimos iniciar sesión: el correo o la contraseña no coinciden. Revisa tus datos e intenta de nuevo.",
                HSymbol.Alert,
                error = true,
            )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun AccessRestrictedScreen(
    nav: UiNavigator,
    email: String? = null,
    useOtherAccount: () -> Unit = { nav.root(UiScreen.ShelterLogin) },
) {
    HScaffold(
        nav,
        "Acceso restringido",
        bottom = {
            HButton(
                "Ir al catálogo de adopción",
                { nav.root(UiScreen.Catalog) },
                icon = HSymbol.Compass,
            )
            HTextAction(
                if (email == null) "Iniciar sesión como refugio" else "Usar otra cuenta",
                useOtherAccount,
            )
        },
    ) {
        HEmpty(
            "Esta sección es solo para refugios autorizados",
            if (email == null) "Inicia sesión con una cuenta de refugio autorizada para continuar."
            else
                "La cuenta $email no tiene una membresía activa para administrar perritos, solicitudes ni seguimientos.",
            HSymbol.Shield,
            "Regresar",
            { nav.root(UiScreen.Catalog) },
        )
        HBanner(
            "Como visitante o adoptante puedes consultar perritos y los datos públicos de cada refugio. Las solicitudes privadas y los seguimientos solo los ve el refugio correspondiente."
        )
    }
}
