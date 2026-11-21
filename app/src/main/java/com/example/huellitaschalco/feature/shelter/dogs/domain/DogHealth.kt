package com.example.huellitaschalco.feature.shelter.dogs.domain

enum class HealthStatus(val label: String) {
    Yes("Sí"),
    No("No"),
    Unknown("Sin información");

    companion object {
        fun fromStored(value: String?) = entries.firstOrNull { it.label == value } ?: Unknown
    }
}

enum class HealthField(val label: String) {
    Vaccination("Vacunación"),
    Deworming("Desparasitación"),
    Sterilization("Esterilización"),
}

data class DogHealth(
    val vaccination: HealthStatus = HealthStatus.Unknown,
    val deworming: HealthStatus = HealthStatus.Unknown,
    val sterilization: HealthStatus = HealthStatus.Unknown,
) {
    fun value(field: HealthField) =
        when (field) {
            HealthField.Vaccination -> vaccination
            HealthField.Deworming -> deworming
            HealthField.Sterilization -> sterilization
        }

    fun withValue(field: HealthField, status: HealthStatus) =
        when (field) {
            HealthField.Vaccination -> copy(vaccination = status)
            HealthField.Deworming -> copy(deworming = status)
            HealthField.Sterilization -> copy(sterilization = status)
        }

    fun stored(): Map<String, String> =
        mapOf(
            "vacunacion" to vaccination.label,
            "desparasitacion" to deworming.label,
            "esterilizacion" to sterilization.label,
        )
}

class HealthConflict(val actual: DogHealth) :
    Exception("El estado de salud cambió desde otra sesión.")

data class PublicDog(
    val loading: Boolean = true,
    val dog: RegisteredDog? = null,
    val error: String? = null,
)
