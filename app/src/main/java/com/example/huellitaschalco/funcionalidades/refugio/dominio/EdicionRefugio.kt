package com.example.huellitaschalco.funcionalidades.refugio.dominio

data class BorradorEdicionRefugio(
    val name: String = "",
    val zone: String = "",
    val hours: String = "",
    val requirements: String = "",
    val steps: String = "",
    val latitude: String = "",
    val longitude: String = "",
) {
    fun lines(text: String) = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun coordinates() =
        CoordenadasPublicas.checked(
            latitude.trim().replace(',', '.').toDoubleOrNull(),
            longitude.trim().replace(',', '.').toDoubleOrNull(),
        )

    fun error(): String? =
        when {
            name.trim().length !in 1..100 -> "Escribe un nombre de 1 a 100 caracteres."
            zone.trim().length !in 1..250 -> "Escribe una zona pública de 1 a 250 caracteres."
            hours.trim().length !in 1..250 -> "Escribe un horario de 1 a 250 caracteres."
            listOf(requirements, steps).any {
                lines(it).size !in 1..10 || lines(it).any { line -> line.length > 300 }
            } ->
                "Escribe entre 1 y 10 requisitos y entre 1 y 10 pasos, uno por línea y de hasta 300 caracteres cada uno."
            (latitude.isNotBlank() || longitude.isNotBlank()) && coordinates() == null ->
                "Completa ambas coordenadas: latitud entre -90 y 90 y longitud entre -180 y 180, o deja ambas vacías."
            else -> null
        }

    fun applyTo(original: RefugioPublico) =
        original.copy(
            name = name.trim(),
            zone = zone.trim(),
            hours = hours.trim(),
            policy = PoliticaAdopcion(lines(requirements), lines(steps)),
            location = coordinates(),
        )

    companion object {
        fun from(info: RefugioPublico) =
            BorradorEdicionRefugio(
                info.name,
                info.zone,
                info.hours,
                info.policy?.requirements?.joinToString("\n").orEmpty(),
                info.policy?.steps?.joinToString("\n").orEmpty(),
                info.location?.latitude?.let { it.toString() }.orEmpty(),
                info.location?.longitude?.let { it.toString() }.orEmpty(),
            )
    }
}

class ConflictoEdicionRefugio :
    Exception(
        "La información cambió desde que abriste el formulario. Recarga los datos antes de guardar."
    )
