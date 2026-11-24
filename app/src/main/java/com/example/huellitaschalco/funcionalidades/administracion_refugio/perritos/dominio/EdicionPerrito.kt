package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio

/** Una foto guardada se reutiliza por referencia; una nueva se conserva en caché privada. */
data class FotoPerritoEditable(val remote: FotoPerrito? = null, val local: FotoLocalPerrito? = null) {
    init {
        require((remote == null) != (local == null))
    }

    val id: String
        get() = remote?.id ?: local!!.id
}

data class BorradorEdicionPerrito(val form: BorradorPerrito, val photos: List<FotoPerritoEditable>) {
    fun errors() =
        form.copy(photos = photos.map { FotoLocalPerrito(it.id, it.local?.filePath ?: "") }).errors()

    fun content(uid: String, base: PerritoRegistrado) =
        ContenidoPerrito(
            form.name.trim(),
            requireNotNull(form.ageMonths()),
            form.size,
            form.sex,
            form.description.trim(),
            photos.mapIndexed { index, photo ->
                photo.remote?.copy(order = index)
                    ?: FotoPerrito(
                        photo.id,
                        "perritos/${base.shelterId}/$uid/${base.id}/${photo.id}.jpg",
                        index,
                    )
            },
        )

    companion object {
        fun from(dog: PerritoRegistrado) =
            BorradorEdicionPerrito(
                BorradorPerrito(
                    dog.name,
                    dog.ageMonths.toString(),
                    "Meses",
                    dog.size,
                    dog.sex,
                    dog.description,
                    health = dog.health,
                ),
                dog.photos.map { FotoPerritoEditable(remote = it) },
            )
    }
}

data class ContenidoPerrito(
    val name: String,
    val ageMonths: Int,
    val size: String,
    val sex: String,
    val description: String,
    val photos: List<FotoPerrito>,
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

    fun applyTo(dog: PerritoRegistrado) =
        dog.copy(
            name = name,
            ageMonths = ageMonths,
            size = size,
            sex = sex,
            description = description,
            photos = photos,
        )

    companion object {
        fun from(dog: PerritoRegistrado) =
            ContenidoPerrito(dog.name, dog.ageMonths, dog.size, dog.sex, dog.description, dog.photos)
    }
}

data class IntentoEdicionPerrito(val uid: String, val base: PerritoRegistrado, val draft: BorradorEdicionPerrito)

class ConflictoEdicionPerrito(val actual: PerritoRegistrado) :
    Exception("El registro cambió desde otra sesión.")
