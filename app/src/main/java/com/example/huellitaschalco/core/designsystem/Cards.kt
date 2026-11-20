package com.example.huellitaschalco.core.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.core.preview.DogPreview
import com.example.huellitaschalco.ui.theme.*

@Composable
fun HCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .then(click)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun HPhoto(dog: DogPreview, modifier: Modifier = Modifier) {
    Image(
        painterResource(dog.image),
        contentDescription = "Fotografía de ${dog.name}",
        modifier = modifier.clip(RoundedCornerShape(13.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun HDogRow(
    dog: DogPreview,
    subtitle: String = "${dog.age} · ${dog.size} · ${dog.sex}",
    status: String? = null,
    onClick: (() -> Unit)? = null,
) {
    HCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HPhoto(dog, Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                HHeading(dog.name)
                HCaption(subtitle)
                if (status != null) HTag(status)
            }
            if (onClick != null) HIcon(HSymbol.Chevron, Modifier.size(17.dp), Muted)
        }
    }
}

@Composable
fun HKeyValue(label: String, value: String, divider: Boolean = false) {
    if (divider) HorizontalDivider(color = Line)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun HMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Sage).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HCaption(label)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun HHealth(dog: DogPreview) {
    HCard {
        listOf(
                "Vacunación" to dog.vaccinated,
                "Desparasitación" to dog.dewormed,
                "Esterilización" to dog.sterilized,
            )
            .forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(color = Line)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    HTag(value)
                }
            }
    }
}
