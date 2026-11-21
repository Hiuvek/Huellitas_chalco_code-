package com.example.huellitaschalco.feature.application.presentation

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
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import com.example.huellitaschalco.feature.catalog.presentation.PublicDogDetailViewModel
import com.example.huellitaschalco.feature.refuge.domain.PublicShelterRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.DogRepository

@Composable
fun ApplicationRoute(
    nav: UiNavigator,
    id: String,
    user: AuthUser?,
    repository: AdoptionApplicationRepository,
    dogs: DogRepository,
    shelters: PublicShelterRepository,
) {
    val detail: PublicDogDetailViewModel =
        viewModel(
            key = "public-dog-detail-$id",
            factory = PublicDogDetailViewModel.factory(id, dogs, shelters),
        )
    val context by detail.state.collectAsStateWithLifecycle()
    if (context.loading || context.error != null) {
        HScaffold(nav, "Solicitud de adopción") {
            if (context.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            else {
                HBanner(context.error!!, HSymbol.Alert, error = true)
                HCaption(
                    "Las respuestas de esta sesión se conservan si necesitas reintentar la consulta."
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
                { nav.open(UiScreen.Login, recordId = id) },
                Modifier.testTag("application-login"),
            )
            HButton(
                "Crear cuenta",
                { nav.open(UiScreen.SignUp, recordId = id) },
                Modifier.testTag("application-signup"),
                outlined = true,
            )
        }
        return
    }
    val model: ApplicationFormViewModel =
        viewModel(
            key = "application-${user.uid}-$id",
            factory = ApplicationFormViewModel.factory(repository, user, id, dog.shelterId),
        )
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(user) { model.receiveIdentity(user) }
    var confirmExit by rememberSaveable(user.uid, id) { mutableStateOf(false) }
    BackHandler {
        if (!state.sending) {
            if (state.receipt != null) nav.root(UiScreen.Catalog)
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
                    { nav.root(UiScreen.Catalog) },
                    Modifier.testTag("application-done"),
                )
            else {
                HButton(
                    if (state.sending) "Enviando solicitud…"
                    else if (state.step == 4 && state.error != null) "Reintentar envío"
                    else if (state.step == 4) "Enviar solicitud" else "Continuar",
                    { if (state.step == 4) model.send() else model.next() },
                    Modifier.testTag("application-next"),
                    enabled = !state.sending && allowed,
                )
                if (state.step > 0 && !state.locked)
                    HButton(
                        "Atrás",
                        model::back,
                        Modifier.testTag("application-back"),
                        outlined = true,
                    )
                HButton(
                    "Cancelar solicitud",
                    { confirmExit = true },
                    outlined = true,
                    enabled = !state.sending,
                )
            }
        },
    ) {
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
                { nav.open(UiScreen.RequestDetail, recordId = receipt.id) },
                Modifier.testTag("application-view-request"),
            )
        } else {
            HBody(shelter.name)
            HCaption("Tus respuestas se conservan durante esta sesión.")
            if (dog.demonstration || shelter.demonstration)
                HCaption("Solicitud de demostración: se guarda solo en los emuladores locales.")
            if (!allowed)
                HBanner(
                    "Este perrito no admite solicitudes nuevas o su refugio no tiene requisitos completos.",
                    HSymbol.Alert,
                    error = true,
                )
            val editable = !state.locked && allowed
            val draft = state.draft
            @Composable
            fun input(
                label: String,
                field: ApplicationField,
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
                field: ApplicationField,
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
                    input("Nombre completo", ApplicationField.Name, draft.name)
                    input(
                        "Teléfono celular",
                        ApplicationField.Phone,
                        draft.phone,
                        KeyboardType.Phone,
                    )
                    input(
                        "Correo electrónico",
                        ApplicationField.Email,
                        draft.email,
                        KeyboardType.Email,
                    )
                }
                1 -> {
                    choices(
                        "Tipo de vivienda",
                        ApplicationField.Housing,
                        ApplicationChoices.housing,
                        draft.housing,
                    )
                    choices(
                        "La vivienda es…",
                        ApplicationField.Tenure,
                        ApplicationChoices.tenure,
                        draft.tenure,
                    )
                    choices(
                        "¿Tienes espacio exterior?",
                        ApplicationField.Yard,
                        mapOf("Sí" to "Sí", "No" to "No"),
                        draft.yard?.let { if (it) "Sí" else "No" }.orEmpty(),
                    )
                    input(
                        "Personas en tu hogar",
                        ApplicationField.People,
                        draft.people,
                        KeyboardType.Number,
                    )
                    choices(
                        "Horas al día que estaría solo",
                        ApplicationField.Hours,
                        ApplicationChoices.hours,
                        draft.hours,
                    )
                }
                2 -> {
                    choices(
                        "¿Tienes otras mascotas?",
                        ApplicationField.Pets,
                        mapOf("Sí" to "Sí", "No" to "No"),
                        draft.pets?.let { if (it) "Sí" else "No" }.orEmpty(),
                    )
                    if (draft.pets == true)
                        input(
                            "Cuéntanos sobre ellas (opcional)",
                            ApplicationField.PetsDetails,
                            draft.petsDetails,
                            lines = 3,
                        )
                }
                3 ->
                    input(
                        "¿Por qué quieres adoptar a ${dog.name}?",
                        ApplicationField.Reasons,
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
                            "${ApplicationChoices.housing[draft.housing]} · ${ApplicationChoices.tenure[draft.tenure]}\nEspacio exterior: ${if(draft.yard == true) "Sí" else "No"}\nPersonas: ${draft.people}\nHoras solo: ${ApplicationChoices.hours[draft.hours]}"
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
            state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
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
                        "Tus respuestas se conservan durante esta sesión; todavía no se han enviado."
                )
            },
            confirmButton = { TextButton(onClick = { nav.back() }) { Text("Salir") } },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Continuar aquí") }
            },
        )
}
