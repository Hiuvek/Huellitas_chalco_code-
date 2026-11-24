package com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.datos

import com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.dominio.*
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await

class RepositorioEvaluacionFirebase(
    private val auth: RepositorioAutenticacion,
    private val db: FirebaseFirestore?,
) : RepositorioEvaluacion {
    override suspend fun decide(attempt: IntentoEvaluacion) {
        require(attempt.error() == null)
        require(auth.user.value?.uid == attempt.uid)
        val database = requireNotNull(db)
        val reference = database.collection("solicitudes").document(attempt.requestId)
        reference.get(Source.SERVER).await()
        val error =
            database
                .runTransaction { tx ->
                    check(auth.user.value?.uid == attempt.uid)
                    val request = tx.get(reference)
                    if (request.getString("refugioId") != attempt.shelterId)
                        return@runTransaction "La solicitud no pertenece a este refugio."
                    val status = request.getString("estado")
                    val reason =
                        attempt.reason.trim().takeIf {
                            attempt.decision == DecisionEvaluacion.Reject
                        }
                    val historyRef =
                        database.collection("historial_solicitudes").document(attempt.historyId)
                    if (status == attempt.decision.status) {
                        val history = tx.get(historyRef)
                        return@runTransaction if (
                            history.getString("actorUid") == attempt.uid &&
                                history.get("motivo") == reason &&
                                history.get("estadoAnterior") == attempt.previous
                        )
                            null
                        else "Otra persona ya registró esta decisión. Recarga la solicitud."
                    }
                    if (status != attempt.previous)
                        return@runTransaction "El estado cambió. Revisa la solicitud actualizada antes de decidir."
                    val dogId = requireNotNull(request.getString("perritoId"))
                    val dogRef = database.collection("perritos").document(dogId)
                    val controlRef = database.collection("perritos_control").document(dogId)
                    val dog = tx.get(dogRef)
                    val control = tx.get(controlRef)
                    if (
                        dog.getString("refugioId") != attempt.shelterId ||
                            control.getString("refugioId") != attempt.shelterId ||
                            control.get("adopcionId") != null ||
                            dog.getString("estado") == "Adoptado"
                    )
                        return@runTransaction "La adopción ya fue registrada o el perrito no pertenece al refugio."
                    if (
                        attempt.decision == DecisionEvaluacion.Approve &&
                            (dog.get("publicado") != true ||
                                dog.get("estado") != "Disponible" ||
                                control.get("solicitudAprobadaId") != null)
                    )
                        return@runTransaction "Ya hay una solicitud aprobada o el perrito dejó de estar disponible."
                    tx.update(
                        reference,
                        mapOf(
                            "estado" to attempt.decision.status,
                            "motivoNoAprobacion" to reason,
                            "actualizadoEn" to FieldValue.serverTimestamp(),
                        ),
                    )
                    tx.set(
                        historyRef,
                        mapOf(
                            "solicitudId" to attempt.requestId,
                            "usuarioUid" to request.getString("usuarioUid"),
                            "refugioId" to attempt.shelterId,
                            "estadoAnterior" to attempt.previous,
                            "estadoNuevo" to attempt.decision.status,
                            "motivo" to reason,
                            "actorUid" to attempt.uid,
                            "creadoEn" to FieldValue.serverTimestamp(),
                        ),
                    )
                    if (attempt.decision == DecisionEvaluacion.Approve) {
                        tx.update(controlRef, "solicitudAprobadaId", attempt.requestId)
                        tx.update(
                            dogRef,
                            mapOf(
                                "estado" to "En proceso",
                                "actualizadoEn" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    null
                }
                .await()
        if (error != null) throw ConflictoEvaluacion(error)
    }
}
