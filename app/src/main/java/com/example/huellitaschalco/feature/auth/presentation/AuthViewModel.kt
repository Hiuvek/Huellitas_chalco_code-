package com.example.huellitaschalco.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.huellitaschalco.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * La contraseña vive únicamente en memoria; nunca se guarda en Room, archivos o SavedStateHandle.
 */
class AuthViewModel(private val repository: AuthRepository, private val signUp: Boolean) :
    ViewModel() {
    private val current = MutableStateFlow(AuthUiState())
    val state = current.asStateFlow()

    fun edit(field: AuthField, value: String) {
        if (current.value.isLoading) return
        current.update { old ->
            val edited =
                when (field) {
                    AuthField.Name -> old.copy(name = value)
                    AuthField.Email -> old.copy(email = value)
                    AuthField.Password -> old.copy(password = value)
                }
            edited.copy(fieldErrors = old.fieldErrors - field, error = null, completed = false)
        }
    }

    fun submit() {
        val form = current.value
        if (form.isLoading || form.completed) return
        val errors = AuthValidation.validate(form.name, form.email, form.password, signUp)
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
                val problem = (error as? AuthFailure)?.problem ?: AuthProblem.Unknown
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
        current.value = AuthUiState()
    }

    companion object {
        fun factory(repository: AuthRepository, signUp: Boolean) = viewModelFactory {
            initializer { AuthViewModel(repository, signUp) }
        }
    }
}
