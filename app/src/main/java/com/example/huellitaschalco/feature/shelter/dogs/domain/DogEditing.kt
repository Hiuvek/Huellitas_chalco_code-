package com.example.huellitaschalco.feature.shelter.dogs.domain

/** Una foto guardada se reutiliza por referencia; una nueva se conserva en caché privada. */
data class EditableDogPhoto(val remote: DogPhoto? = null, val local: LocalDogPhoto? = null) {
    init {
        require((remote == null) != (local == null))
    }

    val id: String
        get() = remote?.id ?: local!!.id
}

data class DogEditDraft(val form: DogDraft, val photos: List<EditableDogPhoto>) {
    fun errors() =
        form.copy(photos = photos.map { LocalDogPhoto(it.id, it.local?.filePath ?: "") }).errors()

    fun content(uid: String, base: RegisteredDog) =
        DogContent(
            form.name.trim(),
            requireNotNull(form.ageMonths()),
            form.size,
            form.sex,
            form.description.trim(),
            photos.mapIndexed { index, photo ->
                photo.remote?.copy(order = index)
                    ?: DogPhoto(
                        photo.id,
                        "perritos/${base.shelterId}/$uid/${base.id}/${photo.id}.jpg",
                        index,
                    )
            },
        )

    companion object {
        fun from(dog: RegisteredDog) =
            DogEditDraft(
                DogDraft(
                    dog.name,
                    dog.ageMonths.toString(),
                    "Meses",
                    dog.size,
                    dog.sex,
                    dog.description,
                    health = dog.health,
                ),
                dog.photos.map { EditableDogPhoto(remote = it) },
            )
    }
}

data class DogContent(
    val name: String,
    val ageMonths: Int,
    val size: String,
    val sex: String,
    val description: String,
    val photos: List<DogPhoto>,
) {
    fun fields(): Map<String, Any> =
        mapOf(
            "nombre" to name,
            "edadMeses" to ageMonths,
            "tamano" to size,
            "sexo" to sex,
            "descripcion" to description,
            "fotos" to
                photos.map { mapOf("archivoId" to it.id, "ruta" to it.path, "orden" to it.order) },
        )

    fun applyTo(dog: RegisteredDog) =
        dog.copy(
            name = name,
            ageMonths = ageMonths,
            size = size,
            sex = sex,
            description = description,
            photos = photos,
        )

    companion object {
        fun from(dog: RegisteredDog) =
            DogContent(dog.name, dog.ageMonths, dog.size, dog.sex, dog.description, dog.photos)
    }
}

data class DogEditAttempt(val uid: String, val base: RegisteredDog, val draft: DogEditDraft)

class DogEditConflict(val actual: RegisteredDog) :
    Exception("El registro cambió desde otra sesión.")
