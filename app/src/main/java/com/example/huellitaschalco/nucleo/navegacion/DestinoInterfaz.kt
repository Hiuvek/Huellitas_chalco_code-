package com.example.huellitaschalco.nucleo.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

enum class PantallaInterfaz {
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
    ScheduleReview,
    FollowUpForm,
    FollowUpSaved,
    FollowUpDetail,
    ShelterProfile,
    EditShelter,
    RefugeInformation,
    AccessRestricted,
    PreviewGallery,
}

enum class VarianteInterfaz {
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

data class DestinoInterfaz(
    val screen: PantallaInterfaz,
    val dogId: Int = 6,
    val requestId: Int = 1,
    val variant: VarianteInterfaz = VarianteInterfaz.Normal,
    val recordId: String? = null,
) {
    fun encode() = "${screen.name}|$dogId|$requestId|${variant.name}|${recordId.orEmpty()}"

    companion object {
        fun decode(value: String): DestinoInterfaz {
            val p = value.split('|')
            return DestinoInterfaz(
                PantallaInterfaz.valueOf(p[0]),
                p[1].toInt(),
                p[2].toInt(),
                VarianteInterfaz.valueOf(p[3]),
                p.getOrNull(4)?.takeIf { it.isNotBlank() },
            )
        }
    }
}

/** Solo navegación de presentación. No ejecuta ni persiste operaciones de adopción. */
@Stable
class NavegadorInterfaz(initial: List<String> = listOf(DestinoInterfaz(PantallaInterfaz.Welcome).encode())) {
    val entries = mutableStateListOf<String>().apply { addAll(initial) }
    val current: DestinoInterfaz
        get() = DestinoInterfaz.decode(entries.last())

    val canGoBack: Boolean
        get() = entries.size > 1

    fun open(destination: DestinoInterfaz) {
        entries.add(destination.encode())
    }

    fun open(
        screen: PantallaInterfaz,
        dogId: Int = current.dogId,
        requestId: Int = current.requestId,
        variant: VarianteInterfaz = VarianteInterfaz.Normal,
        recordId: String? = null,
    ) = open(DestinoInterfaz(screen, dogId, requestId, variant, recordId))

    fun back() {
        if (canGoBack) entries.removeAt(entries.lastIndex)
    }

    fun root(screen: PantallaInterfaz) {
        entries.clear()
        entries.add(DestinoInterfaz(screen).encode())
    }
}

@Composable
fun rememberUiNavigator(): NavegadorInterfaz =
    rememberSaveable(
        saver = listSaver(save = { it.entries.toList() }, restore = { NavegadorInterfaz(it) })
    ) {
        NavegadorInterfaz()
    }

/** Todos los destinos administrativos pasan por el mismo control de acceso. */
val PantallaInterfaz.requiresShelterAccess: Boolean
    get() =
        this in
            setOf(
                PantallaInterfaz.ShelterDogs,
                PantallaInterfaz.ShelterDogDetail,
                PantallaInterfaz.DogForm,
                PantallaInterfaz.DogPreview,
                PantallaInterfaz.DogPublished,
                PantallaInterfaz.ShelterRequests,
                PantallaInterfaz.EvaluateRequest,
                PantallaInterfaz.AdoptionForm,
                PantallaInterfaz.AdoptionComplete,
                PantallaInterfaz.AdoptionBlocked,
                PantallaInterfaz.FollowUps,
                PantallaInterfaz.ScheduleReview,
                PantallaInterfaz.FollowUpForm,
                PantallaInterfaz.FollowUpSaved,
                PantallaInterfaz.FollowUpDetail,
                PantallaInterfaz.ShelterProfile,
                PantallaInterfaz.EditShelter,
            )
