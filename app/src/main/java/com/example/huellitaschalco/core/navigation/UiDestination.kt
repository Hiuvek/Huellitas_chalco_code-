package com.example.huellitaschalco.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

enum class UiScreen {
    Welcome,
    Login,
    SignUp,
    Catalog,
    DogDetail,
    Requirements,
    ApplicationContact,
    ApplicationHome,
    ApplicationPets,
    ApplicationReasons,
    ApplicationReview,
    ApplicationSent,
    MyRequests,
    RequestDetail,
    AdopterProfile,
    ShelterLogin,
    ShelterDogs,
    ShelterDogDetail,
    DogForm,
    DogPreview,
    DogPublished,
    ShelterRequests,
    EvaluateRequest,
    AdoptionForm,
    AdoptionComplete,
    AdoptionBlocked,
    FollowUps,
    FollowUpForm,
    FollowUpSaved,
    FollowUpDetail,
    ShelterProfile,
    RefugeInformation,
    AccessRestricted,
    PreviewGallery,
}

enum class UiVariant {
    Normal,
    Offline,
    NoCache,
    Empty,
    Error,
    FieldError,
    Photos,
    PhotoError,
    AiSuggestion,
    AiError,
    InReview,
    Approved,
    NotApproved,
    ApprovalBlocked,
    AfterAdoption,
    History,
    AfterFollowUp,
    OldFollowUp,
    Editing,
    MapError,
}

data class UiDestination(
    val screen: UiScreen,
    val dogId: Int = 6,
    val requestId: Int = 1,
    val variant: UiVariant = UiVariant.Normal,
    val recordId: String? = null,
) {
    fun encode() = "${screen.name}|$dogId|$requestId|${variant.name}|${recordId.orEmpty()}"

    companion object {
        fun decode(value: String): UiDestination {
            val p = value.split('|')
            return UiDestination(
                UiScreen.valueOf(p[0]),
                p[1].toInt(),
                p[2].toInt(),
                UiVariant.valueOf(p[3]),
                p.getOrNull(4)?.takeIf { it.isNotBlank() },
            )
        }
    }
}

/** Solo navegación de presentación. No ejecuta ni persiste operaciones de adopción. */
@Stable
class UiNavigator(initial: List<String> = listOf(UiDestination(UiScreen.Welcome).encode())) {
    val entries = mutableStateListOf<String>().apply { addAll(initial) }
    val current: UiDestination
        get() = UiDestination.decode(entries.last())

    val canGoBack: Boolean
        get() = entries.size > 1

    fun open(destination: UiDestination) {
        entries.add(destination.encode())
    }

    fun open(
        screen: UiScreen,
        dogId: Int = current.dogId,
        requestId: Int = current.requestId,
        variant: UiVariant = UiVariant.Normal,
        recordId: String? = null,
    ) = open(UiDestination(screen, dogId, requestId, variant, recordId))

    fun back() {
        if (canGoBack) entries.removeAt(entries.lastIndex)
    }

    fun root(screen: UiScreen) {
        entries.clear()
        entries.add(UiDestination(screen).encode())
    }
}

@Composable
fun rememberUiNavigator(): UiNavigator =
    rememberSaveable(
        saver = listSaver(save = { it.entries.toList() }, restore = { UiNavigator(it) })
    ) {
        UiNavigator()
    }

/** Todos los destinos administrativos pasan por el mismo control de acceso. */
val UiScreen.requiresShelterAccess: Boolean
    get() =
        this in
            setOf(
                UiScreen.ShelterDogs,
                UiScreen.ShelterDogDetail,
                UiScreen.DogForm,
                UiScreen.DogPreview,
                UiScreen.DogPublished,
                UiScreen.ShelterRequests,
                UiScreen.EvaluateRequest,
                UiScreen.AdoptionForm,
                UiScreen.AdoptionComplete,
                UiScreen.AdoptionBlocked,
                UiScreen.FollowUps,
                UiScreen.FollowUpForm,
                UiScreen.FollowUpSaved,
                UiScreen.FollowUpDetail,
                UiScreen.ShelterProfile,
            )
