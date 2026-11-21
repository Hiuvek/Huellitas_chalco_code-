package com.example.huellitaschalco.feature.shelter.dogs.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Consulta mínima para comprobar el alta de HU-08; la edición queda pendiente de HU-10. */
@Composable
fun RegisteredDogsRoute(nav: UiNavigator, shelterId: String, repository: DogRepository) {
    var refresh by remember { mutableIntStateOf(0) }
    val flow = remember(shelterId, refresh) { repository.observe(shelterId) }
    val state by flow.collectAsStateWithLifecycle(ShelterDogs())
    val recordId = nav.current.recordId
    val selected = state.dogs.firstOrNull { it.id == recordId }
    HScaffold(
        nav,
        if (recordId == null) "Mis perritos" else selected?.name ?: "Registro del perrito",
        back = recordId != null,
        shelter = true,
        tab = if (recordId == null) "Perritos" else null,
    ) {
        if (recordId == null)
            HButton("Registrar perrito", { nav.open(UiScreen.DogForm) }, icon = HSymbol.Plus)
        if (repository.usesEmulator)
            HCaption("Entorno de prueba local: registros ficticios en emuladores.")
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HBanner(state.error!!, HSymbol.Alert, error = true)
                HButton("Reintentar", { refresh++ }, outlined = true)
            }
            recordId != null && selected == null ->
                HBody("No encontramos este registro en el refugio seleccionado.")
            selected != null -> {
                StoredDogPhoto(selected.photos.firstOrNull()?.path, repository)
                HTag("Disponible")
                HHeading(selected.name)
                HBody("${selected.ageMonths} meses · ${selected.size} · ${selected.sex}")
                HBody(selected.description)
                HCaption("Folio: ${selected.id}")
                HCaption(
                    "Salud: Sin información. La edición y el estado de salud se conectarán en sus siguientes historias."
                )
            }
            state.dogs.isEmpty() -> HBody("Todavía no has publicado perritos en este refugio.")
            else ->
                state.dogs.forEach { dog ->
                    HCard(onClick = { nav.open(UiScreen.ShelterDogDetail, recordId = dog.id) }) {
                        StoredDogPhoto(dog.photos.firstOrNull()?.path, repository)
                        HHeading(dog.name)
                        HBody("${dog.ageMonths} meses · ${dog.size} · ${dog.sex}")
                        HTag("Disponible")
                    }
                }
        }
    }
}

@Composable
private fun StoredDogPhoto(path: String?, repository: DogRepository) {
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
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failed = true
                        null
                    }
        }
    image?.let {
        Image(
            it,
            "Fotografía del perrito",
            Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop,
        )
    }
    if (failed) TextButton(onClick = { retry++ }) { Text("Reintentar fotografía") }
}
