package com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.presentacion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.*
import com.example.huellitaschalco.funcionalidades.solicitudes.presentacion.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun RutaSolicitudesRecibidas(
    nav: NavegadorInterfaz,
    uid: String,
    shelterId: String,
    id: String?,
    repository: RepositorioSolicitudesRecibidas,
    evaluationRepository:
        com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.dominio.RepositorioEvaluacion,
) {
    val model: ModeloVistaSolicitudesRecibidas =
        viewModel(
            key = "received-$uid-$shelterId-$id",
            factory = ModeloVistaSolicitudesRecibidas.factory(uid, shelterId, id, repository),
        )
    if (id == null) {
        val state by model.list.collectAsStateWithLifecycle()
        val filter by model.filter.collectAsStateWithLifecycle()
        HScaffold(nav, "Solicitudes recibidas", back = false, shelter = true, tab = "Solicitudes") {
            HScreenIntro(
                "Revisa a las personas interesadas",
                "Selecciona una solicitud para leer sus respuestas y registrar tu decisión.",
            )
            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error != null -> ReceivedError(state.error!!, model::retry)
                state.requests.isEmpty() -> {
                    HEmpty(
                        "Aún no hay solicitudes recibidas",
                        "Cuando alguien solicite un perrito de este refugio, aparecerá aquí.",
                        action = "Actualizar",
                        onAction = model::retry,
                    )
                    HButton("Actualizar", model::retry, outlined = true)
                }
                else -> {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Todas", "Enviada", "En revisión", "Aprobada", "No aprobada")
                            .forEach { status ->
                                FilterChip(
                                    selected = filter == status,
                                    onClick = { model.filter.value = status },
                                    label = {
                                        Text(
                                            "$status (${state.requests.count { status == "Todas" || it.status == status }})"
                                        )
                                    },
                                )
                            }
                    }
                    val visible = state.requests.filter { filter == "Todas" || it.status == filter }
                    if (visible.isEmpty()) HBody("No hay solicitudes con este estado.")
                    visible.forEach { item ->
                        HCard(
                            Modifier.testTag("received-${item.id}"),
                            onClick = { nav.open(PantallaInterfaz.EvaluateRequest, recordId = item.id) },
                        ) {
                            HCaption("Para ${item.dogName ?: item.dogId}")
                            HHeading(item.answers.name)
                            HCaption(item.folio)
                            HTag(item.status)
                            HCaption(requestDate(item.createdAt))
                            HCardAction("Revisar solicitud")
                        }
                    }
                }
            }
        }
    } else {
        val state by model.detail.collectAsStateWithLifecycle()
        HScaffold(nav, "Solicitud recibida", state.request?.folio, shelter = true) {
            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error != null -> ReceivedError(state.error!!, model::retry)
                else ->
                    state.request?.let { item ->
                        HHeading(item.dogName ?: item.dogId)
                        HTag(item.status)
                        AccionesEvaluacion(item, uid, shelterId, evaluationRepository)
                        if (item.status == "Aprobada")
                            HButton(
                                "Registrar entrega",
                                { nav.open(PantallaInterfaz.AdoptionForm, recordId = item.id) },
                                Modifier.testTag("adoption-open"),
                            )
                        HKeyValue("Enviada", requestDate(item.createdAt))
                        item.rejectionReason?.let { HKeyValue("Motivo de la decisión", it) }
                        RequestAnswers(item)
                        HHeading("Historial")
                        state.history.forEach { event ->
                            HCard {
                                HTag(event.status)
                                HCaption(requestDate(event.timestamp))
                                event.reason?.let { HBody(it) }
                            }
                        }
                        HButton(
                            "Volver a solicitudes recibidas",
                            { nav.root(PantallaInterfaz.ShelterRequests) },
                            outlined = true,
                        )
                    }
            }
        }
    }
}

@Composable
private fun ReceivedError(message: String, retry: () -> Unit) {
    HBanner(message, SimboloHuellitas.Alert, error = true)
    HButton("Reintentar", retry, outlined = true)
}
