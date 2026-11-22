package com.example.huellitaschalco.feature.requests.presentation

import androidx.lifecycle.*
import com.example.huellitaschalco.feature.requests.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModel(uid: String, id: String?, repository: AdoptionRequestsRepository) :
    ViewModel() {
    private val refreshes = MutableStateFlow(0)
    val list =
        refreshes
            .flatMapLatest { repository.observeOwn(uid) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0),
                RequestListState(),
            )
    val detail =
        refreshes
            .flatMapLatest {
                if (id == null) flowOf(RequestDetailState())
                else repository.observeOwnDetail(uid, id)
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
        fun factory(uid: String, id: String?, repository: AdoptionRequestsRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RequestsViewModel(uid, id, repository) as T
            }
    }
}
