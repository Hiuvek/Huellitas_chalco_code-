package com.example.huellitaschalco.feature.shelter.dogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.*

@Composable
fun ShelterDogsScreen(nav: UiNavigator, destination: UiDestination) {
    val dogs =
        PreviewData.dogs.filter {
            it.id in
                if (destination.variant == UiVariant.Photos) listOf(8, 1, 2, 4, 6, 7)
                else listOf(1, 2, 4, 6, 7)
        }
    HScaffold(
        nav,
        "Mis perritos",
        "Huellitas del Valle",
        back = false,
        shelter = true,
        tab = "Perritos",
    ) {
        HButton("Registrar perrito", { nav.open(UiScreen.DogForm, dogId = 8) }, icon = HSymbol.Plus)
        HChoices(null, listOf("Todos ${dogs.size}", "Disponible 3", "En proceso", "Adoptado"))
        dogs.forEach { dog ->
            val status =
                if (dog.id == 2 && destination.variant == UiVariant.AfterAdoption) "Adoptado"
                else dog.status
            HDogRow(
                dog,
                status = status,
                onClick = {
                    nav.open(
                        UiScreen.ShelterDogDetail,
                        dogId = dog.id,
                        variant = destination.variant,
                    )
                },
            )
        }
    }
}

@Composable
fun ShelterDogDetailScreen(nav: UiNavigator, destination: UiDestination) {
    val dog = PreviewData.dog(destination.dogId)
    HScaffold(
        nav,
        dog.name,
        "Registro del perrito",
        bottom = {
            HButton(
                "Editar perrito",
                { nav.open(UiScreen.DogForm, variant = UiVariant.Editing) },
                icon = HSymbol.Edit,
            )
        },
    ) {
        HPhoto(dog, Modifier.fillMaxWidth().height(260.dp))
        HTag(
            if (dog.id == 2 && destination.variant == UiVariant.AfterAdoption) "Adoptado"
            else dog.status
        )
        HHeading(dog.name)
        HBody("${dog.age} · ${dog.size} · ${dog.sex}", muted = true)
        HHeading("Descripción")
        HBody(dog.description)
        HHeading("Salud")
        HHealth(dog)
        val requests = PreviewData.requests.filter { it.dogId == dog.id }
        HHeading("Solicitudes (${requests.size})")
        requests.forEach { request ->
            HCard(
                onClick = {
                    nav.open(UiScreen.EvaluateRequest, requestId = request.id, dogId = dog.id)
                }
            ) {
                HHeading(request.user)
                HCaption("${request.folio} · ${request.date}")
                HTag(request.status)
            }
        }
    }
}

@Composable
fun DogPublishedScreen(nav: UiNavigator) {
    HScaffold(
        nav,
        "",
        back = false,
        bottom = {
            HButton(
                "Ver en Mis perritos",
                { nav.open(UiScreen.ShelterDogs, variant = UiVariant.Photos) },
            )
            HTextAction(
                "Registrar otro perrito",
                { nav.open(UiScreen.DogForm, dogId = 8) },
                icon = HSymbol.Plus,
            )
        },
    ) {
        HSuccessHeader(
            "¡Perrito publicado!",
            "Perrito 8 ya aparece en el catálogo de adopción y en Mis perritos.",
        )
        HDogRow(PreviewData.dog(8), "3 años · Mediano · 1 fotografía", "Disponible")
        HCaption("Publicado el 16 nov 2026")
    }
}
