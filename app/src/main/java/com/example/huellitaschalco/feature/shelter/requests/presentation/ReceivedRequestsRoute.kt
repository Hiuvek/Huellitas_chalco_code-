package com.example.huellitaschalco.feature.shelter.requests.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.requests.domain.*
import com.example.huellitaschalco.feature.requests.presentation.*

@Composable
fun ReceivedRequestsRoute(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    shelterName: String,
    id: String?,
    repository: ReceivedRequestsRepository,
) {
    val model: ReceivedRequestsViewModel =
        viewModel(
            key = "received-$uid-$shelterId-$id",
            factory = ReceivedRequestsViewModel.factory(uid, shelterId, id, repository),
        )
    if (id == null) {
        val state by model.list.collectAsStateWithLifecycle()
        val filter by model.filter.collectAsStateWithLifecycle()
        HScaffold(
            nav,
            "Solicitudes recibidas",
            shelterName,
            back = false,
            shelter = true,
            tab = "Solicitudes",
        ) {
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
                            onClick = { nav.open(UiScreen.EvaluateRequest, recordId = item.id) },
                        ) {
                            HCaption("Para ${item.dogName ?: item.dogId}")
                            HHeading(item.answers.name)
                            HCaption(item.folio)
                            HTag(item.status)
                            HCaption(requestDate(item.createdAt))
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
                            { nav.root(UiScreen.ShelterRequests) },
                            outlined = true,
                        )
                    }
            }
        }
    }
}

@Composable
private fun ReceivedError(message: String, retry: () -> Unit) {
    HBanner(message, HSymbol.Alert, error = true)
    HButton("Reintentar", retry, outlined = true)
}
