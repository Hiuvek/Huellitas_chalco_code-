package com.example.huellitaschalco.nucleo.firebase

import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

sealed interface DocumentosServidor {
    data object EnEspera : DocumentosServidor

    data object Fallido : DocumentosServidor

    data class Listo(val documents: List<DocumentSnapshot>) : DocumentosServidor
}

fun Query.serverDocuments() = callbackFlow {
    trySend(DocumentosServidor.EnEspera)
    val listener =
        addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            trySend(
                when {
                    error != null || snapshot == null -> DocumentosServidor.Fallido
                    snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                        DocumentosServidor.EnEspera
                    else -> DocumentosServidor.Listo(snapshot.documents)
                }
            )
        }
    awaitClose { listener.remove() }
}
