package com.example.huellitaschalco.feature.shelter.description.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.core.designsystem.*

@Composable
fun DescriptionAssistantDialog(
    model: DescriptionAssistantViewModel,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = {
            model.close()
            onDismiss()
        },
        title = { HHeading("Redactar con Gemini") },
        text = {
            Column(
                Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.input?.let {
                    HCaption("${it.name} · ${it.ageMonths} meses · ${it.size} · ${it.sex}")
                }
                HBody(
                    "Gemini recibe los datos de esta ficha y el texto escrito. Revisa que la propuesta no agregue hechos. No se publica automáticamente."
                )
                state.proposal?.let {
                    HInput(
                        "Propuesta editable",
                        it,
                        model::edit,
                        Modifier.testTag("ai-proposal"),
                        lines = 5,
                        enabled = !state.generating,
                    )
                }
                state.error?.let { HBanner(it, HSymbol.Alert, error = true) }
                HButton(
                    if (state.generating) "Generando…"
                    else if (state.proposal != null) "Generar otra propuesta"
                    else "Generar propuesta",
                    model::generate,
                    Modifier.testTag("ai-generate"),
                    outlined = true,
                    enabled = !state.generating && state.input != null,
                )
            }
        },
        confirmButton = {
            if (state.proposal != null)
                HButton(
                    "Usar propuesta",
                    {
                        model.selected()?.let {
                            onAccept(it)
                            model.close()
                            onDismiss()
                        }
                    },
                    Modifier.testTag("ai-accept"),
                    enabled = !state.generating,
                )
        },
        dismissButton = {
            HButton(
                if (state.proposal != null) "Descartar propuesta" else "Escribir manualmente",
                {
                    model.close()
                    onDismiss()
                },
                Modifier.testTag("ai-discard"),
                outlined = true,
            )
        },
    )
}
