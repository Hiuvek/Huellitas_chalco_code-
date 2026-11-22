package com.example.huellitaschalco.feature.refuge.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.catalog.presentation.PublicDogDetailViewModel
import com.example.huellitaschalco.feature.refuge.domain.PublicShelterRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.DogRepository

@Composable
fun RequirementsRoute(
    nav: UiNavigator,
    id: String,
    dogs: DogRepository,
    shelters: PublicShelterRepository,
) {
    val model: PublicDogDetailViewModel =
        viewModel(
            key = "public-dog-detail-$id",
            factory = PublicDogDetailViewModel.factory(id, dogs, shelters),
        )
    val state by model.state.collectAsStateWithLifecycle()
    HScaffold(
        nav,
        "Requisitos de adopción",
        bottom =
            if (!state.loading && state.error == null && state.shelter?.policy != null)
                ({
                    HCaption(
                        if (state.dog?.status == "Disponible")
                            "Completa tus datos y revisa tus respuestas antes de enviar."
                        else
                            "Este perrito está en proceso de adopción y no recibe nuevas solicitudes."
                    )
                    HButton(
                        "Iniciar solicitud",
                        { nav.open(UiScreen.ApplicationContact, recordId = id) },
                        Modifier.testTag("requirements-start"),
                        enabled = state.dog?.status == "Disponible",
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
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HBanner(state.error!!, HSymbol.Alert, error = true)
                HButton("Reintentar", model::retry, outlined = true)
            }
            else -> {
                val dog = state.dog!!
                val shelter = state.shelter!!
                HScreenIntro(
                    "Antes de solicitar a ${dog.name}",
                    "Lee las condiciones y los pasos de ${shelter.name}. Después pulsa Iniciar solicitud.",
                )
                if (shelter.demonstration || dog.demonstration)
                    HBanner(
                        "Requisitos de demostración escolar. No corresponden a una adopción real.",
                        HSymbol.Info,
                    )
                val policy = shelter.policy
                if (policy == null) {
                    HBanner(
                        "El refugio aún no tiene requisitos y pasos completos publicados. Consulta su información antes de preparar una solicitud.",
                        HSymbol.Alert,
                        error = true,
                    )
                    HButton(
                        "Información del refugio",
                        { nav.open(UiScreen.RefugeInformation, recordId = id) },
                        outlined = true,
                    )
                } else
                    Column(
                        Modifier.testTag("requirements-ready"),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        HHeading("Condiciones del refugio")
                        policy.requirements.forEach { HCard { HBody(it) } }
                        HHeading("Pasos para adoptar")
                        policy.steps.forEachIndexed { index, step ->
                            HCard { HHeading("${index + 1}. $step") }
                        }
                        if (dog.status == "En proceso")
                            HBanner(
                                "Este perrito está en proceso de adopción. No admite nuevas solicitudes.",
                                HSymbol.Info,
                            )
                    }
            }
        }
    }
}
