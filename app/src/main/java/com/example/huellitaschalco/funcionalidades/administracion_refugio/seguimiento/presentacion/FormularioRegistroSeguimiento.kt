package com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.presentacion

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.ImportadorFotoPerrito
import com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.dominio.*
import com.example.huellitaschalco.nucleo.fechas.civilDateLabel
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FormularioRegistroSeguimiento(
    nav: NavegadorInterfaz,
    uid: String,
    shelterId: String,
    review: RevisionProgramada,
    adopted: PerritoAdoptado,
    repository: RepositorioSeguimiento,
    importer: ImportadorFotoPerrito,
) {
    val model: ModeloVistaRegistroSeguimiento =
        viewModel(
            key = "record-followup-$uid-$shelterId-${review.id}",
            factory =
                ModeloVistaRegistroSeguimiento.factory(
                    uid,
                    shelterId,
                    review,
                    adopted.adoption.deliveryAt,
                    repository,
                    importer,
                ),
        )
    val state by model.state.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { model.addPhoto(it.toString()) }
        }
    if (state.receipt != null) {
        HSuccessHeader(
            "Seguimiento registrado",
            "El servidor confirmó el resultado de esta revisión.",
        )
        HButton(
            "Ver historial",
            { nav.root(PantallaInterfaz.FollowUps) },
            Modifier.testTag("followup-done"),
        )
    } else {
        HBody("Todavía no hay un resultado registrado para esta revisión.")
        HInput(
            "Fecha de realización",
            state.date,
            { model.edit { s -> s.copy(date = it.take(10)) } },
            Modifier.testTag("followup-date"),
            helper = "AAAA-MM-DD; fecha real, no futura.",
            enabled = !state.locked,
        )
        HHeading("Adaptación al hogar")
        Adaptaciones.labels.forEach { (key, label) ->
            FilterChip(
                selected = state.adaptation == key,
                onClick = { model.edit { it.copy(adaptation = key) } },
                enabled = !state.locked,
                label = { Text(label) },
                modifier = Modifier.testTag("adaptation-$key"),
            )
        }
        HInput(
            "Observaciones",
            state.notes,
            { model.edit { s -> s.copy(notes = it.take(2000)) } },
            Modifier.testTag("followup-notes"),
            helper = "De 15 a 2000 caracteres.",
            enabled = !state.locked,
            lines = 4,
        )
        state.photo?.let { photo ->
            val bitmap = remember(photo.filePath) { BitmapFactory.decodeFile(photo.filePath) }
            bitmap?.let {
                Image(
                    it.asImageBitmap(),
                    "Foto elegida para el seguimiento",
                    Modifier.fillMaxWidth().height(180.dp),
                )
            }
            HButton(
                "Quitar fotografía",
                model::removePhoto,
                outlined = true,
                enabled = !state.locked,
            )
        }
        HButton(
            if (state.importing) "Preparando fotografía…" else "Elegir fotografía (opcional)",
            {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            Modifier.testTag("followup-photo"),
            outlined = true,
            enabled = !state.locked && !state.importing,
        )
        HHeading("Después de guardar")
        listOf("PROGRAMAR" to "Programar siguiente revisión", "CERRAR" to "Cerrar este seguimiento")
            .forEach { (key, label) ->
                FilterChip(
                    selected = state.disposition == key,
                    onClick = { model.edit { it.copy(disposition = key) } },
                    enabled = !state.locked,
                    label = { Text(label) },
                    modifier = Modifier.testTag("followup-$key"),
                )
            }
        if (state.disposition == "CERRAR")
            HCaption(
                "Se guardará el resultado sin crear otra cita. Las demás revisiones programadas se conservan."
            )
        if (state.disposition == "PROGRAMAR") {
            HInput(
                "Próxima fecha",
                state.nextDate,
                { model.edit { s -> s.copy(nextDate = it.take(10)) } },
                Modifier.testTag("followup-next-date"),
                helper = "AAAA-MM-DD, posterior a la realización.",
                enabled = !state.locked,
            )
            TiposRevision.labels.forEach { (key, label) ->
                FilterChip(
                    selected = state.nextType == key,
                    onClick = { model.edit { it.copy(nextType = key) } },
                    enabled = !state.locked,
                    label = { Text(label) },
                    modifier = Modifier.testTag("followup-next-$key"),
                )
            }
        }
        state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
        HButton(
            if (state.sending) "Guardando…" else "Revisar y guardar",
            model::prepare,
            Modifier.testTag("followup-prepare"),
            enabled = !state.sending && !state.importing,
        )
    }
    if (state.confirming)
        AlertDialog(
            onDismissRequest = model::cancel,
            title = { HHeading("Confirmar seguimiento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HBody(adopted.dogName)
                    HKeyValue("Realizado", state.date)
                    HKeyValue(
                        "Adaptación",
                        Adaptaciones.labels[state.adaptation] ?: state.adaptation,
                    )
                    HBody(state.notes)
                    HKeyValue("Fotografía", if (state.photo != null) "Sí" else "Sin fotografía")
                    HBody(
                        if (state.disposition == "PROGRAMAR")
                            "Próxima revisión: ${state.nextDate} · ${TiposRevision.labels[state.nextType]}"
                        else "Sin nueva revisión desde este resultado."
                    )
                    state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
                }
            },
            confirmButton = {
                HButton(
                    if (state.sending) "Guardando…" else "Confirmar guardado",
                    model::submit,
                    Modifier.testTag("followup-confirm"),
                    enabled = !state.sending,
                )
            },
            dismissButton = {
                HButton("Volver", model::cancel, outlined = true, enabled = !state.sending)
            },
        )
}

@Composable
internal fun FollowUpResultDetail(
    result: ResultadoSeguimiento,
    board: AgendaSeguimiento,
    repository: RepositorioSeguimiento,
) {
    HHeading("Seguimiento registrado")
    HKeyValue("Fecha de realización", civilDateLabel(result.doneAt))
    HKeyValue("Adaptación", Adaptaciones.labels[result.adaptation] ?: result.adaptation)
    HBody(result.notes)
    HCaption("Folio: ${result.id}")
    val next = board.reviews.firstOrNull { it.id == nextReviewId(result.reviewId) }
    if (next != null)
        HKeyValue(
            "Próxima revisión",
            "${civilDateLabel(next.plannedAt)} · ${TiposRevision.labels[next.type]}",
        )
    else HBody("Sin próxima revisión programada desde este resultado.")
    result.photo?.let { path -> PrivateFollowUpPhoto(path, repository) }
}

@Composable
private fun PrivateFollowUpPhoto(path: String, repository: RepositorioSeguimiento) {
    var retry by remember(path) { mutableIntStateOf(0) }
    val bitmap by
        produceState<android.graphics.Bitmap?>(null, path, retry) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val bytes = repository.photo(path)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        .getOrNull()
                }
        }
    if (bitmap != null)
        Image(
            bitmap!!.asImageBitmap(),
            "Fotografía privada del seguimiento",
            Modifier.fillMaxWidth().height(220.dp),
        )
    else {
        HCaption("Fotografía pendiente de cargar.")
        HButton("Reintentar fotografía", { retry++ }, outlined = true)
    }
}
