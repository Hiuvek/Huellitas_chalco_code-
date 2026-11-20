package com.example.huellitaschalco.feature.catalog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun CatalogScreen(nav: UiNavigator, destination: UiDestination, userName: String? = null) {
    var filters by remember { mutableStateOf(false) }
    val afterAdoption = destination.variant == UiVariant.AfterAdoption
    val dogs = PreviewData.dogs.filter { it.id <= 6 && (!afterAdoption || it.id != 2) }
    HScaffold(
        nav,
        "Perritos en\nadopción en Chalco",
        if (userName != null) "Hola, ${userName.substringBefore(' ')}"
        else "Explora sin iniciar sesión",
        back = false,
        tab = "Explorar",
    ) {
        if (destination.variant == UiVariant.NoCache) {
            HEmpty(
                "Sin conexión y sin catálogo guardado",
                "Todavía no hay perritos guardados en este teléfono. Conéctate a internet para cargar el catálogo; después podrás consultarlo sin conexión.",
                HSymbol.WifiOff,
                "Reintentar",
                { nav.open(UiScreen.Catalog) },
            )
        } else {
            if (destination.variant == UiVariant.Offline)
                HBanner(
                    "Sin conexión. Mostrando los perritos guardados en tu teléfono. Algunos datos podrían no estar actualizados.",
                    HSymbol.WifiOff,
                )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    "",
                    {},
                    placeholder = {
                        Text(
                            "Buscar por nombre o refugio",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingIcon = { HIcon(HSymbol.Search, tint = Muted) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    readOnly = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            unfocusedBorderColor = Line,
                        ),
                )
                OutlinedIconButton(
                    onClick = { filters = true },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, Line),
                ) {
                    HIcon(HSymbol.Filter, description = "Ver filtros")
                }
            }
            HCaption("${dogs.size} perritos buscan hogar")
            dogs.forEach { dog ->
                CatalogDogCard(dog) { nav.open(UiScreen.DogDetail, dogId = dog.id) }
            }
        }
    }
    if (filters)
        AlertDialog(
            onDismissRequest = { filters = false },
            title = { Text("Encuentra a tu compañero") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HChoices("Tamaño", listOf("Todos", "Chico", "Mediano", "Grande"))
                    HChoices("Sexo", listOf("Todos", "Hembra", "Macho"))
                    HChoices("Edad", listOf("Todas", "Cachorro", "Adulto"))
                }
            },
            confirmButton = { TextButton(onClick = { filters = false }) { Text("Ver catálogo") } },
            dismissButton = { TextButton(onClick = { filters = false }) { Text("Cerrar") } },
        )
}

@Composable
private fun CatalogDogCard(dog: DogPreview, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
    ) {
        Column {
            Box {
                Image(
                    painterResource(dog.image),
                    "Fotografía de ${dog.name}",
                    Modifier.fillMaxWidth().height(215.dp),
                    contentScale = ContentScale.Crop,
                )
                if (dog.status == "En proceso") Box(Modifier.padding(12.dp)) { HTag(dog.status) }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dog.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    HCaption(dog.sex)
                }
                HBody("${dog.age} · ${dog.size}", muted = true)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HIcon(HSymbol.Home, Modifier.size(14.dp), Muted)
                    HCaption(dog.refuge)
                }
            }
        }
    }
}
