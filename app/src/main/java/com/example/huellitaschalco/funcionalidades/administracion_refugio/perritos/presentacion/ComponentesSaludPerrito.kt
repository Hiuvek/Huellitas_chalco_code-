package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.presentacion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun DogHealthChoices(
    health: SaludPerrito,
    enabled: Boolean,
    prefix: String,
    onChange: (CampoSalud, EstadoSalud) -> Unit,
) {
    CampoSalud.entries.forEach { field ->
        Column {
            Text(field.label, style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EstadoSalud.entries.forEach { status ->
                    FilterChip(
                        selected = health.value(field) == status,
                        onClick = { onChange(field, status) },
                        label = { Text(status.label) },
                        enabled = enabled,
                        modifier = Modifier.testTag("$prefix-${field.name}-${status.name}"),
                    )
                }
            }
        }
    }
}

@Composable
fun DogHealthSummary(health: SaludPerrito) {
    HHeading("Salud")
    CampoSalud.entries.forEach { field -> HBody("${field.label}: ${health.value(field).label}") }
}

@Composable
fun DogHealthEditor(uid: String, dog: PerritoRegistrado, repository: RepositorioPerritos) {
    val model: ModeloVistaSaludPerrito =
        viewModel(
            key = "dog-health-$uid-${dog.shelterId}-${dog.id}",
            factory = ModeloVistaSaludPerrito.factory(repository, uid, dog.shelterId, dog.id, dog.health),
        )
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(dog.health) { model.receive(dog.health) }
    HHeading("Salud")
    HCaption(
        "Selecciona el estado conocido y guarda los cambios. Usa Sin información cuando no tengas ese dato."
    )
    DogHealthChoices(state.draft, !state.saving, "health", model::edit)
    HButton(
        if (state.saving) "Guardando salud…" else "Guardar salud",
        model::save,
        Modifier.testTag("health-save"),
        enabled = state.dirty && !state.saving && !state.conflict,
    )
    if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (state.saved) HBanner("Salud guardada correctamente.", SimboloHuellitas.Check)
    state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
    if ((state.dirty || state.conflict) && !state.saving)
        HButton(
            if (state.conflict) "Cargar estado actual" else "Restablecer valores guardados",
            model::reload,
            outlined = true,
        )
}
