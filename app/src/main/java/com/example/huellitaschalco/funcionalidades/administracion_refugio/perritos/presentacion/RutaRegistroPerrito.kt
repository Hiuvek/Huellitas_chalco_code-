package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.presentacion

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
import com.example.huellitaschalco.funcionalidades.administracion_refugio.descripcion.dominio.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.descripcion.presentacion.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RutaRegistroPerrito(
    nav: NavegadorInterfaz,
    uid: String,
    shelterId: String,
    repository: RepositorioPerritos,
    importer: ImportadorFotoPerrito,
    drafts: com.example.huellitaschalco.nucleo.local.AlmacenBorradores? = null,
    generator: GeneradorDescripcion? = null,
) {
    val model: ModeloVistaRegistroPerrito =
        viewModel(
            key = "dog-registration-$uid-$shelterId",
            factory = ModeloVistaRegistroPerrito.factory(repository, importer, uid, shelterId, drafts),
        )
    val state by model.state.collectAsStateWithLifecycle()
    if (state.restoring) {
        HScaffold(nav, "Recuperando borrador", shelter = true) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        return
    }
    var assistantOpen by remember(uid, shelterId) { mutableStateOf(false) }
    val assistant: ModeloVistaAsistenteDescripcion? =
        if (generator != null)
            viewModel(
                key = "description-assistant-$uid-$shelterId",
                factory = ModeloVistaAsistenteDescripcion.factory(generator),
            )
        else null
    if (assistantOpen && assistant != null)
        DialogoAsistenteDescripcion(
            assistant,
            { model.edit(CampoPerrito.Description, it) },
            { assistantOpen = false },
        )
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            model.addPhotos(listOfNotNull(uri?.toString()))
        }
    LaunchedEffect(state.published?.id) {
        if (state.published != null && nav.current.screen != PantallaInterfaz.DogPublished)
            nav.open(PantallaInterfaz.DogPublished)
    }
    LaunchedEffect(state.review) {
        if (state.review && nav.current.screen == PantallaInterfaz.DogForm) nav.open(PantallaInterfaz.DogPreview)
    }
    BackHandler(
        enabled = nav.current.screen in listOf(PantallaInterfaz.DogPreview, PantallaInterfaz.DogPublished)
    ) {
        if (!state.publishing) {
            if (nav.current.screen == PantallaInterfaz.DogPublished) {
                model.reset()
                nav.root(PantallaInterfaz.ShelterDogs)
            } else if (!state.locked) {
                model.returnToForm()
                nav.back()
            } else nav.root(PantallaInterfaz.ShelterDogs)
        }
    }
    when (nav.current.screen) {
        PantallaInterfaz.DogPublished -> {
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
                            nav.root(PantallaInterfaz.ShelterDogs)
                        },
                    )
                    HButton(
                        "Registrar otro perrito",
                        {
                            model.reset()
                            nav.root(PantallaInterfaz.ShelterDogs)
                            nav.open(PantallaInterfaz.DogForm)
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
                    HCaption("El registro está disponible en el catálogo de adopción.")
                } else
                    HBody(
                        "No hay una publicación confirmada en esta sesión. Consulta Mis perritos para revisar los registros guardados."
                    )
            }
        }
        PantallaInterfaz.DogPreview ->
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
                        enabled =
                            !state.publishing &&
                                repository.isConfigured &&
                                state.review &&
                                !state.draftBlocked,
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
                    HCaption("Estado inicial: Disponible.")
                    DogHealthSummary(state.draft.health)
                    if (state.publishing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
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
                    "El borrador se guarda en este dispositivo y se separa por cuenta y refugio. No se publica automáticamente."
                )
                if (!repository.isConfigured)
                    HBanner(
                        "La carga de fotografías aún no está habilitada en esta instalación. Puedes preparar el formulario; la publicación está pendiente de configurar Firebase.",
                        SimboloHuellitas.Info,
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
                    icon = SimboloHuellitas.Camera,
                    enabled = !state.importing && !state.locked && state.draft.photos.size < 1,
                )
                HCaption(
                    "${state.draft.photos.size} de 1 fotografía. Obligatoria. Se comprime a un máximo de 100 KB. JPEG, PNG o WebP, hasta 20 MB por imagen."
                )
                state.errors[CampoPerrito.Photos]?.let { HBody(it) }
                val editable = !state.locked && !state.importing
                HInput(
                    "Nombre",
                    state.draft.name,
                    { model.edit(CampoPerrito.Name, it) },
                    Modifier.testTag("dog-name"),
                    error = state.errors[CampoPerrito.Name],
                    enabled = editable,
                )
                HInput(
                    "Edad aproximada",
                    state.draft.age,
                    { model.edit(CampoPerrito.Age, it) },
                    Modifier.testTag("dog-age"),
                    error = state.errors[CampoPerrito.Age],
                    keyboard = KeyboardType.Number,
                    enabled = editable,
                )
                DogChoices("Unidad", listOf("Meses", "Años"), state.draft.ageUnit, editable) {
                    model.edit(CampoPerrito.AgeUnit, it)
                }
                DogChoices(
                    "Tamaño",
                    listOf("Chico", "Mediano", "Grande"),
                    state.draft.size,
                    editable,
                ) {
                    model.edit(CampoPerrito.Size, it)
                }
                state.errors[CampoPerrito.Size]?.let { HBody(it) }
                DogChoices("Sexo", listOf("Hembra", "Macho"), state.draft.sex, editable) {
                    model.edit(CampoPerrito.Sex, it)
                }
                state.errors[CampoPerrito.Sex]?.let { HBody(it) }
                HInput(
                    "Descripción",
                    state.draft.description,
                    { model.edit(CampoPerrito.Description, it) },
                    Modifier.testTag("dog-description"),
                    helper = "De 40 a 2000 caracteres. Describe solo características que conozcas.",
                    error = state.errors[CampoPerrito.Description],
                    lines = 5,
                    enabled = editable,
                )
                if (assistant != null)
                    HButton(
                        "Redactar con Gemini",
                        {
                            assistant.open(DatosDescripcion.fromDraft(state.draft))
                            assistantOpen = true
                        },
                        Modifier.testTag("dog-ai"),
                        outlined = true,
                        enabled = editable,
                    )
                state.draftMessage?.let { HCaption(it) }
                HHeading("Salud")
                DogHealthChoices(state.draft.health, editable, "register-health", model::editHealth)
                state.error?.let { HBanner(it, SimboloHuellitas.Alert, error = true) }
                if (!state.locked && !state.importing)
                    HTextAction(
                        "Descartar registro",
                        {
                            model.reset()
                            nav.root(PantallaInterfaz.ShelterDogs)
                        },
                        danger = true,
                    )
                if (state.locked)
                    HButton(
                        "Volver a la publicación",
                        { nav.open(PantallaInterfaz.DogPreview) },
                        outlined = true,
                    )
            }
    }
}

@Composable
internal fun DogChoices(
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
internal fun PhotoStrip(
    photos: List<FotoLocalPerrito>,
    remove: ((String) -> Unit)? = null,
    showCaption: Boolean = true,
) {
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
                if (showCaption) HCaption(if (index == 0) "Portada" else "Fotografía ${index+1}")
                if (remove != null) TextButton(onClick = { remove(photo.id) }) { Text("Quitar") }
            }
        }
    }
}
