package com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import com.example.huellitaschalco.nucleo.vista_previa.*

@Composable
fun ShelterRequestsScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val after = destination.variant == VarianteInterfaz.AfterAdoption
    HScaffold(
        nav,
        "Solicitudes",
        "Recibidas · Huellitas del Valle",
        back = false,
        shelter = true,
        tab = "Solicitudes",
    ) {
        if (destination.variant == VarianteInterfaz.Empty)
            HEmpty(
                "Aún no hay solicitudes recibidas",
                "Cuando alguien envíe una solicitud para un perrito de tu refugio, aparecerá aquí.",
                action = "Ver mis perritos",
                onAction = { nav.root(PantallaInterfaz.ShelterDogs) },
            )
        else {
            HChoices(
                null,
                listOf(
                    "Todas 8",
                    "Nuevas 3",
                    "En revisión ${if (after) 1 else 2}",
                    "Aprobadas",
                    "No aprobadas",
                ),
            )
            DatosVistaPrevia.requests.forEach { request ->
                val status = if (after && request.id == 8) "No aprobada" else request.status
                HCard(
                    onClick = {
                        nav.open(
                            PantallaInterfaz.EvaluateRequest,
                            dogId = request.dogId,
                            requestId = request.id,
                            variant =
                                if (after && request.id in listOf(3, 8)) VarianteInterfaz.AfterAdoption
                                else VarianteInterfaz.Normal,
                        )
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HPhoto(DatosVistaPrevia.dog(request.dogId), Modifier.size(62.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            HCaption("Para Perrito ${request.dogId}")
                            HHeading(request.user)
                            HTag(if (status == "Adopción realizada") "Aprobada" else status)
                            if (request.id == 6 || after && request.id == 3)
                                HTag("Adopción realizada")
                            if (after && request.id == 8)
                                HCaption("Motivo: el perrito fue adoptado por otra persona.")
                            HCaption(request.date)
                        }
                        Iconos(SimboloHuellitas.Chevron, Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluateRequestScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val request = DatosVistaPrevia.request(destination.requestId)
    val dog = DatosVistaPrevia.dog(request.dogId)
    val after = destination.variant == VarianteInterfaz.AfterAdoption
    val blocked = destination.variant == VarianteInterfaz.ApprovalBlocked
    val completed = request.status == "Adopción realizada" || after && request.id != 8
    val state =
        when {
            after && request.id == 8 -> "No aprobada"
            after -> "Aprobada"
            destination.variant == VarianteInterfaz.Approved -> "Aprobada"
            destination.variant == VarianteInterfaz.NotApproved -> "No aprobada"
            destination.variant in listOf(VarianteInterfaz.InReview, VarianteInterfaz.ApprovalBlocked) ->
                "En revisión"
            completed -> "Aprobada"
            else -> request.status
        }
    var decision by remember { mutableStateOf<String?>(null) }
    HScaffold(
        nav,
        "Evaluar solicitud",
        "Folio ${request.folio}",
        bottom = {
            when {
                completed ->
                    HButton(
                        "Ver seguimientos",
                        { nav.open(PantallaInterfaz.FollowUps, dogId = dog.id) },
                        icon = SimboloHuellitas.Calendar,
                    )
                after || state == "No aprobada" ->
                    HButton(
                        "Volver a solicitudes",
                        { nav.root(PantallaInterfaz.ShelterRequests) },
                        outlined = true,
                    )
                state == "Aprobada" ->
                    HButton(
                        "Registrar adopción",
                        { nav.open(PantallaInterfaz.AdoptionForm) },
                        icon = SimboloHuellitas.Home,
                    )
                state == "Enviada" -> {
                    HButton(
                        "Pasar a En revisión",
                        { nav.open(PantallaInterfaz.EvaluateRequest, variant = VarianteInterfaz.InReview) },
                        icon = SimboloHuellitas.Eye,
                    )
                    HCaption("Para aprobar o no aprobar, primero pasa la solicitud a En revisión.")
                }
                else ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HButton(
                            "No aprobar",
                            { decision = "No aprobada" },
                            Modifier.weight(1f),
                            outlined = true,
                            danger = true,
                        )
                        HButton(
                            "Aprobar",
                            { decision = "Aprobada" },
                            Modifier.weight(1f),
                            enabled = !blocked,
                        )
                    }
            }
        },
    ) {
        HCard {
            HHeading(request.user)
            HBody("550000000${request.id}")
            HBody("adoptante${request.id}@correo.invalid", muted = true)
            HorizontalDivider()
            HKeyValue("Estado actual", state)
            if (completed) HTag("Adopción realizada")
        }
        HDogRow(
            dog,
            status =
                if (after || completed) "Adoptado"
                else if (state == "Aprobada" || blocked) "En proceso" else dog.status,
        )
        if (blocked)
            HBanner(
                "No se puede aprobar esta solicitud. Ya hay una solicitud aprobada para Perrito 2 (Usuario 3, folio HC-2026-0139). Solo puede aprobarse una por perrito.",
                SimboloHuellitas.Alert,
                true,
            )
        if (after && state == "No aprobada")
            HBanner(
                "El perrito fue adoptado por otra persona. Esta solicitud no admite más cambios.",
                SimboloHuellitas.Home,
                true,
            )
        HHeading("Respuestas")
        HCard {
            HKeyValue("Vivienda", "Casa")
            HKeyValue("La vivienda es", "Propia", true)
            HKeyValue("Patio o espacio exterior", "Sí", true)
            HKeyValue("Personas en el hogar", "3", true)
            HKeyValue("Horas solo", "De 4 a 8 h", true)
            HKeyValue("Otras mascotas", "No", true)
        }
        HCard {
            HHeading("Motivos para adoptar")
            HBody(
                if (request.id == 1) DatosVistaPrevia.reasons
                else
                    "Queremos darle un hogar seguro y acompañarlo con cuidado y cariño. Tenemos tiempo para atenderlo todos los días."
            )
        }
        HHeading("Historial")
        HTimeline(
            buildList {
                add(Triple("Enviada", request.date, "Solicitud recibida."))
                if (state != "Enviada")
                    add(
                        Triple(
                            "En revisión",
                            request.date,
                            "Revisión de las respuestas del adoptante.",
                        )
                    )
                if (state == "Aprobada")
                    add(
                        Triple(
                            "Aprobada",
                            if (request.id == 3) "11 nov 2026" else request.date,
                            "Entrevista y visita satisfactorias.",
                        )
                    )
                if (after || completed)
                    add(
                        Triple(
                            if (state == "No aprobada") "No aprobada" else "Adopción realizada",
                            if (request.id == 6) "4 oct 2026" else "16 nov 2026",
                            if (state == "No aprobada") "El perrito fue adoptado por otra persona."
                            else "Entrega realizada y carta de adopción firmada.",
                        )
                    )
            }
        )
        HBanner(
            "La decisión es del refugio después de revisar las respuestas y realizar la entrevista. La app y la ayuda de redacción no deciden adopciones."
        )
    }
    if (decision != null)
        ModalBottomSheet(onDismissRequest = { decision = null }) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Confirmar decisión", style = MaterialTheme.typography.titleLarge)
                HBody("Revisa los datos antes de registrar la decisión.", muted = true)
                HCard {
                    HKeyValue("Solicitante", request.user)
                    HKeyValue("Perrito", dog.name, true)
                    HKeyValue("Folio", request.folio, true)
                    HKeyValue("Decisión", decision.orEmpty(), true)
                }
                HBanner(
                    if (decision == "Aprobada")
                        "Al aprobar, ${dog.name} pasará a En proceso, dejará de recibir solicitudes nuevas y no podrá aprobarse otra solicitud para él."
                    else "La persona solicitante podrá consultar la decisión y el motivo."
                )
                HInput(
                    "Nota para la persona adoptante (opcional)",
                    placeholder = "Escribe una nota…",
                    lines = 2,
                )
                HButton(
                    "Confirmar decisión",
                    {
                        val approved = decision == "Aprobada"
                        decision = null
                        nav.open(
                            PantallaInterfaz.EvaluateRequest,
                            variant = if (approved) VarianteInterfaz.Approved else VarianteInterfaz.NotApproved,
                        )
                    },
                    icon = SimboloHuellitas.Check,
                )
                HTextAction("Cancelar", { decision = null })
            }
        }
}
