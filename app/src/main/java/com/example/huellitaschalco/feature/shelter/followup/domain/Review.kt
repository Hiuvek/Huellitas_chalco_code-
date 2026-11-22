package com.example.huellitaschalco.feature.shelter.followup.domain

import com.example.huellitaschalco.core.date.civilDateMillis
import com.example.huellitaschalco.feature.shelter.adoption.domain.Adoption
import kotlinx.coroutines.flow.Flow

object ReviewTypes {
    val labels =
        linkedMapOf(
            "VISITA" to "Visita a domicilio",
            "LLAMADA" to "Llamada",
            "VIDEOLLAMADA" to "Videollamada",
        )
}

data class AdoptedDog(val adoption: Adoption, val dogName: String, val adopterName: String)

data class ScheduledReview(
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

data class FollowUpBoard(
    val loading: Boolean = true,
    val adoptions: List<AdoptedDog> = emptyList(),
    val reviews: List<ScheduledReview> = emptyList(),
    val error: String? = null,
    val results: List<FollowUpResult> = emptyList(),
    val incompleteResults: Int = 0,
)

data class ScheduleAttempt(
    val id: String,
    val uid: String,
    val shelterId: String,
    val adoptionId: String,
    val date: String,
    val type: String,
) {
    fun error(deliveryAt: Long): String? =
        when {
            type !in ReviewTypes.labels -> "Selecciona visita, llamada o videollamada."
            civilDateMillis(date) == null -> "Escribe una fecha real con formato AAAA-MM-DD."
            civilDateMillis(date)!! < deliveryAt ->
                "La revisión no puede ser anterior a la entrega."
            else -> null
        }
}

data class ReviewReceipt(
    val id: String,
    val adoptionId: String,
    val date: String,
    val type: String,
    val alreadyExisting: Boolean,
)

class ReviewRejected(message: String) : Exception(message)

interface FollowUpRepository {
    fun observe(uid: String, shelterId: String): Flow<FollowUpBoard>

    suspend fun program(attempt: ScheduleAttempt): ReviewReceipt

    suspend fun record(attempt: FollowUpAttempt): FollowUpReceipt

    suspend fun photo(path: String): ByteArray
}
