package com.example.huellitaschalco.core.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.ui.theme.*

/** La pantalla que requiere contexto proporciona su cabecera sin acoplar el diseño a un feature. */
val LocalWorkspaceHeader = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

@Composable
fun HScaffold(
    nav: UiNavigator,
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
                            HIcon(HSymbol.Back, tint = Ink, description = "Regresar")
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
                    IconButton(onClick = { nav.open(UiScreen.PreviewGallery) }) {
                        HIcon(HSymbol.Eye, description = "Ver todas las pantallas")
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
fun HBottomNavigation(nav: UiNavigator, shelter: Boolean, selected: String) {
    val items =
        if (shelter)
            listOf(
                Triple("Perritos", HSymbol.Paw, UiScreen.ShelterDogs),
                Triple("Solicitudes", HSymbol.Inbox, UiScreen.ShelterRequests),
                Triple("Seguimientos", HSymbol.Calendar, UiScreen.FollowUps),
                Triple("Perfil", HSymbol.User, UiScreen.ShelterProfile),
            )
        else
            listOf(
                Triple("Explorar", HSymbol.Compass, UiScreen.Catalog),
                Triple("Solicitudes", HSymbol.Document, UiScreen.MyRequests),
                Triple("Perfil", HSymbol.User, UiScreen.AdopterProfile),
            )
    NavigationBar(containerColor = Cream, tonalElevation = 0.dp) {
        items.forEach { (label, symbol, screen) ->
            NavigationBarItem(
                selected = label == selected,
                onClick = { nav.root(screen) },
                icon = { HIcon(symbol, tint = if (label == selected) Forest else Muted) },
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
