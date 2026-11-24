package com.example.huellitaschalco.funcionalidades.refugio.datos

import com.example.huellitaschalco.funcionalidades.refugio.dominio.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class RepositorioRefugioPublicoFirebase(private val db: FirebaseFirestore?) :
    RepositorioRefugioPublico {
    override fun observe(id: String): Flow<EstadoRefugioPublico> = callbackFlow {
        val database = db
        if (database == null) {
            trySend(EstadoRefugioPublico(false, error = "No pudimos consultar el refugio."))
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
                            EstadoRefugioPublico(
                                false,
                                error = "La información de este refugio no está disponible.",
                            )
                        doc.metadata.isFromCache || doc.metadata.hasPendingWrites() ->
                            EstadoRefugioPublico()
                        else ->
                            parse(doc)?.let { EstadoRefugioPublico(false, it) }
                                ?: EstadoRefugioPublico(
                                    false,
                                    error = "Este refugio no está disponible en Chalco.",
                                )
                    }
                )
            }
        awaitClose { listener.remove() }
    }

    override suspend fun update(uid: String, expected: RefugioPublico, draft: BorradorEdicionRefugio) {
        require(uid.isNotBlank() && draft.error() == null)
        val database = requireNotNull(db)
        val desired = draft.applyTo(expected)
        database
            .runTransaction { tx ->
                val ref = database.collection("refugios").document(expected.id)
                val current = parse(tx.get(ref)) ?: throw ConflictoEdicionRefugio()
                if (current != desired) {
                    if (current != expected) throw ConflictoEdicionRefugio()
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

    private fun parse(doc: DocumentSnapshot): RefugioPublico? {
        val name = (doc.get("nombre") as? String)?.trim().orEmpty()
        if (
            doc.get("activo") != true ||
                doc.get("municipio") != "Chalco" ||
                doc.get("entidad") != "Estado de México" ||
                name.isEmpty()
        )
            return null
        return RefugioPublico(
            doc.id,
            name,
            "Chalco",
            "Estado de México",
            (doc.get("zonaPublica") as? String)?.takeIf { it.isNotBlank() }
                ?: "Zona pública no registrada",
            (doc.get("horarioPublico") as? String)?.takeIf { it.isNotBlank() }
                ?: "Horario no registrado",
            doc.get("esDemostracion") == true,
            PoliticaAdopcion.fromStored(doc.get("requisitos"), doc.get("pasos")),
            CoordenadasPublicas.fromStored(doc.get("ubicacionAproximada")),
        )
    }
}
