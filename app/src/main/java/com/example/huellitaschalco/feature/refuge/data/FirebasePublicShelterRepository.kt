package com.example.huellitaschalco.feature.refuge.data

import com.example.huellitaschalco.feature.refuge.domain.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class FirebasePublicShelterRepository(private val db: FirebaseFirestore?) :
    PublicShelterRepository {
    override fun observe(id: String): Flow<PublicShelterState> = callbackFlow {
        val database = db
        if (database == null) {
            trySend(PublicShelterState(false, error = "No pudimos consultar el refugio."))
            close()
            return@callbackFlow
        }
        val listener =
            database.collection("refugios").document(id).addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { doc, error ->
                trySend(
                    when {
                        error != null || doc == null ->
                            PublicShelterState(
                                false,
                                error = "La información de este refugio no está disponible.",
                            )
                        doc.metadata.isFromCache || doc.metadata.hasPendingWrites() ->
                            PublicShelterState()
                        else ->
                            parse(doc)?.let { PublicShelterState(false, it) }
                                ?: PublicShelterState(
                                    false,
                                    error = "Este refugio no está disponible en Chalco.",
                                )
                    }
                )
            }
        awaitClose { listener.remove() }
    }

    override suspend fun update(uid: String, expected: PublicShelter, draft: ShelterEditDraft) {
        require(uid.isNotBlank() && draft.error() == null)
        val database = requireNotNull(db)
        val desired = draft.applyTo(expected)
        database
            .runTransaction { tx ->
                val ref = database.collection("refugios").document(expected.id)
                val current = parse(tx.get(ref)) ?: throw ShelterEditConflict()
                if (current != desired) {
                    if (current != expected) throw ShelterEditConflict()
                    tx.update(
                        ref,
                        mapOf(
                            "nombre" to desired.name,
                            "zonaPublica" to desired.zone,
                            "horarioPublico" to desired.hours,
                            "requisitos" to requireNotNull(desired.policy).requirements,
                            "pasos" to desired.policy.steps,
                            "ubicacionAproximada" to
                                desired.location?.let {
                                    mapOf("latitud" to it.latitude, "longitud" to it.longitude)
                                },
                            "actualizadoEn" to FieldValue.serverTimestamp(),
                            "actualizadoPorUid" to uid,
                        ),
                    )
                }
            }
            .await()
    }

    private fun parse(doc: DocumentSnapshot): PublicShelter? {
        val name = (doc.get("nombre") as? String)?.trim().orEmpty()
        if (
            doc.get("activo") != true ||
                doc.get("municipio") != "Chalco" ||
                doc.get("entidad") != "Estado de México" ||
                name.isEmpty()
        )
            return null
        return PublicShelter(
            doc.id,
            name,
            "Chalco",
            "Estado de México",
            (doc.get("zonaPublica") as? String)?.takeIf { it.isNotBlank() }
                ?: "Zona pública no registrada",
            (doc.get("horarioPublico") as? String)?.takeIf { it.isNotBlank() }
                ?: "Horario no registrado",
            doc.get("esDemostracion") == true,
            AdoptionPolicy.fromStored(doc.get("requisitos"), doc.get("pasos")),
            PublicCoordinates.fromStored(doc.get("ubicacionAproximada")),
        )
    }
}
