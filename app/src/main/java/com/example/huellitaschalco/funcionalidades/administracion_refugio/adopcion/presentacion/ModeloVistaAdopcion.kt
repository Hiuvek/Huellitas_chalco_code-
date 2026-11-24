package com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.adopcion.dominio.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoFormularioAdopcion(
    val date: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            .apply { timeZone = TimeZone.getTimeZone("America/Mexico_City") }
            .format(Date()),
    val signed: Boolean = false,
    val confirming: Boolean = false,
    val sending: Boolean = false,
    val locked: Boolean = false,
    val error: String? = null,
    val receipt: ComprobanteAdopcion? = null,
    val pendingClosed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloVistaAdopcion(
    private val uid: String,
    private val shelterId: String,
    private val requestId: String,
    private val dogId: String,
    private val repository: RepositorioAdopcion,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val existing =
        refresh
            .flatMapLatest { repository.observeDog(uid, shelterId, dogId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                EstadoAdopcion(),
            )
    private val mutable = MutableStateFlow(EstadoFormularioAdopcion())
    val form = mutable.asStateFlow()

    fun retry() {
        refresh.update { it + 1 }
    }

    fun date(value: String) {
        if (!form.value.locked) mutable.update { it.copy(date = value.take(10), error = null) }
    }

    fun signed(value: Boolean) {
        if (!form.value.locked) mutable.update { it.copy(signed = value, error = null) }
    }

    private fun attempt() =
        IntentoAdopcion(uid, requestId, shelterId, dogId, form.value.date, form.value.signed)

    fun prepare() {
        if (!form.value.sending && form.value.receipt == null)
            mutable.update {
                it.copy(error = attempt().error(), confirming = attempt().error() == null)
            }
    }

    fun cancel() {
        if (!form.value.sending) mutable.update { it.copy(confirming = false) }
    }

    fun completePending() {
        if (form.value.sending) return
        mutable.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(30_000) { repository.completePending(uid, shelterId, dogId) }
                mutable.update { it.copy(sending = false, pendingClosed = true) }
            } catch (e: CancellationException) {
                mutable.update { it.copy(sending = false) }
                if (e !is TimeoutCancellationException) throw e
                mutable.update {
                    it.copy(
                        error =
                            "No se confirmó el cierre. Puedes reintentar sin duplicar la entrega."
                    )
                }
            } catch (_: Exception) {
                mutable.update {
                    it.copy(
                        sending = false,
                        error =
                            "No se confirmó el cierre. Revisa la conexión y tus permisos y reintenta.",
                    )
                }
            }
        }
    }

    fun submit() {
        if (!form.value.confirming || form.value.sending || form.value.receipt != null) return
        val input = attempt()
        input.error()?.let { error ->
            mutable.update { it.copy(error = error) }
            return
        }
        mutable.update { it.copy(sending = true, locked = true, error = null) }
        viewModelScope.launch {
            try {
                val receipt = withTimeout(30_000) { repository.record(input) }
                mutable.update {
                    it.copy(
                        sending = false,
                        confirming = false,
                        receipt = receipt,
                        pendingClosed = true,
                    )
                }
            } catch (_: TimeoutCancellationException) {
                mutable.update {
                    it.copy(
                        sending = false,
                        error =
                            "No pudimos confirmar la entrega. Conservamos los datos para reintentar.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                mutable.update {
                    it.copy(
                        sending = false,
                        locked = error !is AdopcionRechazada,
                        error =
                            if (error is AdopcionRechazada) error.message
                            else
                                "No se confirmó la adopción. Revisa la conexión, tus permisos y que la solicitud siga aprobada.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            uid: String,
            shelterId: String,
            requestId: String,
            dogId: String,
            repository: RepositorioAdopcion,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaAdopcion(uid, shelterId, requestId, dogId, repository) as T
            }
    }
}
