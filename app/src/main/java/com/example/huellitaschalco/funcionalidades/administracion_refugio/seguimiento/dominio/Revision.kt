package com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.dominio

import com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.dominio.Adopcion
import com.example.huellitaschalco.nucleo.fechas.civilDateMillis
import kotlinx.coroutines.flow.Flow

object TiposRevision {
    val labels =
        linkedMapOf(
            "VISITA" to "Visita a domicilio",
            "LLAMADA" to "Llamada",
            "VIDEOLLAMADA" to "Videollamada",
        )
}

data class PerritoAdoptado(val adoption: Adopcion, val dogName: String, val adopterName: String)

data class RevisionProgramada(
    val id: String,
    val adoptionId: String,
    val dogId: String,
    val shelterId: String,
    val plannedAt: Long,
    val type: String,
    val status: String,
    val followUpId: String?,
    val createdAt: Long,
)

data class AgendaSeguimiento(
    val loading: Boolean = true,
    val adoptions: List<PerritoAdoptado> = emptyList(),
    val reviews: List<RevisionProgramada> = emptyList(),
    val error: String? = null,
    val results: List<ResultadoSeguimiento> = emptyList(),
    val incompleteResults: Int = 0,
)

data class IntentoProgramacion(
    val id: String,
    val uid: String,
    val shelterId: String,
    val adoptionId: String,
    val date: String,
    val type: String,
) {
    fun error(deliveryAt: Long): String? =
        when {
            type !in TiposRevision.labels -> "Selecciona visita, llamada o videollamada."
            civilDateMillis(date) == null -> "Escribe una fecha real con formato AAAA-MM-DD."
            civilDateMillis(date)!! < deliveryAt ->
                "La revisión no puede ser anterior a la entrega."
            else -> null
        }
}

data class ComprobanteRevision(
    val id: String,
    val adoptionId: String,
    val date: String,
    val type: String,
    val alreadyExisting: Boolean,
)

class RevisionRechazada(message: String) : Exception(message)

interface RepositorioSeguimiento {
    fun observe(uid: String, shelterId: String): Flow<AgendaSeguimiento>

    suspend fun program(attempt: IntentoProgramacion): ComprobanteRevision

    suspend fun record(attempt: IntentoSeguimiento): ComprobanteSeguimiento

    suspend fun photo(path: String): ByteArray
}
