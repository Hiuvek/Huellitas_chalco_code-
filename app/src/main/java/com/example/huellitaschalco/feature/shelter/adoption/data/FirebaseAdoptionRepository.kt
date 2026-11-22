package com.example.huellitaschalco.feature.shelter.adoption.data

import com.example.huellitaschalco.core.firebase.CallableFailure
import com.example.huellitaschalco.core.firebase.CallableFunctions
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.shelter.adoption.domain.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAdoptionRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore?,
    private val functions: CallableFunctions?,
) : AdoptionRepository {
    override fun observeDog(uid: String, shelterId: String, dogId: String): Flow<AdoptionState> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(AdoptionState(false, error = "Inicia sesión para consultar la entrega."))
            else
                callbackFlow {
                    trySend(AdoptionState())
                    val listener =
                        db.collection("adopciones")
                            .whereEqualTo("refugioId", shelterId)
                            .whereEqualTo("perritoId", dogId)
                            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                                val result =
                                    when {
                                        error != null || snapshot == null ->
                                            AdoptionState(
                                                false,
                                                error =
                                                    "No pudimos consultar la entrega. Revisa tus permisos y conexión.",
                                            )
                                        snapshot.metadata.isFromCache ||
                                            snapshot.metadata.hasPendingWrites() -> AdoptionState()
                                        else -> runCatching {
                                                AdoptionState(
                                                    false,
                                                    snapshot.documents.singleOrNull()?.let { doc ->
                                                        Adoption(
                                                            doc.id,
                                                            requireNotNull(
                                                                doc.getString("solicitudId")
                                                            ),
                                                            requireNotNull(
                                                                doc.getString("perritoId")
                                                            ),
                                                            requireNotNull(
                                                                doc.getString("usuarioUid")
                                                            ),
                                                            requireNotNull(
                                                                doc.getString("refugioId")
                                                            ),
                                                            requireNotNull(
                                                                    doc.getTimestamp("fechaEntrega")
                                                                )
                                                                .toDate()
                                                                .time,
                                                            doc.getBoolean("cartaFirmada") == true,
                                                            requireNotNull(
                                                                doc.getString("registradoPorUid")
                                                            ),
                                                            requireNotNull(
                                                                    doc.getTimestamp("creadoEn")
                                                                )
                                                                .toDate()
                                                                .time,
                                                        )
                                                    },
                                                )
                                            }
                                                .getOrElse {
                                                    AdoptionState(
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
                                AdoptionState(
                                    false,
                                    error =
                                        "No hay confirmación del servidor. Revisa la conexión y reintenta.",
                                )
                            )
                        }
                    }
        }

    override suspend fun record(attempt: AdoptionAttempt): AdoptionReceipt {
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
            } catch (error: CallableFailure) {
                if (
                    error.status in
                        setOf(
                            "INVALID_ARGUMENT",
                            "FAILED_PRECONDITION",
                            "PERMISSION_DENIED",
                            "ALREADY_EXISTS",
                        )
                )
                    throw AdoptionRejected(
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
        return AdoptionReceipt(
            result["id"] as String,
            attempt.requestId,
            attempt.dogId,
            attempt.shelterId,
            attempt.date,
            result["alreadyExisting"] == true,
        )
    }
}
