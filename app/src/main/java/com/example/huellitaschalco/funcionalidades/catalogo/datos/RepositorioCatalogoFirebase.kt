package com.example.huellitaschalco.funcionalidades.catalogo.datos

import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.datos.parseRegisteredDog
import com.example.huellitaschalco.funcionalidades.catalogo.dominio.*
import com.example.huellitaschalco.funcionalidades.refugio.dominio.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

/** Se muestran únicamente respuestas del servidor; la persistencia offline corresponde a HU-17. */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositorioCatalogoFirebase(
    private val db: FirebaseFirestore?,
    private val waitMillis: Long = 12_000,
) : RepositorioCatalogo {
    override fun observe(): Flow<CatalogoPublico> {
        val database =
            db
                ?: return flowOf(
                    CatalogoPublico(
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
                    dogValue is ConsultaServidor.Fallido || shelterValue is ConsultaServidor.Fallido ->
                        CatalogoPublico(
                            false,
                            error =
                                "No pudimos cargar el catálogo. Revisa tu conexión e intenta de nuevo.",
                        )
                    dogValue !is ConsultaServidor.Listo || shelterValue !is ConsultaServidor.Listo ->
                        CatalogoPublico()
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
                                    PerritoCatalogo(dog, name)
                                }
                                .sortedWith(
                                    compareBy<PerritoCatalogo> { it.dog.name.searchKey() }
                                        .thenBy { it.dog.id }
                                )
                        CatalogoPublico(
                            false,
                            items,
                            shelters =
                                shelterValue.snapshot.documents.mapNotNull { d ->
                                    val name = names[d.id] ?: return@mapNotNull null
                                    RefugioPublico(
                                        d.id,
                                        name,
                                        "Chalco",
                                        "Estado de México",
                                        d.getString("zonaPublica")?.takeIf { it.isNotBlank() }
                                            ?: "Zona pública no registrada",
                                        d.getString("horarioPublico")?.takeIf { it.isNotBlank() }
                                            ?: "Horario no registrado",
                                        d.getBoolean("esDemostracion") == true,
                                        PoliticaAdopcion.fromStored(
                                            d.get("requisitos"),
                                            d.get("pasos"),
                                        ),
                                        CoordenadasPublicas.fromStored(d.get("ubicacionAproximada")),
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
                        CatalogoPublico(
                            false,
                            error =
                                "No pudimos confirmar el catálogo con el servidor. Revisa tu conexión y reintenta.",
                        )
                    )
                }
            }
    }

    private fun Query.values(): Flow<ConsultaServidor> = callbackFlow {
        trySend(ConsultaServidor.EnEspera)
        val listener =
            addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                trySend(
                    when {
                        error != null || snapshot == null -> ConsultaServidor.Fallido
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites() ->
                            ConsultaServidor.EnEspera
                        else -> ConsultaServidor.Listo(snapshot)
                    }
                )
            }
        awaitClose { listener.remove() }
    }

    private sealed interface ConsultaServidor {
        data object EnEspera : ConsultaServidor

        data object Fallido : ConsultaServidor

        data class Listo(val snapshot: QuerySnapshot) : ConsultaServidor
    }
}
