package com.example.huellitaschalco.feature.requests

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.*

@Composable
fun MyRequestsScreen(nav: UiNavigator, destination: UiDestination) {
    HScaffold(nav, "Mis solicitudes", back = false, tab = "Solicitudes") {
        if (destination.variant == UiVariant.Empty)
            HEmpty(
                "Aún no tienes solicitudes",
                "Cuando envíes una solicitud, aquí podrás seguir su avance.",
                action = "Explorar perritos",
                onAction = { nav.root(UiScreen.Catalog) },
            )
        else {
            HHeading("Enviadas")
            HCard(onClick = { nav.open(UiScreen.RequestDetail, variant = destination.variant) }) {
                HDogRow(
                    PreviewData.dog(6),
                    "Enviada el 16 nov 2026 · HC-2026-0148",
                    if (destination.variant == UiVariant.InReview) "En revisión" else "Enviada",
                )
                HBanner(
                    if (destination.variant == UiVariant.InReview)
                        "Siguiente paso: espera la llamada del refugio para agendar una entrevista."
                    else "Siguiente paso: el refugio revisará tus respuestas en los próximos días."
                )
            }
        }
    }
}

@Composable
fun RequestDetailScreen(nav: UiNavigator, destination: UiDestination) {
    var showAnswers by remember { mutableStateOf(false) }
    val review = destination.variant == UiVariant.InReview
    HScaffold(nav, "Detalle de solicitud", "Folio HC-2026-0148") {
        HDogRow(
            PreviewData.dog(destination.dogId),
            "Huellitas del Valle",
            onClick = { nav.open(UiScreen.DogDetail) },
        )
        HCard {
            HCaption("Enviada el 16 nov 2026")
            HTag(if (review) "En revisión" else "Enviada")
            HBanner(
                if (review) "Espera la llamada del refugio para agendar una entrevista."
                else "El refugio revisará tus respuestas en los próximos días."
            )
        }
        HHeading("Historial")
        HTimeline(
            buildList {
                add(Triple("Enviada", "16 nov 2026", "Recibimos tu solicitud."))
                if (review)
                    add(
                        Triple(
                            "En revisión",
                            "17 nov 2026",
                            "El refugio está revisando tus respuestas.",
                        )
                    )
            }
        )
        HButton(
            if (showAnswers) "Ocultar respuestas" else "Tus respuestas",
            { showAnswers = !showAnswers },
            outlined = true,
            icon = HSymbol.Document,
        )
        if (showAnswers)
            HCard {
                HKeyValue("Nombre", "Usuario 1")
                HKeyValue("Hogar", "Casa propia · 3 personas", true)
                HKeyValue("Otras mascotas", "No", true)
                HHeading("Motivos")
                HBody(PreviewData.reasons)
            }
        HBanner(
            "El refugio se comunicará contigo por teléfono o correo. La decisión la toma el personal del refugio."
        )
    }
}
