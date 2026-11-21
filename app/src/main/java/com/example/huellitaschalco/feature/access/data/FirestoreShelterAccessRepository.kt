package com.example.huellitaschalco.feature.access.data

import com.example.huellitaschalco.feature.access.domain.*
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/** Solo una respuesta del servidor puede conceder acceso. La caché y los fallos lo bloquean. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreShelterAccessRepository(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore?,
    scope: CoroutineScope,
) : ShelterAccessRepository {
    private val refreshes = MutableStateFlow(0)

    override fun refresh() {
        refreshes.value += 1
    }

    override val access: StateFlow<ShelterAccess> =
        combine(auth.user, refreshes) { user, _ -> user }
            .flatMapLatest { user ->
                when {
                    user == null -> flowOf(ShelterAccess.SignedOut)
                    firestore == null -> flowOf(ShelterAccess.Unavailable(user.uid))
                    else -> observe(user.uid, firestore)
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, ShelterAccess.SignedOut)

    private fun observe(uid: String, db: FirebaseFirestore): Flow<ShelterAccess> =
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
                    profile is ServerValue.Failed || memberships is ServerValue.Failed ->
                        flowOf(ShelterAccess.Unavailable(uid))
                    profile !is ServerValue.Ready || memberships !is ServerValue.Ready ->
                        flowOf(ShelterAccess.Checking(uid))
                    profile.value.getBoolean("activo") != true ->
                        flowOf(ShelterAccess.Ready(uid, emptyList()))
                    else -> {
                        val ids =
                            memberships.value.documents
                                .mapNotNull { doc ->
                                    val membership =
                                        Membership(
                                            doc.id,
                                            doc.getString("usuarioUid").orEmpty(),
                                            doc.getString("refugioId").orEmpty(),
                                            doc.getBoolean("activo") == true,
                                        )
                                    membership.shelterId.takeIf {
                                        MembershipPolicy.validFor(uid, membership)
                                    }
                                }
                                .distinct()
                                .sorted()
                        if (ids.isEmpty()) flowOf(ShelterAccess.Ready(uid, emptyList()))
                        else
                            combine(
                                ids.map { id ->
                                    db.collection("refugios").document(id).serverSnapshots()
                                }
                            ) { values ->
                                when {
                                    values.any { it is ServerValue.Failed } ->
                                        ShelterAccess.Unavailable(uid)
                                    values.any { it !is ServerValue.Ready } ->
                                        ShelterAccess.Checking(uid)
                                    else ->
                                        ShelterAccess.Ready(
                                            uid,
                                            values.mapNotNull { value ->
                                                val doc =
                                                    (value as ServerValue.Ready<DocumentSnapshot>)
                                                        .value
                                                if (
                                                    doc.getBoolean("activo") != true ||
                                                        doc.getString("nombre").isNullOrBlank()
                                                )
                                                    null
                                                else
                                                    Shelter(
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
            .onStart { emit(ShelterAccess.Checking(uid)) }

    private sealed interface ServerValue<out T> {
        data object Loading : ServerValue<Nothing>

        data object Failed : ServerValue<Nothing>

        data class Ready<T>(val value: T) : ServerValue<T>
    }

    private fun DocumentReference.serverSnapshots(): Flow<ServerValue<DocumentSnapshot>> =
        callbackFlow {
            trySend(ServerValue.Loading)
            val listener =
                addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    trySend(
                        when {
                            error != null || snapshot == null -> ServerValue.Failed
                            snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                                ServerValue.Loading
                            else -> ServerValue.Ready(snapshot)
                        }
                    )
                }
            awaitClose { listener.remove() }
        }

    private fun Query.serverSnapshots(): Flow<ServerValue<QuerySnapshot>> = callbackFlow {
        trySend(ServerValue.Loading)
        val listener =
            addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                trySend(
                    when {
                        error != null || snapshot == null -> ServerValue.Failed
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                            ServerValue.Loading
                        else -> ServerValue.Ready(snapshot)
                    }
                )
            }
        awaitClose { listener.remove() }
    }
}
