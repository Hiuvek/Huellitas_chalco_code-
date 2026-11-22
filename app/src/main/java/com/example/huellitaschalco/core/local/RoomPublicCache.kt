package com.example.huellitaschalco.core.local

import android.content.Context
import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*

/**
 * Only public records belong here. Requests, memberships and follow-up evidence never enter this
 * cache.
 */
class RoomPublicCache(
    private val dao: LocalDao,
    context: Context,
    directoryName: String = "public-catalog-photos",
) {
    private val photos = File(context.applicationContext.cacheDir, directoryName).apply { mkdirs() }

    suspend fun save(catalog: PublicCatalog) {
        require(!catalog.loading && catalog.error == null && !catalog.stale)
        val now = System.currentTimeMillis()
        val refuges =
            catalog.shelters.map { s ->
                val metadata =
                    JSONObject()
                        .put("demo", s.demonstration)
                        .put(
                            "requirements",
                            JSONArray(s.policy?.requirements ?: emptyList<String>()),
                        )
                        .put("steps", JSONArray(s.policy?.steps ?: emptyList<String>()))
                CachedShelterEntity(s.id, s.name, s.zone, s.hours, metadata.toString(), now)
            }
        val dogs =
            catalog.dogs.map { item ->
                val d = item.dog
                val metadata =
                    JSONObject()
                        .put("demo", d.demonstration)
                        .put(
                            "photos",
                            JSONArray(
                                d.photos.map { p ->
                                    JSONObject()
                                        .put("id", p.id)
                                        .put("path", p.path)
                                        .put("order", p.order)
                                }
                            ),
                        )
                CachedDogEntity(
                    d.id,
                    d.shelterId,
                    d.name,
                    d.ageMonths,
                    d.size,
                    d.sex,
                    d.description,
                    d.health.vaccination.label,
                    d.health.deworming.label,
                    d.health.sterilization.label,
                    d.status,
                    metadata.toString(),
                    now,
                )
            }
        check(dogs.all { d -> refuges.any { it.id == d.refugioId } })
        dao.replaceCatalog(refuges, dogs)
        val allowed = catalog.dogs.flatMap { it.dog.photos }.map { photoFile(it.path).name }.toSet()
        withContext(Dispatchers.IO) {
            photos.listFiles()?.filter { it.name !in allowed }?.forEach { it.delete() }
        }
    }

    suspend fun snapshot(): PublicCatalog? {
        val rows = dao.catalog()
        if (rows.isEmpty()) return null
        val shelters = rows.map { row ->
            val r = row.shelter
            val metadata = JSONObject(r.requisitosJson)
            PublicShelter(
                r.id,
                r.nombre,
                "Chalco",
                "Estado de México",
                r.zonaPublica,
                r.horarioPublico,
                metadata.optBoolean("demo"),
                AdoptionPolicy.fromStored(
                    metadata.getJSONArray("requirements").strings(),
                    metadata.getJSONArray("steps").strings(),
                ),
            )
        }
        val dogs =
            rows
                .flatMap { row ->
                    row.dogs.map { d ->
                        val metadata = JSONObject(d.fotosJson)
                        val pics =
                            metadata.getJSONArray("photos").let { list ->
                                (0 until list.length()).map { i ->
                                    val p = list.getJSONObject(i)
                                    DogPhoto(
                                        p.getString("id"),
                                        p.getString("path"),
                                        p.getInt("order"),
                                    )
                                }
                            }
                        CatalogDog(
                            RegisteredDog(
                                d.id,
                                d.refugioId,
                                d.nombre,
                                d.edadMeses,
                                d.tamano,
                                d.sexo,
                                d.descripcion,
                                pics,
                                metadata.optBoolean("demo"),
                                DogHealth(
                                    HealthStatus.fromStored(d.vacunacion),
                                    HealthStatus.fromStored(d.desparasitacion),
                                    HealthStatus.fromStored(d.esterilizacion),
                                ),
                                d.estado,
                            ),
                            row.shelter.nombre,
                        )
                    }
                }
                .sortedWith(compareBy<CatalogDog> { it.dog.name.searchKey() }.thenBy { it.dog.id })
        return PublicCatalog(
            false,
            dogs,
            shelters = shelters,
            stale = true,
            cachedAt = rows.maxOf { it.shelter.actualizadoEn },
        )
    }

    private fun photoFile(path: String) =
        File(
            photos,
            MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).joinToString("") {
                "%02x".format(it)
            } + ".jpg",
        )

    private suspend fun allows(path: String) =
        snapshot()?.dogs?.any { item -> item.dog.photos.any { it.path == path } } == true

    suspend fun savePhoto(path: String, bytes: ByteArray) {
        if (bytes.size <= 2 * 1024 * 1024 && allows(path))
            withContext(Dispatchers.IO) {
                val target = photoFile(path)
                val pending = File.createTempFile("photo-", ".tmp", photos)
                try {
                    pending.writeBytes(bytes)
                    check(pending.renameTo(target))
                } finally {
                    pending.delete()
                }
            }
    }

    suspend fun photo(path: String): ByteArray? =
        if (allows(path))
            withContext(Dispatchers.IO) {
                photoFile(path)
                    .takeIf { it.isFile && it.length() in 1..2L * 1024 * 1024 }
                    ?.readBytes()
            }
        else null
}

internal fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
