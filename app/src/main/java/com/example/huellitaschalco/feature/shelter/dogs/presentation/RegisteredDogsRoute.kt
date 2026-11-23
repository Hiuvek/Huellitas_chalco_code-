package com.example.huellitaschalco.feature.shelter.dogs.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Administración de fichas, salud y eliminación de la lista del refugio. */
@Composable
fun RegisteredDogsRoute(
    nav: UiNavigator,
    uid: String,
    shelterId: String,
    repository: DogRepository,
    importer: DogPhotoImporter,
    generator:
        com.example.huellitaschalco.feature.shelter.description.domain.DescriptionGenerator? =
        null,
) {
    var refresh by remember { mutableIntStateOf(0) }
    val flow = remember(shelterId, refresh) { repository.observe(shelterId) }
    val state by flow.collectAsStateWithLifecycle(ShelterDogs())
    val recordId = nav.current.recordId
    val selected = state.dogs.firstOrNull { it.id == recordId }
    val removal: DogRemovalViewModel =
        viewModel(
            key = "remove-$uid-$shelterId-$recordId",
            factory = DogRemovalViewModel.factory(repository, uid, shelterId, recordId ?: ""),
        )
    val removalState by removal.state.collectAsStateWithLifecycle()
    LaunchedEffect(removalState.removed) {
        if (removalState.removed) nav.root(UiScreen.ShelterDogs)
    }
    var editing by rememberSaveable(uid, shelterId, recordId) { mutableStateOf(false) }
    var session by rememberSaveable(uid, shelterId, recordId) { mutableIntStateOf(0) }
    if (editing && selected != null) {
        DogEditRoute(nav, uid, selected, session, repository, importer, generator) {
            editing = false
        }
        return
    }
    HScaffold(
        nav,
        if (recordId == null) "Mis perritos" else selected?.name ?: "Registro del perrito",
        back = recordId != null,
        shelter = true,
        tab = if (recordId == null) "Perritos" else null,
    ) {
        if (recordId == null)
            HScreenIntro(
                "Administra tus perritos",
                "Registra uno nuevo o abre una tarjeta para revisar sus datos, fotografías y salud.",
            )
        if (recordId == null)
            HButton("Registrar perrito", { nav.open(UiScreen.DogForm) }, icon = HSymbol.Plus)
        if (repository.usesEmulator)
            HCaption("Entorno de prueba local: registros ficticios en emuladores.")
        when {
            removalState.removing || removalState.removed -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                HBody("Eliminando de la lista…")
            }
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HBanner(state.error!!, HSymbol.Alert, error = true)
                HButton("Reintentar", { refresh++ }, outlined = true)
            }
            recordId != null && selected == null ->
                HBody("No encontramos este registro en el refugio seleccionado.")
            selected != null -> {
                StoredDogPhoto(
                    selected.photos.firstOrNull()?.path,
                    repository,
                    showFullPhoto = true,
                )
                HTag(selected.status)
                HHeading(selected.name)
                HBody("${selected.ageMonths} meses · ${selected.size} · ${selected.sex}")
                HBody(selected.description)
                HCaption("Folio: ${selected.id}")
                DogHealthEditor(uid, selected, repository)
                HButton(
                    "Ver ficha pública",
                    { nav.open(UiScreen.DogDetail, recordId = selected.id) },
                    outlined = true,
                )
                HButton(
                    "Editar perrito",
                    {
                        session++
                        editing = true
                    },
                    Modifier.testTag("dog-edit"),
                    icon = HSymbol.Edit,
                )
                HorizontalDivider()
                HButton(
                    "Eliminar de la lista",
                    { removal.ask(selected.name) },
                    Modifier.testTag("dog-remove"),
                    outlined = true,
                    danger = true,
                    enabled = selected.status != "En proceso",
                )
                HCaption(
                    if (selected.status == "En proceso")
                        "Tiene una adopción en proceso. Completa la entrega antes de retirarlo de la lista."
                    else "Se conservarán las solicitudes y el historial relacionados."
                )
            }
            state.dogs.isEmpty() -> HBody("Todavía no has publicado perritos en este refugio.")
            else ->
                state.dogs.forEach { dog ->
                    HCard(onClick = { nav.open(UiScreen.ShelterDogDetail, recordId = dog.id) }) {
                        StoredDogPhoto(dog.photos.firstOrNull()?.path, repository)
                        HHeading(dog.name)
                        HBody("${dog.ageMonths} meses · ${dog.size} · ${dog.sex}")
                        HTag(dog.status)
                        HCardAction("Ver datos y salud")
                    }
                }
        }
    }
    if (removalState.confirming)
        AlertDialog(
            onDismissRequest = removal::cancel,
            title = { Text("¿Eliminar a ${removalState.name} de la lista?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HBody(
                        "Se quitará de Mis perritos y, si está disponible, del catálogo público. Esta acción no se puede deshacer desde la app."
                    )
                    HBody(
                        "Las solicitudes, adopciones y seguimientos se conservan. Las solicitudes pendientes deben resolverse desde Solicitudes."
                    )
                    if (removalState.removing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    removalState.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = removal::confirm,
                    enabled = !removalState.removing,
                    modifier = Modifier.testTag("dog-remove-confirm"),
                ) {
                    Text(
                        if (removalState.removing) "Eliminando…" else "Eliminar de la lista",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = removal::cancel,
                    enabled = !removalState.removing,
                    modifier = Modifier.testTag("dog-remove-cancel"),
                ) {
                    Text("Conservar perrito")
                }
            },
        )
}

@Composable
internal fun StoredDogPhoto(
    path: String?,
    repository: DogRepository,
    showFullPhoto: Boolean = false,
) {
    key(path) { StoredDogPhotoContent(path, repository, showFullPhoto) }
}

@Composable
private fun StoredDogPhotoContent(
    path: String?,
    repository: DogRepository,
    showFullPhoto: Boolean,
) {
    var retry by remember(path) { mutableIntStateOf(0) }
    var failed by remember(path, retry) { mutableStateOf(false) }
    val image by
        produceState<ImageBitmap?>(null, path, retry) {
            value =
                if (path == null) null
                else
                    try {
                        withContext(Dispatchers.IO) {
                            val bytes = repository.photo(path)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                ?: error("La fotografía no es una imagen válida")
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failed = true
                        null
                    }
        }
    // Reserva el espacio para que una imagen tardía no cambie la tarjeta bajo un toque.
    Box(
        Modifier.fillMaxWidth().height(if (showFullPhoto) 320.dp else 200.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            image != null ->
                Image(
                    requireNotNull(image),
                    "Fotografía del perrito",
                    Modifier.fillMaxSize(),
                    contentScale = if (showFullPhoto) ContentScale.Fit else ContentScale.Crop,
                )
            failed -> TextButton(onClick = { retry++ }) { Text("Reintentar fotografía") }
            path == null -> HCaption("Sin fotografía")
            else -> CircularProgressIndicator()
        }
    }
}
