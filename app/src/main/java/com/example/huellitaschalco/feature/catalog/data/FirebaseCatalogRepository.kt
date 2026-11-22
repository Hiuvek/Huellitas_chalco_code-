package com.example.huellitaschalco.feature.catalog.data

import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.data.parseRegisteredDog
import com.google.firebase.firestore.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

/** Se muestran únicamente respuestas del servidor; la persistencia offline corresponde a HU-17. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseCatalogRepository(
    private val db: FirebaseFirestore?,
    private val waitMillis: Long = 12_000,
) : CatalogRepository {
    override fun observe(): Flow<PublicCatalog> {
        val database =
            db
                ?: return flowOf(
                    PublicCatalog(
                        false,
                        error = "Firebase no está configurado. No pudimos cargar el catálogo.",
                    )
                )
        val dogs =
            database
                .collection("perritos")
                .whereEqualTo("publicado", true)
                .whereIn("estado", listOf("Disponible", "En proceso"))
        val shelters =
            database
                .collection("refugios")
                .whereEqualTo("activo", true)
                .whereEqualTo("municipio", "Chalco")
                .whereEqualTo("entidad", "Estado de México")
        return combine(dogs.values(), shelters.values()) { dogValue, shelterValue ->
                when {
                    dogValue is ServerQuery.Failed || shelterValue is ServerQuery.Failed ->
                        PublicCatalog(
                            false,
                            error =
                                "No pudimos cargar el catálogo. Revisa tu conexión e intenta de nuevo.",
                        )
                    dogValue !is ServerQuery.Ready || shelterValue !is ServerQuery.Ready ->
                        PublicCatalog()
                    else -> {
                        val names =
                            shelterValue.snapshot.documents
                                .mapNotNull { doc ->
                                    (doc.get("nombre") as? String)
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { doc.id to it }
                                }
                                .toMap()
                        val items =
                            dogValue.snapshot.documents
                                .mapNotNull { doc ->
                                    val dog = parseRegisteredDog(doc) ?: return@mapNotNull null
                                    val name = names[dog.shelterId] ?: return@mapNotNull null
                                    CatalogDog(dog, name)
                                }
                                .sortedWith(
                                    compareBy<CatalogDog> { it.dog.name.searchKey() }
                                        .thenBy { it.dog.id }
                                )
                        PublicCatalog(
                            false,
                            items,
                            shelters =
                                shelterValue.snapshot.documents.mapNotNull { d ->
                                    val name = names[d.id] ?: return@mapNotNull null
                                    PublicShelter(
                                        d.id,
                                        name,
                                        "Chalco",
                                        "Estado de México",
                                        d.getString("zonaPublica")?.takeIf { it.isNotBlank() }
                                            ?: "Zona pública no registrada",
                                        d.getString("horarioPublico")?.takeIf { it.isNotBlank() }
                                            ?: "Horario no registrado",
                                        d.getBoolean("esDemostracion") == true,
                                        AdoptionPolicy.fromStored(
                                            d.get("requisitos"),
                                            d.get("pasos"),
                                        ),
                                        PublicCoordinates.fromStored(d.get("ubicacionAproximada")),
                                    )
                                },
                        )
                    }
                }
            }
            .transformLatest { state ->
                emit(state)
                if (state.loading) {
                    delay(waitMillis)
                    emit(
                        PublicCatalog(
                            false,
                            error =
                                "No pudimos confirmar el catálogo con el servidor. Revisa tu conexión y reintenta.",
                        )
                    )
                }
            }
    }

    private fun Query.values(): Flow<ServerQuery> = callbackFlow {
        trySend(ServerQuery.Waiting)
        val listener =
            addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                trySend(
                    when {
                        error != null || snapshot == null -> ServerQuery.Failed
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                            ServerQuery.Waiting
                        else -> ServerQuery.Ready(snapshot)
                    }
                )
            }
        awaitClose { listener.remove() }
    }

    private sealed interface ServerQuery {
        data object Waiting : ServerQuery

        data object Failed : ServerQuery

        data class Ready(val snapshot: QuerySnapshot) : ServerQuery
    }
}
