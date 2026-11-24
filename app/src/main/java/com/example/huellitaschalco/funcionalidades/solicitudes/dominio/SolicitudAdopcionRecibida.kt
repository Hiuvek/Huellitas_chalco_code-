package com.example.huellitaschalco.funcionalidades.solicitudes.dominio

import com.example.huellitaschalco.funcionalidades.solicitud.dominio.BorradorSolicitud
import kotlinx.coroutines.flow.Flow

data class SolicitudAdopcionRecibida(
    val id: String,
    val uid: String,
    val dogId: String,
    val shelterId: String,
    val folio: String,
    val status: String,
    val answers: BorradorSolicitud,
    val createdAt: Long,
    val updatedAt: Long,
    val rejectionReason: String? = null,
    val dogName: String? = null,
) {
    val nextStep: String
        get() =
            when (status) {
                "Enviada" -> "El refugio revisará tus respuestas."
                "En revisión" -> "Tu solicitud está en revisión. Espera el contacto del refugio."
                "Aprobada" -> "Contacta al refugio para acordar la entrega y la carta de adopción."
                "No aprobada" ->
                    "Puedes explorar otros perritos y consultar el motivo de la decisión."
                else -> "Consulta al refugio para conocer el siguiente paso."
            }
}

data class HistorialSolicitud(
    val id: String,
    val status: String,
    val timestamp: Long,
    val reason: String?,
)

data class EstadoListaSolicitudes(
    val loading: Boolean = true,
    val requests: List<SolicitudAdopcionRecibida> = emptyList(),
    val error: String? = null,
)

data class EstadoDetalleSolicitud(
    val loading: Boolean = true,
    val request: SolicitudAdopcionRecibida? = null,
    val history: List<HistorialSolicitud> = emptyList(),
    val error: String? = null,
)

interface RepositorioSolicitudesAdopcion {
    fun observeOwn(uid: String): Flow<EstadoListaSolicitudes>

    fun observeOwnDetail(uid: String, id: String): Flow<EstadoDetalleSolicitud>
}

interface RepositorioSolicitudesRecibidas {
    fun observeReceived(uid: String, shelterId: String): Flow<EstadoListaSolicitudes>

    fun observeReceivedDetail(uid: String, shelterId: String, id: String): Flow<EstadoDetalleSolicitud>
}
