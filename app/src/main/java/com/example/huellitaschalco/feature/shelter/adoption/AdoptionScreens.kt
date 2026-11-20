package com.example.huellitaschalco.feature.shelter.adoption

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.*

@Composable
fun AdoptionFormScreen(nav: UiNavigator) {
    val request = PreviewData.request(nav.current.requestId)
    val dog = PreviewData.dog(request.dogId)
    HScaffold(
        nav,
        "Registrar adopción",
        "${dog.name} · ${request.user}",
        bottom = {
            HButton(
                "Confirmar adopción",
                { nav.open(UiScreen.AdoptionComplete, dogId = dog.id, requestId = request.id) },
                icon = HSymbol.Home,
            )
        },
    ) {
        HDogRow(dog, "Con ${request.user}", "Aprobada")
        HBanner(
            "Solicitud aprobada y adopción realizada son pasos distintos. Registra la adopción solo cuando la entrega ya ocurrió."
        )
        HHeading("Para confirmar se necesita")
        HCard {
            HBody(
                if (request.id == 3) "✓ Solicitud aprobada el 11 nov 2026."
                else "✓ Solicitud aprobada."
            )
            HBody("✓ Fecha real de entrega.")
            HBody("Confirmación de entrega y carta de adopción firmada.")
        }
        HHeading("Entrega")
        HInput("Fecha de entrega", "16/11/2026", helper = "Formato DD/MM/AAAA.")
        HCheck("Confirmo que la entrega se realizó y se firmó la carta de adopción.")
        HBanner(
            "Al confirmar, las demás solicitudes pendientes para ${dog.name} pasarán a No aprobada con el motivo: el perrito fue adoptado por otra persona.",
            error = true,
        )
    }
}

@Composable
fun AdoptionCompleteScreen(nav: UiNavigator) {
    val request = PreviewData.request(nav.current.requestId)
    val dog = PreviewData.dog(request.dogId)
    HScaffold(nav, "", back = false) {
        HSuccessHeader(
            "Adopción registrada",
            "${dog.name} ya vive con ${request.user}.",
            HSymbol.Home,
        )
        HDogRow(dog, "Con ${request.user}", "Adoptado")
        HCard {
            HKeyValue("Fecha de entrega", "16 nov 2026")
            HKeyValue("Folio", request.folio, true)
            HTag("Aprobada")
            HTag("Adopción realizada")
        }
        HHeading("Cambios registrados")
        HCard {
            listOf(
                    "${dog.name} aparece como Adoptado en Mis perritos.",
                    "Ya no se muestra en el catálogo público de adopción.",
                    "No se puede registrar otra adopción de ${dog.name}.",
                    "La otra solicitud pasó a No aprobada. Las solicitudes de otros perritos se conservan.",
                )
                .forEach { HBody("✓ $it") }
        }
        HHeading("Primer seguimiento")
        HInput(
            "Fecha de revisión",
            "30/11/2026",
            helper = "Sugerimos entre 2 y 4 semanas después de la entrega.",
        )
        HChoices("Tipo de seguimiento", listOf("Visita a domicilio", "Llamada", "Videollamada"))
        HButton(
            "Programar seguimiento",
            { nav.open(UiScreen.FollowUps, dogId = dog.id, variant = UiVariant.AfterAdoption) },
            icon = HSymbol.Calendar,
        )
        HTextAction(
            "Programar más tarde",
            { nav.open(UiScreen.ShelterDogs, variant = UiVariant.AfterAdoption) },
        )
        HButton(
            "Ver solicitudes actualizadas",
            { nav.open(UiScreen.ShelterRequests, variant = UiVariant.AfterAdoption) },
            outlined = true,
        )
    }
}

@Composable
fun AdoptionBlockedScreen(nav: UiNavigator) {
    HScaffold(nav, "Registrar adopción") {
        HEmpty(
            "Adopción ya registrada",
            "Perrito 2 ya tiene una adopción registrada. No se puede registrar otra adopción del mismo perrito.",
            HSymbol.Home,
            "Ver adopciones",
            { nav.open(UiScreen.FollowUps) },
        )
    }
}
