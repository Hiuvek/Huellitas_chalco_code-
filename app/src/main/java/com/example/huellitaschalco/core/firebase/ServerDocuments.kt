package com.example.huellitaschalco.core.firebase

import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

sealed interface ServerDocuments {
    data object Waiting : ServerDocuments

    data object Failed : ServerDocuments

    data class Ready(val documents: List<DocumentSnapshot>) : ServerDocuments
}

fun Query.serverDocuments() = callbackFlow {
    trySend(ServerDocuments.Waiting)
    val listener =
        addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            trySend(
                when {
                    error != null || snapshot == null -> ServerDocuments.Failed
                    snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                        ServerDocuments.Waiting
                    else -> ServerDocuments.Ready(snapshot.documents)
                }
            )
        }
    awaitClose { listener.remove() }
}
