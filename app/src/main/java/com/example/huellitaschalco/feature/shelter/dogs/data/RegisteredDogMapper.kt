package com.example.huellitaschalco.feature.shelter.dogs.data

import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import com.google.firebase.firestore.DocumentSnapshot

internal fun parseRegisteredDog(doc: DocumentSnapshot): RegisteredDog? =
    runCatching {
            if (doc.getBoolean("publicado") != true || doc.getBoolean("eliminado") == true)
                return null
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
                DogHealth(
                    HealthStatus.fromStored(doc.getString("vacunacion")),
                    HealthStatus.fromStored(doc.getString("desparasitacion")),
                    HealthStatus.fromStored(doc.getString("esterilizacion")),
                ),
                doc.getString("estado") ?: "Disponible",
            )
        }
        .getOrNull()
