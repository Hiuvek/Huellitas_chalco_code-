package com.example.huellitaschalco.aplicacion

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.funcionalidades.acceso.dominio.*
import com.example.huellitaschalco.funcionalidades.acceso.presentacion.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.presentacion.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.*
import com.example.huellitaschalco.funcionalidades.autenticacion.*
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.example.huellitaschalco.funcionalidades.autenticacion.presentacion.RutaAutenticacion
import com.example.huellitaschalco.funcionalidades.catalogo.*
import com.example.huellitaschalco.funcionalidades.catalogo.presentacion.RutaCatalogo
import com.example.huellitaschalco.funcionalidades.perfil.*
import com.example.huellitaschalco.funcionalidades.solicitud.*
import com.example.huellitaschalco.funcionalidades.solicitudes.*
import com.example.huellitaschalco.nucleo.navegacion.*

@Composable
fun AplicacionHuellitas(
    authRepository: RepositorioAutenticacion,
    accessRepository: RepositorioAccesoRefugio,
    initialPreview: DestinoInterfaz? = null,
) {
    val container = (LocalContext.current.applicationContext as InicioHuellitas).container
    val nav = rememberUiNavigator()
    val user by authRepository.user.collectAsStateWithLifecycle()
    val access by accessRepository.access.collectAsStateWithLifecycle()
    var selectedShelterId by rememberSaveable(user?.uid) { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, accessRepository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) accessRepository.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(user?.uid, access) {
        if (user != null && nav.current.screen == PantallaInterfaz.Welcome && initialPreview == null) {
            if (access is AccesoRefugio.Listo && (access as AccesoRefugio.Listo).uid == user?.uid)
                nav.root(
                    if (access.sheltersFor(user?.uid).isNotEmpty()) PantallaInterfaz.ShelterDogs
                    else PantallaInterfaz.Catalog
                )
            else if (access is AccesoRefugio.NoDisponible) nav.root(PantallaInterfaz.Catalog)
        }
    }
    LaunchedEffect(initialPreview) {
        if (initialPreview != null && nav.entries.size == 1) nav.open(initialPreview)
    }
    BackHandler(enabled = nav.canGoBack, onBack = nav::back)
    val destination = nav.current
    val render: @Composable () -> Unit = {
        key(destination) {
            Surface {
                when (destination.screen) {
                    PantallaInterfaz.Welcome -> PantallaBienvenida(nav)
                    PantallaInterfaz.Login,
                    PantallaInterfaz.SignUp ->
                        if (destination.variant == VarianteInterfaz.Normal)
                            RutaAutenticacion(nav, destination, authRepository)
                        else AuthScreen(nav, destination)
                    PantallaInterfaz.ShelterLogin -> RutaAutenticacion(nav, destination, authRepository)
                    PantallaInterfaz.AccessRestricted ->
                        AccessRestrictedScreen(
                            nav,
                            user?.email,
                            {
                                authRepository.signOut()
                                nav.root(PantallaInterfaz.ShelterLogin)
                            },
                        )
                    PantallaInterfaz.Catalog ->
                        if (destination.variant != VarianteInterfaz.Normal)
                            PantallaCatalogo(nav, destination, user?.label)
                        else
                            RutaCatalogo(
                                nav,
                                user?.label,
                                container.catalogRepository,
                                container.dogRepository,
                            )
                    PantallaInterfaz.DogDetail ->
                        if (destination.recordId != null)
                            RutaFichaPublicaPerrito(
                                nav,
                                destination.recordId,
                                container.dogRepository,
                                container.publicShelterRepository,
                            )
                        else PantallaFichaPerrito(nav, destination)
                    PantallaInterfaz.DogPreview ->
                        if (destination.variant != VarianteInterfaz.Normal)
                            PantallaFichaPerrito(nav, destination)
                        else
                            LocalShelterScope.current?.let { scope ->
                                RutaRegistroPerrito(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.dogRepository,
                                    container.dogPhotoImporter,
                                    container.draftStore,
                                    container.descriptionGenerator,
                                )
                            }
                    PantallaInterfaz.Requirements ->
                        if (destination.recordId != null)
                            com.example.huellitaschalco.funcionalidades.refugio.presentacion.RutaRequisitos(
                                    nav,
                                    destination.recordId,
                                    container.dogRepository,
                                    container.publicShelterRepository,
                                )
                        else RequirementsScreen(nav)
                    PantallaInterfaz.RefugeInformation ->
                        if (destination.recordId != null)
                            RutaFichaPublicaPerrito(
                                nav,
                                destination.recordId,
                                container.dogRepository,
                                container.publicShelterRepository,
                                refugeOnly = true,
                            )
                        else RefugeInformationScreen(nav)
                    PantallaInterfaz.ApplicationContact,
                    PantallaInterfaz.ApplicationHome,
                    PantallaInterfaz.ApplicationPets,
                    PantallaInterfaz.ApplicationReasons,
                    PantallaInterfaz.ApplicationReview ->
                        if (destination.recordId != null && destination.variant == VarianteInterfaz.Normal)
                            com.example.huellitaschalco.funcionalidades.solicitud.presentacion.RutaSolicitud(
                                    nav,
                                    destination.recordId,
                                    user,
                                    container.adoptionApplicationRepository,
                                    container.dogRepository,
                                    container.publicShelterRepository,
                                    container.draftStore,
                                )
                        else ApplicationScreen(nav, destination)
                    PantallaInterfaz.ApplicationSent -> ApplicationSentScreen(nav)
                    PantallaInterfaz.MyRequests ->
                        if (destination.variant == VarianteInterfaz.Normal)
                            com.example.huellitaschalco.funcionalidades.solicitudes.presentacion.RutaMisSolicitudes(
                                    nav,
                                    user,
                                    null,
                                    container.adoptionRequestsRepository,
                                )
                        else MyRequestsScreen(nav, destination)
                    PantallaInterfaz.RequestDetail ->
                        if (destination.variant == VarianteInterfaz.Normal)
                            com.example.huellitaschalco.funcionalidades.solicitudes.presentacion.RutaMisSolicitudes(
                                    nav,
                                    user,
                                    destination.recordId,
                                    container.adoptionRequestsRepository,
                                )
                        else RequestDetailScreen(nav, destination)
                    PantallaInterfaz.AdopterProfile ->
                        ProfileScreen(
                            nav,
                            false,
                            user,
                            authRepository::signOut,
                            container.publicShelterRepository,
                        )
                    PantallaInterfaz.ShelterProfile ->
                        ProfileScreen(
                            nav,
                            true,
                            user,
                            authRepository::signOut,
                            container.publicShelterRepository,
                        )
                    PantallaInterfaz.EditShelter ->
                        LocalShelterScope.current?.let { scope ->
                            com.example.huellitaschalco.funcionalidades.refugio.presentacion.RutaEdicionRefugio(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.publicShelterRepository,
                                )
                        }
                    PantallaInterfaz.ShelterDogs,
                    PantallaInterfaz.ShelterDogDetail ->
                        if (destination.variant != VarianteInterfaz.Normal) {
                            if (destination.screen == PantallaInterfaz.ShelterDogs)
                                ShelterDogsScreen(nav, destination)
                            else ShelterDogDetailScreen(nav, destination)
                        } else
                            LocalShelterScope.current?.let { scope ->
                                RutaPerritosRegistrados(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.dogRepository,
                                    container.dogPhotoImporter,
                                    container.descriptionGenerator,
                                )
                            }
                    PantallaInterfaz.DogForm ->
                        if (destination.variant != VarianteInterfaz.Normal) PantallaFormularioPerrito(nav, destination)
                        else
                            LocalShelterScope.current?.let { scope ->
                                RutaRegistroPerrito(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.dogRepository,
                                    container.dogPhotoImporter,
                                    container.draftStore,
                                    container.descriptionGenerator,
                                )
                            }
                    PantallaInterfaz.DogPublished ->
                        LocalShelterScope.current?.let { scope ->
                            RutaRegistroPerrito(
                                nav,
                                requireNotNull(user).uid,
                                scope.current.id,
                                container.dogRepository,
                                container.dogPhotoImporter,
                                container.draftStore,
                                container.descriptionGenerator,
                            )
                        }
                    PantallaInterfaz.ShelterRequests,
                    PantallaInterfaz.EvaluateRequest ->
                        if (destination.variant != VarianteInterfaz.Normal) {
                            if (destination.screen == PantallaInterfaz.ShelterRequests)
                                ShelterRequestsScreen(nav, destination)
                            else EvaluateRequestScreen(nav, destination)
                        } else
                            LocalShelterScope.current?.let { scope ->
                                com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.presentacion.RutaSolicitudesRecibidas(
                                        nav,
                                        requireNotNull(user).uid,
                                        scope.current.id,
                                        if (destination.screen == PantallaInterfaz.EvaluateRequest)
                                            destination.recordId
                                        else null,
                                        container.adoptionRequestsRepository,
                                        container.evaluationRepository,
                                    )
                            }
                    PantallaInterfaz.AdoptionForm,
                    PantallaInterfaz.AdoptionComplete,
                    PantallaInterfaz.AdoptionBlocked ->
                        LocalShelterScope.current?.let { scope ->
                            com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.presentacion.RutaAdopcion(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    destination.recordId,
                                    container.adoptionRequestsRepository,
                                    container.adoptionRepository,
                                )
                        }
                    PantallaInterfaz.FollowUps,
                    PantallaInterfaz.ScheduleReview,
                    PantallaInterfaz.FollowUpForm,
                    PantallaInterfaz.FollowUpSaved,
                    PantallaInterfaz.FollowUpDetail ->
                        if (destination.variant != VarianteInterfaz.Normal) {
                            when (destination.screen) {
                                PantallaInterfaz.FollowUps -> FollowUpsScreen(nav, destination)
                                PantallaInterfaz.FollowUpDetail -> FollowUpDetailScreen(nav, destination)
                                PantallaInterfaz.FollowUpSaved -> FollowUpSavedScreen(nav)
                                else -> FollowUpFormScreen(nav)
                            }
                        } else
                            LocalShelterScope.current?.let { scope ->
                                com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.presentacion.RutasSeguimiento(
                                        nav,
                                        requireNotNull(user).uid,
                                        scope.current.id,
                                        container.followUpRepository,
                                        container.dogPhotoImporter,
                                    )
                            }
                    PantallaInterfaz.PreviewGallery ->
                        LaunchedEffect(Unit) { nav.root(PantallaInterfaz.Catalog) }
                }
            }
        }
    }
    if (destination.screen.requiresShelterAccess)
        ControlAccesoRefugio(
            nav,
            user,
            access,
            accessRepository::refresh,
            authRepository::signOut,
            selectedShelterId,
            { selectedShelterId = it },
            render,
        )
    else render()
}
