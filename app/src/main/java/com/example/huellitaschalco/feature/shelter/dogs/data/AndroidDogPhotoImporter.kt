package com.example.huellitaschalco.feature.shelter.dogs.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copias privadas temporales, sin GPS ni otros metadatos de la fotografía original. */
class AndroidDogPhotoImporter(context: Context) : DogPhotoImporter {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.filesDir, "draft-photos").apply { mkdirs() }

    override suspend fun import(uri: String): LocalDogPhoto =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val source = File(directory, "$id.source")
            val target = File(directory, "$id.jpg")
            try {
                resolver.openInputStream(Uri.parse(uri)).use { input ->
                    requireNotNull(input)
                    source.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 20 * 1024 * 1024)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(source.path, options)
                require(options.outWidth in 1..20000 && options.outHeight in 1..20000)
                require(options.outMimeType in listOf("image/jpeg", "image/png", "image/webp"))
                options.inJustDecodeBounds = false
                options.inSampleSize = 1
                while (
                    options.outWidth / options.inSampleSize > 1600 ||
                        options.outHeight / options.inSampleSize > 1600
                ) options.inSampleSize *= 2
                val bitmap = requireNotNull(BitmapFactory.decodeFile(source.path, options))
                val orientation = runCatching {
                    ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                }
                    .getOrDefault(1)
                val matrix =
                    Matrix().apply {
                        when (orientation) {
                            2 -> setScale(-1f, 1f)
                            3 -> setRotate(180f)
                            4 -> setScale(1f, -1f)
                            5 -> {
                                setRotate(90f)
                                postScale(-1f, 1f)
                            }
                            6 -> setRotate(90f)
                            7 -> {
                                setRotate(-90f)
                                postScale(-1f, 1f)
                            }
                            8 -> setRotate(-90f)
                        }
                    }
                val rotated =
                    if (matrix.isIdentity) bitmap
                    else
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                try {
                    for (quality in listOf(85, 65, 40)) {
                        target.outputStream().use {
                            require(rotated.compress(Bitmap.CompressFormat.JPEG, quality, it))
                        }
                        if (target.length() <= 2 * 1024 * 1024) break
                    }
                    require(target.length() in 1..(2 * 1024 * 1024).toLong())
                } finally {
                    if (rotated !== bitmap) rotated.recycle()
                    bitmap.recycle()
                }
                LocalDogPhoto(id, target.path)
            } catch (error: Exception) {
                target.delete()
                throw error
            } finally {
                source.delete()
            }
        }

    override fun remove(photo: LocalDogPhoto) {
        val file = File(photo.filePath)
        if (file.parentFile == directory) file.delete()
    }
}
