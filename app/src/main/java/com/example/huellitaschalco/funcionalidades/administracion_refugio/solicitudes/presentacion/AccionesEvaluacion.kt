package com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.presentacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.dominio.*
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.SolicitudAdopcionRecibida
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun AccionesEvaluacion(
    item: SolicitudAdopcionRecibida,
    uid: String,
    shelterId: String,
    repository: RepositorioEvaluacion,
) {
    val model: ModeloVistaEvaluacion =
        viewModel(
            key = "evaluate-$uid-$shelterId-${item.id}",
            factory = ModeloVistaEvaluacion.factory(uid, shelterId, item.id, repository),
        )
    val state by model.state.collectAsStateWithLifecycle()
    state.savedStatus?.let {
        Box(Modifier.testTag("evaluation-saved")) { HCaption("Estado guardado: $it") }
    }
    val ready = !state.sending && (state.savedStatus == null || state.savedStatus == item.status)
    if (item.status == "Enviada")
        HButton(
            "Pasar a En revisión",
            { model.select(item, DecisionEvaluacion.Review) },
            Modifier.testTag("evaluation-review"),
            enabled = ready,
        )
    if (item.status == "En revisión")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HButton(
                "No aprobar",
                { model.select(item, DecisionEvaluacion.Reject) },
                Modifier.weight(1f).testTag("evaluation-reject"),
                outlined = true,
                enabled = ready,
            )
            HButton(
                "Aprobar",
                { model.select(item, DecisionEvaluacion.Approve) },
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
                    if (attempt.decision == DecisionEvaluacion.Approve)
                        HBody(
                            "El perrito pasará a En proceso y no podrá recibir solicitudes nuevas ni otra aprobación."
                        )
                    if (attempt.decision == DecisionEvaluacion.Reject)
                        HInput(
                            "Motivo para la persona solicitante",
                            attempt.reason,
                            model::reason,
                            Modifier.testTag("evaluation-reason"),
                            lines = 3,
                            enabled = !state.sending,
                        )
                    state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
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
