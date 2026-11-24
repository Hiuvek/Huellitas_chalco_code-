package com.example.huellitaschalco.funcionalidades.autenticacion.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * La contraseña vive únicamente en memoria; nunca se guarda en Room, archivos o SavedStateHandle.
 */
class ModeloVistaAutenticacion(private val repository: RepositorioAutenticacion, private val signUp: Boolean) :
    ViewModel() {
    private val current = MutableStateFlow(EstadoInterfazAutenticacion())
    val state = current.asStateFlow()

    fun edit(field: CampoAutenticacion, value: String) {
        if (current.value.isLoading) return
        current.update { old ->
            val edited =
                when (field) {
                    CampoAutenticacion.Name -> old.copy(name = value)
                    CampoAutenticacion.Email -> old.copy(email = value)
                    CampoAutenticacion.Password -> old.copy(password = value)
                }
            edited.copy(fieldErrors = old.fieldErrors - field, error = null, completed = false)
        }
    }

    fun submit() {
        val form = current.value
        if (form.isLoading || form.completed) return
        val errors = ValidacionAutenticacion.validate(form.name, form.email, form.password, signUp)
        if (errors.isNotEmpty()) {
            current.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        current.update { it.copy(isLoading = true, fieldErrors = emptyMap(), error = null) }
        viewModelScope.launch {
            try {
                if (signUp) repository.signUp(form.name.trim(), form.email.trim(), form.password)
                else repository.signIn(form.email.trim(), form.password)
                // Se anuncia éxito únicamente después de que Firebase termine.
                current.update { it.copy(isLoading = false, password = "", completed = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val problem = (error as? FalloAutenticacion)?.problem ?: ProblemaAutenticacion.Unknown
                current.update {
                    it.copy(
                        isLoading = false,
                        fieldErrors =
                            problem.field?.let { field -> mapOf(field to problem.message) }
                                ?: emptyMap(),
                        error = if (problem.field == null) problem.message else null,
                    )
                }
            }
        }
    }

    fun clear() {
        current.value = EstadoInterfazAutenticacion()
    }

    companion object {
        fun factory(repository: RepositorioAutenticacion, signUp: Boolean) = viewModelFactory {
            initializer { ModeloVistaAutenticacion(repository, signUp) }
        }
    }
}
