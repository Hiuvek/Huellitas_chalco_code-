package com.example.huellitaschalco.funcionalidades.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.funcionalidades.acceso.presentacion.LocalShelterScope
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.UsuarioAutenticado
import com.example.huellitaschalco.funcionalidades.refugio.dominio.*
import com.example.huellitaschalco.ui.theme.*
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*
import kotlinx.coroutines.flow.flowOf

@Composable
fun ProfileScreen(
    nav: NavegadorInterfaz,
    shelter: Boolean,
    user: UsuarioAutenticado? = null,
    onSignOut: () -> Unit = {},
    shelters: RepositorioRefugioPublico? = null,
) {
    val refuge = LocalShelterScope.current?.current
    var refresh by remember { mutableIntStateOf(0) }
    val publicFlow =
        remember(refuge?.id, shelters, refresh) {
            refuge?.id?.let { shelters?.observe(it) } ?: flowOf(EstadoRefugioPublico(false))
        }
    val publicState by publicFlow.collectAsStateWithLifecycle(EstadoRefugioPublico())
    var logout by remember { mutableStateOf(false) }
    HScaffold(
        nav,
        if (shelter) "Perfil del refugio" else "Mi cuenta",
        back = false,
        tab = "Perfil",
        shelter = shelter,
    ) {
        if (!shelter) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(64.dp).background(Forest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user?.label?.take(2)?.uppercase() ?: "HC",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(user?.label ?: "Visitante", style = MaterialTheme.typography.titleLarge)
                    HCaption(user?.email ?: "Sin sesión iniciada")
                    if (user != null) HTag("Adoptante")
                }
            }
        }
        if (shelter) {
            ContenidoPerfilRefugio(nav, refuge, user, publicState) { refresh++ }
        } else if (user != null) {
            HHeading("Mis adopciones")
            HButton(
                "Ver mis solicitudes",
                { nav.root(PantallaInterfaz.MyRequests) },
                icon = SimboloHuellitas.Document,
            )
            HButton(
                "Explorar perritos",
                { nav.root(PantallaInterfaz.Catalog) },
                outlined = true,
                icon = SimboloHuellitas.Paw,
            )
        } else {
            HHeading("Encuentra un compañero")
            HButton("Explorar perritos", { nav.root(PantallaInterfaz.Catalog) }, icon = SimboloHuellitas.Paw)
            HBody("Inicia sesión o crea una cuenta para solicitar una adopción.", muted = true)
            HButton("Iniciar sesión", { nav.open(PantallaInterfaz.Login) }, outlined = true)
            HTextAction("Crear cuenta", { nav.open(PantallaInterfaz.SignUp) })
            HTextAction(
                "Acceso para refugios",
                { nav.root(PantallaInterfaz.ShelterDogs) },
                icon = SimboloHuellitas.Shield,
            )
        }
        if (user != null || shelter)
            HButton("Cerrar sesión", { logout = true }, outlined = true, icon = SimboloHuellitas.Logout)
        if (BuildConfig.SHOW_PREVIEW_GALLERY)
            HTextAction(
                "Ver todas las pantallas",
                { nav.open(PantallaInterfaz.PreviewGallery) },
                icon = SimboloHuellitas.Eye,
            )
    }
    if (logout)
        AlertDialog(
            onDismissRequest = { logout = false },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    "Tus solicitudes enviadas se conservan. Los borradores de esta cuenta permanecen en este teléfono."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        logout = false
                        onSignOut()
                        nav.root(PantallaInterfaz.Welcome)
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { logout = false }) { Text("Seguir en mi cuenta") }
            },
        )
}
