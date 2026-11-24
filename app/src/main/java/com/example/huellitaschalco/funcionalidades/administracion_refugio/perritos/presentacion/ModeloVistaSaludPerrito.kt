package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoSaludPerrito(
    val draft: SaludPerrito,
    val baseline: SaludPerrito,
    val latest: SaludPerrito,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val conflict: Boolean = false,
) {
    val dirty
        get() = draft != baseline
}

class ModeloVistaSaludPerrito(
    private val repository: RepositorioPerritos,
    private val uid: String,
    private val shelterId: String,
    private val dogId: String,
    initial: SaludPerrito,
) : ViewModel() {
    private val current = MutableStateFlow(EstadoSaludPerrito(initial, initial, initial))
    val state = current.asStateFlow()

    fun receive(health: SaludPerrito) {
        current.update {
            if (it.saving || it.dirty) it.copy(latest = health)
            else
                it.copy(
                    draft = health,
                    baseline = health,
                    latest = health,
                    saved = it.saved && health == it.baseline,
                )
        }
    }

    fun edit(field: CampoSalud, status: EstadoSalud) {
        if (current.value.saving) return
        current.update {
            it.copy(
                draft = it.draft.withValue(field, status),
                saved = false,
                error = if (it.conflict) it.error else null,
            )
        }
    }

    fun reload() {
        if (!current.value.saving)
            current.update {
                it.copy(
                    draft = it.latest,
                    baseline = it.latest,
                    error = null,
                    conflict = false,
                    saved = false,
                )
            }
    }

    fun save() {
        val before = current.value
        if (before.saving || before.conflict || !before.dirty) return
        current.update {
            it.copy(saving = true, saved = false, error = if (it.conflict) it.error else null)
        }
        viewModelScope.launch {
            try {
                withTimeout(30_000) {
                    repository.updateHealth(uid, shelterId, dogId, before.baseline, before.draft)
                }
                current.update {
                    val confirmed =
                        if (it.latest != before.baseline && it.latest != before.draft) it.latest
                        else before.draft
                    it.copy(
                        saving = false,
                        baseline = confirmed,
                        draft = confirmed,
                        latest = confirmed,
                        saved = true,
                        conflict = false,
                    )
                }
            } catch (_: TimeoutCancellationException) {
                current.update {
                    it.copy(
                        saving = false,
                        error =
                            "No pudimos confirmar el guardado a tiempo. Conservamos tus selecciones; reintenta para comprobar el resultado.",
                    )
                }
            } catch (error: ConflictoSalud) {
                current.update {
                    it.copy(
                        saving = false,
                        latest = error.actual,
                        conflict = true,
                        error =
                            "Otro responsable cambió la salud de este perrito. Conservamos tus selecciones; carga el estado actual antes de guardar otra vez.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.update {
                    it.copy(
                        saving = false,
                        error =
                            "No se pudo guardar la salud. Revisa tu conexión y permisos. Conservamos tus selecciones para reintentar.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: RepositorioPerritos,
            uid: String,
            shelterId: String,
            dogId: String,
            initial: SaludPerrito,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaSaludPerrito(repository, uid, shelterId, dogId, initial) as T
            }
    }
}
