package com.example.huellitaschalco.feature.shelter.requests.presentation

import androidx.lifecycle.*
import com.example.huellitaschalco.feature.requests.domain.AdoptionRequest
import com.example.huellitaschalco.feature.shelter.requests.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EvaluationUiState(
    val attempt: EvaluationAttempt? = null,
    val sending: Boolean = false,
    val error: String? = null,
    val savedStatus: String? = null,
)

class EvaluationViewModel(
    private val uid: String,
    private val shelterId: String,
    private val id: String,
    private val repository: EvaluationRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(EvaluationUiState())
    val state = mutable.asStateFlow()

    fun select(item: AdoptionRequest, decision: EvaluationDecision) {
        if (
            !state.value.sending &&
                item.id == id &&
                item.shelterId == shelterId &&
                decision.allowedFrom(item.status)
        )
            mutable.update {
                it.copy(
                    attempt = EvaluationAttempt(uid, shelterId, id, item.status, decision),
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
                mutable.value = EvaluationUiState(savedStatus = attempt.decision.status)
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
                            if (e is EvaluationConflict) e.message
                            else
                                "No pudimos guardar la decisión. Revisa tu conexión y permisos e intenta de nuevo.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(uid: String, shelterId: String, id: String, repository: EvaluationRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EvaluationViewModel(uid, shelterId, id, repository) as T
            }
    }
}
