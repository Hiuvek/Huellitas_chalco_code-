package com.example.huellitaschalco.feature.application.data

import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.refuge.domain.AdoptionPolicy
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await

class FirebaseAdoptionApplicationRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore?,
) : AdoptionApplicationRepository {
    override suspend fun submit(attempt: ApplicationAttempt): ApplicationReceipt {
        require(auth.user.value?.uid == attempt.uid)
        require(attempt.draft.errors().isEmpty())
        require(
            attempt.id.matches(
                Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            )
        )
        val database = requireNotNull(db)
        val key =
            database
                .collection("solicitudes_claves")
                .document(applicationKey(attempt.uid, attempt.dogId))
        // Evita confirmar o dejar una escritura local en cola cuando no hay servidor.
        val initial = key.get(Source.SERVER).await()
        if (initial.exists())
            return receipt(
                attempt,
                database
                    .collection("solicitudes")
                    .document(requireNotNull(initial.getString("solicitudId")))
                    .get(Source.SERVER)
                    .await(),
            )
        val result =
            try {
                database
                    .runTransaction { tx ->
                        require(auth.user.value?.uid == attempt.uid)
                        val previous = tx.get(key)
                        if (previous.exists()) {
                            val existing =
                                tx.get(
                                    database
                                        .collection("solicitudes")
                                        .document(requireNotNull(previous.getString("solicitudId")))
                                )
                            TransactionResult(receipt(attempt, existing))
                        } else {
                            val dog =
                                tx.get(database.collection("perritos").document(attempt.dogId))
                            val refuge =
                                tx.get(database.collection("refugios").document(attempt.shelterId))
                            when {
                                !dog.exists() ||
                                    dog.get("publicado") != true ||
                                    dog.get("estado") != "Disponible" ||
                                    dog.get("refugioId") != attempt.shelterId ->
                                    TransactionResult(
                                        error = "Este perrito ya no admite nuevas solicitudes."
                                    )
                                refuge.get("activo") != true ||
                                    refuge.get("municipio") != "Chalco" ||
                                    refuge.get("entidad") != "Estado de México" ||
                                    AdoptionPolicy.fromStored(
                                        refuge.get("requisitos"),
                                        refuge.get("pasos"),
                                    ) == null ->
                                    TransactionResult(
                                        error =
                                            "El refugio no tiene requisitos completos disponibles."
                                    )
                                else -> {
                                    tx.set(
                                        database.collection("solicitudes").document(attempt.id),
                                        attempt.fields() +
                                            mapOf(
                                                "creadoEn" to FieldValue.serverTimestamp(),
                                                "actualizadoEn" to FieldValue.serverTimestamp(),
                                            ),
                                    )
                                    tx.set(
                                        key,
                                        mapOf(
                                            "usuarioUid" to attempt.uid,
                                            "perritoId" to attempt.dogId,
                                            "solicitudId" to attempt.id,
                                        ),
                                    )
                                    tx.set(
                                        database
                                            .collection("historial_solicitudes")
                                            .document("${attempt.id}-1"),
                                        mapOf(
                                            "solicitudId" to attempt.id,
                                            "usuarioUid" to attempt.uid,
                                            "refugioId" to attempt.shelterId,
                                            "estadoAnterior" to null,
                                            "estadoNuevo" to "Enviada",
                                            "motivo" to null,
                                            "actorUid" to attempt.uid,
                                            "creadoEn" to FieldValue.serverTimestamp(),
                                        ),
                                    )
                                    TransactionResult(
                                        ApplicationReceipt(
                                            attempt.id,
                                            attempt.folio,
                                            attempt.dogId,
                                            attempt.shelterId,
                                            "Enviada",
                                        )
                                    )
                                }
                            }
                        }
                    }
                    .await()
            } catch (error: FirebaseFirestoreException) {
                // Una escritura competidora puede ser rechazada antes del reintento del SDK.
                // Solo se confirma si el servidor permite leer la solicitud del mismo propietario.
                if (auth.user.value?.uid == attempt.uid) {
                    val confirmed =
                        try {
                            val existing = key.get(Source.SERVER).await()
                            if (existing.exists())
                                receipt(
                                    attempt,
                                    database
                                        .collection("solicitudes")
                                        .document(requireNotNull(existing.getString("solicitudId")))
                                        .get(Source.SERVER)
                                        .await(),
                                )
                            else null
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    if (confirmed != null) return confirmed
                }
                throw error
            }
        result.error?.let { throw ApplicationUnavailable(it) }
        return requireNotNull(result.receipt)
    }

    private fun receipt(
        attempt: ApplicationAttempt,
        request: DocumentSnapshot,
    ): ApplicationReceipt {
        check(
            request.exists() &&
                request.getString("usuarioUid") == attempt.uid &&
                request.getString("perritoId") == attempt.dogId
        )
        if (request.id == attempt.id)
            check(
                attempt
                    .fields()
                    .filterKeys { it !in setOf("estado", "motivoNoAprobacion") }
                    .all { (field, value) -> request.get(field) == value }
            )
        return ApplicationReceipt(
            request.id,
            requireNotNull(request.getString("folio")),
            attempt.dogId,
            requireNotNull(request.getString("refugioId")),
            requireNotNull(request.getString("estado")),
            request.id != attempt.id,
        )
    }

    private data class TransactionResult(
        val receipt: ApplicationReceipt? = null,
        val error: String? = null,
    )
}
