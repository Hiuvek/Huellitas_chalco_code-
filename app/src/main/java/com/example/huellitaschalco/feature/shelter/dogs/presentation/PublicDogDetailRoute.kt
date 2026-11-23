package com.example.huellitaschalco.feature.shelter.dogs.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.catalog.presentation.PublicDogDetailViewModel
import com.example.huellitaschalco.feature.refuge.domain.PublicShelterRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.*

@Composable
fun PublicDogDetailRoute(
    nav: UiNavigator,
    id: String,
    repository: DogRepository,
    shelters: PublicShelterRepository,
    refugeOnly: Boolean = false,
) {
    val model: PublicDogDetailViewModel =
        viewModel(
            key = "public-dog-detail-$id",
            factory = PublicDogDetailViewModel.factory(id, repository, shelters),
        )
    val state by model.state.collectAsStateWithLifecycle()
    var selectedPhoto by rememberSaveable(id) { mutableStateOf<String?>(null) }
    HScaffold(
        nav,
        if (refugeOnly) "Información del refugio" else "Conoce a tu compañero",
        bottom =
            if (!refugeOnly && !state.loading && state.error == null && state.dog != null)
                ({
                    HCaption(
                        if (state.dog?.status == "Disponible")
                            "El siguiente paso es revisar las condiciones del refugio."
                        else "Consulta los requisitos y la disponibilidad del perrito."
                    )
                    HButton(
                        "Ver requisitos para adoptar",
                        { nav.open(UiScreen.Requirements, recordId = id) },
                        Modifier.testTag("detail-requirements"),
                    )
                })
            else null,
    ) {
        if (state.stale) {
            HBanner(
                "Última copia guardada. La información y la disponibilidad pueden haber cambiado.",
                HSymbol.Alert,
            )
            HButton("Actualizar ficha", model::retry, outlined = true)
        }
        when {
            state.loading ->
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("detail-loading"))
            state.error != null -> {
                HBanner(state.error!!, HSymbol.Alert, error = true)
                HButton(
                    "Reintentar",
                    model::retry,
                    Modifier.testTag("detail-retry"),
                    outlined = true,
                )
            }
            else -> {
                val dog = state.dog!!
                val shelter = state.shelter!!
                if (!refugeOnly) {
                    val index =
                        dog.photos.indexOfFirst { it.id == selectedPhoto }.takeIf { it >= 0 } ?: 0
                    val photo = dog.photos.getOrNull(index)
                    Column(Modifier.testTag("detail-photo-${photo?.id.orEmpty()}")) {
                        StoredDogPhoto(photo?.path, repository, showFullPhoto = true)
                    }
                    if (dog.photos.isNotEmpty())
                        HCaption("Fotografía ${index + 1} de ${dog.photos.size}")
                    if (dog.photos.size > 1)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            HButton(
                                "Anterior",
                                { selectedPhoto = dog.photos[index - 1].id },
                                Modifier.weight(1f).testTag("detail-previous-photo"),
                                outlined = true,
                                enabled = index > 0,
                            )
                            HButton(
                                "Siguiente",
                                { selectedPhoto = dog.photos[index + 1].id },
                                Modifier.weight(1f).testTag("detail-next-photo"),
                                outlined = true,
                                enabled = index < dog.photos.lastIndex,
                            )
                        }
                    HHeading(dog.name)
                    HTag(dog.status)
                    Row {
                        HMetric("Edad", "${dog.ageMonths} meses", Modifier.weight(1f))
                        HMetric("Tamaño", dog.size, Modifier.weight(1f))
                        HMetric("Sexo", dog.sex, Modifier.weight(1f))
                    }
                    HHeading("Sobre ${dog.name}")
                    HBody(dog.description)
                    DogHealthSummary(dog.health)
                    HCaption("Información de salud declarada por el refugio.")
                    HHeading("Dónde se encuentra")
                    HCard(onClick = { nav.open(UiScreen.RefugeInformation, recordId = id) }) {
                        HHeading(shelter.name)
                        HBody(shelter.zone, muted = true)
                        HBody(shelter.hours, muted = true)
                    }
                    HButton(
                        "Ver refugio y ubicación",
                        { nav.open(UiScreen.RefugeInformation, recordId = id) },
                        Modifier.testTag("detail-refuge"),
                        outlined = true,
                    )
                } else {
                    HHeading(shelter.name)
                    HCard {
                        HKeyValue("Municipio", "${shelter.municipality}, ${shelter.state}")
                        HKeyValue("Zona", shelter.zone, true)
                        HKeyValue("Horario", shelter.hours, true)
                    }
                    com.example.huellitaschalco.feature.refuge.maps.PublicMapPanel(shelter)
                    HButton(
                        "Ver requisitos de adopción",
                        { nav.open(UiScreen.Requirements, recordId = id) },
                        Modifier.testTag("refuge-requirements"),
                        outlined = true,
                    )
                    HBanner(
                        "La dirección exacta se comparte cuando el refugio agenda tu visita.",
                        HSymbol.Shield,
                    )
                }
            }
        }
    }
}
