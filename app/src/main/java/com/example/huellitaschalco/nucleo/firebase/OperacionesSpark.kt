package com.example.huellitaschalco.nucleo.firebase

import com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.dominio.nextReviewId
import com.example.huellitaschalco.nucleo.fechas.civilDateMillis
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await

/** Operaciones sin Functions. Las reglas validan permisos, estados y escrituras relacionadas. */
class OperacionesSpark(private val db: FirebaseFirestore, private val auth: FirebaseAuth) :
    OperacionesInvocables {
    private fun date(value: Any?): Timestamp =
        Timestamp(java.util.Date(requireNotNull(civilDateMillis(value as String))))

    private fun ref(collection: String, id: String) = db.collection(collection).document(id)

    private fun reject(message: String): Nothing =
        throw FalloOperacion("FAILED_PRECONDITION", message)

    private val now
        get() = FieldValue.serverTimestamp()

    override suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?> {
        val uid = requireNotNull(auth.currentUser).uid
        val shelter = data["shelterId"] as String
        fun text(key: String) = data[key] as String
        return when (name) {
            "recordAdoption" -> {
                val dogId = text("dogId")
                val requestId = text("requestId")
                val delivery = date(data["date"])
                require(data["signedLetter"] == true)
                val existing =
                    db.runTransaction { tx ->
                            val adoptionRef = ref("adopciones", dogId)
                            val adoption = tx.get(adoptionRef)
                            val application = tx.get(ref("solicitudes", requestId))
                            val dog = tx.get(ref("perritos", dogId))
                            val control = tx.get(ref("perritos_control", dogId))
                            check(
                                application.getString("refugioId") == shelter &&
                                    application.getString("perritoId") == dogId &&
                                    dog.getString("refugioId") == shelter
                            )
                            if (adoption.exists()) {
                                if (
                                    adoption.getString("solicitudId") != requestId ||
                                        adoption.getTimestamp("fechaEntrega") != delivery ||
                                        adoption.getString("registradoPorUid") != uid
                                )
                                    reject("Este perrito ya tiene otra entrega registrada.")
                                true
                            } else {
                                if (
                                    application.getString("estado") != "Aprobada" ||
                                        dog.getString("estado") != "En proceso" ||
                                        control.getString("solicitudAprobadaId") != requestId ||
                                        control.get("adopcionId") != null
                                )
                                    reject(
                                        "Se necesita una solicitud aprobada y un perrito En proceso."
                                    )
                                tx.set(
                                    adoptionRef,
                                    mapOf(
                                        "solicitudId" to requestId,
                                        "perritoId" to dogId,
                                        "usuarioUid" to application.getString("usuarioUid"),
                                        "refugioId" to shelter,
                                        "fechaEntrega" to delivery,
                                        "cartaFirmada" to true,
                                        "registradoPorUid" to uid,
                                        "creadoEn" to now,
                                    ),
                                )
                                tx.update(
                                    dog.reference,
                                    mapOf("estado" to "Adoptado", "actualizadoEn" to now),
                                )
                                tx.update(control.reference, "adopcionId", dogId)
                                false
                            }
                        }
                        .await()
                closePending(uid, shelter, dogId)
                data + mapOf("id" to dogId, "alreadyExisting" to existing)
            }
            "completeAdoptionRequests" -> {
                closePending(uid, shelter, text("dogId"))
                emptyMap()
            }
            "programReview" -> {
                val id = text("id")
                val adoptionId = text("adoptionId")
                val planned = date(data["date"])
                val existing =
                    db.runTransaction { tx ->
                            val adoption = tx.get(ref("adopciones", adoptionId))
                            val review = tx.get(ref("revisiones", id))
                            check(adoption.getString("refugioId") == shelter)
                            if (review.exists()) {
                                if (
                                    review.getString("adopcionId") != adoptionId ||
                                        review.getString("refugioId") != shelter ||
                                        review.getString("tipo") != text("type") ||
                                        review.getTimestamp("fechaProgramada") != planned
                                )
                                    reject("Este intento ya programó otra revisión.")
                                true
                            } else {
                                tx.set(
                                    review.reference,
                                    reviewFields(adoptionId, shelter, planned, text("type")),
                                )
                                false
                            }
                        }
                        .await()
                data + mapOf("alreadyExisting" to existing)
            }
            "recordFollowUp" -> {
                val id = text("reviewId")
                val done = date(data["date"])
                @Suppress("UNCHECKED_CAST") val next = data["next"] as? Map<String, Any?>
                val nextId = next?.let { nextReviewId(id) }
                val existing =
                    db.runTransaction { tx ->
                            val review = tx.get(ref("revisiones", id))
                            val result = tx.get(ref("seguimientos", id))
                            val adoptionId = requireNotNull(review.getString("adopcionId"))
                            val adoption = tx.get(ref("adopciones", adoptionId))
                            val nextDoc = next?.let { tx.get(ref("revisiones", nextReviewId(id))) }
                            check(
                                review.getString("refugioId") == shelter &&
                                    adoption.getString("refugioId") == shelter
                            )
                            val path = data["photo"] as? String
                            val photos =
                                path?.let {
                                    listOf(
                                        mapOf(
                                            "archivoId" to
                                                it.substringAfterLast('/').removeSuffix(".jpg"),
                                            "ruta" to it,
                                            "orden" to 0L,
                                        )
                                    )
                                } ?: emptyList()
                            val fields =
                                mapOf(
                                    "adopcionId" to adoptionId,
                                    "revisionId" to id,
                                    "perritoId" to review.getString("perritoId"),
                                    "refugioId" to shelter,
                                    "fechaRealizacion" to done,
                                    "adaptacion" to data["adaptation"],
                                    "observaciones" to data["notes"],
                                    "fotos" to photos,
                                    "registradoPorUid" to uid,
                                    "siguienteRevisionId" to nextId,
                                )
                            if (result.exists()) {
                                if (
                                    !fields.all { (key, value) -> result.get(key) == value } ||
                                        (next != null &&
                                            (nextDoc?.getTimestamp("fechaProgramada") !=
                                                date(next["date"]) ||
                                                nextDoc?.getString("tipo") != next["type"]))
                                )
                                    reject("Esta revisión ya tiene otro resultado guardado.")
                                true
                            } else {
                                if (
                                    review.getString("estado") != "PROGRAMADA" ||
                                        nextDoc?.exists() == true
                                )
                                    reject("La revisión ya no admite un resultado nuevo.")
                                tx.set(result.reference, fields + ("creadoEn" to now))
                                tx.update(
                                    review.reference,
                                    mapOf("estado" to "REALIZADA", "seguimientoId" to id),
                                )
                                if (next != null)
                                    tx.set(
                                        requireNotNull(nextDoc).reference,
                                        reviewFields(
                                            adoptionId,
                                            shelter,
                                            date(next["date"]),
                                            next["type"] as String,
                                        ) + ("revisionOrigenId" to id),
                                    )
                                false
                            }
                        }
                        .await()
                data + mapOf("id" to id, "nextReviewId" to nextId, "alreadyExisting" to existing)
            }
            else -> error("Operación desconocida")
        }
    }

    private suspend fun closePending(uid: String, shelter: String, dogId: String) {
        val adoption = ref("adopciones", dogId).get(Source.SERVER).await()
        check(adoption.getString("refugioId") == shelter)
        val requestId = requireNotNull(adoption.getString("solicitudId"))
        val others =
            db.collection("solicitudes")
                .whereEqualTo("refugioId", shelter)
                .whereEqualTo("perritoId", dogId)
                .get(Source.SERVER)
                .await()
        for (other in others.documents.filter { it.id != requestId }) {
            db.runTransaction { tx ->
                    val current = tx.get(other.reference)
                    if (current.getString("estado") in listOf("Enviada", "En revisión")) {
                        val reason = "El perrito fue adoptado por otra persona."
                        tx.update(
                            other.reference,
                            mapOf(
                                "estado" to "No aprobada",
                                "motivoNoAprobacion" to reason,
                                "actualizadoEn" to now,
                            ),
                        )
                        tx.set(
                            ref("historial_solicitudes", "${other.id}-no-aprobada"),
                            mapOf(
                                "solicitudId" to other.id,
                                "usuarioUid" to current.getString("usuarioUid"),
                                "refugioId" to shelter,
                                "estadoAnterior" to current.getString("estado"),
                                "estadoNuevo" to "No aprobada",
                                "motivo" to reason,
                                "actorUid" to uid,
                                "creadoEn" to now,
                            ),
                        )
                    }
                }
                .await()
        }
    }

    private fun reviewFields(
        adoption: String,
        shelter: String,
        planned: Timestamp,
        type: String,
    ): Map<String, Any?> =
        mapOf(
            "adopcionId" to adoption,
            "perritoId" to adoption,
            "refugioId" to shelter,
            "fechaProgramada" to planned,
            "tipo" to type,
            "estado" to "PROGRAMADA",
            "seguimientoId" to null,
            "creadoEn" to now,
        )
}
