package com.example.huellitaschalco.core.local

import com.example.huellitaschalco.feature.catalog.domain.*
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CachedCatalogRepository(
    private val remote: CatalogRepository,
    private val cache: RoomPublicCache,
) : CatalogRepository {
    override fun observe(): Flow<PublicCatalog> = flow {
        var saved = runCatching { cache.snapshot() }.getOrNull()
        saved?.let { emit(it) }
        remote.observe().collect { state ->
            when {
                state.loading -> emit(saved ?: state)
                state.error != null ->
                    emit(
                        saved?.copy(
                            cacheWarning =
                                "No se pudo actualizar. Puedes consultar la última copia guardada."
                        )
                            ?: state.copy(
                                error =
                                    "No hay catálogo guardado para usar sin conexión. Conéctate y pulsa Reintentar."
                            )
                    )
                else -> {
                    val persisted = runCatching { cache.save(state) }.isSuccess
                    if (persisted) saved = cache.snapshot()
                    emit(
                        state.copy(
                            cacheWarning =
                                if (persisted) null
                                else
                                    "El catálogo está actualizado, pero no pudo guardarse en este dispositivo."
                        )
                    )
                }
            }
        }
    }
}

class CachedPublicShelterRepository(
    private val remote: PublicShelterRepository,
    private val cache: RoomPublicCache,
) : PublicShelterRepository {
    override fun observe(id: String): Flow<PublicShelterState> = flow {
        val saved = runCatching {
            cache.snapshot()?.shelters?.firstOrNull { it.id == id }
        }.getOrNull()
        remote.observe(id).collect { state ->
            if ((state.loading || state.error != null) && saved != null)
                emit(PublicShelterState(false, saved, stale = true))
            else emit(state)
        }
    }
}

class CachedDogRepository(private val remote: DogRepository, private val cache: RoomPublicCache) :
    DogRepository by remote {
    override fun observePublic(id: String): Flow<PublicDog> = flow {
        val saved = runCatching {
            cache.snapshot()?.dogs?.firstOrNull { it.dog.id == id }?.dog
        }.getOrNull()
        remote.observePublic(id).collect { state ->
            if ((state.loading || state.error != null) && saved != null)
                emit(PublicDog(false, saved, stale = true))
            else emit(state)
        }
    }

    override suspend fun photo(path: String): ByteArray {
        try {
            val bytes = withTimeout(5_000) { remote.photo(path) }
            runCatching { cache.savePhoto(path, bytes) }
            return bytes
        } catch (e: TimeoutCancellationException) {
            return cache.photo(path) ?: throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return cache.photo(path) ?: throw e
        }
    }
}
