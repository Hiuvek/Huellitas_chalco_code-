package com.example.huellitaschalco.nucleo.sistema_diseno

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.ui.theme.*
import com.example.huellitaschalco.nucleo.navegacion.*

/** La pantalla que requiere contexto proporciona su cabecera sin acoplar el diseño a un feature. */
val LocalWorkspaceHeader = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

@Composable
fun HScaffold(
    nav: NavegadorInterfaz,
    title: String,
    subtitle: String? = null,
    back: Boolean = true,
    tab: String? = null,
    shelter: Boolean = false,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            Column(Modifier.background(Cream).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (back) {
                        IconButton(onClick = nav::back) {
                            Iconos(SimboloHuellitas.Back, tint = Ink, description = "Regresar")
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style =
                                if (back) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.headlineMedium,
                        )
                        if (subtitle != null)
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted,
                            )
                    }
                    if (BuildConfig.SHOW_PREVIEW_GALLERY)
                        IconButton(onClick = { nav.open(PantallaInterfaz.PreviewGallery) }) {
                            Iconos(SimboloHuellitas.Eye, description = "Ver todas las pantallas")
                        }
                }
                LocalWorkspaceHeader.current?.let { header ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        header()
                    }
                }
                HorizontalDivider(color = Line)
            }
        },
        bottomBar = {
            if (bottom != null || tab != null)
                Column(Modifier.background(Cream).navigationBarsPadding().imePadding()) {
                    HorizontalDivider(color = Line)
                    if (bottom != null)
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            content = bottom,
                        )
                    if (tab != null) HBottomNavigation(nav, shelter, tab)
                }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

@Composable
fun HBottomNavigation(nav: NavegadorInterfaz, shelter: Boolean, selected: String) {
    val items =
        if (shelter)
            listOf(
                Triple("Perritos", SimboloHuellitas.Paw, PantallaInterfaz.ShelterDogs),
                Triple("Solicitudes", SimboloHuellitas.Inbox, PantallaInterfaz.ShelterRequests),
                Triple("Seguimientos", SimboloHuellitas.Calendar, PantallaInterfaz.FollowUps),
                Triple("Perfil", SimboloHuellitas.User, PantallaInterfaz.ShelterProfile),
            )
        else
            listOf(
                Triple("Explorar", SimboloHuellitas.Compass, PantallaInterfaz.Catalog),
                Triple("Solicitudes", SimboloHuellitas.Document, PantallaInterfaz.MyRequests),
                Triple("Perfil", SimboloHuellitas.User, PantallaInterfaz.AdopterProfile),
            )
    NavigationBar(containerColor = Cream, tonalElevation = 0.dp) {
        items.forEach { (label, symbol, screen) ->
            NavigationBarItem(
                selected = label == selected,
                onClick = { nav.root(screen) },
                icon = { Iconos(symbol, tint = if (label == selected) Forest else Muted) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = Sage,
                        selectedTextColor = Forest,
                        unselectedTextColor = Muted,
                    ),
            )
        }
    }
}
