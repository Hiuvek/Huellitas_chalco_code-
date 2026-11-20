package com.example.huellitaschalco.app

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitaschalco.core.navigation.*
import com.example.huellitaschalco.feature.application.*
import com.example.huellitaschalco.feature.auth.*
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.auth.presentation.AuthRoute
import com.example.huellitaschalco.feature.catalog.*
import com.example.huellitaschalco.feature.preview.*
import com.example.huellitaschalco.feature.profile.*
import com.example.huellitaschalco.feature.requests.*
import com.example.huellitaschalco.feature.shelter.adoption.*
import com.example.huellitaschalco.feature.shelter.dogs.*
import com.example.huellitaschalco.feature.shelter.followup.*
import com.example.huellitaschalco.feature.shelter.requests.*

@Composable
fun HuellitasApp(authRepository: AuthRepository, initialPreview: UiDestination? = null) {
    val nav = rememberUiNavigator()
    val user by authRepository.user.collectAsStateWithLifecycle()
    LaunchedEffect(user?.uid) {
        if (user != null && nav.current.screen == UiScreen.Welcome && initialPreview == null)
            nav.root(UiScreen.Catalog)
    }
    LaunchedEffect(initialPreview) {
        if (initialPreview != null && nav.entries.size == 1) nav.open(initialPreview)
    }
    BackHandler(enabled = nav.canGoBack, onBack = nav::back)
    val destination = nav.current
    key(destination) {
        Surface {
            when (destination.screen) {
                UiScreen.Welcome -> WelcomeScreen(nav)
                UiScreen.Login,
                UiScreen.SignUp ->
                    if (destination.variant == UiVariant.Normal)
                        AuthRoute(nav, destination, authRepository)
                    else AuthScreen(nav, destination)
                UiScreen.ShelterLogin -> AuthScreen(nav, destination)
                UiScreen.AccessRestricted -> AccessRestrictedScreen(nav)
                UiScreen.Catalog -> CatalogScreen(nav, destination, user?.label)
                UiScreen.DogDetail,
                UiScreen.DogPreview -> DogDetailScreen(nav, destination)
                UiScreen.Requirements -> RequirementsScreen(nav)
                UiScreen.RefugeInformation -> RefugeInformationScreen(nav)
                UiScreen.ApplicationContact,
                UiScreen.ApplicationHome,
                UiScreen.ApplicationPets,
                UiScreen.ApplicationReasons,
                UiScreen.ApplicationReview -> ApplicationScreen(nav, destination)
                UiScreen.ApplicationSent -> ApplicationSentScreen(nav)
                UiScreen.MyRequests -> MyRequestsScreen(nav, destination)
                UiScreen.RequestDetail -> RequestDetailScreen(nav, destination)
                UiScreen.AdopterProfile -> ProfileScreen(nav, false, user, authRepository::signOut)
                UiScreen.ShelterProfile -> ProfileScreen(nav, true)
                UiScreen.ShelterDogs -> ShelterDogsScreen(nav, destination)
                UiScreen.ShelterDogDetail -> ShelterDogDetailScreen(nav, destination)
                UiScreen.DogForm -> DogFormScreen(nav, destination)
                UiScreen.DogPublished -> DogPublishedScreen(nav)
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
