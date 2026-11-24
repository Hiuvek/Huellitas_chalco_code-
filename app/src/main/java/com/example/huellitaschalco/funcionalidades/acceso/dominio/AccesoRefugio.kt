package com.example.huellitaschalco.funcionalidades.acceso.dominio

import kotlinx.coroutines.flow.StateFlow

data class Refugio(
    val id: String,
    val name: String,
    val publicZone: String,
    val publicHours: String,
)

sealed interface AccesoRefugio {
    data object SinSesion : AccesoRefugio

    data class Comprobando(val uid: String) : AccesoRefugio

    data class Listo(val uid: String, val shelters: List<Refugio>) : AccesoRefugio

    data class NoDisponible(val uid: String) : AccesoRefugio
}

interface RepositorioAccesoRefugio {
    val access: StateFlow<AccesoRefugio>

    fun refresh()
}

/** La autorización de otra cuenta nunca es válida mientras Firebase cambia de sesión. */
fun AccesoRefugio.sheltersFor(uid: String?): List<Refugio> =
    if (this is AccesoRefugio.Listo && uid != null && this.uid == uid) shelters else emptyList()

data class Membresia(
    val id: String,
    val userUid: String,
    val shelterId: String,
    val active: Boolean,
)

object PoliticaMembresias {
    private val safeId = Regex("^[A-Za-z0-9_-]{1,100}$")

    fun validFor(uid: String, membership: Membresia): Boolean =
        membership.active &&
            membership.userUid == uid &&
            safeId.matches(membership.shelterId) &&
            membership.id == "${uid}__${membership.shelterId}"
}
