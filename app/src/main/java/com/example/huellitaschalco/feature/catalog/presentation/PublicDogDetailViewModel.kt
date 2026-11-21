package com.example.huellitaschalco.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.feature.refuge.domain.*
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DogDetailState(
    val loading: Boolean = true,
    val dog: RegisteredDog? = null,
    val shelter: PublicShelter? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PublicDogDetailViewModel(id: String, dogs: DogRepository, shelters: PublicShelterRepository) :
    ViewModel() {
    private val refreshes = MutableStateFlow(0)
    val state =
        refreshes
            .flatMapLatest { dogs.observePublic(id) }
            .flatMapLatest { value ->
                when {
                    value.loading -> flowOf(DogDetailState())
                    value.error != null || value.dog == null ->
                        flowOf(
                            DogDetailState(
                                false,
                                error = value.error ?: "Este perrito no está disponible.",
                            )
                        )
                    else ->
                        shelters.observe(value.dog.shelterId).map { refuge ->
                            when {
                                refuge.loading -> DogDetailState()
                                refuge.error != null || refuge.shelter == null ->
                                    DogDetailState(
                                        false,
                                        error = refuge.error ?: "Este refugio no está disponible.",
                                    )
                                else -> DogDetailState(false, value.dog, refuge.shelter)
                            }
                        }
                }
            }
            .transformLatest { value ->
                emit(value)
                if (value.loading) {
                    delay(12_000)
                    emit(
                        DogDetailState(
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
                DogDetailState(),
            )

    fun retry() {
        refreshes.update { it + 1 }
    }

    companion object {
        fun factory(id: String, dogs: DogRepository, shelters: PublicShelterRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PublicDogDetailViewModel(id, dogs, shelters) as T
            }
    }
}
