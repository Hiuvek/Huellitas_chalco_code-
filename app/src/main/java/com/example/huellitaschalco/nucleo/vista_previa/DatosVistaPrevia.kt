package com.example.huellitaschalco.nucleo.vista_previa

import com.example.huellitaschalco.R

/** Datos inmutables para presentar las pantallas; no son registros de una base de datos. */
data class PerritoVistaPrevia(
    val id: Int,
    val age: String,
    val size: String,
    val sex: String,
    val image: Int,
    val refuge: String = "Huellitas del Valle",
    val status: String = "Disponible",
    val vaccinated: String = "Sí",
    val dewormed: String = "Sí",
    val sterilized: String = "Sí",
) {
    val name
        get() = "Perrito $id"

    val description
        get() =
            when (id) {
                1 ->
                    "Fue rescatada en la calle y hoy es una perrita tranquila que disfruta las caminatas. Convive bien con otros perros y busca una familia paciente que le dé rutina y cariño."
                6 ->
                    "Es amigable con personas y niños. Se pone nervioso con ruidos fuertes, así que necesita un hogar tranquilo donde ganar confianza poco a poco."
                8 ->
                    "Perrito 8 es una perrita de 3 años aproximadamente, de tamaño mediano. Cuenta con vacunación. Está desparasitada. Aún no está esterilizada."
                else ->
                    "Busca un hogar responsable donde recibir cuidado y compañía. Conoce sus características y consulta los requisitos del refugio antes de solicitar su adopción."
            }
}

data class SolicitudVistaPrevia(
    val id: Int,
    val dogId: Int,
    val user: String,
    val folio: String,
    val date: String,
    val status: String,
)

object DatosVistaPrevia {
    val dogs =
        listOf(
            PerritoVistaPrevia(1, "2 años", "Mediano", "Hembra", R.drawable.dog_1),
            PerritoVistaPrevia(2, "4 meses", "Chico", "Macho", R.drawable.dog_2, status = "En proceso"),
            PerritoVistaPrevia(
                3,
                "6 años",
                "Grande",
                "Hembra",
                R.drawable.dog_3,
                refuge = "Huellitas del Valle",
            ),
            PerritoVistaPrevia(4, "1 año", "Chico", "Macho", R.drawable.dog_4),
            PerritoVistaPrevia(
                5,
                "9 años",
                "Grande",
                "Macho",
                R.drawable.dog_5,
                refuge = "Huellitas del Valle",
            ),
            PerritoVistaPrevia(
                6,
                "5 años",
                "Mediano",
                "Macho",
                R.drawable.dog_6,
                vaccinated = "No",
                sterilized = "No",
            ),
            PerritoVistaPrevia(7, "3 años", "Mediano", "Hembra", R.drawable.dog_7, status = "Adoptado"),
            PerritoVistaPrevia(8, "3 años", "Mediano", "Hembra", R.drawable.dog_8, sterilized = "No"),
        )

    fun dog(id: Int) = dogs.first { it.id == id }

    val requests =
        listOf(
            SolicitudVistaPrevia(1, 6, "Usuario 1", "HC-2026-0148", "16 nov 2026", "Enviada"),
            SolicitudVistaPrevia(4, 6, "Usuario 4", "HC-2026-0146", "15 nov 2026", "Enviada"),
            SolicitudVistaPrevia(2, 1, "Usuario 2", "HC-2026-0145", "14 nov 2026", "Enviada"),
            SolicitudVistaPrevia(7, 4, "Usuario 7", "HC-2026-0142", "10 nov 2026", "En revisión"),
            SolicitudVistaPrevia(8, 2, "Usuario 8", "HC-2026-0141", "7 nov 2026", "En revisión"),
            SolicitudVistaPrevia(3, 2, "Usuario 3", "HC-2026-0139", "5 nov 2026", "Aprobada"),
            SolicitudVistaPrevia(5, 1, "Usuario 5", "HC-2026-0133", "18 oct 2026", "No aprobada"),
            SolicitudVistaPrevia(6, 7, "Usuario 6", "HC-2026-0128", "20 sep 2026", "Adopción realizada"),
        )

    fun request(id: Int) = requests.first { it.id == id }

    val requirements =
        listOf(
            "Ser mayor de 18 años y presentar identificación oficial.",
            "Presentar comprobante de domicilio en Chalco o municipios cercanos.",
            "Contar con un espacio seguro, con sombra y agua limpia.",
            "Si rentas, tener permiso del arrendador para tener mascotas.",
            "Cubrir vacunas o esterilización pendientes, si aplica.",
            "Aceptar llamadas o visitas de seguimiento después de la adopción.",
        )
    val process =
        listOf(
            "Envía tu solicitud" to "Responde el formulario desde la app.",
            "Revisión del refugio" to "El refugio lee tus respuestas y te contacta.",
            "Entrevista y visita" to "Conoces al perrito en el refugio.",
            "Entrega y carta de adopción" to "Firmas el compromiso de cuidado responsable.",
            "Seguimiento" to "El refugio acompaña la adaptación en casa.",
        )
    const val followUpNotes = "Come bien, duerme dentro de casa y convive tranquila con la familia."
    const val oldFollowUpNotes =
        "Se adaptó bien; duerme dentro de casa y ya pasea con correa sin jalar."
    const val reasons =
        "Trabajo desde casa y tengo un hogar tranquilo para que Perrito 6 gane confianza poco a poco."
}
