package com.example.huellitaschalco.feature.application.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.feature.application.domain.*
import com.example.huellitaschalco.feature.auth.domain.AuthUser
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ApplicationFormState(
    val draft: ApplicationDraft,
    val step: Int = 0,
    val errors: Map<ApplicationField, String> = emptyMap(),
    val sending: Boolean = false,
    val locked: Boolean = false,
    val error: String? = null,
    val receipt: ApplicationReceipt? = null,
)

class ApplicationFormViewModel(
    private val repo: AdoptionApplicationRepository,
    private val user: AuthUser,
    private val dogId: String,
    private val shelterId: String,
) : ViewModel() {
    private val current =
        MutableStateFlow(
            ApplicationFormState(
                ApplicationDraft(name = user.displayName.orEmpty(), email = user.email.orEmpty())
            )
        )
    val state = current.asStateFlow()
    private var attempt: ApplicationAttempt? = null
    private var nameEdited = false
    private var emailEdited = false

    fun receiveIdentity(identity: AuthUser) {
        if (identity.uid != user.uid || current.value.locked) return
        current.update {
            it.copy(
                draft =
                    it.draft.copy(
                        name = if (nameEdited) it.draft.name else identity.displayName,
                        email = if (emailEdited) it.draft.email else identity.email,
                    )
            )
        }
    }

    fun edit(field: ApplicationField, value: String) {
        if (current.value.locked) return
        if (field == ApplicationField.Name) nameEdited = true
        if (field == ApplicationField.Email) emailEdited = true
        current.update { old ->
            old.copy(
                draft =
                    with(old.draft) {
                        when (field) {
                            ApplicationField.Name -> copy(name = value.take(101))
                            ApplicationField.Email -> copy(email = value.take(255))
                            ApplicationField.Phone -> copy(phone = value.take(15))
                            ApplicationField.Housing -> copy(housing = value)
                            ApplicationField.Tenure -> copy(tenure = value)
                            ApplicationField.Yard ->
                                copy(
                                    yard =
                                        when (value) {
                                            "Sí" -> true
                                            "No" -> false
                                            else -> null
                                        }
                                )
                            ApplicationField.People -> copy(people = value.take(3))
                            ApplicationField.Hours -> copy(hours = value)
                            ApplicationField.Pets ->
                                copy(
                                    pets =
                                        when (value) {
                                            "Sí" -> true
                                            "No" -> false
                                            else -> null
                                        }
                                )
                            ApplicationField.PetsDetails -> copy(petsDetails = value.take(501))
                            ApplicationField.Reasons -> copy(reasons = value.take(2001))
                        }
                    },
                errors = old.errors - field,
                error = null,
            )
        }
    }

    fun next() {
        val before = current.value
        if (before.locked || before.step >= 4) return
        val errors = before.draft.errors(if (before.step == 3) null else before.step)
        current.update {
            it.copy(
                step = if (errors.isEmpty()) it.step + 1 else it.step,
                errors = errors,
                error = null,
            )
        }
    }

    fun back() {
        if (!current.value.locked && current.value.step > 0)
            current.update { it.copy(step = it.step - 1) }
    }

    fun editStep(step: Int) {
        if (!current.value.locked && step in 0..3) current.update { it.copy(step = step) }
    }

    fun send() {
        val before = current.value
        if (before.step != 4 || before.sending || before.receipt != null) return
        val errors = before.draft.errors()
        if (errors.isNotEmpty()) {
            current.update { it.copy(errors = errors, step = errors.keys.minOf { f -> f.step }) }
            return
        }
        val submission =
            attempt
                ?: ApplicationAttempt(
                        UUID.randomUUID().toString(),
                        user.uid,
                        dogId,
                        shelterId,
                        before.draft,
                    )
                    .also { attempt = it }
        current.update { it.copy(sending = true, locked = true, error = null) }
        viewModelScope.launch {
            try {
                val receipt = withTimeout(30_000) { repo.submit(submission) }
                current.update { it.copy(sending = false, receipt = receipt, error = null) }
            } catch (_: TimeoutCancellationException) {
                current.update {
                    it.copy(
                        sending = false,
                        error =
                            "No pudimos confirmar a tiempo. Conservamos tus respuestas; reintenta para comprobar el resultado sin duplicarlo.",
                    )
                }
            } catch (error: ApplicationUnavailable) {
                current.update { it.copy(sending = false, error = error.message) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.update {
                    it.copy(
                        sending = false,
                        error =
                            "No pudimos confirmar el envío. Revisa tu conexión y sesión; conservamos tus respuestas para reintentar.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            repo: AdoptionApplicationRepository,
            user: AuthUser,
            dogId: String,
            shelterId: String,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ApplicationFormViewModel(repo, user, dogId, shelterId) as T
            }
    }
}
