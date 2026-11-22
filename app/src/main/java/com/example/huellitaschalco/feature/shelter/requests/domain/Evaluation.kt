package com.example.huellitaschalco.feature.shelter.requests.domain

enum class EvaluationDecision(val status: String, val suffix: String) {
    Review("En revisión", "revision"),
    Approve("Aprobada", "aprobada"),
    Reject("No aprobada", "no-aprobada");

    fun allowedFrom(status: String) =
        if (this == Review) status == "Enviada" else status == "En revisión"
}

data class EvaluationAttempt(
    val uid: String,
    val shelterId: String,
    val requestId: String,
    val previous: String,
    val decision: EvaluationDecision,
    val reason: String = "",
) {
    val historyId
        get() = "$requestId-${decision.suffix}"

    fun error(): String? =
        when {
            !decision.allowedFrom(previous) ->
                "La solicitud debe pasar a En revisión antes de decidir."
            decision == EvaluationDecision.Reject && reason.trim().length !in 5..1000 ->
                "Escribe el motivo con 5 a 1000 caracteres."
            decision != EvaluationDecision.Reject && reason.isNotEmpty() ->
                "Esta acción no requiere motivo."
            else -> null
        }
}

class EvaluationConflict(message: String) : Exception(message)

interface EvaluationRepository {
    suspend fun decide(attempt: EvaluationAttempt)
}
