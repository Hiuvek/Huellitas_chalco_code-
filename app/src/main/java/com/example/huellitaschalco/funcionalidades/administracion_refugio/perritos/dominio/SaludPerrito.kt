package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio

enum class EstadoSalud(val label: String) {
    Yes("Sí"),
    No("No"),
    Unknown("Sin información");

    companion object {
        fun fromStored(value: String?) = entries.firstOrNull { it.label == value } ?: Unknown
    }
}

enum class CampoSalud(val label: String) {
    Vaccination("Vacunación"),
    Deworming("Desparasitación"),
    Sterilization("Esterilización"),
}

data class SaludPerrito(
    val vaccination: EstadoSalud = EstadoSalud.Unknown,
    val deworming: EstadoSalud = EstadoSalud.Unknown,
    val sterilization: EstadoSalud = EstadoSalud.Unknown,
) {
    fun value(field: CampoSalud) =
        when (field) {
            CampoSalud.Vaccination -> vaccination
            CampoSalud.Deworming -> deworming
            CampoSalud.Sterilization -> sterilization
        }

    fun withValue(field: CampoSalud, status: EstadoSalud) =
        when (field) {
            CampoSalud.Vaccination -> copy(vaccination = status)
            CampoSalud.Deworming -> copy(deworming = status)
            CampoSalud.Sterilization -> copy(sterilization = status)
        }

    fun stored(): Map<String, String> =
        mapOf(
            "vacunacion" to vaccination.label,
            "desparasitacion" to deworming.label,
            "esterilizacion" to sterilization.label,
        )
}

class ConflictoSalud(val actual: SaludPerrito) :
    Exception("El estado de salud cambió desde otra sesión.")

data class PerritoPublico(
    val loading: Boolean = true,
    val dog: PerritoRegistrado? = null,
    val error: String? = null,
    val stale: Boolean = false,
)
