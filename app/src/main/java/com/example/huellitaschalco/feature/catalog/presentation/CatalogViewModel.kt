package com.example.huellitaschalco.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.feature.catalog.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class CatalogUiState(
    val catalog: PublicCatalog = PublicCatalog(),
    val filters: CatalogFilters = CatalogFilters(),
) {
    val visible
        get() = catalog.dogs.filter(filters::matches)
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(repository: CatalogRepository) : ViewModel() {
    private val refreshes = MutableStateFlow(0)
    private val filters = MutableStateFlow(CatalogFilters())
    val state =
        combine(refreshes.flatMapLatest { repository.observe() }, filters) { catalog, selection ->
                CatalogUiState(catalog, selection)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0),
                CatalogUiState(),
            )

    fun search(text: String) {
        filters.update { it.copy(query = text.take(100)) }
    }

    fun size(value: String) {
        if (value in listOf("Todos", "Chico", "Mediano", "Grande"))
            filters.update { it.copy(size = value) }
    }

    fun sex(value: String) {
        if (value in listOf("Todos", "Hembra", "Macho")) filters.update { it.copy(sex = value) }
    }

    fun age(value: String) {
        if (value in listOf("Todas", "Cachorro", "Adulto")) filters.update { it.copy(age = value) }
    }

    fun clear() {
        filters.value = CatalogFilters()
    }

    fun retry() {
        refreshes.update { it + 1 }
    }

    companion object {
        fun factory(repository: CatalogRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CatalogViewModel(repository) as T
            }
    }
}
