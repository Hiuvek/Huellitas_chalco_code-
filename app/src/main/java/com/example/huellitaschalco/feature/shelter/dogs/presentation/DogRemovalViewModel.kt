package com.example.huellitaschalco.feature.shelter.dogs.presentation

import androidx.lifecycle.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DogRemovalState(
    val name: String = "",
    val confirming: Boolean = false,
    val removing: Boolean = false,
    val removed: Boolean = false,
    val error: String? = null,
)

class DogRemovalViewModel(
    private val repository: DogRepository,
    private val uid: String,
    private val shelterId: String,
    private val dogId: String,
) : ViewModel() {
    private val mutable = MutableStateFlow(DogRemovalState())
    val state = mutable.asStateFlow()

    fun ask(name: String) {
        if (!state.value.removing && !state.value.removed)
            mutable.update { it.copy(name = name, confirming = true, error = null) }
    }

    fun cancel() {
        if (!state.value.removing) mutable.update { it.copy(confirming = false, error = null) }
    }

    fun confirm() {
        if (!state.value.confirming || state.value.removing || state.value.removed) return
        mutable.update { it.copy(removing = true, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(30_000) { repository.remove(uid, shelterId, dogId) }
                mutable.update { it.copy(removing = false, confirming = false, removed = true) }
            } catch (cancelled: CancellationException) {
                if (cancelled !is TimeoutCancellationException) throw cancelled
                mutable.update {
                    it.copy(
                        removing = false,
                        error =
                            "No se confirmó la eliminación. Revisa tu conexión y reintenta; no se duplicará la operación.",
                    )
                }
            } catch (error: Exception) {
                mutable.update {
                    it.copy(
                        removing = false,
                        error =
                            if (error is DogRemovalRejected) error.message
                            else
                                "No se confirmó la eliminación. Revisa tu conexión y permisos antes de reintentar.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: DogRepository, uid: String, shelterId: String, dogId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DogRemovalViewModel(repository, uid, shelterId, dogId) as T
            }
    }
}
