package com.example.huellitaschalco.feature.shelter.followup.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.date.civilDateLabel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.followup.domain.*

@Composable
fun FollowUpRoutes(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    repository: FollowUpRepository,
    importer: com.example.huellitaschalco.feature.shelter.dogs.domain.DogPhotoImporter,
) {
    val board: FollowUpBoardViewModel =
        viewModel(
            key = "followups-$uid-$shelterId",
            factory = FollowUpBoardViewModel.factory(uid, shelterId, repository),
        )
    val state by board.state.collectAsStateWithLifecycle()
    val screen = nav.current.screen
    val selectedId = nav.current.recordId
    val title =
        when (screen) {
            UiScreen.ScheduleReview -> "Programar revisión"
            UiScreen.FollowUpForm -> "Revisión programada"
            else -> "Seguimientos"
        }
    HScaffold(
        nav,
        title,
        back = screen != UiScreen.FollowUps,
        shelter = true,
        tab = if (screen == UiScreen.FollowUps) "Seguimientos" else null,
    ) {
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HBanner(state.error!!, HSymbol.Alert, error = true)
                HButton("Reintentar", board::retry, outlined = true)
            }
            screen == UiScreen.ScheduleReview -> {
                val adopted = state.adoptions.firstOrNull { it.adoption.id == selectedId }
                if (adopted == null)
                    HBody("Selecciona una adopción registrada para programar su revisión.")
                else ScheduleReviewForm(nav, uid, shelterId, adopted, repository)
            }
            screen in
                listOf(UiScreen.FollowUpForm, UiScreen.FollowUpDetail, UiScreen.FollowUpSaved) -> {
                val revision = state.reviews.firstOrNull { it.id == selectedId }
                val adopted = state.adoptions.firstOrNull { it.adoption.id == revision?.adoptionId }
                if (revision == null) HBody("Esta revisión no está disponible para este refugio.")
                else {
                    HHeading(adopted?.dogName ?: revision.dogId)
                    HKeyValue("Fecha programada", civilDateLabel(revision.plannedAt))
                    HKeyValue("Tipo", ReviewTypes.labels[revision.type] ?: revision.type)
                    HTag(if (revision.status == "PROGRAMADA") "Programada" else "Realizada")
                    val result = state.results.firstOrNull { it.reviewId == revision.id }
                    if (result != null) {
                        FollowUpResultDetail(result, state, repository)
                        HButton(
                            "Volver a seguimientos",
                            { nav.root(UiScreen.FollowUps) },
                            Modifier.testTag("followup-done"),
                        )
                    } else if (revision.status == "PROGRAMADA" && adopted != null) {
                        RecordFollowUpForm(
                            nav,
                            uid,
                            shelterId,
                            revision,
                            adopted,
                            repository,
                            importer,
                        )
                    } else
                        HBody(
                            "El resultado de esta revisión no está disponible. Reintenta la consulta."
                        )
                }
            }
            else -> {
                var tab by rememberSaveable(uid, shelterId) { mutableStateOf("Programados") }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Programados", "Adopciones", "Historial").forEach { name ->
                        FilterChip(
                            selected = tab == name,
                            onClick = { tab = name },
                            label = { Text(name) },
                            modifier = Modifier.testTag("followups-$name"),
                        )
                    }
                }
                if (tab == "Adopciones") {
                    if (state.adoptions.isEmpty())
                        HBody("Todavía no hay adopciones registradas en este refugio.")
                    state.adoptions.forEach { adopted ->
                        HCard {
                            HHeading(adopted.dogName)
                            HBody("Con ${adopted.adopterName}")
                            HCaption("Entrega: ${civilDateLabel(adopted.adoption.deliveryAt)}")
                            HButton(
                                "Programar revisión",
                                {
                                    nav.open(
                                        UiScreen.ScheduleReview,
                                        recordId = adopted.adoption.id,
                                    )
                                },
                                Modifier.testTag("schedule-${adopted.adoption.id}"),
                                outlined = true,
                            )
                        }
                    }
                } else if (tab == "Historial") {
                    if (state.incompleteResults > 0)
                        HBanner(
                            "${state.incompleteResults} registros incompletos no se pueden presentar. Solicita su revisión al responsable.",
                            HSymbol.Alert,
                            error = true,
                        )
                    if (state.results.isEmpty()) HBody("No hay seguimientos registrados.")
                    state.results.forEach { result ->
                        HCard(Modifier.testTag("result-${result.id}")) {
                            HHeading(
                                state.adoptions
                                    .firstOrNull { it.adoption.id == result.adoptionId }
                                    ?.dogName ?: result.dogId
                            )
                            HCaption(civilDateLabel(result.doneAt))
                            HTag(Adaptations.labels[result.adaptation] ?: result.adaptation)
                            HBody(result.notes)
                            HButton(
                                "Ver seguimiento",
                                { nav.open(UiScreen.FollowUpDetail, recordId = result.reviewId) },
                                outlined = true,
                            )
                        }
                    }
                } else {
                    val pending = state.reviews.filter { it.status == "PROGRAMADA" }
                    if (pending.isEmpty()) HBody("No hay revisiones programadas.")
                    pending.forEach { revision ->
                        val adopted =
                            state.adoptions.firstOrNull { it.adoption.id == revision.adoptionId }
                        HCard(Modifier.testTag("review-${revision.id}")) {
                            HHeading(adopted?.dogName ?: revision.dogId)
                            HCaption(civilDateLabel(revision.plannedAt))
                            HTag(ReviewTypes.labels[revision.type] ?: revision.type)
                            HButton(
                                "Abrir revisión",
                                { nav.open(UiScreen.FollowUpForm, recordId = revision.id) },
                                outlined = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleReviewForm(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    adopted: AdoptedDog,
    repository: FollowUpRepository,
) {
    val model: ScheduleReviewViewModel =
        viewModel(
            key = "schedule-$uid-$shelterId-${adopted.adoption.id}",
            factory = ScheduleReviewViewModel.factory(uid, shelterId, adopted, repository),
        )
    val state by model.state.collectAsStateWithLifecycle()
    HHeading(adopted.dogName)
    HCaption("Entrega: ${civilDateLabel(adopted.adoption.deliveryAt)}")
    if (state.receipt != null) {
        HSuccessHeader(
            "Revisión programada",
            "La cita se guardó en la agenda del refugio.",
            HSymbol.Calendar,
        )
        HKeyValue("Fecha", state.receipt!!.date)
        HKeyValue("Tipo", ReviewTypes.labels[state.receipt!!.type] ?: state.receipt!!.type)
        HButton(
            "Ver revisiones programadas",
            {
                model.newSchedule()
                nav.root(UiScreen.FollowUps)
            },
            Modifier.testTag("schedule-done"),
        )
        HButton("Programar otra revisión", model::newSchedule, outlined = true)
    } else {
        HInput(
            "Fecha de revisión",
            state.date,
            model::date,
            Modifier.testTag("schedule-date"),
            helper = "AAAA-MM-DD, no anterior a la entrega.",
            enabled = !state.locked,
        )
        ReviewTypes.labels.forEach { (key, label) ->
            FilterChip(
                selected = state.type == key,
                onClick = { model.type(key) },
                enabled = !state.locked,
                label = { Text(label) },
                modifier = Modifier.testTag("schedule-$key"),
            )
        }
        state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
        HButton(
            if (state.sending) "Programando…" else "Programar revisión",
            model::prepare,
            Modifier.testTag("schedule-prepare"),
            enabled = !state.sending,
        )
    }
    if (state.confirming)
        AlertDialog(
            onDismissRequest = model::cancel,
            title = { HHeading("Confirmar revisión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HBody(adopted.dogName)
                    HKeyValue("Fecha", state.date)
                    HKeyValue("Tipo", ReviewTypes.labels[state.type] ?: state.type)
                    state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                }
            },
            confirmButton = {
                HButton(
                    if (state.sending) "Programando…" else "Guardar revisión",
                    model::submit,
                    Modifier.testTag("schedule-confirm"),
                    enabled = !state.sending,
                )
            },
            dismissButton = {
                HButton("Cancelar", model::cancel, outlined = true, enabled = !state.sending)
            },
        )
}
