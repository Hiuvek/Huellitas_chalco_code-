package com.example.huellitaschalco.app

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.access.domain.*
import com.example.huellitaschalco.feature.access.presentation.*
import com.example.huellitaschalco.feature.application.*
import com.example.huellitaschalco.feature.auth.*
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.auth.presentation.AuthRoute
import com.example.huellitaschalco.feature.catalog.*
import com.example.huellitaschalco.feature.catalog.presentation.CatalogRoute
import com.example.huellitaschalco.feature.preview.*
import com.example.huellitaschalco.feature.profile.*
import com.example.huellitaschalco.feature.requests.*
import com.example.huellitaschalco.feature.shelter.adoption.*
import com.example.huellitaschalco.feature.shelter.dogs.*
import com.example.huellitaschalco.feature.shelter.dogs.presentation.*
import com.example.huellitaschalco.feature.shelter.followup.*
import com.example.huellitaschalco.feature.shelter.requests.*

@Composable
fun HuellitasApp(
    authRepository: AuthRepository,
    accessRepository: ShelterAccessRepository,
    initialPreview: UiDestination? = null,
) {
    val container = (LocalContext.current.applicationContext as HuellitasApplication).container
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
        if (user != null && nav.current.screen == UiScreen.Welcome && initialPreview == null) {
            if (access is ShelterAccess.Ready && (access as ShelterAccess.Ready).uid == user?.uid)
                nav.root(
                    if (access.sheltersFor(user?.uid).isNotEmpty()) UiScreen.ShelterDogs
                    else UiScreen.Catalog
                )
            else if (access is ShelterAccess.Unavailable) nav.root(UiScreen.Catalog)
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
                    UiScreen.Welcome -> WelcomeScreen(nav)
                    UiScreen.Login,
                    UiScreen.SignUp ->
                        if (destination.variant == UiVariant.Normal)
                            AuthRoute(nav, destination, authRepository)
                        else AuthScreen(nav, destination)
                    UiScreen.ShelterLogin -> AuthRoute(nav, destination, authRepository)
                    UiScreen.AccessRestricted ->
                        AccessRestrictedScreen(
                            nav,
                            user?.email,
                            {
                                authRepository.signOut()
                                nav.root(UiScreen.ShelterLogin)
                            },
                        )
                    UiScreen.Catalog ->
                        if (destination.variant != UiVariant.Normal)
                            CatalogScreen(nav, destination, user?.label)
                        else
                            CatalogRoute(
                                nav,
                                user?.label,
                                container.catalogRepository,
                                container.dogRepository,
                            )
                    UiScreen.DogDetail ->
                        if (destination.recordId != null)
                            PublicDogDetailRoute(
                                nav,
                                destination.recordId,
                                container.dogRepository,
                                container.publicShelterRepository,
                            )
                        else DogDetailScreen(nav, destination)
                    UiScreen.DogPreview ->
                        if (destination.variant != UiVariant.Normal)
                            DogDetailScreen(nav, destination)
                        else
                            LocalShelterScope.current?.let { scope ->
                                DogRegistrationRoute(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.dogRepository,
                                    container.dogPhotoImporter,
                                )
                            }
                    UiScreen.Requirements -> RequirementsScreen(nav)
                    UiScreen.RefugeInformation ->
                        if (destination.recordId != null)
                            PublicDogDetailRoute(
                                nav,
                                destination.recordId,
                                container.dogRepository,
                                container.publicShelterRepository,
                                refugeOnly = true,
                            )
                        else RefugeInformationScreen(nav)
                    UiScreen.ApplicationContact,
                    UiScreen.ApplicationHome,
                    UiScreen.ApplicationPets,
                    UiScreen.ApplicationReasons,
                    UiScreen.ApplicationReview -> ApplicationScreen(nav, destination)
                    UiScreen.ApplicationSent -> ApplicationSentScreen(nav)
                    UiScreen.MyRequests -> MyRequestsScreen(nav, destination)
                    UiScreen.RequestDetail -> RequestDetailScreen(nav, destination)
                    UiScreen.AdopterProfile ->
                        ProfileScreen(nav, false, user, authRepository::signOut)
                    UiScreen.ShelterProfile ->
                        ProfileScreen(nav, true, user, authRepository::signOut)
                    UiScreen.ShelterDogs,
                    UiScreen.ShelterDogDetail ->
                        if (destination.variant != UiVariant.Normal) {
                            if (destination.screen == UiScreen.ShelterDogs)
                                ShelterDogsScreen(nav, destination)
                            else ShelterDogDetailScreen(nav, destination)
                        } else
                            LocalShelterScope.current?.let { scope ->
                                RegisteredDogsRoute(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.dogRepository,
                                    container.dogPhotoImporter,
                                )
                            }
                    UiScreen.DogForm ->
                        if (destination.variant != UiVariant.Normal) DogFormScreen(nav, destination)
                        else
                            LocalShelterScope.current?.let { scope ->
                                DogRegistrationRoute(
                                    nav,
                                    requireNotNull(user).uid,
                                    scope.current.id,
                                    container.dogRepository,
                                    container.dogPhotoImporter,
                                )
                            }
                    UiScreen.DogPublished ->
                        LocalShelterScope.current?.let { scope ->
                            DogRegistrationRoute(
                                nav,
                                requireNotNull(user).uid,
                                scope.current.id,
                                container.dogRepository,
                                container.dogPhotoImporter,
                            )
                        }
                    UiScreen.ShelterRequests -> ShelterRequestsScreen(nav, destination)
                    UiScreen.EvaluateRequest -> EvaluateRequestScreen(nav, destination)
                    UiScreen.AdoptionForm -> AdoptionFormScreen(nav)
                    UiScreen.AdoptionComplete -> AdoptionCompleteScreen(nav)
                    UiScreen.AdoptionBlocked -> AdoptionBlockedScreen(nav)
                    UiScreen.FollowUps -> FollowUpsScreen(nav, destination)
                    UiScreen.FollowUpForm -> FollowUpFormScreen(nav)
                    UiScreen.FollowUpSaved -> FollowUpSavedScreen(nav)
                    UiScreen.FollowUpDetail -> FollowUpDetailScreen(nav, destination)
                    UiScreen.PreviewGallery -> PreviewGalleryScreen(nav)
                }
            }
        }
    }
    if (destination.screen.requiresShelterAccess)
        ShelterAccessGate(
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
