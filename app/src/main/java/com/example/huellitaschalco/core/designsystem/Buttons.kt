package com.example.huellitaschalco.core.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun HButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: HSymbol? = null,
    outlined: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val color = if (danger) Terracotta else Forest
    val contents: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            HIcon(icon, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
    }
    if (outlined)
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, if (enabled) color else Line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            content = contents,
        )
    else
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            content = contents,
        )
}

@Composable
fun HTextAction(text: String, onClick: () -> Unit, danger: Boolean = false, icon: HSymbol? = null) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) {
            HIcon(icon, tint = if (danger) Terracotta else Forest, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = if (danger) Terracotta else Forest,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
