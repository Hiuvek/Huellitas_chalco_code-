package com.example.huellitaschalco.funcionalidades.catalogo.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.funcionalidades.catalogo.dominio.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class EstadoInterfazCatalogo(
    val catalog: CatalogoPublico = CatalogoPublico(),
    val filters: FiltrosCatalogo = FiltrosCatalogo(),
) {
    val visible
        get() = catalog.dogs.filter(filters::matches)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloVistaCatalogo(repository: RepositorioCatalogo) : ViewModel() {
    private val refreshes = MutableStateFlow(0)
    private val filters = MutableStateFlow(FiltrosCatalogo())
    val state =
        combine(refreshes.flatMapLatest { repository.observe() }, filters) { catalog, selection ->
                EstadoInterfazCatalogo(catalog, selection)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0),
                EstadoInterfazCatalogo(),
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
        filters.value = FiltrosCatalogo()
    }

    fun retry() {
        refreshes.update { it + 1 }
    }

    companion object {
        fun factory(repository: RepositorioCatalogo) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaCatalogo(repository) as T
            }
    }
}
