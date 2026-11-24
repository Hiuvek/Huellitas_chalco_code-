package com.example.huellitaschalco.funcionalidades.solicitudes.datos

import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.example.huellitaschalco.funcionalidades.solicitud.dominio.BorradorSolicitud
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class RepositorioSolicitudesAdopcionFirebase(
    private val auth: RepositorioAutenticacion,
    private val db: FirebaseFirestore?,
    private val waitMillis: Long = 12_000,
) : RepositorioSolicitudesAdopcion, RepositorioSolicitudesRecibidas {
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
    ): SolicitudAdopcionRecibida {
        if (shelterId != null) check(doc.getString("refugioId") == shelterId)
        return parseRequest(
            doc,
            if (shelterId == null) uid else requireNotNull(doc.getString("usuarioUid")),
        )
    }

    private fun observeList(uid: String, shelterId: String?): Flow<EstadoListaSolicitudes> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(
                    EstadoListaSolicitudes(false, error = "Inicia sesión para consultar tus solicitudes.")
                )
            else
                scopedQuery("solicitudes", uid, shelterId)
                    .serverValues()
                    .mapLatest { result ->
                        when (result) {
                            ValorConsulta.EnEspera -> EstadoListaSolicitudes()
                            ValorConsulta.Fallido ->
                                EstadoListaSolicitudes(
                                    false,
                                    error =
                                        "No pudimos confirmar tus solicitudes. Revisa la conexión y reintenta.",
                                )
                            is ValorConsulta.Listo ->
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
                                    EstadoListaSolicitudes(
                                        false,
                                        items.sortedWith(
                                            compareByDescending<SolicitudAdopcionRecibida> { it.createdAt }
                                                .thenBy { it.id }
                                        ),
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    EstadoListaSolicitudes(
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
                                EstadoListaSolicitudes(
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
    ): Flow<EstadoDetalleSolicitud> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(
                    EstadoDetalleSolicitud(
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
                            record == ValorConsulta.Fallido || events == ValorConsulta.Fallido ->
                                EstadoDetalleSolicitud(
                                    false,
                                    error =
                                        "No pudimos confirmar la solicitud y su historial. Reintenta.",
                                )
                            record !is ValorConsulta.Listo || events !is ValorConsulta.Listo ->
                                EstadoDetalleSolicitud()
                            record.snapshot.isEmpty ->
                                EstadoDetalleSolicitud(
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
                                                HistorialSolicitud(
                                                    event.id,
                                                    requireNotNull(event.getString("estadoNuevo")),
                                                    requireNotNull(event.getTimestamp("creadoEn"))
                                                        .toDate()
                                                        .time,
                                                    event.getString("motivo"),
                                                )
                                            }
                                            .sortedWith(
                                                compareBy<HistorialSolicitud> { it.timestamp }
                                                    .thenBy { it.id }
                                            )
                                    check(auth.user.value?.uid == uid)
                                    EstadoDetalleSolicitud(false, item, changes)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    EstadoDetalleSolicitud(
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
                                EstadoDetalleSolicitud(
                                    false,
                                    error =
                                        "No hay confirmación del servidor. Revisa tu conexión y reintenta.",
                                )
                            )
                        }
                    }
            }
        }

    private suspend fun withDogName(request: SolicitudAdopcionRecibida): SolicitudAdopcionRecibida {
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

    private fun Query.serverValues(): Flow<ValorConsulta> = callbackFlow {
        trySend(ValorConsulta.EnEspera)
        val listener =
            addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                trySend(
                    when {
                        error != null || snapshot == null -> ValorConsulta.Fallido
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                            ValorConsulta.EnEspera
                        else -> ValorConsulta.Listo(snapshot)
                    }
                )
            }
        awaitClose { listener.remove() }
    }

    private sealed interface ValorConsulta {
        data object EnEspera : ValorConsulta

        data object Fallido : ValorConsulta

        data class Listo(val snapshot: QuerySnapshot) : ValorConsulta
    }
}

internal fun parseRequest(doc: DocumentSnapshot, uid: String): SolicitudAdopcionRecibida {
    check(doc.getString("usuarioUid") == uid)
    val contact = doc.get("contacto") as Map<*, *>
    val home = doc.get("hogar") as Map<*, *>
    val pets = doc.get("otrasMascotas") as Map<*, *>
    return SolicitudAdopcionRecibida(
        doc.id,
        uid,
        requireNotNull(doc.getString("perritoId")),
        requireNotNull(doc.getString("refugioId")),
        requireNotNull(doc.getString("folio")),
        requireNotNull(doc.getString("estado")),
        BorradorSolicitud(
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
