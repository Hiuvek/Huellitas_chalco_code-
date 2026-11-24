package com.example.huellitaschalco.funcionalidades.catalogo.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.funcionalidades.refugio.dominio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoFichaPerrito(
    val loading: Boolean = true,
    val dog: PerritoRegistrado? = null,
    val shelter: RefugioPublico? = null,
    val error: String? = null,
    val stale: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloVistaFichaPublicaPerrito(id: String, dogs: RepositorioPerritos, shelters: RepositorioRefugioPublico) :
    ViewModel() {
    private val refreshes = MutableStateFlow(0)
    val state =
        refreshes
            .flatMapLatest { dogs.observePublic(id) }
            .flatMapLatest { value ->
                when {
                    value.loading -> flowOf(EstadoFichaPerrito())
                    value.error != null || value.dog == null ->
                        flowOf(
                            EstadoFichaPerrito(
                                false,
                                error = value.error ?: "Este perrito no está disponible.",
                            )
                        )
                    else ->
                        shelters.observe(value.dog.shelterId).map { refuge ->
                            when {
                                refuge.loading -> EstadoFichaPerrito()
                                refuge.error != null || refuge.shelter == null ->
                                    EstadoFichaPerrito(
                                        false,
                                        error = refuge.error ?: "Este refugio no está disponible.",
                                    )
                                else ->
                                    EstadoFichaPerrito(
                                        false,
                                        value.dog,
                                        refuge.shelter,
                                        stale = value.stale || refuge.stale,
                                    )
                            }
                        }
                }
            }
            .transformLatest { value ->
                emit(value)
                if (value.loading) {
                    delay(12_000)
                    emit(
                        EstadoFichaPerrito(
                            false,
                            error =
                                "No pudimos confirmar la ficha con el servidor. Revisa tu conexión y reintenta.",
                        )
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0),
                EstadoFichaPerrito(),
            )

    fun retry() {
        refreshes.update { it + 1 }
    }

    companion object {
        fun factory(id: String, dogs: RepositorioPerritos, shelters: RepositorioRefugioPublico) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaFichaPublicaPerrito(id, dogs, shelters) as T
            }
    }
}
