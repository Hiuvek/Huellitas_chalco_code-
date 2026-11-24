package com.example.huellitaschalco.funcionalidades.acceso.presentacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.huellitaschalco.funcionalidades.acceso.dominio.*
import com.example.huellitaschalco.funcionalidades.autenticacion.AccessRestrictedScreen
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.UsuarioAutenticado
import com.example.huellitaschalco.nucleo.navegacion.*
import com.example.huellitaschalco.nucleo.sistema_diseno.*

class ContextoRefugio(val current: Refugio, val choices: List<Refugio>, val select: (String) -> Unit)

val LocalShelterScope = staticCompositionLocalOf<ContextoRefugio?> { null }

/** Se aplica a todas las entradas privadas, incluso al muestrario y a los enlaces de debug. */
@Composable
fun ControlAccesoRefugio(
    nav: NavegadorInterfaz,
    user: UsuarioAutenticado?,
    access: AccesoRefugio,
    retry: () -> Unit,
    signOut: () -> Unit,
    selectedId: String?,
    onSelect: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val shelters = access.sheltersFor(user?.uid)
    val current = shelters.firstOrNull { it.id == selectedId } ?: shelters.firstOrNull()
    when {
        user != null && current != null -> {
            val scope = ContextoRefugio(current, shelters, onSelect)
            CompositionLocalProvider(
                LocalShelterScope provides scope,
                LocalWorkspaceHeader provides { ShelterScopeHeader(scope) },
            ) {
                key(user.uid, current.id) {
                    Box(Modifier.fillMaxSize().testTag("shelter-authorized")) { content() }
                }
            }
        }
        user != null &&
            (access is AccesoRefugio.Comprobando ||
                access is AccesoRefugio.SinSesion ||
                (access is AccesoRefugio.Listo && access.uid != user.uid)) ->
            HScaffold(
                nav,
                "Comprobando acceso",
                bottom = {
                    HButton("Reintentar", retry, outlined = true)
                    HTextAction("Ir al catálogo de adopción", { nav.root(PantallaInterfaz.Catalog) })
                },
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("shelter-checking"))
                HBody(
                    "Estamos comprobando tus permisos. Necesitas conexión para entrar a la administración del refugio."
                )
            }
        user != null && access is AccesoRefugio.NoDisponible ->
            HScaffold(
                nav,
                "No pudimos verificar el acceso",
                bottom = {
                    HButton("Reintentar", retry)
                    HTextAction("Ir al catálogo de adopción", { nav.root(PantallaInterfaz.Catalog) })
                },
            ) {
                HBanner(
                    "Revisa tu conexión e intenta de nuevo. La administración permanecerá bloqueada hasta verificar tus permisos.",
                    SimboloHuellitas.Shield,
                    error = true,
                )
            }
        else ->
            AccessRestrictedScreen(
                nav,
                user?.email,
                {
                    signOut()
                    nav.root(PantallaInterfaz.ShelterLogin)
                },
            )
    }
}

@Composable
fun ShelterScopeHeader(scope: ContextoRefugio) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        if (scope.choices.size > 1) {
            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.testTag("shelter-selector"),
                ) {
                    Text(scope.current.name)
                }
                DropdownMenu(expanded, { expanded = false }) {
                    scope.choices.forEach { shelter ->
                        DropdownMenuItem(
                            text = { Text(shelter.name) },
                            onClick = {
                                expanded = false
                                scope.select(shelter.id)
                            },
                        )
                    }
                }
            }
        } else HCaption(scope.current.name)
    }
}
