package com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.solicitudes.dominio.*
import com.example.huellitaschalco.funcionalidades.solicitudes.dominio.SolicitudAdopcionRecibida
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoInterfazEvaluacion(
    val attempt: IntentoEvaluacion? = null,
    val sending: Boolean = false,
    val error: String? = null,
    val savedStatus: String? = null,
)

class ModeloVistaEvaluacion(
    private val uid: String,
    private val shelterId: String,
    private val id: String,
    private val repository: RepositorioEvaluacion,
) : ViewModel() {
    private val mutable = MutableStateFlow(EstadoInterfazEvaluacion())
    val state = mutable.asStateFlow()

    fun select(item: SolicitudAdopcionRecibida, decision: DecisionEvaluacion) {
        if (
            !state.value.sending &&
                item.id == id &&
                item.shelterId == shelterId &&
                decision.allowedFrom(item.status)
        )
            mutable.update {
                it.copy(
                    attempt = IntentoEvaluacion(uid, shelterId, id, item.status, decision),
                    error = null,
                )
            }
    }

    fun reason(text: String) {
        if (!state.value.sending)
            mutable.update {
                it.copy(attempt = it.attempt?.copy(reason = text.take(1001)), error = null)
            }
    }

    fun cancel() {
        if (!state.value.sending) mutable.update { it.copy(attempt = null, error = null) }
    }

    fun confirm() {
        val value = state.value
        if (value.sending) return
        val attempt = value.attempt ?: return
        attempt.error()?.let { error ->
            mutable.update { it.copy(error = error) }
            return
        }
        mutable.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(30_000) { repository.decide(attempt) }
                mutable.value = EstadoInterfazEvaluacion(savedStatus = attempt.decision.status)
            } catch (_: TimeoutCancellationException) {
                mutable.update {
                    it.copy(
                        sending = false,
                        error =
                            "No pudimos confirmar el cambio. Conservamos la decisión para reintentar.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update {
                    it.copy(
                        sending = false,
                        error =
                            if (e is ConflictoEvaluacion) e.message
                            else
                                "No pudimos guardar la decisión. Revisa tu conexión y permisos e intenta de nuevo.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(uid: String, shelterId: String, id: String, repository: RepositorioEvaluacion) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaEvaluacion(uid, shelterId, id, repository) as T
            }
    }
}
