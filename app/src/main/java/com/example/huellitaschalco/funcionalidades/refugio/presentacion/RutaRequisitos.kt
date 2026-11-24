package com.example.huellitaschalco.funcionalidades.refugio.presentacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.RepositorioPerritos
import com.example.huellitaschalco.funcionalidades.catalogo.presentacion.ModeloVistaFichaPublicaPerrito
import com.example.huellitaschalco.funcionalidades.refugio.dominio.RepositorioRefugioPublico
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun RutaRequisitos(
    nav: NavegadorInterfaz,
    id: String,
    dogs: RepositorioPerritos,
    shelters: RepositorioRefugioPublico,
) {
    val model: ModeloVistaFichaPublicaPerrito =
        viewModel(
            key = "public-dog-detail-$id",
            factory = ModeloVistaFichaPublicaPerrito.factory(id, dogs, shelters),
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
                        { nav.open(PantallaInterfaz.ApplicationContact, recordId = id) },
                        Modifier.testTag("requirements-start"),
                        enabled = state.dog?.status == "Disponible",
                    )
                })
            else null,
    ) {
        if (state.stale) {
            HBanner(
                "Última copia guardada. La información y la disponibilidad pueden haber cambiado.",
                SimboloHuellitas.Alert,
            )
            HButton("Actualizar ficha", model::retry, outlined = true)
        }
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HBanner(state.error!!, SimboloHuellitas.Alert, error = true)
                HButton("Reintentar", model::retry, outlined = true)
            }
            else -> {
                val dog = state.dog!!
                val shelter = state.shelter!!
                HScreenIntro(
                    "Antes de solicitar a ${dog.name}",
                    "Lee las condiciones y los pasos de ${shelter.name}. Después pulsa Iniciar solicitud.",
                )
                val policy = shelter.policy
                if (policy == null) {
                    HBanner(
                        "El refugio aún no tiene requisitos y pasos completos publicados. Consulta su información antes de preparar una solicitud.",
                        SimboloHuellitas.Alert,
                        error = true,
                    )
                    HButton(
                        "Información del refugio",
                        { nav.open(PantallaInterfaz.RefugeInformation, recordId = id) },
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
                                SimboloHuellitas.Info,
                            )
                    }
            }
        }
    }
}
