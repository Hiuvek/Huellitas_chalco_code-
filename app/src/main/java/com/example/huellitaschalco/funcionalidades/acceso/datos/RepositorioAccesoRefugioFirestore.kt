package com.example.huellitaschalco.funcionalidades.acceso.datos

import com.example.huellitaschalco.funcionalidades.acceso.dominio.*
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/** Solo una respuesta del servidor puede conceder acceso. La caché y los fallos lo bloquean. */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositorioAccesoRefugioFirestore(
    auth: RepositorioAutenticacion,
    firestore: FirebaseFirestore?,
    scope: CoroutineScope,
) : RepositorioAccesoRefugio {
    private val refreshes = MutableStateFlow(0)

    override fun refresh() {
        refreshes.value += 1
    }

    override val access: StateFlow<AccesoRefugio> =
        combine(auth.user, refreshes) { user, _ -> user }
            .flatMapLatest { user ->
                when {
                    user == null -> flowOf(AccesoRefugio.SinSesion)
                    firestore == null -> flowOf(AccesoRefugio.NoDisponible(user.uid))
                    else -> observe(user.uid, firestore)
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, AccesoRefugio.SinSesion)

    private fun observe(uid: String, db: FirebaseFirestore): Flow<AccesoRefugio> =
        combine(
                db.collection("usuarios").document(uid).serverSnapshots(),
                db.collection("membresias")
                    .whereEqualTo("usuarioUid", uid)
                    .whereEqualTo("activo", true)
                    .serverSnapshots(),
            ) { profile, memberships ->
                profile to memberships
            }
            .flatMapLatest { (profile, memberships) ->
                when {
                    profile is ValorServidor.Fallido || memberships is ValorServidor.Fallido ->
                        flowOf(AccesoRefugio.NoDisponible(uid))
                    profile !is ValorServidor.Listo || memberships !is ValorServidor.Listo ->
                        flowOf(AccesoRefugio.Comprobando(uid))
                    profile.value.getBoolean("activo") != true ->
                        flowOf(AccesoRefugio.Listo(uid, emptyList()))
                    else -> {
                        val ids =
                            memberships.value.documents
                                .mapNotNull { doc ->
                                    val membership =
                                        Membresia(
                                            doc.id,
                                            doc.getString("usuarioUid").orEmpty(),
                                            doc.getString("refugioId").orEmpty(),
                                            doc.getBoolean("activo") == true,
                                        )
                                    membership.shelterId.takeIf {
                                        PoliticaMembresias.validFor(uid, membership)
                                    }
                                }
                                .distinct()
                                .sorted()
                        if (ids.isEmpty()) flowOf(AccesoRefugio.Listo(uid, emptyList()))
                        else
                            combine(
                                ids.map { id ->
                                    db.collection("refugios").document(id).serverSnapshots()
                                }
                            ) { values ->
                                when {
                                    values.any { it is ValorServidor.Fallido } ->
                                        AccesoRefugio.NoDisponible(uid)
                                    values.any { it !is ValorServidor.Listo } ->
                                        AccesoRefugio.Comprobando(uid)
                                    else ->
                                        AccesoRefugio.Listo(
                                            uid,
                                            values.mapNotNull { value ->
                                                val doc =
                                                    (value as ValorServidor.Listo<DocumentSnapshot>)
                                                        .value
                                                if (
                                                    doc.getBoolean("activo") != true ||
                                                        doc.getString("nombre").isNullOrBlank()
                                                )
                                                    null
                                                else
                                                    Refugio(
                                                        doc.id,
                                                        doc.getString("nombre").orEmpty(),
                                                        doc.getString("zonaPublica").orEmpty(),
                                                        doc.getString("horarioPublico").orEmpty(),
                                                    )
                                            },
                                        )
                                }
                            }
                    }
                }
            }
            .onStart { emit(AccesoRefugio.Comprobando(uid)) }

    private sealed interface ValorServidor<out T> {
        data object Cargando : ValorServidor<Nothing>

        data object Fallido : ValorServidor<Nothing>

        data class Listo<T>(val value: T) : ValorServidor<T>
    }

    private fun DocumentReference.serverSnapshots(): Flow<ValorServidor<DocumentSnapshot>> =
        callbackFlow {
            trySend(ValorServidor.Cargando)
            val listener =
                addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    trySend(
                        when {
                            error != null || snapshot == null -> ValorServidor.Fallido
                            snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                                ValorServidor.Cargando
                            else -> ValorServidor.Listo(snapshot)
                        }
                    )
                }
            awaitClose { listener.remove() }
        }

    private fun Query.serverSnapshots(): Flow<ValorServidor<QuerySnapshot>> = callbackFlow {
        trySend(ValorServidor.Cargando)
        val listener =
            addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                trySend(
                    when {
                        error != null || snapshot == null -> ValorServidor.Fallido
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                            ValorServidor.Cargando
                        else -> ValorServidor.Listo(snapshot)
                    }
                )
            }
        awaitClose { listener.remove() }
    }
}
