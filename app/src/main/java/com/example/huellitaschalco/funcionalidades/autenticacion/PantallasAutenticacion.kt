package com.example.huellitaschalco.funcionalidades.autenticacion

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun AuthScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val signUp = destination.screen == PantallaInterfaz.SignUp
    val shelter = destination.screen == PantallaInterfaz.ShelterLogin
    val error = destination.variant == VarianteInterfaz.Error
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
                { nav.root(if (shelter) PantallaInterfaz.ShelterDogs else PantallaInterfaz.Catalog) },
                icon = if (shelter) SimboloHuellitas.Shield else null,
            )
            if (!shelter)
                HTextAction(
                    if (signUp) "¿Ya tienes cuenta? Iniciar sesión"
                    else "¿No tienes cuenta? Crear cuenta",
                    { nav.open(if (signUp) PantallaInterfaz.Login else PantallaInterfaz.SignUp) },
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
                    if (destination.variant == VarianteInterfaz.FieldError) "Escribe tu nombre." else null,
            )
        HInput(
            if (shelter) "Correo del refugio" else "Correo electrónico",
            if (signUp) ""
            else if (shelter) "adopciones@huellitasdelvalle.invalid" else "ana@correo.invalid",
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
                SimboloHuellitas.Alert,
                error = true,
            )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun AccessRestrictedScreen(
    nav: NavegadorInterfaz,
    email: String? = null,
    useOtherAccount: () -> Unit = { nav.root(PantallaInterfaz.ShelterLogin) },
) {
    HScaffold(
        nav,
        "Acceso restringido",
        bottom = {
            HButton(
                "Ir al catálogo de adopción",
                { nav.root(PantallaInterfaz.Catalog) },
                icon = SimboloHuellitas.Compass,
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
            SimboloHuellitas.Shield,
            "Regresar",
            { nav.root(PantallaInterfaz.Catalog) },
        )
        HBanner(
            "Como visitante o adoptante puedes consultar perritos y los datos públicos de cada refugio. Las solicitudes privadas y los seguimientos solo los ve el refugio correspondiente."
        )
    }
}
