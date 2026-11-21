package com.example.huellitaschalco.feature.shelter.dogs.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*

/** Ficha pública por ID para comprobar la salud; el catálogo real se conecta en HU-02. */
@Composable
fun PublicDogDetailRoute(nav: UiNavigator, id: String, repository: DogRepository) {
    var retry by remember(id) { mutableIntStateOf(0) }
    val flow = remember(id, retry) { repository.observePublic(id) }
    val state by flow.collectAsStateWithLifecycle(PublicDog())
    HScaffold(nav, "Ficha pública") {
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> {
                HBanner(state.error!!, HSymbol.Alert, error = true)
                HButton("Reintentar", { retry++ }, outlined = true)
            }
            else ->
                state.dog?.let { dog ->
                    StoredDogPhoto(dog.photos.firstOrNull()?.path, repository)
                    HHeading(dog.name)
                    HTag(dog.status)
                    HBody("${dog.ageMonths} meses · ${dog.size} · ${dog.sex}")
                    HBody(dog.description)
                    DogHealthSummary(dog.health)
                    if (dog.demonstration) HCaption("Registro de demostración local.")
                }
        }
    }
}
