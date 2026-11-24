package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio

import kotlinx.coroutines.flow.Flow

enum class CampoPerrito {
    Name,
    Age,
    AgeUnit,
    Size,
    Sex,
    Description,
    Photos,
}

data class FotoLocalPerrito(val id: String, val filePath: String)

data class BorradorPerrito(
    val name: String = "",
    val age: String = "",
    val ageUnit: String = "Meses",
    val size: String = "",
    val sex: String = "",
    val description: String = "",
    val photos: List<FotoLocalPerrito> = emptyList(),
    val health: SaludPerrito = SaludPerrito(),
) {
    fun ageMonths(): Int? =
        age.toIntOrNull()?.let {
            if (ageUnit == "Años" && it in 0..30) it * 12 else if (ageUnit == "Meses") it else null
        }

    fun errors(): Map<CampoPerrito, String> = buildMap {
        if (name.trim().length !in 1..60)
            put(CampoPerrito.Name, "Escribe un nombre de 1 a 60 caracteres.")
        if ((ageMonths() ?: -1) !in 0..360)
            put(CampoPerrito.Age, "Escribe una edad entera entre 0 y 360 meses (30 años).")
        if (this@BorradorPerrito.size !in listOf("Chico", "Mediano", "Grande"))
            put(CampoPerrito.Size, "Selecciona el tamaño.")
        if (sex !in listOf("Hembra", "Macho")) put(CampoPerrito.Sex, "Selecciona el sexo.")
        if (description.trim().length !in 40..2000)
            put(CampoPerrito.Description, "Escribe una descripción de 40 a 2000 caracteres.")
        if (photos.size != 1 || photos.map { it.id }.distinct().size != photos.size)
            put(CampoPerrito.Photos, "Agrega una fotografía.")
    }
}

data class PublicacionPerrito(
    val id: String,
    val userUid: String,
    val shelterId: String,
    val draft: BorradorPerrito,
)

data class FotoPerrito(val id: String, val path: String, val order: Int)

data class PerritoRegistrado(
    val id: String,
    val shelterId: String,
    val name: String,
    val ageMonths: Int,
    val size: String,
    val sex: String,
    val description: String,
    val photos: List<FotoPerrito>,
    val demonstration: Boolean,
    val health: SaludPerrito = SaludPerrito(),
    val status: String = "Disponible",
)

data class PerritosRefugio(
    val loading: Boolean = true,
    val dogs: List<PerritoRegistrado> = emptyList(),
    val error: String? = null,
)

interface RepositorioPerritos {
    val isConfigured: Boolean
    val usesEmulator: Boolean

    suspend fun publish(publication: PublicacionPerrito, progress: (String) -> Unit): PerritoRegistrado

    suspend fun remove(uid: String, shelterId: String, dogId: String) {
        throw UnsupportedOperationException("La eliminación no está disponible.")
    }

    suspend fun updateDog(attempt: IntentoEdicionPerrito, progress: (String) -> Unit): PerritoRegistrado

    fun observe(shelterId: String): Flow<PerritosRefugio>

    suspend fun photo(path: String): ByteArray

    fun observePublic(id: String): Flow<PerritoPublico>

    suspend fun updateHealth(
        uid: String,
        shelterId: String,
        dogId: String,
        expected: SaludPerrito,
        desired: SaludPerrito,
    )
}

interface ImportadorFotoPerrito {
    suspend fun import(uri: String): FotoLocalPerrito

    fun remove(photo: FotoLocalPerrito)
}
