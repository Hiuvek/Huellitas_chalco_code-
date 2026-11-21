package com.example.huellitaschalco.feature.refuge.data

import com.example.huellitaschalco.feature.refuge.domain.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

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
                val state =
                    when {
                        error != null || doc == null ->
                            PublicShelterState(
                                false,
                                error = "La información de este refugio no está disponible.",
                            )
                        doc.metadata.isFromCache || doc.metadata.hasPendingWrites() ->
                            PublicShelterState()
                        else -> {
                            val name = (doc.get("nombre") as? String)?.trim().orEmpty()
                            if (
                                doc.get("activo") != true ||
                                    doc.get("municipio") != "Chalco" ||
                                    doc.get("entidad") != "Estado de México" ||
                                    name.isEmpty()
                            )
                                PublicShelterState(
                                    false,
                                    error = "Este refugio no está disponible en Chalco.",
                                )
                            else
                                PublicShelterState(
                                    false,
                                    PublicShelter(
                                        doc.id,
                                        name,
                                        "Chalco",
                                        "Estado de México",
                                        (doc.get("zonaPublica") as? String)?.takeIf {
                                            it.isNotBlank()
                                        } ?: "Zona pública no registrada",
                                        (doc.get("horarioPublico") as? String)?.takeIf {
                                            it.isNotBlank()
                                        } ?: "Horario no registrado",
                                        doc.get("esDemostracion") == true,
                                        AdoptionPolicy.fromStored(
                                            doc.get("requisitos"),
                                            doc.get("pasos"),
                                        ),
                                    ),
                                )
                        }
                    }
                trySend(state)
            }
        awaitClose { listener.remove() }
    }
}
