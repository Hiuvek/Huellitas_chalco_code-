package com.example.huellitaschalco.feature.shelter.dogs.data

import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.google.firebase.firestore.*
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDogRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore?,
    override val usesEmulator: Boolean,
) : DogRepository {
    override val isConfigured
        get() = db != null

    override suspend fun remove(uid: String, shelterId: String, dogId: String) {
        require(auth.user.value?.uid == uid)
        val database = requireNotNull(db)
        database
            .runTransaction { tx ->
                val reference = database.collection("perritos").document(dogId)
                val dog = tx.get(reference)
                val control = tx.get(database.collection("perritos_control").document(dogId))
                if (
                    dog.getString("refugioId") != shelterId ||
                        control.getString("refugioId") != shelterId
                )
                    throw DogRemovalRejected("Este perrito no pertenece al refugio seleccionado.")
                if (dog.getBoolean("eliminado") != true) {
                    val status = dog.getString("estado")
                    if (
                        status !in listOf("Disponible", "Adoptado") ||
                            (status == "Disponible" &&
                                (control.getString("solicitudAprobadaId") != null ||
                                    control.getString("adopcionId") != null)) ||
                            (status == "Adoptado" && control.getString("adopcionId") != dogId)
                    )
                        throw DogRemovalRejected(
                            "No se puede eliminar un perrito con una adopción en proceso. Completa primero la entrega."
                        )
                    tx.update(
                        reference,
                        mapOf(
                            "eliminado" to true,
                            "eliminadoEn" to FieldValue.serverTimestamp(),
                            "eliminadoPorUid" to uid,
                            "publicado" to
                                (status == "Adoptado" && dog.getBoolean("publicado") == true),
                            "actualizadoEn" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            }
            .await()
    }

    override suspend fun publish(
        publication: DogPublication,
        progress: (String) -> Unit,
    ): RegisteredDog {
        require(publication.draft.errors().isEmpty())
        require(publication.id.matches(Regex("^[A-Za-z0-9-]{1,100}$")))
        require(publication.shelterId.matches(Regex("^[A-Za-z0-9_-]{1,100}$")))
        require(auth.user.value?.uid == publication.userUid)
        val database = requireNotNull(db)
        val draft = publication.draft
        val photos =
            draft.photos.mapIndexed { index, photo ->
                require(photo.id.matches(Regex("^[A-Za-z0-9-]{1,100}$")))
                DogPhoto(
                    photo.id,
                    "perritos/${publication.shelterId}/${publication.userUid}/${publication.id}/${photo.id}.jpg",
                    index,
                )
            }
        val fields =
            mapOf(
                "refugioId" to publication.shelterId,
                "nombre" to draft.name.trim(),
                "edadMeses" to requireNotNull(draft.ageMonths()),
                "tamano" to draft.size,
                "sexo" to draft.sex,
                "descripcion" to draft.description.trim(),
                "vacunacion" to draft.health.vaccination.label,
                "desparasitacion" to draft.health.deworming.label,
                "esterilizacion" to draft.health.sterilization.label,
                "estado" to "Disponible",
                "publicado" to true,
                "esDemostracion" to usesEmulator,
                "fotos" to
                    photos.map {
                        mapOf("archivoId" to it.id, "ruta" to it.path, "orden" to it.order.toLong())
                    },
            )
        val reference = database.collection("perritos").document(publication.id)
        fun matches(snapshot: DocumentSnapshot): Boolean =
            fields.all { (key, value) ->
                if (key == "edadMeses") snapshot.getLong(key) == (value as Int).toLong()
                else snapshot.get(key) == value
            }
        fun result() =
            RegisteredDog(
                publication.id,
                publication.shelterId,
                draft.name.trim(),
                requireNotNull(draft.ageMonths()),
                draft.size,
                draft.sex,
                draft.description.trim(),
                photos,
                usesEmulator,
                draft.health,
            )
        progress("Comprobando publicación…")
        val previous = reference.get(Source.SERVER).await()
        if (previous.exists()) {
            check(matches(previous))
            return result()
        }
        draft.photos.forEachIndexed { index, photo ->
            require(auth.user.value?.uid == publication.userUid)
            val file = File(photo.filePath)
            require(file.length() in 1..(2 * 1024 * 1024).toLong())
            progress("Subiendo fotografía ${index+1} de ${photos.size}…")
            com.example.huellitaschalco.core.firebase
                .FirestorePhotos(database)
                .upload(photos[index].path, file)
        }
        require(auth.user.value?.uid == publication.userUid)
        progress("Guardando el registro…")
        // La transacción necesita servidor: sin conexión no queda una escritura en cola.
        database
            .runTransaction { transaction ->
                val existing = transaction.get(reference)
                if (existing.exists()) check(matches(existing))
                else {
                    transaction.set(
                        reference,
                        fields +
                            mapOf(
                                "creadoEn" to FieldValue.serverTimestamp(),
                                "actualizadoEn" to FieldValue.serverTimestamp(),
                            ),
                    )
                    transaction.set(
                        database.collection("perritos_control").document(publication.id),
                        mapOf(
                            "refugioId" to publication.shelterId,
                            "solicitudAprobadaId" to null,
                            "adopcionId" to null,
                        ),
                    )
                }
            }
            .await()
        // Los números de Firestore se deserializan como Long, también dentro de los mapas.
        return result()
    }

    override suspend fun updateDog(
        attempt: DogEditAttempt,
        progress: (String) -> Unit,
    ): RegisteredDog {
        val (uid, base, draft) = attempt
        require(auth.user.value?.uid == uid)
        require(draft.errors().isEmpty())
        require(draft.photos.all { it.remote == null || it.remote in base.photos })
        val database = requireNotNull(db)
        val ref = database.collection("perritos").document(base.id)
        val desired = draft.content(uid, base)
        val expected = DogContent.from(base)
        progress("Comprobando registro…")
        val previous = requireNotNull(parse(ref.get(Source.SERVER).await()))
        check(previous.shelterId == base.shelterId)
        if (DogContent.from(previous) == desired) return previous
        if (DogContent.from(previous) != expected) throw DogEditConflict(previous)
        draft.photos.forEachIndexed { index, photo ->
            photo.local?.let { local ->
                require(auth.user.value?.uid == uid)
                require(local.id.matches(Regex("^[A-Za-z0-9-]{1,100}$")))
                val file = File(local.filePath)
                require(file.length() in 1..(2 * 1024 * 1024).toLong())
                progress("Subiendo fotografía ${index + 1} de ${draft.photos.size}…")
                com.example.huellitaschalco.core.firebase
                    .FirestorePhotos(database)
                    .upload(desired.photos[index].path, file)
            }
        }
        progress("Guardando cambios…")
        val (result, conflict) =
            database
                .runTransaction { transaction ->
                    require(auth.user.value?.uid == uid)
                    val actual = requireNotNull(parse(transaction.get(ref)))
                    check(actual.shelterId == base.shelterId)
                    when (DogContent.from(actual)) {
                        desired -> actual to false
                        expected -> {
                            transaction.update(
                                ref,
                                desired.fields() +
                                    mapOf("actualizadoEn" to FieldValue.serverTimestamp()),
                            )
                            desired.applyTo(actual) to false
                        }
                        else -> actual to true
                    }
                }
                .await()
        if (conflict) throw DogEditConflict(result)
        return result
    }

    override fun observe(shelterId: String): Flow<ShelterDogs> = callbackFlow {
        val database = db
        if (database == null) {
            trySend(ShelterDogs(false, error = "Firebase no está configurado."))
            close()
            return@callbackFlow
        }
        val listener =
            database
                .collection("perritos")
                .whereEqualTo("refugioId", shelterId)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    when {
                        error != null || snapshot == null ->
                            trySend(
                                ShelterDogs(
                                    false,
                                    error =
                                        "No pudimos consultar tus perritos. Revisa tu conexión y permisos.",
                                )
                            )
                        snapshot.metadata.isFromCache -> trySend(ShelterDogs())
                        else ->
                            trySend(
                                ShelterDogs(
                                    false,
                                    snapshot.documents.mapNotNull(::parse).sortedBy {
                                        it.name.lowercase()
                                    },
                                )
                            )
                    }
                }
        awaitClose { listener.remove() }
    }

    private fun parse(doc: DocumentSnapshot) = parseRegisteredDog(doc)

    private fun readHealth(doc: DocumentSnapshot) =
        DogHealth(
            HealthStatus.fromStored(doc.getString("vacunacion")),
            HealthStatus.fromStored(doc.getString("desparasitacion")),
            HealthStatus.fromStored(doc.getString("esterilizacion")),
        )

    override suspend fun updateHealth(
        uid: String,
        shelterId: String,
        dogId: String,
        expected: DogHealth,
        desired: DogHealth,
    ) {
        require(auth.user.value?.uid == uid)
        val database = requireNotNull(db)
        val reference = database.collection("perritos").document(dogId)
        // Confirma conectividad con el servidor incluso si el SDK conserva un canal de
        // transacciones.
        // La transacción vuelve a leer para validar permisos y detectar cambios concurrentes.
        reference.get(Source.SERVER).await()
        val conflict =
            database
                .runTransaction<DogHealth?> { transaction ->
                    require(auth.user.value?.uid == uid)
                    val snapshot = transaction.get(reference)
                    check(snapshot.exists() && snapshot.getString("refugioId") == shelterId)
                    val actual = readHealth(snapshot)
                    when {
                        actual == desired -> null
                        actual != expected -> actual
                        else -> {
                            transaction.update(
                                reference,
                                desired.stored() +
                                    mapOf("actualizadoEn" to FieldValue.serverTimestamp()),
                            )
                            null
                        }
                    }
                }
                .await()
        if (conflict != null) throw HealthConflict(conflict)
    }

    override fun observePublic(id: String): Flow<PublicDog> = callbackFlow {
        val database = db
        if (database == null) {
            trySend(PublicDog(false, error = "No pudimos consultar esta ficha."))
            close()
            return@callbackFlow
        }
        val listener =
            database.collection("perritos").document(id).addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { snapshot, error ->
                when {
                    error != null || snapshot == null ->
                        trySend(
                            PublicDog(
                                false,
                                error =
                                    "Esta ficha no está disponible. Revisa tu conexión e intenta de nuevo.",
                            )
                        )
                    snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                        trySend(PublicDog())
                    snapshot.getBoolean("publicado") != true ||
                        snapshot.getString("estado") !in listOf("Disponible", "En proceso") ->
                        trySend(
                            PublicDog(
                                false,
                                error = "Esta ficha ya no está disponible para adopción.",
                            )
                        )
                    else -> {
                        val dog = parse(snapshot)
                        trySend(
                            if (dog == null) PublicDog(false, error = "No pudimos leer esta ficha.")
                            else PublicDog(false, dog)
                        )
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun photo(path: String): ByteArray =
        com.example.huellitaschalco.core.firebase.FirestorePhotos(requireNotNull(db)).read(path)
}
