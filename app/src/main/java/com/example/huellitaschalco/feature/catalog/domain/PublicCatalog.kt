package com.example.huellitaschalco.feature.catalog.domain

import com.example.huellitaschalco.feature.shelter.dogs.domain.RegisteredDog
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.Flow

data class CatalogDog(val dog: RegisteredDog, val shelterName: String)

data class PublicCatalog(
    val loading: Boolean = true,
    val dogs: List<CatalogDog> = emptyList(),
    val error: String? = null,
)

interface CatalogRepository {
    fun observe(): Flow<PublicCatalog>
}

data class CatalogFilters(
    val query: String = "",
    val size: String = "Todos",
    val sex: String = "Todos",
    val age: String = "Todas",
) {
    fun matches(item: CatalogDog): Boolean =
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
