package com.example.huellitaschalco.core.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun HTag(text: String) {
    val warm = text in listOf("No", "No aprobada", "En proceso", "En revisión", "Esta semana")
    val color = if (warm) Terracotta else Forest
    val icon =
        when (text) {
            "No",
            "No aprobada" -> HSymbol.Close
            "En proceso",
            "Esta semana" -> HSymbol.Clock
            "En revisión" -> HSymbol.Eye
            "Enviada" -> HSymbol.Send
            "Adoptado",
            "Adopción realizada" -> HSymbol.Home
            else -> HSymbol.Check
        }
    Row(
        Modifier.clip(CircleShape)
            .background(if (warm) Peach else Sage)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HIcon(icon, Modifier.size(13.dp), color)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun HBanner(
    text: String,
    icon: HSymbol = HSymbol.Info,
    error: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (error) Peach else Sage)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HIcon(icon, Modifier.size(20.dp), if (error) Terracotta else Forest)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) Terracotta else Ink,
            )
            content?.invoke(this)
        }
    }
}

@Composable
fun HEmpty(
    title: String,
    message: String,
    icon: HSymbol = HSymbol.Inbox,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            Modifier.size(80.dp).background(Sage, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            HIcon(icon, Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        HButton(action, onAction)
    }
}

@Composable
fun HSuccessHeader(title: String, subtitle: String, icon: HSymbol = HSymbol.Check) {
    Column(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            Modifier.size(100.dp).background(Forest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            HIcon(icon, Modifier.size(46.dp), Color.White)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun HTimeline(events: List<Triple<String, String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        events.forEach { (title, date, detail) ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(36.dp).background(Sage, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    HIcon(
                        if (title == "Enviada") HSymbol.Send else HSymbol.Check,
                        Modifier.size(19.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    HCaption(date)
                    HBody(detail)
                }
            }
        }
    }
}
