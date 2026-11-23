package com.example.huellitaschalco.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.access.domain.Shelter
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.refuge.maps.AndroidPublicMapLauncher
import com.example.huellitaschalco.ui.theme.Line

/** Datos públicos y cuenta; las tareas de administración permanecen en la navegación inferior. */
@Composable
internal fun ShelterProfileContent(
    nav: UiNavigator,
    refuge: Shelter?,
    user: AuthUser?,
    state: PublicShelterState,
    retry: () -> Unit,
) {
    val info = state.shelter
    var expanded by rememberSaveable(refuge?.id) { mutableStateOf(false) }
    HCard {
        HHeading("Información del refugio")
        if (info?.demonstration == true) HTag("Refugio de demostración")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HCaption("Zona pública")
            HBody(
                info?.let { "${it.zone}, ${it.municipality}, ${it.state}" }
                    ?: refuge?.publicZone?.takeIf { it.isNotBlank() }
                    ?: "Sin registrar"
            )
        }
        HorizontalDivider(color = Line)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HCaption("Horario de atención")
            HBody(
                info?.hours?.takeIf { it.isNotBlank() }
                    ?: refuge?.publicHours?.takeIf { it.isNotBlank() }
                    ?: "Sin registrar"
            )
        }
        if (info != null) ShelterMapAction(info)
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HCaption("No pudimos actualizar estos datos. Revisa tu conexión.")
                HTextAction("Reintentar actualización", retry, icon = HSymbol.Refresh)
            }
            state.stale -> HCaption("Información guardada. Conéctate para actualizarla.")
        }
    }
    val policy = info?.policy
    if (policy != null) {
        HCard {
            HHeading("Proceso de adopción")
            HCaption(
                "${policy.requirements.size} requisitos · ${policy.steps.size} pasos publicados"
            )
            HButton(
                if (expanded) "Ocultar requisitos y pasos" else "Ver requisitos y pasos",
                { expanded = !expanded },
                Modifier.testTag("profile-policy-toggle"),
                outlined = true,
                icon = HSymbol.Document,
            )
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HHeading("Requisitos")
                    policy.requirements.forEachIndexed { i, text -> HBody("${i + 1}. $text") }
                    HorizontalDivider(color = Line)
                    HHeading("Pasos de adopción")
                    policy.steps.forEachIndexed { i, text -> HBody("${i + 1}. $text") }
                }
            }
        }
    } else if (!state.loading && state.error == null) {
        HCard {
            HHeading("Proceso de adopción")
            HBody("El refugio todavía no tiene requisitos y pasos completos publicados.")
        }
    }
    HCard(onClick = { nav.root(UiScreen.Catalog) }) {
        HHeading("Vista para adoptantes")
        HCaption("Consulta cómo se presentan los perritos en el catálogo público.")
        HCardAction("Ver catálogo público")
    }
    HCard {
        HHeading("Cuenta responsable")
        HBody(user?.label ?: "Responsable")
        HCaption(user?.email ?: "Sin sesión iniciada")
        HCaption("Acceso autorizado a este refugio")
    }
}

@Composable
private fun ShelterMapAction(shelter: PublicShelter) {
    val context = LocalContext.current
    val launcher = remember(context) { AndroidPublicMapLauncher(context.applicationContext) }
    val url = remember(shelter) { shelter.googleMapsUrl() }
    var failed by remember(shelter.id, url) { mutableStateOf(false) }
    if (url == null) {
        HCaption("Todavía no se ha publicado una ubicación para Google Maps.")
    } else {
        HTextAction(
            if (failed) "Reintentar abrir Google Maps" else "Abrir ubicación en Google Maps",
            {
                failed =
                    try {
                        launcher.open(url)
                        false
                    } catch (_: Exception) {
                        true
                    }
            },
            icon = HSymbol.Pin,
        )
        if (failed)
            HBanner("No se pudo abrir Google Maps. Puedes reintentar.", HSymbol.Alert, error = true)
        HCaption("Ubicación aproximada. Confirma la visita con el refugio.")
    }
}
