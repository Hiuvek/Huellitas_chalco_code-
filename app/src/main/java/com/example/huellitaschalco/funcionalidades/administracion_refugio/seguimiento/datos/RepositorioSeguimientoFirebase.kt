package com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.datos

import com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.dominio.Adopcion
import com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.dominio.*
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.RepositorioAutenticacion
import com.example.huellitaschalco.nucleo.firebase.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class RepositorioSeguimientoFirebase(
    private val auth: RepositorioAutenticacion,
    private val db: FirebaseFirestore?,
    private val functions: OperacionesInvocables?,
) : RepositorioSeguimiento {
    override fun observe(uid: String, shelterId: String): Flow<AgendaSeguimiento> =
        auth.user.flatMapLatest { user ->
            if (user?.uid != uid || db == null)
                flowOf(AgendaSeguimiento(false, error = "Inicia sesión para consultar la agenda."))
            else
                combine(
                        listOf(
                                "adopciones",
                                "revisiones",
                                "perritos",
                                "solicitudes",
                                "seguimientos",
                            )
                            .map { name ->
                                db.collection(name)
                                    .whereEqualTo("refugioId", shelterId)
                                    .serverDocuments()
                            }
                    ) { values ->
                        when {
                            values.any { it == DocumentosServidor.Fallido } ->
                                AgendaSeguimiento(
                                    false,
                                    error =
                                        "No pudimos confirmar la agenda. Revisa tus permisos y conexión.",
                                )
                            values.any { it !is DocumentosServidor.Listo } -> AgendaSeguimiento()
                            else ->
                                try {
                                    val docs =
                                        values.map { (it as DocumentosServidor.Listo).documents }
                                    val dogs =
                                        docs[2].associate {
                                            it.id to (it.getString("nombre") ?: it.id)
                                        }
                                    val people =
                                        docs[3].associate {
                                            it.id to
                                                ((it.get("contacto") as? Map<*, *>)?.get("nombre")
                                                    as? String)
                                        }
                                    val adoptions =
                                        docs[0]
                                            .map { d ->
                                                val a =
                                                    Adopcion(
                                                        d.id,
                                                        requireNotNull(d.getString("solicitudId")),
                                                        requireNotNull(d.getString("perritoId")),
                                                        requireNotNull(d.getString("usuarioUid")),
                                                        requireNotNull(d.getString("refugioId")),
                                                        requireNotNull(
                                                                d.getTimestamp("fechaEntrega")
                                                            )
                                                            .toDate()
                                                            .time,
                                                        d.getBoolean("cartaFirmada") == true,
                                                        requireNotNull(
                                                            d.getString("registradoPorUid")
                                                        ),
                                                        requireNotNull(d.getTimestamp("creadoEn"))
                                                            .toDate()
                                                            .time,
                                                    )
                                                check(a.shelterId == shelterId && a.id == a.dogId)
                                                PerritoAdoptado(
                                                    a,
                                                    dogs[a.dogId] ?: a.dogId,
                                                    people[a.requestId] ?: "Cuenta ${a.uid}",
                                                )
                                            }
                                            .sortedByDescending { it.adoption.deliveryAt }
                                    val reviews =
                                        docs[1]
                                            .map { d ->
                                                RevisionProgramada(
                                                    d.id,
                                                    requireNotNull(d.getString("adopcionId")),
                                                    requireNotNull(d.getString("perritoId")),
                                                    requireNotNull(d.getString("refugioId")),
                                                    requireNotNull(
                                                            d.getTimestamp("fechaProgramada")
                                                        )
                                                        .toDate()
                                                        .time,
                                                    requireNotNull(d.getString("tipo")),
                                                    requireNotNull(d.getString("estado")),
                                                    d.getString("seguimientoId"),
                                                    requireNotNull(d.getTimestamp("creadoEn"))
                                                        .toDate()
                                                        .time,
                                                )
                                            }
                                            .sortedWith(
                                                compareBy<RevisionProgramada> { it.plannedAt }
                                                    .thenBy { it.id }
                                            )
                                    val results =
                                        docs[4]
                                            .mapNotNull { d ->
                                                runCatching {
                                                        val r =
                                                            ResultadoSeguimiento(
                                                                d.id,
                                                                requireNotNull(
                                                                    d.getString("adopcionId")
                                                                ),
                                                                requireNotNull(
                                                                    d.getString("revisionId")
                                                                ),
                                                                requireNotNull(
                                                                    d.getString("perritoId")
                                                                ),
                                                                requireNotNull(
                                                                    d.getString("refugioId")
                                                                ),
                                                                requireNotNull(
                                                                        d.getTimestamp(
                                                                            "fechaRealizacion"
                                                                        )
                                                                    )
                                                                    .toDate()
                                                                    .time,
                                                                requireNotNull(
                                                                    d.getString("adaptacion")
                                                                ),
                                                                requireNotNull(
                                                                    d.getString("observaciones")
                                                                ),
                                                                ((d.get("fotos") as? List<*>)
                                                                        ?.firstOrNull()
                                                                        as? Map<*, *>)
                                                                    ?.get("ruta") as? String,
                                                                requireNotNull(
                                                                    d.getString("registradoPorUid")
                                                                ),
                                                                requireNotNull(
                                                                        d.getTimestamp("creadoEn")
                                                                    )
                                                                    .toDate()
                                                                    .time,
                                                            )
                                                        check(
                                                            r.shelterId == shelterId &&
                                                                r.id == r.reviewId &&
                                                                r.adaptation in Adaptaciones.labels
                                                        )
                                                        check(
                                                            reviews.any {
                                                                it.id == r.reviewId &&
                                                                    it.followUpId == r.id &&
                                                                    it.status == "REALIZADA" &&
                                                                    it.adoptionId == r.adoptionId &&
                                                                    it.dogId == r.dogId
                                                            }
                                                        )
                                                        r
                                                    }
                                                    .getOrNull()
                                            }
                                            .sortedByDescending { it.doneAt }
                                    AgendaSeguimiento(
                                        false,
                                        adoptions,
                                        reviews,
                                        results = results,
                                        incompleteResults = docs[4].size - results.size,
                                    )
                                } catch (_: Exception) {
                                    AgendaSeguimiento(
                                        false,
                                        error =
                                            "Hay datos incompletos en la agenda. Consulta al responsable e intenta de nuevo.",
                                    )
                                }
                        }
                    }
                    .transformLatest { state ->
                        emit(state)
                        if (state.loading) {
                            delay(12_000)
                            emit(
                                AgendaSeguimiento(
                                    false,
                                    error =
                                        "No hay confirmación del servidor. Revisa la conexión y reintenta.",
                                )
                            )
                        }
                    }
        }

    override suspend fun program(attempt: IntentoProgramacion): ComprobanteRevision {
        require(auth.user.value?.uid == attempt.uid)
        require(attempt.error(1) == null)
        val result =
            try {
                requireNotNull(functions)
                    .call(
                        "programReview",
                        mapOf(
                            "id" to attempt.id,
                            "adoptionId" to attempt.adoptionId,
                            "shelterId" to attempt.shelterId,
                            "date" to attempt.date,
                            "type" to attempt.type,
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
                    throw RevisionRechazada(
                        error.message ?: "La revisión no cumple las condiciones necesarias."
                    )
                throw error
            }
        check(
            result["id"] == attempt.id &&
                result["adoptionId"] == attempt.adoptionId &&
                result["date"] == attempt.date &&
                result["type"] == attempt.type
        )
        return ComprobanteRevision(
            attempt.id,
            attempt.adoptionId,
            attempt.date,
            attempt.type,
            result["alreadyExisting"] == true,
        )
    }

    override suspend fun photo(path: String): ByteArray =
        FotosFirestore(requireNotNull(db)).read(path)

    override suspend fun record(attempt: IntentoSeguimiento): ComprobanteSeguimiento {
        require(auth.user.value?.uid == attempt.uid)
        require(attempt.error(1) == null)
        val database = requireNotNull(db)
        val path = attempt.photoPath()
        // A retry after an ambiguous response must not overwrite immutable evidence.
        val existing =
            database
                .collection("seguimientos")
                .whereEqualTo("refugioId", attempt.shelterId)
                .get(Source.SERVER)
                .await()
        if (path != null && existing.documents.none { it.id == attempt.reviewId }) {
            FotosFirestore(database)
                .upload(path, java.io.File(requireNotNull(attempt.photo).filePath))
        }
        val result =
            try {
                requireNotNull(functions)
                    .call(
                        "recordFollowUp",
                        mapOf(
                            "reviewId" to attempt.reviewId,
                            "shelterId" to attempt.shelterId,
                            "date" to attempt.date,
                            "adaptation" to attempt.adaptation,
                            "notes" to attempt.notes.trim(),
                            "photo" to path,
                            "next" to
                                if (attempt.disposition == "PROGRAMAR")
                                    mapOf("date" to attempt.nextDate, "type" to attempt.nextType)
                                else null,
                        ),
                    )
            } catch (e: FalloOperacion) {
                if (
                    e.status in
                        setOf(
                            "INVALID_ARGUMENT",
                            "FAILED_PRECONDITION",
                            "PERMISSION_DENIED",
                            "ALREADY_EXISTS",
                        )
                )
                    throw SeguimientoRechazado(e.message ?: "No se admitió el seguimiento.")
                throw e
            }
        check(
            result["id"] == attempt.reviewId &&
                result["reviewId"] == attempt.reviewId &&
                result["date"] == attempt.date
        )
        val next = result["nextReviewId"] as? String
        check(
            next == if (attempt.disposition == "PROGRAMAR") nextReviewId(attempt.reviewId) else null
        )
        return ComprobanteSeguimiento(
            attempt.reviewId,
            attempt.reviewId,
            attempt.date,
            next,
            result["alreadyExisting"] == true,
        )
    }
}
