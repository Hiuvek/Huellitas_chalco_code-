package com.example.huellitaschalco.feature.access.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.huellitaschalco.core.designsystem.*
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.access.domain.*
import com.example.huellitaschalco.feature.auth.AccessRestrictedScreen
import com.example.huellitaschalco.feature.auth.domain.AuthUser

class ShelterScope(val current: Shelter, val choices: List<Shelter>, val select: (String) -> Unit)

val LocalShelterScope = staticCompositionLocalOf<ShelterScope?> { null }

/** Se aplica a todas las entradas privadas, incluso al muestrario y a los enlaces de debug. */
@Composable
fun ShelterAccessGate(
    nav: UiNavigator,
    user: AuthUser?,
    access: ShelterAccess,
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
            val scope = ShelterScope(current, shelters, onSelect)
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
            (access is ShelterAccess.Checking ||
                access is ShelterAccess.SignedOut ||
                (access is ShelterAccess.Ready && access.uid != user.uid)) ->
            HScaffold(
                nav,
                "Comprobando acceso",
                bottom = {
                    HButton("Reintentar", retry, outlined = true)
                    HTextAction("Ir al catálogo de adopción", { nav.root(UiScreen.Catalog) })
                },
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("shelter-checking"))
                HBody(
                    "Estamos comprobando tus permisos. Necesitas conexión para entrar a la administración del refugio."
                )
            }
        user != null && access is ShelterAccess.Unavailable ->
            HScaffold(
                nav,
                "No pudimos verificar el acceso",
                bottom = {
                    HButton("Reintentar", retry)
                    HTextAction("Ir al catálogo de adopción", { nav.root(UiScreen.Catalog) })
                },
            ) {
                HBanner(
                    "Revisa tu conexión e intenta de nuevo. La administración permanecerá bloqueada hasta verificar tus permisos.",
                    HSymbol.Shield,
                    error = true,
                )
            }
        else ->
            AccessRestrictedScreen(
                nav,
                user?.email,
                {
                    signOut()
                    nav.root(UiScreen.ShelterLogin)
                },
            )
    }
}

@Composable
fun ShelterScopeHeader(scope: ShelterScope) {
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
        HCaption(
            "Las pantallas de perritos, solicitudes y seguimientos aún muestran datos de ejemplo."
        )
    }
}
