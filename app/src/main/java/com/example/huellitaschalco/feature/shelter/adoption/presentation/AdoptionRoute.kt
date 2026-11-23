package com.example.huellitaschalco.feature.shelter.adoption.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.requests.domain.ReceivedRequestsRepository
import com.example.huellitaschalco.feature.requests.presentation.requestDate
import com.example.huellitaschalco.feature.shelter.adoption.domain.AdoptionRepository
import com.example.huellitaschalco.feature.shelter.requests.presentation.ReceivedRequestsViewModel

@Composable
fun AdoptionRoute(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    id: String?,
    requests: ReceivedRequestsRepository,
    repository: AdoptionRepository,
) {
    if (id == null) {
        HScaffold(nav, "Registrar adopción", shelter = true) {
            HBody("Selecciona una solicitud aprobada para registrar su entrega.")
            HButton("Ver solicitudes", { nav.root(UiScreen.ShelterRequests) })
        }
        return
    }
    val requestModel: ReceivedRequestsViewModel =
        viewModel(
            key = "received-$uid-$shelterId-$id",
            factory = ReceivedRequestsViewModel.factory(uid, shelterId, id, requests),
        )
    val selected by requestModel.detail.collectAsStateWithLifecycle()
    val item = selected.request
    if (item == null) {
        HScaffold(nav, "Registrar adopción", shelter = true) {
            if (selected.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            else {
                HBanner(
                    selected.error ?: "La solicitud no está disponible.",
                    HSymbol.Alert,
                    error = true,
                )
                HButton("Reintentar", requestModel::retry, outlined = true)
            }
        }
        return
    }
    val model: AdoptionViewModel =
        viewModel(
            key = "adoption-$uid-$shelterId-$id",
            factory = AdoptionViewModel.factory(uid, shelterId, id, item.dogId, repository),
        )
    val existing by model.existing.collectAsStateWithLifecycle()
    val form by model.form.collectAsStateWithLifecycle()
    val saved = existing.adoption
    val completed = saved != null || form.receipt != null
    HScaffold(
        nav,
        "Registrar adopción",
        item.folio,
        shelter = true,
        bottom = {
            if (
                !completed &&
                    !existing.loading &&
                    existing.error == null &&
                    item.status == "Aprobada"
            )
                HButton(
                    if (form.sending) "Registrando…"
                    else if (form.locked) "Reintentar confirmación" else "Confirmar adopción",
                    model::prepare,
                    Modifier.testTag("adoption-prepare"),
                    enabled = !form.sending,
                )
        },
    ) {
        HHeading(item.dogName ?: item.dogId)
        when {
            existing.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            existing.error != null -> {
                HBanner(existing.error!!, HSymbol.Alert, error = true)
                HButton("Reintentar consulta", model::retry, outlined = true)
            }
            completed -> {
                HSuccessHeader(
                    "Adopción registrada",
                    "La entrega está guardada; este perrito ya está Adoptado.",
                    HSymbol.Home,
                )
                HKeyValue("Solicitud de la entrega", saved?.requestId ?: form.receipt!!.requestId)
                HKeyValue(
                    "Fecha de entrega",
                    saved?.let { requestDate(it.deliveryAt) } ?: form.receipt!!.date,
                )
                if (form.pendingClosed)
                    HBody(
                        "Cierre completado. No quedan otras solicitudes pendientes para este perrito."
                    )
                else
                    HBody(
                        "La entrega está confirmada. Puedes completar el cierre de las solicitudes pendientes si hubo una interrupción."
                    )
                form.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                if (!form.pendingClosed)
                    HButton(
                        if (form.sending) "Completando cierre…"
                        else "Completar cierre de solicitudes",
                        model::completePending,
                        Modifier.testTag("adoption-close-pending"),
                        enabled = !form.sending,
                        outlined = true,
                    )
                HButton(
                    "Programar revisión",
                    { nav.open(UiScreen.ScheduleReview, recordId = item.dogId) },
                    Modifier.testTag("adoption-schedule"),
                )
                HButton(
                    "Ver solicitudes actualizadas",
                    { nav.root(UiScreen.ShelterRequests) },
                    outlined = true,
                )
                HButton("Ver mis perritos", { nav.root(UiScreen.ShelterDogs) }, outlined = true)
            }
            item.status != "Aprobada" ->
                HBanner(
                    "Se necesita una solicitud Aprobada para registrar la entrega.",
                    HSymbol.Alert,
                    error = true,
                )
            else -> {
                HKeyValue("Adoptante", item.answers.name)
                HBanner(
                    "Registra la adopción solo cuando la entrega ya ocurrió y la carta esté firmada."
                )
                HInput(
                    "Fecha real de entrega",
                    form.date,
                    model::date,
                    Modifier.testTag("adoption-date"),
                    helper =
                        "Hoy por defecto. Puedes cambiarla si la entrega fue antes. Formato AAAA-MM-DD.",
                    enabled = !form.locked,
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        form.signed,
                        model::signed,
                        Modifier.testTag("adoption-signed"),
                        enabled = !form.locked,
                    )
                    HBody("Confirmo que se realizó la entrega y se firmó la carta de adopción.")
                }
                HBody(
                    "Al confirmar, el perrito dejará el catálogo y sus otras solicitudes pendientes pasarán a No aprobada."
                )
                form.error?.let { HBanner(it, HSymbol.Alert, error = true) }
            }
        }
    }
    if (form.confirming && !completed)
        AlertDialog(
            onDismissRequest = model::cancel,
            title = { HHeading("Confirmar entrega") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HBody(item.folio)
                    HKeyValue("Adoptante", item.answers.name)
                    HKeyValue("Fecha", form.date)
                    HBody(
                        "Se registrará una sola adopción y se cerrarán las demás solicitudes pendientes de este perrito."
                    )
                    form.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                }
            },
            confirmButton = {
                HButton(
                    if (form.sending) "Registrando…" else "Registrar entrega",
                    model::submit,
                    Modifier.testTag("adoption-confirm"),
                    enabled = !form.sending,
                )
            },
            dismissButton = {
                HButton("Cancelar", model::cancel, outlined = true, enabled = !form.sending)
            },
        )
}
