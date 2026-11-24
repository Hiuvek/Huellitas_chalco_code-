package com.example.huellitaschalco.funcionalidades.solicitudes.presentacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.UsuarioAutenticado
import com.example.huellitaschalco.funcionalidades.solicitud.dominio.OpcionesSolicitud
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun RutaMisSolicitudes(
    nav: NavegadorInterfaz,
    user: UsuarioAutenticado?,
    id: String?,
    repository: RepositorioSolicitudesAdopcion,
) {
    if (user == null) {
        HScaffold(nav, "Mis solicitudes", back = false, tab = "Solicitudes") {
            HBody("Inicia sesión para consultar únicamente las solicitudes de tu cuenta.")
            HButton(
                "Iniciar sesión",
                { nav.open(PantallaInterfaz.Login) },
                Modifier.testTag("requests-login"),
            )
        }
        return
    }
    val model: ModeloVistaSolicitudes =
        viewModel(
            key = "requests-${user.uid}-$id",
            factory = ModeloVistaSolicitudes.factory(user.uid, id, repository),
        )
    if (id == null) {
        val state by model.list.collectAsStateWithLifecycle()
        HScaffold(nav, "Mis solicitudes", back = false, tab = "Solicitudes") {
            HScreenIntro(
                "Sigue tus solicitudes",
                "Abre una tarjeta para consultar el estado, la respuesta del refugio y los datos que enviaste.",
            )
            when {
                state.loading ->
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("requests-loading"))
                state.error != null -> RequestError(state.error!!, model::retry)
                state.requests.isEmpty() -> {
                    HEmpty(
                        "Aún no tienes solicitudes",
                        "Cuando envíes una solicitud, aquí podrás seguir su avance.",
                        action = "Explorar perritos",
                        onAction = { nav.root(PantallaInterfaz.Catalog) },
                    )
                    HButton("Actualizar", model::retry, outlined = true)
                }
                else ->
                    state.requests.forEach { item ->
                        HCard(
                            Modifier.testTag("request-${item.id}"),
                            onClick = { nav.open(PantallaInterfaz.RequestDetail, recordId = item.id) },
                        ) {
                            HHeading(item.dogName ?: "Perrito ${item.dogId}")
                            HCaption(item.folio)
                            HCaption("Enviada: ${requestDate(item.createdAt)}")
                            HTag(item.status)
                            HBody(item.nextStep)
                            HCardAction("Ver estado y respuestas")
                        }
                    }
            }
        }
    } else {
        val state by model.detail.collectAsStateWithLifecycle()
        var showAnswers by remember(user.uid, id) { mutableStateOf(false) }
        HScaffold(nav, "Detalle de solicitud", state.request?.folio) {
            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error != null -> RequestError(state.error!!, model::retry)
                else ->
                    state.request?.let { item ->
                        HHeading(item.dogName ?: "Perrito ${item.dogId}")
                        HCaption("Refugio: ${item.shelterId}")
                        HTag(item.status)
                        HBanner(item.nextStep)
                        item.rejectionReason?.let { HKeyValue("Motivo de la decisión", it) }
                        HKeyValue("Enviada", requestDate(item.createdAt))
                        HKeyValue("Última actualización", requestDate(item.updatedAt))
                        HHeading("Historial")
                        if (state.history.isEmpty()) HBody("No hay cambios de estado registrados.")
                        state.history.forEach { event ->
                            HCard {
                                HTag(event.status)
                                HCaption(requestDate(event.timestamp))
                                event.reason?.let { HBody(it) }
                            }
                        }
                        HButton(
                            if (showAnswers) "Ocultar respuestas" else "Tus respuestas",
                            { showAnswers = !showAnswers },
                            Modifier.testTag("request-answers"),
                            outlined = true,
                        )
                        if (showAnswers) RequestAnswers(item)
                        HButton(
                            "Volver a mis solicitudes",
                            { nav.root(PantallaInterfaz.MyRequests) },
                            outlined = true,
                        )
                    }
            }
        }
    }
}

@Composable
private fun RequestError(error: String, retry: () -> Unit) {
    HBanner(error, SimboloHuellitas.Alert, error = true)
    HButton("Reintentar", retry, Modifier.testTag("requests-retry"), outlined = true)
}

@Composable
fun RequestAnswers(item: SolicitudAdopcionRecibida) {
    val a = item.answers
    HCard {
        HHeading("Contacto")
        HKeyValue("Nombre", a.name)
        HKeyValue("Correo", a.email)
        HKeyValue("Teléfono", a.phone)
        HHeading("Hogar")
        HKeyValue("Vivienda", OpcionesSolicitud.housing[a.housing] ?: a.housing)
        HKeyValue("Tenencia", OpcionesSolicitud.tenure[a.tenure] ?: a.tenure)
        HKeyValue("Espacio exterior", if (a.yard == true) "Sí" else "No")
        HKeyValue("Personas", a.people)
        HKeyValue("Horas que estaría solo", OpcionesSolicitud.hours[a.hours] ?: a.hours)
        HHeading("Otras mascotas")
        HKeyValue("Tiene mascotas", if (a.pets == true) "Sí" else "No")
        if (a.pets == true) HBody(a.petsDetails.ifBlank { "Sin descripción adicional." })
        HHeading("Motivos")
        HBody(a.reasons)
    }
}

fun requestDate(milliseconds: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-MX"))
        .apply { timeZone = TimeZone.getTimeZone("America/Mexico_City") }
        .format(Date(milliseconds))
