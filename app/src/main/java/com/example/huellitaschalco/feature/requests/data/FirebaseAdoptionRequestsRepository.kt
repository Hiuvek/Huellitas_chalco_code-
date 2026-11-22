package com.example.huellitaschalco.feature.requests.data

import com.example.huellitaschalco.feature.application.domain.ApplicationDraft
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.requests.domain.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAdoptionRequestsRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore?,
    private val waitMillis: Long = 12_000,
) : AdoptionRequestsRepository, ReceivedRequestsRepository {
    override fun observeOwn(uid: String) = observeList(uid, null)

    override fun observeOwnDetail(uid: String, id: String) = observeDetail(uid, null, id)

    override fun observeReceived(uid: String, shelterId: String) = observeList(uid, shelterId)

    override fun observeReceivedDetail(uid: String, shelterId: String, id: String) =
        observeDetail(uid, shelterId, id)

    private fun scopedQuery(collection: String, uid: String, shelterId: String?): Query =
        requireNotNull(db)
            .collection(collection)
            .whereEqualTo(if (shelterId == null) "usuarioUid" else "refugioId", shelterId ?: uid)

    private fun parseScoped(
        doc: DocumentSnapshot,
        uid: String,
        shelterId: String?,
    ): AdoptionRequest {
        if (shelterId != null) check(doc.getString("refugioId") == shelterId)
        return parseRequest(
            doc,
            if (shelterId == null) uid else requireNotNull(doc.getString("usuarioUid")),
        )
    }

    private fun observeList(uid: String, shelterId: String?): Flow<RequestListState> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(
                    RequestListState(false, error = "Inicia sesión para consultar tus solicitudes.")
                )
            else
                scopedQuery("solicitudes", uid, shelterId)
                    .serverValues()
                    .mapLatest { result ->
                        when (result) {
                            QueryValue.Waiting -> RequestListState()
                            QueryValue.Failed ->
                                RequestListState(
                                    false,
                                    error =
                                        "No pudimos confirmar tus solicitudes. Revisa la conexión y reintenta.",
                                )
                            is QueryValue.Ready ->
                                try {
                                    val items = coroutineScope {
                                        result.snapshot.documents
                                            .map { document ->
                                                async {
                                                    withDogName(
                                                        parseScoped(document, uid, shelterId)
                                                    )
                                                }
                                            }
                                            .awaitAll()
                                    }
                                    check(auth.user.value?.uid == uid)
                                    RequestListState(
                                        false,
                                        items.sortedWith(
                                            compareByDescending<AdoptionRequest> { it.createdAt }
                                                .thenBy { it.id }
                                        ),
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    RequestListState(
                                        false,
                                        error =
                                            "No pudimos leer tus solicitudes completas. Reintenta.",
                                    )
                                }
                        }
                    }
                    .transformLatest { state ->
                        emit(state)
                        if (state.loading) {
                            delay(waitMillis)
                            emit(
                                RequestListState(
                                    false,
                                    error =
                                        "No hay confirmación del servidor. Revisa tu conexión y reintenta.",
                                )
                            )
                        }
                    }
        }

    private fun observeDetail(
        uid: String,
        shelterId: String?,
        id: String,
    ): Flow<RequestDetailState> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(
                    RequestDetailState(
                        false,
                        error = "Inicia sesión para consultar esta solicitud.",
                    )
                )
            else {
                val request =
                    scopedQuery("solicitudes", uid, shelterId)
                        .whereEqualTo(FieldPath.documentId(), id)
                        .serverValues()
                val history =
                    scopedQuery("historial_solicitudes", uid, shelterId)
                        .whereEqualTo("solicitudId", id)
                        .serverValues()
                combine(request, history) { record, events -> record to events }
                    .mapLatest { (record, events) ->
                        when {
                            record == QueryValue.Failed || events == QueryValue.Failed ->
                                RequestDetailState(
                                    false,
                                    error =
                                        "No pudimos confirmar la solicitud y su historial. Reintenta.",
                                )
                            record !is QueryValue.Ready || events !is QueryValue.Ready ->
                                RequestDetailState()
                            record.snapshot.isEmpty ->
                                RequestDetailState(
                                    false,
                                    error = "Esta solicitud no existe o no pertenece a tu cuenta.",
                                )
                            else ->
                                try {
                                    val item =
                                        withDogName(
                                            parseScoped(
                                                record.snapshot.documents.single(),
                                                uid,
                                                shelterId,
                                            )
                                        )
                                    val changes =
                                        events.snapshot.documents
                                            .map { event ->
                                                check(
                                                    event.getString("usuarioUid") == item.uid &&
                                                        event.getString("refugioId") ==
                                                            item.shelterId &&
                                                        event.getString("solicitudId") == id
                                                )
                                                RequestHistory(
                                                    event.id,
                                                    requireNotNull(event.getString("estadoNuevo")),
                                                    requireNotNull(event.getTimestamp("creadoEn"))
                                                        .toDate()
                                                        .time,
                                                    event.getString("motivo"),
                                                )
                                            }
                                            .sortedWith(
                                                compareBy<RequestHistory> { it.timestamp }
                                                    .thenBy { it.id }
                                            )
                                    check(auth.user.value?.uid == uid)
                                    RequestDetailState(false, item, changes)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    RequestDetailState(
                                        false,
                                        error =
                                            "La solicitud está incompleta. Reintenta o consulta al refugio.",
                                    )
                                }
                        }
                    }
                    .transformLatest { state ->
                        emit(state)
                        if (state.loading) {
                            delay(waitMillis)
                            emit(
                                RequestDetailState(
                                    false,
                                    error =
                                        "No hay confirmación del servidor. Revisa tu conexión y reintenta.",
                                )
                            )
                        }
                    }
            }
        }

    private suspend fun withDogName(request: AdoptionRequest): AdoptionRequest {
        val name =
            try {
                withTimeout(3_000) {
                    db!!
                        .collection("perritos")
                        .document(request.dogId)
                        .get(Source.SERVER)
                        .await()
                        .getString("nombre")
                }
            } catch (_: TimeoutCancellationException) {
                null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        return request.copy(dogName = name)
    }

    private fun Query.serverValues(): Flow<QueryValue> = callbackFlow {
        trySend(QueryValue.Waiting)
        val listener =
            addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                trySend(
                    when {
                        error != null || snapshot == null -> QueryValue.Failed
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                            QueryValue.Waiting
                        else -> QueryValue.Ready(snapshot)
                    }
                )
            }
        awaitClose { listener.remove() }
    }

    private sealed interface QueryValue {
        data object Waiting : QueryValue

        data object Failed : QueryValue

        data class Ready(val snapshot: QuerySnapshot) : QueryValue
    }
}

internal fun parseRequest(doc: DocumentSnapshot, uid: String): AdoptionRequest {
    check(doc.getString("usuarioUid") == uid)
    val contact = doc.get("contacto") as Map<*, *>
    val home = doc.get("hogar") as Map<*, *>
    val pets = doc.get("otrasMascotas") as Map<*, *>
    return AdoptionRequest(
        doc.id,
        uid,
        requireNotNull(doc.getString("perritoId")),
        requireNotNull(doc.getString("refugioId")),
        requireNotNull(doc.getString("folio")),
        requireNotNull(doc.getString("estado")),
        ApplicationDraft(
            contact["nombre"] as String,
            contact["correo"] as String,
            contact["telefono"] as String,
            home["tipoVivienda"] as String,
            home["tenencia"] as String,
            home["espacioExterior"] as Boolean,
            (home["personas"] as Number).toInt().toString(),
            home["horasSolo"] as String,
            pets["tiene"] as Boolean,
            pets["detalle"] as? String ?: "",
            requireNotNull(doc.getString("motivos")),
        ),
        requireNotNull(doc.getTimestamp("creadoEn")).toDate().time,
        requireNotNull(doc.getTimestamp("actualizadoEn")).toDate().time,
        doc.getString("motivoNoAprobacion"),
    )
}
