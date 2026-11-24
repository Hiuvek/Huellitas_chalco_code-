package com.example.huellitaschalco.funcionalidades.solicitud.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.UsuarioAutenticado
import com.example.huellitaschalco.funcionalidades.solicitud.dominio.*
import com.example.huellitaschalco.nucleo.local.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EstadoFormularioSolicitud(
    val draft: BorradorSolicitud,
    val step: Int = 0,
    val errors: Map<CampoSolicitud, String> = emptyMap(),
    val sending: Boolean = false,
    val locked: Boolean = false,
    val error: String? = null,
    val receipt: ComprobanteSolicitud? = null,
    val restoring: Boolean = false,
    val draftMessage: String? = null,
    val draftBlocked: Boolean = false,
)

class ModeloVistaFormularioSolicitud(
    private val repo: RepositorioSolicitudAdopcion,
    private val user: UsuarioAutenticado,
    private val dogId: String,
    private val shelterId: String,
    private val drafts: AlmacenBorradores? = null,
) : ViewModel() {
    private val current =
        MutableStateFlow(
            EstadoFormularioSolicitud(
                BorradorSolicitud(name = user.displayName.orEmpty(), email = user.email.orEmpty()),
                restoring = drafts != null,
            )
        )
    val state = current.asStateFlow()
    private var attempt: IntentoSolicitud? = null
    private var nameEdited = false
    private var emailEdited = false

    private val draftMutex = Mutex()

    private fun saved(value: EstadoFormularioSolicitud = state.value) =
        SolicitudGuardada(
            value.draft,
            value.step,
            attempt?.id,
            value.locked,
            nameEdited,
            emailEdited,
        )

    init {
        if (drafts != null)
            viewModelScope.launch {
                try {
                    val raw = drafts.get(user.uid, "SOLICITUD", dogId)
                    if (raw != null) {
                        val restored = ConversoresBorradores.application(raw)
                        check(!restored.locked || restored.attemptId != null)
                        nameEdited = restored.nameEdited
                        emailEdited = restored.emailEdited
                        attempt =
                            restored.attemptId?.let {
                                IntentoSolicitud(it, user.uid, dogId, shelterId, restored.draft)
                            }
                        current.value =
                            EstadoFormularioSolicitud(
                                restored.draft,
                                restored.step,
                                locked = restored.locked,
                                draftMessage = "Borrador restaurado en este dispositivo.",
                            )
                    } else current.update { it.copy(restoring = false) }
                    current
                        .map { saved(it) to (it.receipt != null) }
                        .distinctUntilChanged()
                        .collectLatest { (value, sent) ->
                            draftMutex.withLock {
                                if (sent) drafts.remove(user.uid, "SOLICITUD", dogId)
                                else
                                    drafts.put(
                                        user.uid,
                                        "SOLICITUD",
                                        dogId,
                                        ConversoresBorradores.application(value),
                                    )
                            }
                            current.update {
                                it.copy(
                                    draftMessage =
                                        if (sent) "Borrador eliminado después del envío confirmado."
                                        else "Borrador guardado en este dispositivo."
                                )
                            }
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    current.update {
                        it.copy(
                            restoring = false,
                            draftBlocked = true,
                            draftMessage =
                                "No se pudo recuperar o guardar el borrador. Vuelve a abrir la pantalla para reintentar; no se enviará la solicitud.",
                        )
                    }
                }
            }
    }

    fun receiveIdentity(identity: UsuarioAutenticado) {
        if (identity.uid != user.uid || current.value.locked || current.value.restoring) return
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

    fun edit(field: CampoSolicitud, value: String) {
        if (current.value.locked || current.value.restoring) return
        if (field == CampoSolicitud.Name) nameEdited = true
        if (field == CampoSolicitud.Email) emailEdited = true
        current.update { old ->
            old.copy(
                draft =
                    with(old.draft) {
                        when (field) {
                            CampoSolicitud.Name -> copy(name = value.take(101))
                            CampoSolicitud.Email -> copy(email = value.take(255))
                            CampoSolicitud.Phone -> copy(phone = value.take(15))
                            CampoSolicitud.Housing -> copy(housing = value)
                            CampoSolicitud.Tenure -> copy(tenure = value)
                            CampoSolicitud.Yard ->
                                copy(
                                    yard =
                                        when (value) {
                                            "Sí" -> true
                                            "No" -> false
                                            else -> null
                                        }
                                )
                            CampoSolicitud.People -> copy(people = value.take(3))
                            CampoSolicitud.Hours -> copy(hours = value)
                            CampoSolicitud.Pets ->
                                copy(
                                    pets =
                                        when (value) {
                                            "Sí" -> true
                                            "No" -> false
                                            else -> null
                                        }
                                )
                            CampoSolicitud.PetsDetails -> copy(petsDetails = value.take(501))
                            CampoSolicitud.Reasons -> copy(reasons = value.take(2001))
                        }
                    },
                errors = old.errors - field,
                error = null,
            )
        }
    }

    fun next() {
        val before = current.value
        if (before.locked || before.restoring || before.step >= 4) return
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
        if (
            before.step != 4 ||
                before.sending ||
                before.receipt != null ||
                before.restoring ||
                before.draftBlocked
        )
            return
        val errors = before.draft.errors()
        if (errors.isNotEmpty()) {
            current.update { it.copy(errors = errors, step = errors.keys.minOf { f -> f.step }) }
            return
        }
        val submission =
            attempt
                ?: IntentoSolicitud(
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
                if (drafts != null)
                    draftMutex.withLock {
                        drafts.put(user.uid, "SOLICITUD", dogId, ConversoresBorradores.application(saved()))
                    }
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
            } catch (error: SolicitudNoDisponible) {
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
            repo: RepositorioSolicitudAdopcion,
            user: UsuarioAutenticado,
            dogId: String,
            shelterId: String,
            drafts: AlmacenBorradores? = null,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaFormularioSolicitud(repo, user, dogId, shelterId, drafts) as T
            }
    }
}
