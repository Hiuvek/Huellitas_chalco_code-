package com.example.huellitaschalco.funcionalidades.solicitudes.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloVistaSolicitudes(uid: String, id: String?, repository: RepositorioSolicitudesAdopcion) :
    ViewModel() {
    private val refreshes = MutableStateFlow(0)
    val list =
        refreshes
            .flatMapLatest { repository.observeOwn(uid) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                EstadoListaSolicitudes(),
            )
    val detail =
        refreshes
            .flatMapLatest {
                if (id == null) flowOf(EstadoDetalleSolicitud())
                else repository.observeOwnDetail(uid, id)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                EstadoDetalleSolicitud(),
            )

    fun retry() {
        refreshes.update { it + 1 }
    }

    companion object {
        fun factory(uid: String, id: String?, repository: RepositorioSolicitudesAdopcion) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaSolicitudes(uid, id, repository) as T
            }
    }
}
