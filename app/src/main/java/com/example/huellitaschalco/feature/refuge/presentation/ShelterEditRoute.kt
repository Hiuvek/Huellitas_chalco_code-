package com.example.huellitaschalco.feature.refuge.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.refuge.domain.*

@Composable
fun ShelterEditRoute(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    repository: PublicShelterRepository,
) {
    val model: ShelterEditViewModel =
        viewModel(
            key = "edit-shelter-$uid-$shelterId-${nav.current.recordId}",
            factory = ShelterEditViewModel.factory(uid, repository),
        )
    val form by model.state.collectAsStateWithLifecycle()
    var retry by remember { mutableIntStateOf(0) }
    val flow = remember(shelterId, repository, retry) { repository.observe(shelterId) }
    val public by flow.collectAsStateWithLifecycle(PublicShelterState())
    LaunchedEffect(public.shelter, public.stale) {
        if (!public.stale) public.shelter?.let(model::load)
    }
    var discard by rememberSaveable { mutableStateOf(false) }
    val close = {
        if (!form.saving) {
            if (form.dirty && !form.saved) discard = true else nav.back()
        }
    }
    BackHandler(onBack = close)
    HScaffold(
        nav,
        "Editar refugio",
        back = false,
        shelter = true,
        bottom = {
            if (form.original != null && !form.saved) {
                form.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                if (form.conflict) {
                    HCaption("Recargar reemplazará los cambios sin guardar de este formulario.")
                    HButton(
                        "Recargar información",
                        { public.shelter?.takeUnless { public.stale }?.let(model::reload) },
                        enabled = public.shelter != null && !public.stale,
                        outlined = true,
                    )
                }
                HButton(
                    if (form.saving) "Guardando…" else "Guardar cambios",
                    model::save,
                    Modifier.testTag("shelter-edit-save"),
                    enabled = !form.saving && !form.conflict,
                )
                TextButton(
                    onClick = close,
                    enabled = !form.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancelar")
                }
            }
        },
    ) {
        when {
            form.saved -> {
                HSuccessHeader(
                    "Información actualizada",
                    "Los adoptantes verán los cambios al actualizar la información del refugio.",
                )
                HButton(
                    "Volver al perfil",
                    { nav.root(UiScreen.ShelterProfile) },
                    Modifier.testTag("shelter-edit-done"),
                )
            }
            form.original == null -> {
                if (public.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                else {
                    HBanner(
                        "Necesitas información actual del servidor para editar el refugio.",
                        HSymbol.Alert,
                        error = true,
                    )
                    HButton("Reintentar", { retry++ }, outlined = true)
                }
                HTextAction("Volver al perfil", { nav.back() })
            }
            else -> {
                HScreenIntro(
                    "Información pública",
                    "Estos datos aparecen para los adoptantes. Revisa la información antes de guardar.",
                )
                val draft = form.draft
                HInput(
                    "Nombre del refugio",
                    draft.name,
                    { v -> model.edit { it.copy(name = v) } },
                    Modifier.testTag("shelter-edit-name"),
                    enabled = !form.saving,
                )
                HInput(
                    "Zona pública",
                    draft.zone,
                    { v -> model.edit { it.copy(zone = v) } },
                    Modifier.testTag("shelter-edit-zone"),
                    helper = "Chalco, Estado de México. Publica una zona aproximada.",
                    enabled = !form.saving,
                )
                HInput(
                    "Horario de atención",
                    draft.hours,
                    { v -> model.edit { it.copy(hours = v) } },
                    Modifier.testTag("shelter-edit-hours"),
                    enabled = !form.saving,
                    lines = 2,
                )
                HHeading("Proceso de adopción")
                HInput(
                    "Requisitos",
                    draft.requirements,
                    { v -> model.edit { it.copy(requirements = v) } },
                    Modifier.testTag("shelter-edit-requirements"),
                    helper = "Uno por línea. Entre 1 y 10, de hasta 300 caracteres cada uno.",
                    lines = 4,
                    enabled = !form.saving,
                )
                HInput(
                    "Pasos de adopción",
                    draft.steps,
                    { v -> model.edit { it.copy(steps = v) } },
                    Modifier.testTag("shelter-edit-steps"),
                    helper = "Uno por línea, en el orden en que se realizan. Entre 1 y 10.",
                    lines = 4,
                    enabled = !form.saving,
                )
                HHeading("Ubicación aproximada (opcional)")
                HCaption(
                    "Completa ambas coordenadas o deja ambas vacías para usar la zona pública en Google Maps."
                )
                HInput(
                    "Latitud",
                    draft.latitude,
                    { v -> model.edit { it.copy(latitude = v) } },
                    Modifier.testTag("shelter-edit-latitude"),
                    enabled = !form.saving,
                )
                HInput(
                    "Longitud",
                    draft.longitude,
                    { v -> model.edit { it.copy(longitude = v) } },
                    Modifier.testTag("shelter-edit-longitude"),
                    enabled = !form.saving,
                )
            }
        }
    }
    if (discard)
        AlertDialog(
            onDismissRequest = { discard = false },
            title = { Text("¿Descartar los cambios?") },
            text = { Text("La información guardada del refugio se conservará.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        discard = false
                        nav.back()
                    }
                ) {
                    Text("Descartar cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { discard = false }) { Text("Seguir editando") }
            },
        )
}
