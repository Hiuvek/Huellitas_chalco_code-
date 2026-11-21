package com.example.huellitaschalco.feature.access.domain

import kotlinx.coroutines.flow.StateFlow

data class Shelter(
    val id: String,
    val name: String,
    val publicZone: String,
    val publicHours: String,
)

sealed interface ShelterAccess {
    data object SignedOut : ShelterAccess

    data class Checking(val uid: String) : ShelterAccess

    data class Ready(val uid: String, val shelters: List<Shelter>) : ShelterAccess

    data class Unavailable(val uid: String) : ShelterAccess
}

interface ShelterAccessRepository {
    val access: StateFlow<ShelterAccess>

    fun refresh()
}

/** La autorización de otra cuenta nunca es válida mientras Firebase cambia de sesión. */
fun ShelterAccess.sheltersFor(uid: String?): List<Shelter> =
    if (this is ShelterAccess.Ready && uid != null && this.uid == uid) shelters else emptyList()

data class Membership(
    val id: String,
    val userUid: String,
    val shelterId: String,
    val active: Boolean,
)

object MembershipPolicy {
    private val safeId = Regex("^[A-Za-z0-9_-]{1,100}$")

    fun validFor(uid: String, membership: Membership): Boolean =
        membership.active &&
            membership.userUid == uid &&
            safeId.matches(membership.shelterId) &&
            membership.id == "${uid}__${membership.shelterId}"
}
