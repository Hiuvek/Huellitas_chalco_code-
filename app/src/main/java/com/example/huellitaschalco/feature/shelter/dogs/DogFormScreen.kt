package com.example.huellitaschalco.feature.shelter.dogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun DogFormScreen(nav: UiNavigator, destination: UiDestination) {
    val editing = destination.variant == UiVariant.Editing
    val filled =
        editing ||
            destination.variant in
                listOf(
                    UiVariant.Photos,
                    UiVariant.PhotoError,
                    UiVariant.AiSuggestion,
                    UiVariant.AiError,
                )
    val dog = PreviewData.dog(if (editing) destination.dogId else 8)
    val errors = destination.variant == UiVariant.FieldError
    var count by rememberSaveable(destination.variant) { mutableIntStateOf(if (filled) 1 else 0) }
    var suggestion by rememberSaveable {
        mutableStateOf(destination.variant == UiVariant.AiSuggestion)
    }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var discarded by remember { mutableStateOf(false) }
    HScaffold(
        nav,
        if (editing) "Editar perrito" else "Registrar perrito",
        if (editing) "${dog.name} · ${dog.status}" else "Refugio 1",
        bottom = {
            HButton(
                if (editing) "Guardar cambios" else "Revisar antes de publicar",
                {
                    nav.open(
                        if (editing) UiScreen.ShelterDogDetail else UiScreen.DogPreview,
                        dogId = dog.id,
                    )
                },
                icon = if (editing) HSymbol.Save else HSymbol.Eye,
            )
            if (editing)
                HTextAction(
                    "Ver vista previa",
                    { nav.open(UiScreen.DogPreview, dogId = dog.id, variant = UiVariant.Editing) },
                    icon = HSymbol.Eye,
                )
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HIcon(HSymbol.Save, Modifier.size(16.dp))
            HCaption(
                if (editing) "Mostrando los datos actuales. Los cambios quedan como borrador."
                else "Borrador guardado en este teléfono"
            )
        }
        HHeading("Fotografías")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(count) { index ->
                Box(Modifier.size(92.dp)) {
                    HPhoto(dog, Modifier.fillMaxSize())
                    IconButton(
                        onClick = { count-- },
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .size(30.dp)
                                .background(Forest.copy(alpha = .8f), RoundedCornerShape(20.dp)),
                    ) {
                        HIcon(
                            HSymbol.Close,
                            Modifier.size(16.dp),
                            androidx.compose.ui.graphics.Color.White,
                            "Quitar fotografía",
                        )
                    }
                    if (index == 0)
                        Text(
                            "Portada",
                            style = MaterialTheme.typography.labelSmall,
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(5.dp)
                                    .background(Cream, RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                        )
                }
            }
            if (destination.variant == UiVariant.PhotoError)
                Surface(
                    onClick = { nav.open(UiScreen.DogForm, variant = UiVariant.Photos) },
                    Modifier.size(92.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Peach,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        HIcon(HSymbol.Alert, tint = Terracotta)
                        HCaption("No se pudo cargar")
                        Text(
                            "Reintentar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta,
                        )
                    }
                }
            if (count < 6)
                OutlinedButton(
                    onClick = { count++ },
                    modifier = Modifier.size(92.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (errors) Terracotta else Forest),
                    contentPadding = PaddingValues(5.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HIcon(HSymbol.Camera)
                        Text("Agregar", style = MaterialTheme.typography.labelSmall)
                    }
                }
        }
        HCaption("$count de 6 fotografías. Obligatoria al menos 1. La primera es la portada.")
        if (errors)
            Text(
                "Agrega al menos una fotografía antes de publicar.",
                style = MaterialTheme.typography.bodySmall,
                color = Terracotta,
            )
        HHeading("Datos generales")
        HInput(
            "Nombre",
            if (filled) dog.name else "",
            error = if (errors) "Escribe el nombre del perrito." else null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                HInput(
                    "Edad aproximada",
                    if (filled) dog.age.substringBefore(' ') else "",
                    keyboard = KeyboardType.Number,
                    error = if (errors) "Escribe una edad válida." else null,
                )
            }
            Column(Modifier.weight(1f).padding(top = 27.dp)) {
                HChoices(
                    null,
                    listOf("Meses", "Años"),
                    if (dog.age.contains("mes")) "Meses" else "Años",
                )
            }
        }
        HChoices("Tamaño", listOf("Chico", "Mediano", "Grande"), dog.size)
        HChoices("Sexo", listOf("Hembra", "Macho"), dog.sex)
        HHeading("Salud")
        HChoices(
            "Vacunación",
            listOf("Sí", "No", "Sin información"),
            if (filled) dog.vaccinated else "Sin información",
        )
        HChoices(
            "Desparasitación",
            listOf("Sí", "No", "Sin información"),
            if (filled) dog.dewormed else "Sin información",
        )
        HChoices(
            "Esterilización",
            listOf("Sí", "No", "Sin información"),
            if (filled) dog.sterilized else "Sin información",
        )
        HHeading("Descripción")
        key(accepted) {
            HInput(
                "Cuéntanos cómo es",
                if (filled && !suggestion || accepted) dog.description else "",
                placeholder = "Carácter, hábitos, con quién convive bien…",
                helper = "Mínimo 40 caracteres.",
                lines = 5,
                error = if (errors) "La descripción debe tener al menos 40 caracteres." else null,
            )
        }
        if (suggestion && !accepted)
            HBanner(
                "Sugerencia para revisar. Basada únicamente en los datos capturados. No se publica automáticamente.",
                HSymbol.Sparkles,
                true,
            ) {
                HBody(dog.description)
                HButton("Usar descripción", { accepted = true }, icon = HSymbol.Check)
                HButton(
                    "Editar propuesta",
                    { accepted = true },
                    outlined = true,
                    icon = HSymbol.Edit,
                )
                HTextAction("Descartar", { suggestion = false })
            }
        else
            HButton(
                "Ayudarme a redactar",
                {
                    suggestion = true
                    accepted = false
                },
                icon = HSymbol.Sparkles,
                danger = true,
            )
        if (destination.variant == UiVariant.AiError)
            HBanner(
                "No se pudo preparar la sugerencia. Tu descripción no cambió.",
                HSymbol.Alert,
                true,
            ) {
                HButton(
                    "Reintentar",
                    { suggestion = true },
                    outlined = true,
                    icon = HSymbol.Refresh,
                )
                HTextAction("Escribir manualmente", { accepted = true })
            }
        HCaption(
            "Prepara una sugerencia usando solo los datos que capturaste. Tú decides si la usas; nunca se publica sola."
        )
        if (errors)
            HBanner(
                "Completa los campos obligatorios marcados antes de continuar.",
                HSymbol.Alert,
                true,
            )
        HTextAction(
            if (editing) "Descartar cambios" else "Descartar registro",
            { discarded = true },
            danger = true,
            icon = HSymbol.Close,
        )
    }
    if (discarded)
        AlertDialog(
            onDismissRequest = { discarded = false },
            title = { Text("¿Descartar este borrador?") },
            text = { Text("Puedes regresar al listado del refugio.") },
            confirmButton = {
                TextButton(onClick = { nav.root(UiScreen.ShelterDogs) }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { discarded = false }) { Text("Continuar editando") }
            },
        )
}
