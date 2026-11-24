package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import com.example.huellitaschalco.nucleo.vista_previa.*

@Composable
fun ShelterDogsScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val dogs =
        DatosVistaPrevia.dogs.filter {
            it.id in
                if (destination.variant == VarianteInterfaz.Photos) listOf(8, 1, 2, 4, 6, 7)
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
        HButton("Registrar perrito", { nav.open(PantallaInterfaz.DogForm, dogId = 8) }, icon = SimboloHuellitas.Plus)
        HChoices(null, listOf("Todos ${dogs.size}", "Disponible 3", "En proceso", "Adoptado"))
        dogs.forEach { dog ->
            val status =
                if (dog.id == 2 && destination.variant == VarianteInterfaz.AfterAdoption) "Adoptado"
                else dog.status
            HDogRow(
                dog,
                status = status,
                onClick = {
                    nav.open(
                        PantallaInterfaz.ShelterDogDetail,
                        dogId = dog.id,
                        variant = destination.variant,
                    )
                },
            )
        }
    }
}

@Composable
fun ShelterDogDetailScreen(nav: NavegadorInterfaz, destination: DestinoInterfaz) {
    val dog = DatosVistaPrevia.dog(destination.dogId)
    HScaffold(
        nav,
        dog.name,
        "Registro del perrito",
        bottom = {
            HButton(
                "Editar perrito",
                { nav.open(PantallaInterfaz.DogForm, variant = VarianteInterfaz.Editing) },
                icon = SimboloHuellitas.Edit,
            )
        },
    ) {
        HPhoto(dog, Modifier.fillMaxWidth().height(260.dp))
        HTag(
            if (dog.id == 2 && destination.variant == VarianteInterfaz.AfterAdoption) "Adoptado"
            else dog.status
        )
        HHeading(dog.name)
        HBody("${dog.age} · ${dog.size} · ${dog.sex}", muted = true)
        HHeading("Descripción")
        HBody(dog.description)
        HHeading("Salud")
        HHealth(dog)
        val requests = DatosVistaPrevia.requests.filter { it.dogId == dog.id }
        HHeading("Solicitudes (${requests.size})")
        requests.forEach { request ->
            HCard(
                onClick = {
                    nav.open(PantallaInterfaz.EvaluateRequest, requestId = request.id, dogId = dog.id)
                }
            ) {
                HHeading(request.user)
                HCaption("${request.folio} · ${request.date}")
                HTag(request.status)
            }
        }
    }
}
