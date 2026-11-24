package com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.presentacion

import androidx.lifecycle.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.funcionalidades.administracion_refugio.seguimiento.dominio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoFormularioSeguimiento(
    val date: String = "",
    val adaptation: String = "",
    val notes: String = "",
    val photo: FotoLocalPerrito? = null,
    val importing: Boolean = false,
    val disposition: String = "",
    val nextDate: String = "",
    val nextType: String = "",
    val confirming: Boolean = false,
    val sending: Boolean = false,
    val locked: Boolean = false,
    val error: String? = null,
    val receipt: ComprobanteSeguimiento? = null,
)

class ModeloVistaRegistroSeguimiento(
    private val uid: String,
    private val shelterId: String,
    private val review: RevisionProgramada,
    private val deliveryAt: Long,
    private val repository: RepositorioSeguimiento,
    private val importer: ImportadorFotoPerrito,
) : ViewModel() {
    private val mutable = MutableStateFlow(EstadoFormularioSeguimiento())
    val state = mutable.asStateFlow()

    fun edit(transform: (EstadoFormularioSeguimiento) -> EstadoFormularioSeguimiento) {
        if (!state.value.locked && !state.value.importing)
            mutable.update { transform(it).copy(error = null, confirming = false) }
    }

    fun addPhoto(uri: String) {
        if (state.value.locked || state.value.importing) return
        mutable.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            try {
                val photo = importer.import(uri)
                state.value.photo?.let(importer::remove)
                mutable.update { it.copy(photo = photo, importing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(
                        importing = false,
                        error = "No pudimos abrir esa imagen. Elige otra fotografía.",
                    )
                }
            }
        }
    }

    fun removePhoto() {
        if (!state.value.locked && !state.value.importing) {
            state.value.photo?.let(importer::remove)
            mutable.update { it.copy(photo = null) }
        }
    }

    private fun attempt() =
        state.value.let {
            IntentoSeguimiento(
                uid,
                shelterId,
                review.id,
                it.date,
                it.adaptation,
                it.notes,
                it.photo,
                it.disposition,
                it.nextDate,
                it.nextType,
            )
        }

    fun prepare() {
        if (state.value.sending || state.value.importing || state.value.receipt != null) return
        val error = attempt().error(deliveryAt)
        mutable.update { it.copy(error = error, confirming = error == null) }
    }

    fun cancel() {
        if (!state.value.sending) mutable.update { it.copy(confirming = false) }
    }

    fun submit() {
        if (!state.value.confirming || state.value.sending || state.value.receipt != null) return
        val input = attempt()
        input.error(deliveryAt)?.let {
            return
        }
        mutable.update { it.copy(sending = true, locked = true, error = null) }
        viewModelScope.launch {
            try {
                val receipt = withTimeout(30_000) { repository.record(input) }
                mutable.update { it.copy(sending = false, confirming = false, receipt = receipt) }
                input.photo?.let(importer::remove)
            } catch (_: TimeoutCancellationException) {
                mutable.update {
                    it.copy(
                        sending = false,
                        error =
                            "No se confirmó el resultado. Conservamos los datos para reintentar.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update {
                    it.copy(
                        sending = false,
                        locked = e !is SeguimientoRechazado,
                        error =
                            if (e is SeguimientoRechazado) e.message
                            else
                                "No se confirmó el seguimiento. Revisa la conexión y reintenta con los mismos datos.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            uid: String,
            shelterId: String,
            review: RevisionProgramada,
            deliveryAt: Long,
            repository: RepositorioSeguimiento,
            importer: ImportadorFotoPerrito,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaRegistroSeguimiento(
                        uid,
                        shelterId,
                        review,
                        deliveryAt,
                        repository,
                        importer,
                    )
                        as T
            }
    }
}
