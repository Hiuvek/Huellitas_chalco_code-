package com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.datos

import com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.dominio.*
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.example.huellitaschalco.nucleo.firebase.FalloOperacion
import com.example.huellitaschalco.nucleo.firebase.OperacionesInvocables
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class RepositorioAdopcionFirebase(
    private val auth: RepositorioAutenticacion,
    private val db: FirebaseFirestore?,
    private val functions: OperacionesInvocables?,
) : RepositorioAdopcion {
    override fun observeDog(uid: String, shelterId: String, dogId: String): Flow<EstadoAdopcion> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(EstadoAdopcion(false, error = "Inicia sesión para consultar la entrega."))
            else
                callbackFlow {
                        trySend(EstadoAdopcion())
                        val listener =
                            db.collection("adopciones")
                                .whereEqualTo("refugioId", shelterId)
                                .whereEqualTo("perritoId", dogId)
                                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                                    val result =
                                        when {
                                            error != null || snapshot == null ->
                                                EstadoAdopcion(
                                                    false,
                                                    error =
                                                        "No pudimos consultar la entrega. Revisa tus permisos y conexión.",
                                                )
                                            snapshot.metadata.isFromCache ||
                                                snapshot.metadata.hasPendingWrites() ->
                                                EstadoAdopcion()
                                            else ->
                                                runCatching {
                                                        EstadoAdopcion(
                                                            false,
                                                            snapshot.documents
                                                                .singleOrNull()
                                                                ?.let { doc ->
                                                                    Adopcion(
                                                                        doc.id,
                                                                        requireNotNull(
                                                                            doc.getString(
                                                                                "solicitudId"
                                                                            )
                                                                        ),
                                                                        requireNotNull(
                                                                            doc.getString(
                                                                                "perritoId"
                                                                            )
                                                                        ),
                                                                        requireNotNull(
                                                                            doc.getString(
                                                                                "usuarioUid"
                                                                            )
                                                                        ),
                                                                        requireNotNull(
                                                                            doc.getString(
                                                                                "refugioId"
                                                                            )
                                                                        ),
                                                                        requireNotNull(
                                                                                doc.getTimestamp(
                                                                                    "fechaEntrega"
                                                                                )
                                                                            )
                                                                            .toDate()
                                                                            .time,
                                                                        doc.getBoolean(
                                                                            "cartaFirmada"
                                                                        ) == true,
                                                                        requireNotNull(
                                                                            doc.getString(
                                                                                "registradoPorUid"
                                                                            )
                                                                        ),
                                                                        requireNotNull(
                                                                                doc.getTimestamp(
                                                                                    "creadoEn"
                                                                                )
                                                                            )
                                                                            .toDate()
                                                                            .time,
                                                                    )
                                                                },
                                                        )
                                                    }
                                                    .getOrElse {
                                                        EstadoAdopcion(
                                                            false,
                                                            error =
                                                                "La entrega está incompleta. Consulta al responsable.",
                                                        )
                                                    }
                                        }
                                    trySend(result)
                                }
                        awaitClose { listener.remove() }
                    }
                    .transformLatest { value ->
                        emit(value)
                        if (value.loading) {
                            delay(12_000)
                            emit(
                                EstadoAdopcion(
                                    false,
                                    error =
                                        "No hay confirmación del servidor. Revisa la conexión y reintenta.",
                                )
                            )
                        }
                    }
        }

    override suspend fun completePending(uid: String, shelterId: String, dogId: String) {
        require(auth.user.value?.uid == uid)
        requireNotNull(functions)
            .call("completeAdoptionRequests", mapOf("shelterId" to shelterId, "dogId" to dogId))
    }

    override suspend fun record(attempt: IntentoAdopcion): ComprobanteAdopcion {
        require(auth.user.value?.uid == attempt.uid)
        require(attempt.error() == null)
        val result =
            try {
                requireNotNull(functions)
                    .call(
                        "recordAdoption",
                        mapOf(
                            "requestId" to attempt.requestId,
                            "shelterId" to attempt.shelterId,
                            "dogId" to attempt.dogId,
                            "date" to attempt.date,
                            "signedLetter" to attempt.signedLetter,
                        ),
                    )
            } catch (error: FalloOperacion) {
                if (
                    error.status in
                        setOf(
                            "INVALID_ARGUMENT",
                            "FAILED_PRECONDITION",
                            "PERMISSION_DENIED",
                            "ALREADY_EXISTS",
                        )
                )
                    throw AdopcionRechazada(
                        error.message ?: "La entrega no cumple las condiciones necesarias."
                    )
                throw error
            }
        check(
            result["id"] == attempt.dogId &&
                result["requestId"] == attempt.requestId &&
                result["shelterId"] == attempt.shelterId &&
                result["date"] == attempt.date
        )
        return ComprobanteAdopcion(
            result["id"] as String,
            attempt.requestId,
            attempt.dogId,
            attempt.shelterId,
            attempt.date,
            result["alreadyExisting"] == true,
        )
    }
}
