package com.example.huellitaschalco.nucleo.local

import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.funcionalidades.catalogo.dominio.*
import com.example.huellitaschalco.funcionalidades.refugio.dominio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class RepositorioCatalogoConCache(
    private val remote: RepositorioCatalogo,
    private val cache: CachePublicaRoom,
) : RepositorioCatalogo {
    override fun observe(): Flow<CatalogoPublico> = flow {
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

class RepositorioRefugioPublicoConCache(
    private val remote: RepositorioRefugioPublico,
    private val cache: CachePublicaRoom,
) : RepositorioRefugioPublico {
    override suspend fun update(uid: String, expected: RefugioPublico, draft: BorradorEdicionRefugio) =
        remote.update(uid, expected, draft)

    override fun observe(id: String): Flow<EstadoRefugioPublico> = flow {
        val saved =
            runCatching { cache.snapshot()?.shelters?.firstOrNull { it.id == id } }.getOrNull()
        remote.observe(id).collect { state ->
            if ((state.loading || state.error != null) && saved != null)
                emit(EstadoRefugioPublico(false, saved, stale = true))
            else emit(state)
        }
    }
}

class RepositorioPerritosConCache(private val remote: RepositorioPerritos, private val cache: CachePublicaRoom) :
    RepositorioPerritos by remote {
    override fun observePublic(id: String): Flow<PerritoPublico> = flow {
        val saved =
            runCatching { cache.snapshot()?.dogs?.firstOrNull { it.dog.id == id }?.dog }.getOrNull()
        remote.observePublic(id).collect { state ->
            if ((state.loading || state.error != null) && saved != null)
                emit(PerritoPublico(false, saved, stale = true))
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
