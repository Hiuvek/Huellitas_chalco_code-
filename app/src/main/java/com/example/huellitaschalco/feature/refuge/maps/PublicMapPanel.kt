package com.example.huellitaschalco.feature.refuge.maps

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.feature.refuge.domain.*

@Composable
fun PublicMapPanel(shelter: PublicShelter, launcher: PublicMapLauncher? = null) {
    val context = LocalContext.current
    val actualLauncher =
        launcher ?: remember(context) { AndroidPublicMapLauncher(context.applicationContext) }
    val url = remember(shelter) { shelter.googleMapsUrl() }
    var error by remember(shelter.id, url) { mutableStateOf(false) }
    HCard {
        HHeading("Ubicación pública aproximada")
        HBody("${shelter.zone}, ${shelter.municipality}, ${shelter.state}")
        if (url == null) HBody("El refugio todavía no ha publicado una ubicación para Google Maps.")
        else {
            HCaption(
                "Se abrirá Google Maps o el navegador con la zona pública. Confirma la visita con el refugio."
            )
            if (error)
                HBanner(
                    "No se pudo abrir Google Maps. Conservamos la zona del refugio; puedes reintentar.",
                    HSymbol.Alert,
                    error = true,
                )
            HButton(
                if (error) "Reintentar abrir Google Maps" else "Ver en Google Maps",
                {
                    error =
                        try {
                            actualLauncher.open(url)
                            false
                        } catch (_: Exception) {
                            true
                        }
                },
                Modifier.testTag("public-map-open"),
                outlined = false,
            )
        }
    }
}
