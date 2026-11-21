package com.example.huellitaschalco.feature.shelter.dogs.data

import android.net.Uri
import com.example.huellitaschalco.feature.auth.domain.AuthRepository
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.google.firebase.firestore.*
import com.google.firebase.storage.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class FirebaseDogRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore?,
    private val storage: FirebaseStorage?,
    override val usesEmulator: Boolean,
) : DogRepository {
    override val isConfigured
        get() = db != null && storage != null

    override suspend fun publish(
        publication: DogPublication,
        progress: (String) -> Unit,
    ): RegisteredDog {
        require(publication.draft.errors().isEmpty())
        require(publication.id.matches(Regex("^[A-Za-z0-9-]{1,100}$")))
        require(publication.shelterId.matches(Regex("^[A-Za-z0-9_-]{1,100}$")))
        require(auth.user.value?.uid == publication.userUid)
        val database = requireNotNull(db)
        val bucket = requireNotNull(storage)
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
                "vacunacion" to "Sin información",
                "desparasitacion" to "Sin información",
                "esterilizacion" to "Sin información",
                "estado" to "Disponible",
                "publicado" to true,
                "esDemostracion" to usesEmulator,
                "fotos" to
                    photos.map {
                        mapOf("archivoId" to it.id, "ruta" to it.path, "orden" to it.order.toLong())
                    },
            )
        val reference = database.collection("perritos").document(publication.id)
        fun matches(snapshot: DocumentSnapshot): Boolean = fields.all { (key, value) ->
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
            val task =
                bucket.reference
                    .child(photos[index].path)
                    .putFile(
                        Uri.fromFile(file),
                        StorageMetadata.Builder().setContentType("image/jpeg").build(),
                    )
            suspendCancellableCoroutine<Unit> { continuation ->
                task.addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                task.addOnFailureListener {
                    if (continuation.isActive) continuation.resumeWithException(it)
                }
                continuation.invokeOnCancellation { task.cancel() }
            }
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

    private fun parse(doc: DocumentSnapshot): RegisteredDog? = runCatching {
        if (doc.getBoolean("publicado") != true) return null
        val photos =
            (doc.get("fotos") as List<*>)
                .map { raw ->
                    val map = raw as Map<*, *>
                    DogPhoto(
                        map["archivoId"] as String,
                        map["ruta"] as String,
                        (map["orden"] as Number).toInt(),
                    )
                }
                .sortedBy { it.order }
        RegisteredDog(
            doc.id,
            doc.getString("refugioId")!!,
            doc.getString("nombre")!!,
            doc.getLong("edadMeses")!!.toInt(),
            doc.getString("tamano")!!,
            doc.getString("sexo")!!,
            doc.getString("descripcion")!!,
            photos,
            doc.getBoolean("esDemostracion") == true,
        )
    }
        .getOrNull()

    override suspend fun photo(path: String): ByteArray =
        requireNotNull(storage).reference.child(path).getBytes(2L * 1024 * 1024).await()
}
