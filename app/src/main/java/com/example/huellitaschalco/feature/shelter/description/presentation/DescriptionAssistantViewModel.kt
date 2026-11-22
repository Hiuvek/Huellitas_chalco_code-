package com.example.huellitaschalco.feature.shelter.description.presentation

import androidx.lifecycle.*
import com.example.huellitaschalco.feature.shelter.description.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DescriptionAssistantState(
    val input: DescriptionInput? = null,
    val proposal: String? = null,
    val generating: Boolean = false,
    val error: String? = null,
)

class DescriptionAssistantViewModel(private val generator: DescriptionGenerator) : ViewModel() {
    private val mutable = MutableStateFlow(DescriptionAssistantState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun open(input: DescriptionInput?) {
        job?.cancel()
        mutable.value =
            DescriptionAssistantState(
                input = input,
                error =
                    if (input == null) "Primero completa nombre, edad, tamaño y sexo del perrito."
                    else null,
            )
    }

    fun generate() {
        val before = state.value
        if (before.generating) return
        val input = before.input ?: return
        if (!generator.isConfigured) {
            mutable.update {
                it.copy(
                    error =
                        "Gemini no está configurado en esta instalación. Puedes escribir la descripción manualmente."
                )
            }
            return
        }
        mutable.update { it.copy(generating = true, error = null) }
        job = viewModelScope.launch {
            try {
                val result =
                    withTimeout(30_000) {
                        DescriptionPolicy.validate(input, generator.generate(input))
                    }
                mutable.update { it.copy(generating = false, proposal = result, error = null) }
            } catch (_: TimeoutCancellationException) {
                mutable.update {
                    it.copy(
                        generating = false,
                        error =
                            "Gemini tardó demasiado. Puedes reintentar o escribir manualmente; tu descripción se conserva.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update {
                    it.copy(
                        generating = false,
                        error =
                            if (e is DescriptionRejected || e is DescriptionServiceFailure)
                                e.message
                            else
                                "No pudimos obtener una propuesta de Gemini. Revisa la conexión o sigue escribiendo manualmente.",
                    )
                }
            }
        }
    }

    fun edit(value: String) {
        if (!state.value.generating && state.value.proposal != null)
            mutable.update { it.copy(proposal = value.take(2000), error = null) }
    }

    fun selected(): String? {
        if (state.value.generating) return null
        val text = state.value.proposal?.trim() ?: return null
        if (text.length !in 40..2000) {
            mutable.update { it.copy(error = "La descripción debe tener de 40 a 2000 caracteres.") }
            return null
        }
        return text
    }

    fun close() {
        job?.cancel()
        mutable.value = DescriptionAssistantState()
    }

    companion object {
        fun factory(generator: DescriptionGenerator) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DescriptionAssistantViewModel(generator) as T
            }
    }
}
