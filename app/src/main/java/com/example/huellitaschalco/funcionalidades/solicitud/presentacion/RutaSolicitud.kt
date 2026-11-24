package com.example.huellitaschalco.funcionalidades.solicitud.presentacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.RepositorioPerritos
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.UsuarioAutenticado
import com.example.huellitaschalco.funcionalidades.catalogo.presentacion.ModeloVistaFichaPublicaPerrito
import com.example.huellitaschalco.funcionalidades.refugio.dominio.RepositorioRefugioPublico
import com.example.huellitaschalco.funcionalidades.solicitud.dominio.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun RutaSolicitud(
    nav: NavegadorInterfaz,
    id: String,
    user: UsuarioAutenticado?,
    repository: RepositorioSolicitudAdopcion,
    dogs: RepositorioPerritos,
    shelters: RepositorioRefugioPublico,
    drafts: com.example.huellitaschalco.nucleo.local.AlmacenBorradores? = null,
) {
    val detail: ModeloVistaFichaPublicaPerrito =
        viewModel(
            key = "public-dog-detail-$id",
            factory = ModeloVistaFichaPublicaPerrito.factory(id, dogs, shelters),
        )
    val context by detail.state.collectAsStateWithLifecycle()
    if (context.loading || context.error != null) {
        HScaffold(nav, "Solicitud de adopción") {
            if (context.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            else {
                HBanner(context.error!!, SimboloHuellitas.Alert, error = true)
                HCaption(
                    "El borrador de esta cuenta se conserva si necesitas reintentar la consulta."
                )
                HButton("Reintentar", detail::retry, outlined = true)
            }
        }
        return
    }
    val dog = context.dog!!
    val shelter = context.shelter!!
    if (user == null) {
        HScaffold(nav, "Solicitud para ${dog.name}") {
            HBody(
                "Inicia sesión o crea una cuenta para preparar tu solicitud y consultar su estado."
            )
            HBody(shelter.name)
            HButton(
                "Iniciar sesión",
                { nav.open(PantallaInterfaz.Login, recordId = id) },
                Modifier.testTag("application-login"),
            )
            HButton(
                "Crear cuenta",
                { nav.open(PantallaInterfaz.SignUp, recordId = id) },
                Modifier.testTag("application-signup"),
                outlined = true,
            )
        }
        return
    }
    val model: ModeloVistaFormularioSolicitud =
        viewModel(
            key = "application-${user.uid}-$id",
            factory = ModeloVistaFormularioSolicitud.factory(repository, user, id, dog.shelterId, drafts),
        )
    val state by model.state.collectAsStateWithLifecycle()
    if (state.restoring) {
        HScaffold(nav, "Recuperando borrador") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        return
    }
    LaunchedEffect(user) { model.receiveIdentity(user) }
    var confirmExit by rememberSaveable(user.uid, id) { mutableStateOf(false) }
    BackHandler {
        if (!state.sending) {
            if (state.receipt != null) nav.root(PantallaInterfaz.Catalog)
            else if (state.step > 0 && !state.locked) model.back() else confirmExit = true
        }
    }
    val allowed = dog.status == "Disponible" && shelter.policy != null
    HScaffold(
        nav,
        if (state.receipt != null) "Solicitud registrada" else "Solicitud para ${dog.name}",
        if (state.receipt == null) "Paso ${state.step + 1} de 5" else null,
        back = false,
        bottom = {
            if (state.receipt != null)
                HButton(
                    "Seguir explorando",
                    { nav.root(PantallaInterfaz.Catalog) },
                    Modifier.testTag("application-done"),
                    outlined = true,
                )
            else {
                HCaption(
                    if (state.step == 4)
                        "Revisa tus respuestas. Solo se enviarán al pulsar Enviar solicitud."
                    else "Paso ${state.step + 1} de 5 · Tu solicitud todavía no se ha enviado."
                )
                HButton(
                    if (state.sending) "Enviando solicitud…"
                    else if (state.step == 4 && state.error != null) "Reintentar envío"
                    else if (state.step == 4) "Enviar solicitud"
                    else
                        when (state.step) {
                            0 -> "Continuar: tu hogar"
                            1 -> "Continuar: otras mascotas"
                            2 -> "Continuar: motivos"
                            else -> "Revisar mi solicitud"
                        },
                    { if (state.step == 4) model.send() else model.next() },
                    Modifier.testTag("application-next"),
                    enabled =
                        !state.sending &&
                            !state.draftBlocked &&
                            allowed &&
                            !(state.step == 4 && context.stale),
                )
                if (state.step > 0 && !state.locked)
                    HButton(
                        "Atrás",
                        model::back,
                        Modifier.testTag("application-back"),
                        outlined = true,
                    )
                HButton(
                    "Salir y conservar borrador",
                    { confirmExit = true },
                    outlined = true,
                    enabled = !state.sending,
                )
            }
        },
    ) {
        state.draftMessage?.let { HCaption(it) }
        if (context.stale) HButton("Actualizar ficha", detail::retry, outlined = true)
        if (context.stale)
            HBanner(
                "Consulta de la última copia guardada. Puedes preparar el borrador; conéctate y actualiza la ficha para enviar.",
                SimboloHuellitas.Alert,
            )

        if (state.receipt != null) {
            val receipt = state.receipt!!
            HSuccessHeader(
                if (receipt.alreadyExisting) "Ya tienes una solicitud"
                else if (receipt.status != "Enviada") "Solicitud registrada"
                else "¡Solicitud enviada!",
                if (receipt.alreadyExisting)
                    "Conservamos tu solicitud anterior para este perrito; no se creó otra."
                else "Tu solicitud para ${dog.name} se guardó en el refugio.",
            )
            HKeyValue("Folio", receipt.folio)
            HTag(receipt.status)
            HBody("El refugio revisará tus respuestas y actualizará el estado de la solicitud.")
            HButton(
                "Ver mi solicitud",
                { nav.open(PantallaInterfaz.RequestDetail, recordId = receipt.id) },
                Modifier.testTag("application-view-request"),
            )
        } else {
            HBody(shelter.name)
            HCaption("El borrador se guarda en este dispositivo para esta cuenta.")
            if (!allowed)
                HBanner(
                    "Este perrito no admite solicitudes nuevas o su refugio no tiene requisitos completos.",
                    SimboloHuellitas.Alert,
                    error = true,
                )
            val editable = !state.locked && allowed
            val draft = state.draft
            @Composable
            fun input(
                label: String,
                field: CampoSolicitud,
                value: String,
                keyboard: KeyboardType = KeyboardType.Text,
                lines: Int = 1,
            ) {
                HInput(
                    label,
                    value,
                    { model.edit(field, it) },
                    Modifier.testTag("application-${field.name}"),
                    error = state.errors[field],
                    keyboard = keyboard,
                    lines = lines,
                    enabled = editable,
                )
            }
            @Composable
            fun choices(
                label: String,
                field: CampoSolicitud,
                options: Map<String, String>,
                selected: String,
            ) {
                HHeading(label)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { (key, title) ->
                        FilterChip(
                            selected = key == selected,
                            onClick = { model.edit(field, key) },
                            label = { Text(title) },
                            enabled = editable,
                            modifier = Modifier.testTag("application-${field.name}-$key"),
                        )
                    }
                }
                state.errors[field]?.let { HBody(it) }
            }
            when (state.step) {
                0 -> {
                    HHeading("Contacto")
                    input("Nombre completo", CampoSolicitud.Name, draft.name)
                    input(
                        "Teléfono celular",
                        CampoSolicitud.Phone,
                        draft.phone,
                        KeyboardType.Phone,
                    )
                    input(
                        "Correo electrónico",
                        CampoSolicitud.Email,
                        draft.email,
                        KeyboardType.Email,
                    )
                }
                1 -> {
                    choices(
                        "Tipo de vivienda",
                        CampoSolicitud.Housing,
                        OpcionesSolicitud.housing,
                        draft.housing,
                    )
                    choices(
                        "La vivienda es…",
                        CampoSolicitud.Tenure,
                        OpcionesSolicitud.tenure,
                        draft.tenure,
                    )
                    choices(
                        "¿Tienes espacio exterior?",
                        CampoSolicitud.Yard,
                        mapOf("Sí" to "Sí", "No" to "No"),
                        draft.yard?.let { if (it) "Sí" else "No" }.orEmpty(),
                    )
                    input(
                        "Personas en tu hogar",
                        CampoSolicitud.People,
                        draft.people,
                        KeyboardType.Number,
                    )
                    choices(
                        "Horas al día que estaría solo",
                        CampoSolicitud.Hours,
                        OpcionesSolicitud.hours,
                        draft.hours,
                    )
                }
                2 -> {
                    choices(
                        "¿Tienes otras mascotas?",
                        CampoSolicitud.Pets,
                        mapOf("Sí" to "Sí", "No" to "No"),
                        draft.pets?.let { if (it) "Sí" else "No" }.orEmpty(),
                    )
                    if (draft.pets == true)
                        input(
                            "Cuéntanos sobre ellas (opcional)",
                            CampoSolicitud.PetsDetails,
                            draft.petsDetails,
                            lines = 3,
                        )
                }
                3 ->
                    input(
                        "¿Por qué quieres adoptar a ${dog.name}?",
                        CampoSolicitud.Reasons,
                        draft.reasons,
                        lines = 6,
                    )
                4 -> {
                    HHeading("Revisa tus respuestas")
                    HCard {
                        HHeading("Contacto")
                        HBody("${draft.name}\n${draft.phone}\n${draft.email}")
                        HButton(
                            "Editar contacto",
                            { model.editStep(0) },
                            outlined = true,
                            enabled = editable,
                        )
                    }
                    HCard {
                        HHeading("Tu hogar")
                        HBody(
                            "${OpcionesSolicitud.housing[draft.housing]} · ${OpcionesSolicitud.tenure[draft.tenure]}\nEspacio exterior: ${if(draft.yard == true) "Sí" else "No"}\nPersonas: ${draft.people}\nHoras solo: ${OpcionesSolicitud.hours[draft.hours]}"
                        )
                        HButton(
                            "Editar hogar",
                            { model.editStep(1) },
                            outlined = true,
                            enabled = editable,
                        )
                    }
                    HCard {
                        HHeading("Otras mascotas")
                        HBody(if (draft.pets == true) "Sí. ${draft.petsDetails}" else "No")
                        HButton(
                            "Editar mascotas",
                            { model.editStep(2) },
                            outlined = true,
                            enabled = editable,
                        )
                    }
                    HCard {
                        HHeading("Motivos")
                        HBody(draft.reasons)
                        HButton(
                            "Editar motivos",
                            { model.editStep(3) },
                            outlined = true,
                            enabled = editable,
                        )
                    }
                }
            }
            if (state.sending) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
        }
    }
    if (confirmExit)
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Salir de la solicitud?") },
            text = {
                Text(
                    if (state.locked)
                        "El resultado del último envío puede no haberse confirmado. Reintentar comprobará la solicitud existente antes de crear otra."
                    else
                        "El borrador se conserva en este dispositivo para esta cuenta; todavía no se ha enviado."
                )
            },
            confirmButton = { TextButton(onClick = { nav.back() }) { Text("Salir") } },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Continuar aquí") }
            },
        )
}
