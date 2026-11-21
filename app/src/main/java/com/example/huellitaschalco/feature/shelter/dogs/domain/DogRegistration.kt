package com.example.huellitaschalco.feature.shelter.dogs.domain

import kotlinx.coroutines.flow.Flow

enum class DogField {
    Name,
    Age,
    AgeUnit,
    Size,
    Sex,
    Description,
    Photos,
}

data class LocalDogPhoto(val id: String, val filePath: String)

data class DogDraft(
    val name: String = "",
    val age: String = "",
    val ageUnit: String = "Meses",
    val size: String = "",
    val sex: String = "",
    val description: String = "",
    val photos: List<LocalDogPhoto> = emptyList(),
    val health: DogHealth = DogHealth(),
) {
    fun ageMonths(): Int? =
        age.toIntOrNull()?.let {
            if (ageUnit == "Años" && it in 0..30) it * 12 else if (ageUnit == "Meses") it else null
        }

    fun errors(): Map<DogField, String> = buildMap {
        if (name.trim().length !in 1..60)
            put(DogField.Name, "Escribe un nombre de 1 a 60 caracteres.")
        if ((ageMonths() ?: -1) !in 0..360)
            put(DogField.Age, "Escribe una edad entera entre 0 y 360 meses (30 años).")
        if (this@DogDraft.size !in listOf("Chico", "Mediano", "Grande"))
            put(DogField.Size, "Selecciona el tamaño.")
        if (sex !in listOf("Hembra", "Macho")) put(DogField.Sex, "Selecciona el sexo.")
        if (description.trim().length !in 40..2000)
            put(DogField.Description, "Escribe una descripción de 40 a 2000 caracteres.")
        if (photos.size !in 1..6 || photos.map { it.id }.distinct().size != photos.size)
            put(DogField.Photos, "Agrega entre 1 y 6 fotografías distintas.")
    }
}

data class DogPublication(
    val id: String,
    val userUid: String,
    val shelterId: String,
    val draft: DogDraft,
)

data class DogPhoto(val id: String, val path: String, val order: Int)

data class RegisteredDog(
    val id: String,
    val shelterId: String,
    val name: String,
    val ageMonths: Int,
    val size: String,
    val sex: String,
    val description: String,
    val photos: List<DogPhoto>,
    val demonstration: Boolean,
    val health: DogHealth = DogHealth(),
)

data class ShelterDogs(
    val loading: Boolean = true,
    val dogs: List<RegisteredDog> = emptyList(),
    val error: String? = null,
)

interface DogRepository {
    val isConfigured: Boolean
    val usesEmulator: Boolean

    suspend fun publish(publication: DogPublication, progress: (String) -> Unit): RegisteredDog

    suspend fun updateDog(attempt: DogEditAttempt, progress: (String) -> Unit): RegisteredDog

    fun observe(shelterId: String): Flow<ShelterDogs>

    suspend fun photo(path: String): ByteArray

    fun observePublic(id: String): Flow<PublicDog>

    suspend fun updateHealth(
        uid: String,
        shelterId: String,
        dogId: String,
        expected: DogHealth,
        desired: DogHealth,
    )
}

interface DogPhotoImporter {
    suspend fun import(uri: String): LocalDogPhoto

    fun remove(photo: LocalDogPhoto)
}
