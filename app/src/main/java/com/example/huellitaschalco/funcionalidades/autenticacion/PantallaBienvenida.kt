package com.example.huellitaschalco.funcionalidades.autenticacion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.R
import com.example.huellitaschalco.ui.theme.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

@Composable
fun PantallaBienvenida(nav: NavegadorInterfaz) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Cream).safeDrawingPadding()) {
        val heroHeight = (maxHeight * .43f).coerceIn(260.dp, 390.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(heroHeight)) {
                Image(
                    painterResource(R.drawable.welcome_hero),
                    "Una persona con su perrito",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Surface(color = Cream, shape = CircleShape, modifier = Modifier.padding(16.dp)) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier.size(30.dp).background(Forest, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Iconos(SimboloHuellitas.Paw, Modifier.size(20.dp), Color.White)
                        }
                        Text("Huellitas Chalco", style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (BuildConfig.SHOW_PREVIEW_GALLERY)
                    Surface(
                        Modifier.align(Alignment.TopEnd).padding(16.dp),
                        shape = CircleShape,
                        color = Cream,
                    ) {
                        IconButton(onClick = { nav.open(PantallaInterfaz.PreviewGallery) }) {
                            Iconos(SimboloHuellitas.Eye, description = "Ver todas las pantallas")
                        }
                    }
            }
            Column(
                Modifier.offset(y = (-24).dp)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(Cream)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Dale un hogar a un\nperrito de Chalco",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    "Conoce a los perritos de refugios locales, revisa sus requisitos y envía tu solicitud desde tu teléfono.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                )
                HButton("Explorar perritos", { nav.root(PantallaInterfaz.Catalog) }, icon = SimboloHuellitas.Paw)
                HCaption(
                    "Puedes ver el catálogo sin una cuenta. Para solicitar una adopción, inicia sesión."
                )
                HButton("Iniciar sesión", { nav.open(PantallaInterfaz.Login) }, outlined = true)
                HTextAction("Crear cuenta", { nav.open(PantallaInterfaz.SignUp) })
                HTextAction(
                    "Acceso para refugios",
                    { nav.open(PantallaInterfaz.ShelterLogin) },
                    icon = SimboloHuellitas.Shield,
                )
            }
        }
    }
}
