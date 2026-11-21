package com.example.huellitaschalco.feature.catalog.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.DogRepository
import com.example.huellitaschalco.feature.shelter.dogs.presentation.StoredDogPhoto
import com.example.huellitaschalco.ui.theme.Line

@Composable
fun CatalogRoute(
    nav: UiNavigator,
    userName: String?,
    repository: CatalogRepository,
    photos: DogRepository,
) {
    val model: CatalogViewModel =
        viewModel(key = "public-catalog", factory = CatalogViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    HScaffold(
        nav,
        "Perritos en\nadopción en Chalco",
        if (userName != null) "Hola, ${userName.substringBefore(' ')}"
        else "Explora sin iniciar sesión",
        back = false,
        tab = "Explorar",
    ) {
        if (photos.usesEmulator) HCaption("Entorno de prueba local: registros de demostración.")
        HInput(
            "Buscar por nombre o refugio",
            state.filters.query,
            model::search,
            Modifier.testTag("catalog-search"),
        )
        HButton(
            "Ver filtros",
            { showFilters = true },
            Modifier.testTag("catalog-filters"),
            outlined = true,
            icon = HSymbol.Filter,
        )
        if (state.filters != CatalogFilters())
            HButton(
                "Limpiar búsqueda y filtros",
                model::clear,
                Modifier.testTag("catalog-clear"),
                outlined = true,
            )
        when {
            state.catalog.loading -> {
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("catalog-loading"))
                HBody("Consultando perritos disponibles…")
            }
            state.catalog.error != null ->
                Column(Modifier.testTag("catalog-error")) {
                    HBanner(state.catalog.error!!, HSymbol.Alert, error = true)
                    HButton(
                        "Reintentar",
                        model::retry,
                        Modifier.testTag("catalog-retry"),
                        outlined = true,
                    )
                }
            state.catalog.dogs.isEmpty() ->
                Column(Modifier.testTag("catalog-empty")) {
                    HEmpty(
                        "Todavía no hay perritos disponibles",
                        "Cuando un refugio activo de Chalco publique perritos, podrás conocerlos aquí.",
                        HSymbol.Home,
                        "Actualizar",
                        model::retry,
                    )
                }
            state.visible.isEmpty() ->
                Column(Modifier.testTag("catalog-no-results")) {
                    HBody("No encontramos perritos con esa búsqueda y filtros.")
                    HButton("Ver todos", model::clear, outlined = true)
                }
            else -> {
                HCaption("${state.visible.size} perritos buscan hogar")
                state.visible.forEach { item ->
                    Surface(
                        onClick = { nav.open(UiScreen.DogDetail, recordId = item.dog.id) },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Line),
                        modifier = Modifier.testTag("catalog-dog-${item.dog.id}"),
                    ) {
                        Column {
                            StoredDogPhoto(item.dog.photos.firstOrNull()?.path, photos)
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                HHeading(item.dog.name)
                                HBody(
                                    "${item.dog.ageMonths} meses · ${item.dog.size} · ${item.dog.sex}",
                                    muted = true,
                                )
                                HCaption(item.shelterName)
                                HTag(item.dog.status)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showFilters)
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Encuentra a tu compañero") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CatalogChoices(
                        "Tamaño",
                        "size",
                        listOf("Todos", "Chico", "Mediano", "Grande"),
                        state.filters.size,
                        model::size,
                    )
                    CatalogChoices(
                        "Sexo",
                        "sex",
                        listOf("Todos", "Hembra", "Macho"),
                        state.filters.sex,
                        model::sex,
                    )
                    CatalogChoices(
                        "Edad",
                        "age",
                        listOf("Todas", "Cachorro", "Adulto"),
                        state.filters.age,
                        model::age,
                    )
                    HCaption("Cachorro: menos de 12 meses. Adulto: desde 12 meses.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilters = false }) { Text("Ver catálogo") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        model.clear()
                        showFilters = false
                    }
                ) {
                    Text("Limpiar filtros")
                }
            },
        )
}

@Composable
private fun CatalogChoices(
    label: String,
    prefix: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(value) },
                modifier = Modifier.testTag("catalog-$prefix-$value"),
            )
        }
    }
}
