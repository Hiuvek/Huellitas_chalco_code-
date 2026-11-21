package com.example.huellitaschalco.feature.shelter.dogs.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.UiNavigator
import com.example.huellitaschalco.feature.shelter.dogs.domain.*

@Composable
fun DogEditRoute(
    nav: UiNavigator,
    uid: String,
    initial: RegisteredDog,
    session: Int,
    repository: DogRepository,
    importer: DogPhotoImporter,
    onClose: () -> Unit,
) {
    val model: DogEditViewModel =
        viewModel(
            key = "dog-edit-$uid-${initial.shelterId}-${initial.id}-$session",
            factory = DogEditViewModel.factory(repository, importer, uid, initial),
        )
    val state by model.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris
            ->
            model.addPhotos(uris.map { it.toString() })
        }
    val close = {
        model.releasePhotos()
        onClose()
    }
    val cancel = { if (state.dirty && state.saved == null) confirmDiscard = true else close() }
    BackHandler {
        if (!state.saving && !state.importing) {
            if (state.review && !state.locked) model.returnToForm() else cancel()
        }
    }
    val editable = !state.locked && !state.importing
    HScaffold(
        nav,
        if (state.saved != null) "Cambios guardados"
        else if (state.review) "Revisar cambios" else "Editar perrito",
        back = false,
        bottom = {
            when {
                state.saved != null ->
                    HButton("Volver al registro", close, Modifier.testTag("edit-done"))
                state.review -> {
                    HButton(
                        if (state.saving) state.progress
                        else if (state.error != null) "Reintentar guardado" else "Guardar cambios",
                        model::save,
                        Modifier.testTag("edit-save"),
                        enabled = !state.saving && state.conflict == null,
                    )
                    if (!state.locked)
                        HButton("Volver a editar", model::returnToForm, outlined = true)
                }
                else ->
                    HButton(
                        if (state.importing) "Preparando fotografías…" else "Revisar cambios",
                        model::review,
                        Modifier.testTag("edit-review"),
                        enabled = editable && state.dirty,
                    )
            }
            if (state.saved == null)
                HButton(
                    "Cancelar edición",
                    cancel,
                    outlined = true,
                    enabled = !state.saving && !state.importing,
                )
        },
    ) {
        if (state.saved != null) {
            HSuccessHeader(
                "¡Cambios guardados!",
                "La ficha de ${state.saved!!.name} se actualizó correctamente.",
            )
            HCaption("Folio: ${initial.id}")
        } else if (state.review) {
            EditPhotos(state.draft.photos, repository, false, model::removePhoto, model::cover)
            HHeading(state.draft.form.name.trim())
            HBody(
                "${state.draft.form.ageMonths()} meses · ${state.draft.form.size} · ${state.draft.form.sex}"
            )
            HBody(state.draft.form.description.trim())
            HCaption("La salud se administra por separado desde el registro del perrito.")
        } else {
            HCaption(
                "Folio: ${initial.id}. Conserva al menos una fotografía; la primera será la portada."
            )
            EditPhotos(state.draft.photos, repository, editable, model::removePhoto, model::cover)
            HButton(
                "Agregar fotografías",
                {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                Modifier.testTag("edit-add-photos"),
                outlined = true,
                enabled = editable && state.draft.photos.size < 6,
            )
            HCaption(
                "${state.draft.photos.size} de 6 fotografías. JPEG, PNG o WebP de hasta 20 MB."
            )
            state.errors[DogField.Photos]?.let { HBody(it) }
            HInput(
                "Nombre",
                state.draft.form.name,
                { model.edit(DogField.Name, it) },
                Modifier.testTag("edit-name"),
                error = state.errors[DogField.Name],
                enabled = editable,
            )
            HInput(
                "Edad aproximada",
                state.draft.form.age,
                { model.edit(DogField.Age, it) },
                Modifier.testTag("edit-age"),
                error = state.errors[DogField.Age],
                keyboard = KeyboardType.Number,
                enabled = editable,
            )
            DogChoices("Unidad", listOf("Meses", "Años"), state.draft.form.ageUnit, editable) {
                model.edit(DogField.AgeUnit, it)
            }
            DogChoices(
                "Tamaño",
                listOf("Chico", "Mediano", "Grande"),
                state.draft.form.size,
                editable,
            ) {
                model.edit(DogField.Size, it)
            }
            state.errors[DogField.Size]?.let { HBody(it) }
            DogChoices("Sexo", listOf("Hembra", "Macho"), state.draft.form.sex, editable) {
                model.edit(DogField.Sex, it)
            }
            state.errors[DogField.Sex]?.let { HBody(it) }
            HInput(
                "Descripción",
                state.draft.form.description,
                { model.edit(DogField.Description, it) },
                Modifier.testTag("edit-description"),
                error = state.errors[DogField.Description],
                lines = 5,
                enabled = editable,
            )
        }
        if (state.saving || state.importing) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
        if (state.conflict != null)
            HButton(
                "Cargar registro actual",
                model::reloadConflict,
                Modifier.testTag("edit-reload"),
                outlined = true,
            )
    }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Salir de la edición?") },
            text = {
                Text(
                    if (state.locked)
                        "No se confirmó el resultado del último intento. Consulta la ficha al salir para comprobar si se guardó."
                    else "Los cambios sin guardar se perderán."
                )
            },
            confirmButton = { TextButton(onClick = close) { Text("Salir") } },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") }
            },
        )
}

@Composable
private fun EditPhotos(
    photos: List<EditableDogPhoto>,
    repository: DogRepository,
    editable: Boolean,
    remove: (String) -> Unit,
    cover: (String) -> Unit,
) {
    photos.forEachIndexed { index, photo ->
        HCard {
            photo.remote?.let { StoredDogPhoto(it.path, repository) }
            photo.local?.let { PhotoStrip(listOf(it), showCaption = false) }
            HCaption(if (index == 0) "Portada" else "Fotografía ${index + 1}")
            if (editable) {
                Row {
                    TextButton(
                        onClick = { remove(photo.id) },
                        modifier = Modifier.testTag("edit-remove-$index"),
                    ) {
                        Text("Quitar")
                    }
                    if (index > 0)
                        TextButton(
                            onClick = { cover(photo.id) },
                            modifier = Modifier.testTag("edit-cover-$index"),
                        ) {
                            Text("Usar como portada")
                        }
                }
            }
        }
    }
}
