package com.example.huellitaschalco.feature.shelter.adoption.domain

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

data class Adoption(
    val id: String,
    val requestId: String,
    val dogId: String,
    val uid: String,
    val shelterId: String,
    val deliveryAt: Long,
    val signedLetter: Boolean,
    val registeredBy: String,
    val createdAt: Long,
)

data class AdoptionState(
    val loading: Boolean = true,
    val adoption: Adoption? = null,
    val error: String? = null,
)

data class AdoptionAttempt(
    val uid: String,
    val requestId: String,
    val shelterId: String,
    val dogId: String,
    val date: String,
    val signedLetter: Boolean,
) {
    fun error(now: Long = System.currentTimeMillis()): String? {
        if (!signedLetter) return "Confirma que se realizó la entrega y se firmó la carta."
        val parsed =
            if (date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")))
                runCatching {
                    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        .apply {
                            isLenient = false
                            timeZone = TimeZone.getTimeZone("America/Mexico_City")
                        }
                        .parse(date)
                }
                    .getOrNull()
            else null
        return if (parsed == null || parsed.time <= 0 || parsed.time > now)
            "Escribe una fecha real, sin fechas futuras, con formato AAAA-MM-DD."
        else null
    }
}

data class AdoptionReceipt(
    val id: String,
    val requestId: String,
    val dogId: String,
    val shelterId: String,
    val date: String,
    val alreadyExisting: Boolean,
)

interface AdoptionRepository {
    fun observeDog(uid: String, shelterId: String, dogId: String): Flow<AdoptionState>

    suspend fun record(attempt: AdoptionAttempt): AdoptionReceipt
}

class AdoptionRejected(message: String) : Exception(message)
