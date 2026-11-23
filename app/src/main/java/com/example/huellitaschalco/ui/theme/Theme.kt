package com.example.huellitaschalco.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HuellitasColors =
    lightColorScheme(
        primary = Forest,
        onPrimary = Color.White,
        primaryContainer = Sage,
        onPrimaryContainer = Forest,
        secondary = Forest,
        onSecondary = Color.White,
        secondaryContainer = Sage,
        onSecondaryContainer = Forest,
        tertiary = Forest,
        onTertiary = Color.White,
        tertiaryContainer = Sage,
        onTertiaryContainer = Forest,
        background = Cream,
        onBackground = Ink,
        surface = Cream,
        onSurface = Ink,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Sage,
        onSurfaceVariant = Muted,
        outline = Line,
        outlineVariant = Line,
        error = Terracotta,
        onError = Color.White,
        errorContainer = Peach,
        onErrorContainer = Terracotta,
    )

@Composable
fun HuellitasChalcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HuellitasColors,
        typography = Typography,
        shapes =
            Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(30.dp),
            ),
        content = content,
    )
}
