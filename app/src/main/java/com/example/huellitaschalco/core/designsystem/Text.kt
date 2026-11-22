package com.example.huellitaschalco.core.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun HHeading(text: String) {
    Text(
        text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = Ink,
    )
}

@Composable
fun HBody(text: String, muted: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (muted) Muted else Ink)
}

@Composable
fun HCaption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Muted)
}

/** Encabezado y orientación breve para distinguir la tarea de los datos del registro. */
@Composable
fun HScreenIntro(title: String, instruction: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
        )
        HBody(instruction, muted = true)
    }
}

/** Acción visible dentro de una tarjeta que se puede tocar por completo. */
@Composable
fun HCardAction(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = Forest,
        )
        HIcon(HSymbol.Forward, Modifier.padding(start = 8.dp).size(20.dp), tint = Forest)
    }
}
