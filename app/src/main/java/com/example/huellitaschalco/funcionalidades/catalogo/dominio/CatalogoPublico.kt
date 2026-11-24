package com.example.huellitaschalco.funcionalidades.catalogo.dominio

import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.PerritoRegistrado
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.Flow

data class PerritoCatalogo(val dog: PerritoRegistrado, val shelterName: String)

data class CatalogoPublico(
    val loading: Boolean = true,
    val dogs: List<PerritoCatalogo> = emptyList(),
    val error: String? = null,
    val shelters: List<com.example.huellitaschalco.funcionalidades.refugio.dominio.RefugioPublico> =
        emptyList(),
    val stale: Boolean = false,
    val cachedAt: Long? = null,
    val cacheWarning: String? = null,
)

interface RepositorioCatalogo {
    fun observe(): Flow<CatalogoPublico>
}

data class FiltrosCatalogo(
    val query: String = "",
    val size: String = "Todos",
    val sex: String = "Todos",
    val age: String = "Todas",
) {
    fun matches(item: PerritoCatalogo): Boolean =
        (query.isBlank() ||
            "${item.dog.name} ${item.shelterName}"
                .searchKey()
                .contains(query.trim().searchKey())) &&
            (size == "Todos" || item.dog.size == size) &&
            (sex == "Todos" || item.dog.sex == sex) &&
            when (age) {
                "Cachorro" -> item.dog.ageMonths < 12
                "Adulto" -> item.dog.ageMonths >= 12
                else -> true
            }
}

internal fun String.searchKey() =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
