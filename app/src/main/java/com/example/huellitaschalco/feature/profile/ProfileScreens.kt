package com.example.huellitaschalco.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.access.presentation.LocalShelterScope
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.refuge.maps.PublicMapPanel
import com.example.huellitaschalco.ui.theme.*
import kotlinx.coroutines.flow.flowOf

@Composable
fun ProfileScreen(
    nav: UiNavigator,
    shelter: Boolean,
    user: AuthUser? = null,
    onSignOut: () -> Unit = {},
    shelters: PublicShelterRepository? = null,
) {
    val refuge = LocalShelterScope.current?.current
    var refresh by remember { mutableIntStateOf(0) }
    val publicFlow =
        remember(refuge?.id, shelters, refresh) {
            refuge?.id?.let { shelters?.observe(it) } ?: flowOf(PublicShelterState(false))
        }
    val publicState by publicFlow.collectAsStateWithLifecycle(PublicShelterState())
    var logout by remember { mutableStateOf(false) }
    HScaffold(
        nav,
        if (shelter) "Perfil del refugio" else "Mi cuenta",
        back = false,
        tab = "Perfil",
        shelter = shelter,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(64.dp).background(Forest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (shelter) refuge?.name?.take(2)?.uppercase() ?: "HC"
                    else user?.label?.take(2)?.uppercase() ?: "HC",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (shelter) refuge?.name ?: "Refugio" else user?.label ?: "Visitante",
                    style = MaterialTheme.typography.titleLarge,
                )
                HCaption(user?.email ?: "Sin sesión iniciada")
                if (shelter) HTag("Cuenta autorizada")
            }
        }
        if (shelter) {
            HScreenIntro(
                "Tu espacio de administración",
                "Elige qué quieres revisar. Los registros se consultan desde tu refugio actual.",
            )
            HButton("Ver mis perritos", { nav.root(UiScreen.ShelterDogs) }, icon = HSymbol.Paw)
            HButton(
                "Revisar solicitudes",
                { nav.root(UiScreen.ShelterRequests) },
                outlined = true,
                icon = HSymbol.Inbox,
            )
            HButton(
                "Abrir seguimientos",
                { nav.root(UiScreen.FollowUps) },
                outlined = true,
                icon = HSymbol.Calendar,
            )
            HHeading("Información pública del refugio")
            HCard {
                HKeyValue("Zona", refuge?.publicZone?.takeIf { it.isNotBlank() } ?: "Sin registrar")
                HKeyValue(
                    "Horario",
                    refuge?.publicHours?.takeIf { it.isNotBlank() } ?: "Sin registrar",
                    true,
                )
            }
            when {
                publicState.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                publicState.shelter != null -> {
                    val info = requireNotNull(publicState.shelter)
                    info.policy?.let { policy ->
                        HHeading("Requisitos publicados")
                        HCard {
                            policy.requirements.forEachIndexed { i, text ->
                                HBody("${i + 1}. $text")
                            }
                        }
                        HHeading("Pasos de adopción")
                        HCard {
                            policy.steps.forEachIndexed { i, text -> HBody("${i + 1}. $text") }
                        }
                    }
                    PublicMapPanel(info)
                }
                publicState.error != null -> {
                    HBanner(
                        "No se pudo actualizar la información pública. Revisa tu conexión.",
                        HSymbol.Alert,
                    )
                    HButton("Actualizar información", { refresh++ }, outlined = true)
                }
            }
            HButton("Ver catálogo público", { nav.root(UiScreen.Catalog) }, outlined = true)
        } else {
            HScreenIntro(
                if (user == null) "Explora y conoce a los perritos"
                else "Consulta el avance de tu adopción",
                if (user == null)
                    "Puedes explorar sin cuenta. Inicia sesión para enviar solicitudes y consultar su respuesta."
                else
                    "En Mis solicitudes encontrarás el estado, las respuestas y el historial de cada solicitud.",
            )
            if (user != null)
                HButton(
                    "Ver mis solicitudes",
                    { nav.root(UiScreen.MyRequests) },
                    icon = HSymbol.Document,
                )
            HButton(
                "Explorar perritos",
                { nav.root(UiScreen.Catalog) },
                outlined = user != null,
                icon = HSymbol.Paw,
            )
            HHeading("Tus datos y borradores")
            HBody(
                "Los borradores se conservan en este teléfono y se separan por cuenta. Vuelve a la solicitud del perrito para continuar donde te quedaste."
            )
            HBody(
                "Sin conexión puedes consultar la última copia del catálogo. Conéctate para actualizar la disponibilidad y enviar solicitudes.",
                muted = true,
            )
            HTextAction(
                "Acceso para refugios",
                { nav.root(UiScreen.ShelterDogs) },
                icon = HSymbol.Shield,
            )
        }
        HCaption(
            "Proyecto académico. Los registros identificados como demostración son ejemplos escolares."
        )
        if (user == null && !shelter)
            HButton("Iniciar sesión", { nav.open(UiScreen.Login) }, outlined = true)
        else HButton("Cerrar sesión", { logout = true }, outlined = true, icon = HSymbol.Logout)
        if (BuildConfig.SHOW_PREVIEW_GALLERY)
            HTextAction(
                "Ver todas las pantallas",
                { nav.open(UiScreen.PreviewGallery) },
                icon = HSymbol.Eye,
            )
    }
    if (logout)
        AlertDialog(
            onDismissRequest = { logout = false },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    "Tus solicitudes enviadas se conservan. Los borradores de esta cuenta permanecen en este teléfono."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        logout = false
                        onSignOut()
                        nav.root(UiScreen.Welcome)
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { logout = false }) { Text("Seguir en mi cuenta") }
            },
        )
}
