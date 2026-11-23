package com.example.huellitaschalco.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.*

@Composable
fun DogDetailScreen(nav: UiNavigator, destination: UiDestination) {
    val dog = PreviewData.dog(destination.dogId)
    val preview = destination.screen == UiScreen.DogPreview
    val editing = preview && destination.variant == UiVariant.Editing
    HScaffold(
        nav,
        if (preview) "Vista previa" else dog.name,
        if (preview) "Así lo verá la persona adoptante" else null,
        bottom = {
            HButton(
                if (editing) "Guardar cambios"
                else if (preview) "Publicar perrito" else "Conocer requisitos",
                {
                    nav.open(
                        if (editing) UiScreen.ShelterDogDetail
                        else if (preview) UiScreen.DogPublished else UiScreen.Requirements
                    )
                },
                icon = if (preview) HSymbol.Check else null,
            )
            if (preview) HButton("Volver a editar", nav::back, outlined = true, icon = HSymbol.Edit)
        },
    ) {
        if (preview)
            HBanner(
                if (editing) "Revisa los cambios antes de guardar."
                else "Este registro aún no aparece en el catálogo. Revísalo antes de publicar."
            )
        HPhoto(dog, Modifier.fillMaxWidth().height(290.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dog.name, style = MaterialTheme.typography.headlineMedium)
            HTag(if (preview && !editing) "Disponible" else dog.status)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HMetric("Edad", dog.age, Modifier.weight(1f))
            HMetric("Tamaño", dog.size, Modifier.weight(1f))
            HMetric("Sexo", dog.sex, Modifier.weight(1f))
        }
        HHeading("Sobre ${dog.name}")
        HBody(dog.description)
        HHeading("Salud")
        HHealth(dog)
        HHeading("Refugio")
        HCard(onClick = { nav.open(UiScreen.RefugeInformation) }) {
            HHeading(dog.refuge)
            HCaption("Refugio ficticio · demostración")
            HBody("Zona centro de Chalco", muted = true)
            HBody("Lunes a sábado · 10:00 a 17:00", muted = true)
        }
        HMap(destination.variant == UiVariant.MapError) { nav.open(UiScreen.RefugeInformation) }
        HCaption(
            if (editing) "Los cambios se mostrarán al confirmar."
            else if (preview) "Se publicará al confirmar." else "Publicado el 3 nov 2026"
        )
    }
}

@Composable
fun RequirementsScreen(nav: UiNavigator) {
    HScaffold(
        nav,
        "Requisitos de adopción",
        "${PreviewData.dog(nav.current.dogId).name} · ${PreviewData.dog(nav.current.dogId).refuge}",
        bottom = { HButton("Iniciar solicitud", { nav.open(UiScreen.ApplicationContact) }) },
    ) {
        HBanner(
            "Requisitos de ejemplo. Cada refugio define sus propias condiciones; confírmalas durante la entrevista."
        )
        HHeading("Condiciones del refugio")
        PreviewData.requirements.forEach { requirement ->
            HCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HIcon(HSymbol.Check)
                    Text(
                        requirement,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        HHeading("Pasos para adoptar")
        PreviewData.process.forEachIndexed { i, (title, description) ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        "${i + 1}",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Column {
                    HHeading(title)
                    HBody(description, muted = true)
                }
            }
        }
    }
}

@Composable
fun RefugeInformationScreen(nav: UiNavigator) {
    HScaffold(nav, PreviewData.dog(nav.current.dogId).refuge, "Información pública") {
        HTag("Refugio ficticio")
        HCard {
            HKeyValue("Municipio", "Chalco, Estado de México")
            HKeyValue("Zona", "Centro de Chalco", true)
            HKeyValue("Atención", "Lunes a sábado", true)
            HKeyValue("Horario", "10:00 a 17:00", true)
        }
        HMap(showAction = false) {}
        HBanner(
            "La dirección exacta se comparte cuando el refugio agenda tu visita.",
            HSymbol.Shield,
        )
        HButton("Ver requisitos de adopción", { nav.open(UiScreen.Requirements) }, outlined = true)
    }
}
