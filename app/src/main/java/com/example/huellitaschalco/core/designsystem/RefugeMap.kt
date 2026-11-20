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
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

@Composable
fun HMap(error: Boolean = false, showAction: Boolean = true, onOpen: () -> Unit) {
    HCard {
        if (error) {
            Column(
                Modifier.fillMaxWidth().height(140.dp).background(Sage, RoundedCornerShape(12.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HIcon(HSymbol.Pin, tint = Terracotta)
                HHeading("No se pudo cargar el mapa")
                HCaption("Los datos del refugio siguen disponibles.")
            }
            HButton("Reintentar", onOpen, outlined = true, icon = HSymbol.Refresh)
        } else {
            Box(
                Modifier.fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3E6DB)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val road = Color.White
                    listOf(.13f, .49f, .78f).forEach { f ->
                        drawLine(
                            road,
                            androidx.compose.ui.geometry.Offset(size.width * f, 0f),
                            androidx.compose.ui.geometry.Offset(
                                size.width * (f + .15f),
                                size.height,
                            ),
                            9f,
                        )
                    }
                    listOf(.22f, .58f, .87f).forEach { f ->
                        drawLine(
                            road,
                            androidx.compose.ui.geometry.Offset(0f, size.height * f),
                            androidx.compose.ui.geometry.Offset(
                                size.width,
                                size.height * (f - .12f),
                            ),
                            10f,
                        )
                    }
                    drawCircle(Terracotta.copy(alpha = .12f), 48.dp.toPx())
                    drawCircle(
                        Terracotta.copy(alpha = .3f),
                        48.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                    )
                }
                HIcon(HSymbol.Pin, Modifier.size(34.dp), Terracotta)
                Text(
                    "Mapa ilustrativo",
                    style = MaterialTheme.typography.labelSmall,
                    modifier =
                        Modifier.align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.White, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            HCaption(
                "Ubicación pública aproximada. La dirección exacta se comparte al agendar tu visita."
            )
            if (showAction) HTextAction("Ver información del refugio", onOpen, icon = HSymbol.Pin)
        }
    }
}
