package com.example.huellitaschalco.funcionalidades.solicitud.dominio

import java.security.MessageDigest
import java.util.Locale

enum class CampoSolicitud(val step: Int) {
    Name(0),
    Email(0),
    Phone(0),
    Housing(1),
    Tenure(1),
    Yard(1),
    People(1),
    Hours(1),
    Pets(2),
    PetsDetails(2),
    Reasons(3),
}

object OpcionesSolicitud {
    val housing = linkedMapOf("CASA" to "Casa", "DEPARTAMENTO" to "Departamento", "OTRO" to "Otro")
    val tenure = linkedMapOf("PROPIA" to "Propia", "RENTADA" to "Rentada", "FAMILIAR" to "Familiar")
    val hours =
        linkedMapOf(
            "MENOS_DE_4" to "Menos de 4 h",
            "DE_4_A_8" to "De 4 a 8 h",
            "MAS_DE_8" to "Más de 8 h",
        )
}

data class BorradorSolicitud(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val housing: String = "",
    val tenure: String = "",
    val yard: Boolean? = null,
    val people: String = "",
    val hours: String = "",
    val pets: Boolean? = null,
    val petsDetails: String = "",
    val reasons: String = "",
) {
    fun errors(step: Int? = null): Map<CampoSolicitud, String> = buildMap {
        if (name.trim().length !in 2..100)
            put(CampoSolicitud.Name, "Escribe un nombre de 2 a 100 caracteres.")
        if (
            email.trim().length !in 3..254 ||
                !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
        )
            put(CampoSolicitud.Email, "Escribe un correo válido.")
        if (!phone.matches(Regex("^[0-9]{10}$")))
            put(CampoSolicitud.Phone, "El teléfono debe tener 10 dígitos.")
        if (housing !in OpcionesSolicitud.housing)
            put(CampoSolicitud.Housing, "Selecciona el tipo de vivienda.")
        if (tenure !in OpcionesSolicitud.tenure)
            put(CampoSolicitud.Tenure, "Selecciona la tenencia de la vivienda.")
        if (yard == null) put(CampoSolicitud.Yard, "Indica si tienes espacio exterior.")
        if ((people.toIntOrNull() ?: 0) !in 1..20)
            put(CampoSolicitud.People, "Indica de 1 a 20 personas.")
        if (hours !in OpcionesSolicitud.hours)
            put(CampoSolicitud.Hours, "Selecciona cuántas horas estaría solo.")
        if (pets == null) put(CampoSolicitud.Pets, "Indica si tienes otras mascotas.")
        if (pets == true && petsDetails.trim().length > 500)
            put(CampoSolicitud.PetsDetails, "Usa hasta 500 caracteres.")
        if (reasons.trim().length !in 30..2000)
            put(CampoSolicitud.Reasons, "Explica tus motivos con 30 a 2000 caracteres.")
    }
        .let { errors -> if (step == null) errors else errors.filterKeys { it.step == step } }

    fun fields(): Map<String, Any?> =
        mapOf(
            "contacto" to
                mapOf("nombre" to name.trim(), "correo" to email.trim(), "telefono" to phone),
            "hogar" to
                mapOf(
                    "tipoVivienda" to housing,
                    "tenencia" to tenure,
                    "espacioExterior" to requireNotNull(yard),
                    "personas" to requireNotNull(people.toLongOrNull()),
                    "horasSolo" to hours,
                ),
            "otrasMascotas" to
                mapOf(
                    "tiene" to requireNotNull(pets),
                    "detalle" to petsDetails.trim().takeIf { pets == true && it.isNotEmpty() },
                ),
            "motivos" to reasons.trim(),
        )
}

data class IntentoSolicitud(
    val id: String,
    val uid: String,
    val dogId: String,
    val shelterId: String,
    val draft: BorradorSolicitud,
) {
    val folio
        get() = "HC-$id"

    fun fields(): Map<String, Any?> =
        draft.fields() +
            mapOf(
                "folio" to folio,
                "usuarioUid" to uid,
                "perritoId" to dogId,
                "refugioId" to shelterId,
                "estado" to "Enviada",
                "motivoNoAprobacion" to null,
            )
}

data class ComprobanteSolicitud(
    val id: String,
    val folio: String,
    val dogId: String,
    val shelterId: String,
    val status: String,
    val alreadyExisting: Boolean = false,
)

class SolicitudNoDisponible(message: String) : Exception(message)

interface RepositorioSolicitudAdopcion {
    suspend fun submit(attempt: IntentoSolicitud): ComprobanteSolicitud
}

fun applicationKey(uid: String, dogId: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$uid\u0000$dogId".toByteArray(Charsets.UTF_8))
        .joinToString("") { String.format(Locale.ROOT, "%02x", it.toInt() and 255) }
