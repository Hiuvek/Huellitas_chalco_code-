package com.example.huellitaschalco.feature.refuge.presentation

import androidx.lifecycle.*
import com.example.huellitaschalco.feature.refuge.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ShelterEditState(
    val original: PublicShelter? = null,
    val draft: ShelterEditDraft = ShelterEditDraft(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val conflict: Boolean = false,
) {
    val dirty
        get() = original?.let { ShelterEditDraft.from(it) != draft } == true
}

class ShelterEditViewModel(
    private val uid: String,
    private val repository: PublicShelterRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(ShelterEditState())
    val state = mutable.asStateFlow()

    fun load(info: PublicShelter) {
        if (state.value.original == null) reload(info)
    }

    fun reload(info: PublicShelter) {
        if (!state.value.saving) mutable.value = ShelterEditState(info, ShelterEditDraft.from(info))
    }

    fun edit(change: (ShelterEditDraft) -> ShelterEditDraft) {
        if (!state.value.saving && !state.value.saved)
            mutable.update { it.copy(draft = change(it.draft), error = null) }
    }

    fun save() {
        val before = state.value
        if (before.saving || before.saved || before.original == null || before.conflict) return
        before.draft.error()?.let { error ->
            mutable.update { it.copy(error = error) }
            return
        }
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(30_000) { repository.update(uid, before.original, before.draft) }
                mutable.update { it.copy(saving = false, saved = true) }
            } catch (cancelled: CancellationException) {
                if (cancelled !is TimeoutCancellationException) throw cancelled
                mutable.update {
                    it.copy(
                        saving = false,
                        error =
                            "No se confirmó el guardado. Conservamos tus cambios para reintentar.",
                    )
                }
            } catch (error: Exception) {
                val conflict = error is ShelterEditConflict || error.cause is ShelterEditConflict
                mutable.update {
                    it.copy(
                        saving = false,
                        conflict = conflict,
                        error =
                            if (conflict) ShelterEditConflict().message
                            else
                                "No se confirmó el guardado. Revisa tu conexión y permisos y reintenta.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(uid: String, repository: PublicShelterRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShelterEditViewModel(uid, repository) as T
            }
    }
}
