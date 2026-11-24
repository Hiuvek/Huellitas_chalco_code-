package com.example.huellitaschalco.nucleo.local

import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.funcionalidades.solicitud.dominio.BorradorSolicitud
import org.json.*

data class SolicitudGuardada(
    val draft: BorradorSolicitud,
    val step: Int,
    val attemptId: String?,
    val locked: Boolean,
    val nameEdited: Boolean,
    val emailEdited: Boolean,
)

data class PerritoGuardado(val draft: BorradorPerrito, val publicationId: String?, val locked: Boolean)

object ConversoresBorradores {
    fun application(value: SolicitudGuardada): String =
        with(value) {
            JSONObject()
                .put("version", 1)
                .put("step", step)
                .put("attemptId", attemptId)
                .put("locked", locked)
                .put("nameEdited", nameEdited)
                .put("emailEdited", emailEdited)
                .put(
                    "draft",
                    with(draft) {
                        JSONObject()
                            .put("name", name)
                            .put("email", email)
                            .put("phone", phone)
                            .put("housing", housing)
                            .put("tenure", tenure)
                            .put("yard", yard ?: JSONObject.NULL)
                            .put("people", people)
                            .put("hours", hours)
                            .put("pets", pets ?: JSONObject.NULL)
                            .put("petsDetails", petsDetails)
                            .put("reasons", reasons)
                    },
                )
                .toString()
        }

    fun application(json: String): SolicitudGuardada {
        val o = JSONObject(json)
        require(o.getInt("version") == 1)
        val d = o.getJSONObject("draft")
        return SolicitudGuardada(
            BorradorSolicitud(
                d.getString("name"),
                d.getString("email"),
                d.getString("phone"),
                d.getString("housing"),
                d.getString("tenure"),
                if (d.isNull("yard")) null else d.getBoolean("yard"),
                d.getString("people"),
                d.getString("hours"),
                if (d.isNull("pets")) null else d.getBoolean("pets"),
                d.getString("petsDetails"),
                d.getString("reasons"),
            ),
            o.getInt("step").coerceIn(0, 4),
            o.optString("attemptId").takeIf { it.isNotBlank() },
            o.getBoolean("locked"),
            o.getBoolean("nameEdited"),
            o.getBoolean("emailEdited"),
        )
    }

    fun dog(value: PerritoGuardado): String =
        with(value) {
            JSONObject()
                .put("version", 1)
                .put("publicationId", publicationId)
                .put("locked", locked)
                .put(
                    "draft",
                    with(draft) {
                        JSONObject()
                            .put("name", name)
                            .put("age", age)
                            .put("ageUnit", ageUnit)
                            .put("size", size)
                            .put("sex", sex)
                            .put("description", description)
                            .put(
                                "photos",
                                JSONArray(
                                    photos.map {
                                        JSONObject().put("id", it.id).put("file", it.filePath)
                                    }
                                ),
                            )
                            .put("vaccination", health.vaccination.label)
                            .put("deworming", health.deworming.label)
                            .put("sterilization", health.sterilization.label)
                    },
                )
                .toString()
        }

    fun dog(json: String): PerritoGuardado {
        val o = JSONObject(json)
        require(o.getInt("version") == 1)
        val d = o.getJSONObject("draft")
        val photos = d.getJSONArray("photos")
        return PerritoGuardado(
            BorradorPerrito(
                d.getString("name"),
                d.getString("age"),
                d.getString("ageUnit"),
                d.getString("size"),
                d.getString("sex"),
                d.getString("description"),
                (0 until photos.length()).map { i ->
                    photos.getJSONObject(i).let {
                        FotoLocalPerrito(it.getString("id"), it.getString("file"))
                    }
                },
                SaludPerrito(
                    EstadoSalud.fromStored(d.getString("vaccination")),
                    EstadoSalud.fromStored(d.getString("deworming")),
                    EstadoSalud.fromStored(d.getString("sterilization")),
                ),
            ),
            o.optString("publicationId").takeIf { it.isNotBlank() },
            o.getBoolean("locked"),
        )
    }
}
