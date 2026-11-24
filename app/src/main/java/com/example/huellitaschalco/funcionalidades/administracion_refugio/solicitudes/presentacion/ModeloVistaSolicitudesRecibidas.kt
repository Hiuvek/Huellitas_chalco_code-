package com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloVistaSolicitudesRecibidas(
    uid: String,
    shelterId: String,
    id: String?,
    repository: RepositorioSolicitudesRecibidas,
) : ViewModel() {
    private val refreshes = MutableStateFlow(0)
    val filter = MutableStateFlow("Todas")
    val list =
        refreshes
            .flatMapLatest { repository.observeReceived(uid, shelterId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                EstadoListaSolicitudes(),
            )
    val detail =
        refreshes
            .flatMapLatest {
                if (id == null) flowOf(EstadoDetalleSolicitud())
                else repository.observeReceivedDetail(uid, shelterId, id)
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
        fun factory(
            uid: String,
            shelterId: String,
            id: String?,
            repository: RepositorioSolicitudesRecibidas,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaSolicitudesRecibidas(uid, shelterId, id, repository) as T
            }
    }
}
