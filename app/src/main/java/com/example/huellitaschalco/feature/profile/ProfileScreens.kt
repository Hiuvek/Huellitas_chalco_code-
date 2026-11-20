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
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun ProfileScreen(nav: UiNavigator, shelter: Boolean) {
    var logout by remember { mutableStateOf(false) }
    HScaffold(
        nav,
        if (shelter) "Perfil del refugio" else "Perfil",
        back = false,
        tab = "Perfil",
        shelter = shelter,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(72.dp).background(Forest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (shelter) "R1" else "U1",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (shelter) "Refugio 1" else "Usuario 1",
                    style = MaterialTheme.typography.titleLarge,
                )
                HCaption(if (shelter) "Refugio ficticio · demostración" else "usuario1@demo.mx")
                if (shelter) HTag("Cuenta autorizada")
            }
        }
        if (shelter) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HMetric("Publicados", "4", Modifier.weight(1f))
                HMetric("Nuevas", "3", Modifier.weight(1f))
                HMetric("Revisiones", "1", Modifier.weight(1f))
            }
            HHeading("Información pública")
            HCard {
                HKeyValue("Zona", "Centro de Chalco")
                HKeyValue("Horario", "Lunes a sábado · 10 a 17 h", true)
                HKeyValue("Responsable", "Responsable 1", true)
            }
            HHeading("Administración")
            HButton(
                "Datos del refugio",
                { nav.open(UiScreen.RefugeInformation) },
                outlined = true,
                icon = HSymbol.Edit,
            )
            HButton(
                "Requisitos de adopción",
                { nav.open(UiScreen.Requirements) },
                outlined = true,
                icon = HSymbol.Document,
            )
            HBanner(
                "Solo las cuentas autorizadas pueden administrar un refugio, publicar perritos y evaluar solicitudes.",
                HSymbol.Shield,
            )
        } else {
            HHeading("Mis datos")
            HCard {
                HKeyValue("Correo", "usuario1@demo.mx")
                HKeyValue("Teléfono", "5500000001", true)
                HKeyValue("Solicitudes enviadas", "1", true)
            }
            HHeading("En este teléfono")
            HCard {
                HHeading("Catálogo disponible sin conexión")
                HCaption("Actualizado hoy a las 9:30")
                HorizontalDivider(color = Line)
                HHeading("Borradores de solicitud")
                HCaption("Ninguno")
            }
        }
        HBanner("Proyecto académico. Los refugios, perritos y personas son ficticios.")
        HButton(
            "Cerrar sesión",
            { logout = true },
            outlined = true,
            danger = true,
            icon = HSymbol.Logout,
        )
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
            text = { Text("Puedes volver a explorar el catálogo cuando quieras.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        logout = false
                        nav.root(UiScreen.Welcome)
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = { TextButton(onClick = { logout = false }) { Text("Cancelar") } },
        )
}
