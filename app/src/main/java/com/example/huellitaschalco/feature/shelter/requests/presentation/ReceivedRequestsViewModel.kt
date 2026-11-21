package com.example.huellitaschalco.feature.shelter.requests.presentation

import androidx.lifecycle.*
import com.example.huellitaschalco.feature.requests.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReceivedRequestsViewModel(
    uid: String,
    shelterId: String,
    id: String?,
    repository: ReceivedRequestsRepository,
) : ViewModel() {
    private val refreshes = MutableStateFlow(0)
    val filter = MutableStateFlow("Todas")
    val list =
        refreshes
            .flatMapLatest { repository.observeReceived(uid, shelterId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                RequestListState(),
            )
    val detail =
        refreshes
            .flatMapLatest {
                if (id == null) flowOf(RequestDetailState())
                else repository.observeReceivedDetail(uid, shelterId, id)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                RequestDetailState(),
            )

    fun retry() {
        refreshes.update { it + 1 }
    }

    companion object {
        fun factory(
            uid: String,
            shelterId: String,
            id: String?,
            repository: ReceivedRequestsRepository,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReceivedRequestsViewModel(uid, shelterId, id, repository) as T
            }
    }
}
