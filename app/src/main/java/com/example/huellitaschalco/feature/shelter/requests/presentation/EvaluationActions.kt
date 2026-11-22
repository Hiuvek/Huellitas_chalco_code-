package com.example.huellitaschalco.feature.shelter.requests.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.feature.requests.domain.AdoptionRequest
import com.example.huellitaschalco.feature.shelter.requests.domain.*

@Composable
fun EvaluationActions(
    item: AdoptionRequest,
    uid: String,
    shelterId: String,
    repository: EvaluationRepository,
) {
    val model: EvaluationViewModel =
        viewModel(
            key = "evaluate-$uid-$shelterId-${item.id}",
            factory = EvaluationViewModel.factory(uid, shelterId, item.id, repository),
        )
    val state by model.state.collectAsStateWithLifecycle()
    state.savedStatus?.let {
        Box(Modifier.testTag("evaluation-saved")) { HCaption("Estado guardado: $it") }
    }
    val ready = !state.sending && (state.savedStatus == null || state.savedStatus == item.status)
    if (item.status == "Enviada")
        HButton(
            "Pasar a En revisión",
            { model.select(item, EvaluationDecision.Review) },
            Modifier.testTag("evaluation-review"),
            enabled = ready,
        )
    if (item.status == "En revisión")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HButton(
                "No aprobar",
                { model.select(item, EvaluationDecision.Reject) },
                Modifier.weight(1f).testTag("evaluation-reject"),
                outlined = true,
                enabled = ready,
            )
            HButton(
                "Aprobar",
                { model.select(item, EvaluationDecision.Approve) },
                Modifier.weight(1f).testTag("evaluation-approve"),
                enabled = ready,
            )
        }
    state.attempt?.let { attempt ->
        AlertDialog(
            onDismissRequest = model::cancel,
            title = { HHeading("Confirmar cambio de estado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HBody(item.folio)
                    HKeyValue("Solicitante", item.answers.name)
                    HKeyValue("Decisión", attempt.decision.status)
                    if (attempt.decision == EvaluationDecision.Approve)
                        HBody(
                            "El perrito pasará a En proceso y no podrá recibir solicitudes nuevas ni otra aprobación."
                        )
                    if (attempt.decision == EvaluationDecision.Reject)
                        HInput(
                            "Motivo para la persona solicitante",
                            attempt.reason,
                            model::reason,
                            Modifier.testTag("evaluation-reason"),
                            lines = 3,
                            enabled = !state.sending,
                        )
                    state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                }
            },
            confirmButton = {
                HButton(
                    if (state.sending) "Guardando…" else "Confirmar decisión",
                    model::confirm,
                    Modifier.testTag("evaluation-confirm"),
                    enabled = !state.sending,
                )
            },
            dismissButton = {
                HButton("Cancelar", model::cancel, outlined = true, enabled = !state.sending)
            },
        )
    }
}
