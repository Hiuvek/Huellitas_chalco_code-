package com.example.huellitaschalco.feature.shelter.dogs.presentation

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
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*

@Composable
fun DogHealthChoices(
    health: DogHealth,
    enabled: Boolean,
    prefix: String,
    onChange: (HealthField, HealthStatus) -> Unit,
) {
    HealthField.entries.forEach { field ->
        Column {
            Text(field.label, style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HealthStatus.entries.forEach { status ->
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
fun DogHealthSummary(health: DogHealth) {
    HHeading("Salud")
    HealthField.entries.forEach { field -> HBody("${field.label}: ${health.value(field).label}") }
}

@Composable
fun DogHealthEditor(uid: String, dog: RegisteredDog, repository: DogRepository) {
    val model: DogHealthViewModel =
        viewModel(
            key = "dog-health-$uid-${dog.shelterId}-${dog.id}",
            factory = DogHealthViewModel.factory(repository, uid, dog.shelterId, dog.id, dog.health),
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
    if (state.saved) HBanner("Salud guardada correctamente.", HSymbol.Check)
    state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
    if ((state.dirty || state.conflict) && !state.saving)
        HButton(
            if (state.conflict) "Cargar estado actual" else "Restablecer valores guardados",
            model::reload,
            outlined = true,
        )
}
