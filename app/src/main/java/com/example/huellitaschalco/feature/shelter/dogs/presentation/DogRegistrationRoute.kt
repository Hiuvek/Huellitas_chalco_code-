package com.example.huellitaschalco.feature.shelter.dogs.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DogRegistrationRoute(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    repository: DogRepository,
    importer: DogPhotoImporter,
) {
    val model: DogRegistrationViewModel =
        viewModel(
            key = "dog-registration-$uid-$shelterId",
            factory = DogRegistrationViewModel.factory(repository, importer, uid, shelterId),
        )
    val state by model.state.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris
            ->
            model.addPhotos(uris.map { it.toString() })
        }
    LaunchedEffect(state.published?.id) {
        if (state.published != null && nav.current.screen != UiScreen.DogPublished)
            nav.open(UiScreen.DogPublished)
    }
    LaunchedEffect(state.review) {
        if (state.review && nav.current.screen == UiScreen.DogForm) nav.open(UiScreen.DogPreview)
    }
    BackHandler(
        enabled = nav.current.screen in listOf(UiScreen.DogPreview, UiScreen.DogPublished)
    ) {
        if (!state.publishing) {
            if (nav.current.screen == UiScreen.DogPublished) {
                model.reset()
                nav.root(UiScreen.ShelterDogs)
            } else if (!state.locked) {
                model.returnToForm()
                nav.back()
            } else nav.root(UiScreen.ShelterDogs)
        }
    }
    when (nav.current.screen) {
        UiScreen.DogPublished -> {
            val dog = state.published
            HScaffold(
                nav,
                "Publicación",
                back = false,
                bottom = {
                    HButton(
                        "Ver en Mis perritos",
                        {
                            model.reset()
                            nav.root(UiScreen.ShelterDogs)
                        },
                    )
                    HButton(
                        "Registrar otro perrito",
                        {
                            model.reset()
                            nav.root(UiScreen.ShelterDogs)
                            nav.open(UiScreen.DogForm)
                        },
                        outlined = true,
                    )
                },
            ) {
                if (dog != null) {
                    HSuccessHeader(
                        "¡Perrito publicado!",
                        "${dog.name} se guardó correctamente en tu refugio.",
                    )
                    HBody("${dog.ageMonths} meses · ${dog.size} · ${dog.sex}")
                    HCaption("Folio: ${dog.id}")
                    HCaption(
                        if (repository.usesEmulator)
                            "Publicación de prueba local. No aparece en Firebase real."
                        else
                            "El registro está disponible para el catálogo. La consulta pública se conectará en HU-02."
                    )
                } else
                    HBody(
                        "No hay una publicación confirmada en esta sesión. Consulta Mis perritos para revisar los registros guardados."
                    )
            }
        }
        UiScreen.DogPreview ->
            HScaffold(
                nav,
                "Revisar antes de publicar",
                back = false,
                bottom = {
                    HButton(
                        if (state.publishing) state.progress
                        else if (state.error != null) "Reintentar publicación"
                        else "Publicar perrito",
                        model::publish,
                        Modifier.testTag("dog-publish"),
                        enabled = !state.publishing && repository.isConfigured && state.review,
                    )
                    if (!state.locked)
                        HButton(
                            "Volver a editar",
                            {
                                model.returnToForm()
                                nav.back()
                            },
                            outlined = true,
                        )
                },
            ) {
                if (state.review) {
                    PhotoStrip(state.draft.photos)
                    HHeading(state.draft.name.trim())
                    HBody(
                        "${state.draft.ageMonths()} meses · ${state.draft.size} · ${state.draft.sex}"
                    )
                    HBody(state.draft.description.trim())
                    HCaption("Estado inicial: Disponible. Salud: Sin información.")
                    if (repository.usesEmulator)
                        HBanner(
                            "Prueba local: los datos y fotografías se guardan únicamente en los emuladores.",
                            HSymbol.Info,
                        )
                    if (state.publishing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                    if (state.locked && !state.publishing)
                        HCaption(
                            "Reintenta con los mismos datos para evitar publicaciones duplicadas."
                        )
                } else HBody("Completa y revisa el formulario antes de publicar.")
            }
        else ->
            HScaffold(
                nav,
                "Registrar perrito",
                bottom = {
                    HButton(
                        if (state.importing) "Preparando fotografías…"
                        else "Revisar antes de publicar",
                        model::review,
                        Modifier.testTag("dog-review"),
                        enabled = !state.importing && !state.locked,
                    )
                },
            ) {
                HCaption(
                    "Los datos se conservan durante esta sesión. El guardado de borradores con Room se implementará en HU-17."
                )
                if (!repository.isConfigured)
                    HBanner(
                        "La carga de fotografías aún no está habilitada en esta instalación. Puedes preparar el formulario; la publicación está pendiente de configurar Storage.",
                        HSymbol.Info,
                    )
                HHeading("Fotografías")
                PhotoStrip(state.draft.photos, if (state.locked) null else model::removePhoto)
                HButton(
                    "Agregar fotografías",
                    {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    Modifier.testTag("dog-add-photos"),
                    outlined = true,
                    icon = HSymbol.Camera,
                    enabled = !state.importing && !state.locked && state.draft.photos.size < 6,
                )
                HCaption(
                    "${state.draft.photos.size} de 6 fotografías. Al menos una obligatoria; la primera es la portada. JPEG, PNG o WebP, hasta 20 MB por imagen."
                )
                state.errors[DogField.Photos]?.let { HBody(it) }
                val editable = !state.locked && !state.importing
                HInput(
                    "Nombre",
                    state.draft.name,
                    { model.edit(DogField.Name, it) },
                    Modifier.testTag("dog-name"),
                    error = state.errors[DogField.Name],
                    enabled = editable,
                )
                HInput(
                    "Edad aproximada",
                    state.draft.age,
                    { model.edit(DogField.Age, it) },
                    Modifier.testTag("dog-age"),
                    error = state.errors[DogField.Age],
                    keyboard = KeyboardType.Number,
                    enabled = editable,
                )
                DogChoices("Unidad", listOf("Meses", "Años"), state.draft.ageUnit, editable) {
                    model.edit(DogField.AgeUnit, it)
                }
                DogChoices(
                    "Tamaño",
                    listOf("Chico", "Mediano", "Grande"),
                    state.draft.size,
                    editable,
                ) {
                    model.edit(DogField.Size, it)
                }
                state.errors[DogField.Size]?.let { HBody(it) }
                DogChoices("Sexo", listOf("Hembra", "Macho"), state.draft.sex, editable) {
                    model.edit(DogField.Sex, it)
                }
                state.errors[DogField.Sex]?.let { HBody(it) }
                HInput(
                    "Descripción",
                    state.draft.description,
                    { model.edit(DogField.Description, it) },
                    Modifier.testTag("dog-description"),
                    helper = "De 40 a 2000 caracteres. Describe solo características que conozcas.",
                    error = state.errors[DogField.Description],
                    lines = 5,
                    enabled = editable,
                )
                HCaption(
                    "La captura del estado de salud se conectará en HU-09. Por ahora se guarda como Sin información."
                )
                state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                if (!state.locked && !state.importing)
                    HTextAction(
                        "Descartar registro",
                        {
                            model.reset()
                            nav.root(UiScreen.ShelterDogs)
                        },
                        danger = true,
                    )
                if (state.locked)
                    HButton(
                        "Volver a la publicación",
                        { nav.open(UiScreen.DogPreview) },
                        outlined = true,
                    )
            }
    }
}

@Composable
private fun DogChoices(
    label: String,
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column {
        HHeading(label)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun PhotoStrip(photos: List<LocalDogPhoto>, remove: ((String) -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        photos.forEachIndexed { index, photo ->
            Column {
                val image by
                    produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, photo.filePath) {
                        value =
                            withContext(Dispatchers.IO) {
                                BitmapFactory.decodeFile(photo.filePath)?.asImageBitmap()
                            }
                    }
                image?.let {
                    Image(
                        it,
                        "Fotografía ${index+1}",
                        Modifier.size(110.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                HCaption(if (index == 0) "Portada" else "Fotografía ${index+1}")
                if (remove != null) TextButton(onClick = { remove(photo.id) }) { Text("Quitar") }
            }
        }
    }
}
