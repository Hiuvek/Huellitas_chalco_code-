package com.example.huellitaschalco.feature.requests.domain

import com.example.huellitaschalco.feature.application.domain.ApplicationDraft
import kotlinx.coroutines.flow.Flow

data class AdoptionRequest(
    val id: String,
    val uid: String,
    val dogId: String,
    val shelterId: String,
    val folio: String,
    val status: String,
    val answers: ApplicationDraft,
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

data class RequestHistory(
    val id: String,
    val status: String,
    val timestamp: Long,
    val reason: String?,
)

data class RequestListState(
    val loading: Boolean = true,
    val requests: List<AdoptionRequest> = emptyList(),
    val error: String? = null,
)

data class RequestDetailState(
    val loading: Boolean = true,
    val request: AdoptionRequest? = null,
    val history: List<RequestHistory> = emptyList(),
    val error: String? = null,
)

interface AdoptionRequestsRepository {
    fun observeOwn(uid: String): Flow<RequestListState>

    fun observeOwnDetail(uid: String, id: String): Flow<RequestDetailState>
}
