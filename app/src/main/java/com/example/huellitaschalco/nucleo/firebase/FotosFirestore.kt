package com.example.huellitaschalco.nucleo.firebase

import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.firestore.*
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.tasks.await

/** Imágenes pequeñas en documentos separados; nunca se descargan con el listado de perritos. */
class FotosFirestore(private val db: FirebaseFirestore) {
    private fun reference(path: String) =
        db.collection("imagenes")
            .document(
                MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).joinToString("") {
                    "%02x".format(it)
                }
            )

    suspend fun upload(path: String, file: File) {
        val parts = path.split('/')
        val dog = parts.size == 5 && parts[0] == "perritos"
        require(dog || (parts.size == 5 && parts[0] == "refugios" && parts[2] == "seguimientos"))
        require(file.length() in 4..102400L)
        val bytes = file.readBytes()
        require(bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte())
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(
            bounds.outWidth in 1..800 &&
                bounds.outHeight in 1..800 &&
                bounds.outMimeType == "image/jpeg"
        )
        val fields =
            mapOf(
                "ruta" to path,
                "refugioId" to parts[1],
                "perritoId" to if (dog) parts[3] else null,
                "revisionId" to if (dog) null else parts[3],
                "datos" to Base64.encodeToString(bytes, Base64.NO_WRAP),
            )
        db.runTransaction { tx ->
                val ref = reference(path)
                val previous = tx.get(ref)
                if (previous.exists())
                    check(previous.data == fields) { "La fotografía ya existe con otro contenido." }
                else tx.set(ref, fields)
            }
            .await()
    }

    suspend fun read(path: String): ByteArray {
        val doc = reference(path).get(Source.SERVER).await()
        check(doc.getString("ruta") == path)
        val bytes = Base64.decode(requireNotNull(doc.getString("datos")), Base64.NO_WRAP)
        check(bytes.size in 4..102400)
        return bytes
    }
}
