package com.example.huellitaschalco.funcionalidades.refugio.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.refugio.dominio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoEdicionRefugio(
    val original: RefugioPublico? = null,
    val draft: BorradorEdicionRefugio = BorradorEdicionRefugio(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val conflict: Boolean = false,
) {
    val dirty
        get() = original?.let { BorradorEdicionRefugio.from(it) != draft } == true
}

class ModeloVistaEdicionRefugio(
    private val uid: String,
    private val repository: RepositorioRefugioPublico,
) : ViewModel() {
    private val mutable = MutableStateFlow(EstadoEdicionRefugio())
    val state = mutable.asStateFlow()

    fun load(info: RefugioPublico) {
        if (state.value.original == null) reload(info)
    }

    fun reload(info: RefugioPublico) {
        if (!state.value.saving) mutable.value = EstadoEdicionRefugio(info, BorradorEdicionRefugio.from(info))
    }

    fun edit(change: (BorradorEdicionRefugio) -> BorradorEdicionRefugio) {
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
                val conflict = error is ConflictoEdicionRefugio || error.cause is ConflictoEdicionRefugio
                mutable.update {
                    it.copy(
                        saving = false,
                        conflict = conflict,
                        error =
                            if (conflict) ConflictoEdicionRefugio().message
                            else
                                "No se confirmó el guardado. Revisa tu conexión y permisos y reintenta.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(uid: String, repository: RepositorioRefugioPublico) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaEdicionRefugio(uid, repository) as T
            }
    }
}
