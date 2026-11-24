package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.datos

import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.google.firebase.firestore.DocumentSnapshot

internal fun parseRegisteredDog(doc: DocumentSnapshot): PerritoRegistrado? =
    runCatching {
            if (doc.getBoolean("publicado") != true || doc.getBoolean("eliminado") == true)
                return null
            val photos =
                (doc.get("fotos") as List<*>)
                    .map { raw ->
                        val map = raw as Map<*, *>
                        FotoPerrito(
                            map["archivoId"] as String,
                            map["ruta"] as String,
                            (map["orden"] as Number).toInt(),
                        )
                    }
                    .sortedBy { it.order }
            PerritoRegistrado(
                doc.id,
                doc.getString("refugioId")!!,
                doc.getString("nombre")!!,
                doc.getLong("edadMeses")!!.toInt(),
                doc.getString("tamano")!!,
                doc.getString("sexo")!!,
                doc.getString("descripcion")!!,
                photos,
                doc.getBoolean("esDemostracion") == true,
                SaludPerrito(
                    EstadoSalud.fromStored(doc.getString("vacunacion")),
                    EstadoSalud.fromStored(doc.getString("desparasitacion")),
                    EstadoSalud.fromStored(doc.getString("esterilizacion")),
                ),
                doc.getString("estado") ?: "Disponible",
            )
        }
        .getOrNull()
