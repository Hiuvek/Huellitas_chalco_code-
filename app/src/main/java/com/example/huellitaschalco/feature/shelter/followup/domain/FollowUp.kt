package com.example.huellitaschalco.feature.shelter.followup.domain

import com.example.huellitaschalco.core.date.civilDateMillis
import com.example.huellitaschalco.feature.shelter.dogs.domain.LocalDogPhoto
import java.security.MessageDigest

object Adaptations {
    val labels =
        linkedMapOf(
            "MUY_BUENA" to "Muy buena",
            "BUENA" to "Buena",
            "REGULAR" to "Regular",
            "CON_DIFICULTADES" to "Con dificultades",
        )
}

fun nextReviewId(reviewId: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("${reviewId}\u0000next".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

data class FollowUpResult(
    val id: String,
    val adoptionId: String,
    val reviewId: String,
    val dogId: String,
    val shelterId: String,
    val doneAt: Long,
    val adaptation: String,
    val notes: String,
    val photo: String?,
    val actorUid: String,
    val createdAt: Long,
)

data class FollowUpAttempt(
    val uid: String,
    val shelterId: String,
    val reviewId: String,
    val date: String,
    val adaptation: String,
    val notes: String,
    val photo: LocalDogPhoto?,
    val disposition: String,
    val nextDate: String,
    val nextType: String,
) {
    fun error(deliveryAt: Long, now: Long = System.currentTimeMillis()): String? =
        when {
            civilDateMillis(date) == null -> "Escribe una fecha real con formato AAAA-MM-DD."
            civilDateMillis(date)!! < deliveryAt -> "La fecha no puede ser anterior a la entrega."
            civilDateMillis(date)!! > now -> "La fecha del seguimiento no puede estar en el futuro."
            adaptation !in Adaptations.labels -> "Selecciona la adaptación observada."
            notes.trim().length !in 15..2000 -> "Escribe observaciones de 15 a 2000 caracteres."
            disposition !in listOf("CERRAR", "PROGRAMAR") ->
                "Elige programar otra revisión o cerrar este seguimiento."
            disposition == "PROGRAMAR" &&
                (civilDateMillis(nextDate) == null ||
                    civilDateMillis(nextDate)!! <= civilDateMillis(date)!!) ->
                "La próxima revisión debe tener una fecha posterior al seguimiento."
            disposition == "PROGRAMAR" && nextType !in ReviewTypes.labels ->
                "Selecciona el tipo de la próxima revisión."
            else -> null
        }

    fun photoPath(): String? = photo?.let {
        "refugios/$shelterId/seguimientos/$reviewId/${it.id}.jpg"
    }
}

data class FollowUpReceipt(
    val id: String,
    val reviewId: String,
    val date: String,
    val nextReviewId: String?,
    val alreadyExisting: Boolean,
)

class FollowUpRejected(message: String) : Exception(message)
