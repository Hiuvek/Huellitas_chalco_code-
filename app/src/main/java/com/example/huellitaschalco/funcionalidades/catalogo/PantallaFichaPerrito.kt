package com.example.huellitaschalco.funcionalidades.catalogo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import com.example.huellitaschalco.nucleo.vista_previa.*

@Composable
fun PantallaFichaPerrito(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val dog = DatosVistaPrevia.dog(destination.dogId)
    val preview = destination.screen == PantallaInterfaz.DogPreview
    val editing = preview && destination.variant == VarianteInterfaz.Editing
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
                        if (editing) PantallaInterfaz.ShelterDogDetail
                        else if (preview) PantallaInterfaz.DogPublished else PantallaInterfaz.Requirements
                    )
                },
                icon = if (preview) SimboloHuellitas.Check else null,
            )
            if (preview) HButton("Volver a editar", nav::back, outlined = true, icon = SimboloHuellitas.Edit)
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
        HCard(onClick = { nav.open(PantallaInterfaz.RefugeInformation) }) {
            HHeading(dog.refuge)
            HBody("Zona centro de Chalco", muted = true)
            HBody("Lunes a sábado · 10:00 a 17:00", muted = true)
        }
        HMap(destination.variant == VarianteInterfaz.MapError) { nav.open(PantallaInterfaz.RefugeInformation) }
        HCaption(
            if (editing) "Los cambios se mostrarán al confirmar."
            else if (preview) "Se publicará al confirmar." else "Publicado el 3 nov 2026"
        )
    }
}

@Composable
fun RequirementsScreen(nav: NavegadorInterfaz) {
    HScaffold(
        nav,
        "Requisitos de adopción",
        "${DatosVistaPrevia.dog(nav.current.dogId).name} · ${DatosVistaPrevia.dog(nav.current.dogId).refuge}",
        bottom = { HButton("Iniciar solicitud", { nav.open(PantallaInterfaz.ApplicationContact) }) },
    ) {
        HBanner("Revisa las condiciones del refugio y confírmalas durante la entrevista.")
        HHeading("Condiciones del refugio")
        DatosVistaPrevia.requirements.forEach { requirement ->
            HCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Iconos(SimboloHuellitas.Check)
                    Text(
                        requirement,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        HHeading("Pasos para adoptar")
        DatosVistaPrevia.process.forEachIndexed { i, (title, description) ->
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
fun RefugeInformationScreen(nav: NavegadorInterfaz) {
    HScaffold(nav, DatosVistaPrevia.dog(nav.current.dogId).refuge, "Información pública") {
        HCard {
            HKeyValue("Municipio", "Chalco, Estado de México")
            HKeyValue("Zona", "Centro de Chalco", true)
            HKeyValue("Atención", "Lunes a sábado", true)
            HKeyValue("Horario", "10:00 a 17:00", true)
        }
        HMap(showAction = false) {}
        HBanner(
            "La dirección exacta se comparte cuando el refugio agenda tu visita.",
            SimboloHuellitas.Shield,
        )
        HButton("Ver requisitos de adopción", { nav.open(PantallaInterfaz.Requirements) }, outlined = true)
    }
}
