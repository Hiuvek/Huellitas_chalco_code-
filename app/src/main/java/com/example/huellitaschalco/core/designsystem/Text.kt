package com.example.huellitaschalco.core.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun HHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun HBody(text: String, muted: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (muted) Muted else Ink)
}

@Composable
fun HCaption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Muted)
}
