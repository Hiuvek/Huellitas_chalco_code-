package com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.dominio.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloVistaAgendaSeguimiento(uid: String, shelterId: String, repository: RepositorioSeguimiento) :
    ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state =
        refresh
            .flatMapLatest { repository.observe(uid, shelterId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                AgendaSeguimiento(),
            )

    fun retry() {
        refresh.update { it + 1 }
    }

    companion object {
        fun factory(uid: String, shelterId: String, repository: RepositorioSeguimiento) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaAgendaSeguimiento(uid, shelterId, repository) as T
            }
    }
}

data class EstadoFormularioProgramacion(
    val date: String = "",
    val type: String = "",
    val confirming: Boolean = false,
    val sending: Boolean = false,
    val locked: Boolean = false,
    val error: String? = null,
    val receipt: ComprobanteRevision? = null,
)

class ModeloVistaProgramacionRevision(
    private val uid: String,
    private val shelterId: String,
    private val adopted: PerritoAdoptado,
    private val repository: RepositorioSeguimiento,
) : ViewModel() {
    private var id = UUID.randomUUID().toString()
    private val mutable = MutableStateFlow(EstadoFormularioProgramacion())
    val state = mutable.asStateFlow()

    fun newSchedule() {
        if (state.value.receipt != null) {
            id = UUID.randomUUID().toString()
            mutable.value = EstadoFormularioProgramacion()
        }
    }

    fun date(value: String) {
        if (!state.value.locked) mutable.update { it.copy(date = value.take(10), error = null) }
    }

    fun type(value: String) {
        if (!state.value.locked && value in TiposRevision.labels)
            mutable.update { it.copy(type = value, error = null) }
    }

    private fun attempt() =
        IntentoProgramacion(id, uid, shelterId, adopted.adoption.id, state.value.date, state.value.type)

    fun prepare() {
        if (!state.value.sending && state.value.receipt == null) {
            val error = attempt().error(adopted.adoption.deliveryAt)
            mutable.update { it.copy(error = error, confirming = error == null) }
        }
    }

    fun cancel() {
        if (!state.value.sending) mutable.update { it.copy(confirming = false) }
    }

    fun submit() {
        if (!state.value.confirming || state.value.sending || state.value.receipt != null) return
        val input = attempt()
        input.error(adopted.adoption.deliveryAt)?.let {
            return
        }
        mutable.update { it.copy(sending = true, locked = true, error = null) }
        viewModelScope.launch {
            try {
                val result = withTimeout(30_000) { repository.program(input) }
                mutable.update { it.copy(sending = false, confirming = false, receipt = result) }
            } catch (_: TimeoutCancellationException) {
                mutable.update {
                    it.copy(
                        sending = false,
                        error =
                            "No pudimos confirmar la revisión. Conservamos los datos para reintentar.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutable.update {
                    it.copy(
                        sending = false,
                        locked = error !is RevisionRechazada,
                        error =
                            if (error is RevisionRechazada) error.message
                            else
                                "No se confirmó la revisión. Revisa tu conexión e intenta de nuevo.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            uid: String,
            shelterId: String,
            adopted: PerritoAdoptado,
            repository: RepositorioSeguimiento,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaProgramacionRevision(uid, shelterId, adopted, repository) as T
            }
    }
}
