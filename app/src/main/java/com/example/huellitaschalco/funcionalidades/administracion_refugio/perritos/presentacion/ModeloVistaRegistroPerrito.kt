package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import com.example.huellitaschalco.nucleo.local.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EstadoRegistro(
    val draft: BorradorPerrito = BorradorPerrito(),
    val errors: Map<CampoPerrito, String> = emptyMap(),
    val importing: Boolean = false,
    val publishing: Boolean = false,
    val review: Boolean = false,
    val locked: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val published: PerritoRegistrado? = null,
    val restoring: Boolean = false,
    val draftMessage: String? = null,
    val draftBlocked: Boolean = false,
)

class ModeloVistaRegistroPerrito(
    private val repository: RepositorioPerritos,
    private val importer: ImportadorFotoPerrito,
    private val uid: String,
    private val shelterId: String,
    private val drafts: AlmacenBorradores? = null,
) : ViewModel() {
    private val current = MutableStateFlow(EstadoRegistro(restoring = drafts != null))
    val state = current.asStateFlow()
    private var publication: PublicacionPerrito? = null
    private var job: Job? = null

    private val draftMutex = Mutex()

    private fun saved(value: EstadoRegistro = state.value) =
        PerritoGuardado(value.draft, publication?.id, value.locked)

    init {
        if (drafts != null)
            viewModelScope.launch {
                try {
                    val raw = drafts.get(uid, "PERRITO", shelterId)
                    if (raw != null) {
                        val restored = ConversoresBorradores.dog(raw)
                        check(!restored.locked || restored.publicationId != null)
                        publication =
                            restored.publicationId?.let {
                                PublicacionPerrito(it, uid, shelterId, restored.draft)
                            }
                        current.value =
                            EstadoRegistro(
                                draft = restored.draft,
                                locked = restored.locked,
                                review = restored.locked,
                                draftMessage = "Borrador restaurado en este dispositivo.",
                            )
                    } else current.update { it.copy(restoring = false) }
                    current
                        .map { saved(it) to (it.published != null) }
                        .distinctUntilChanged()
                        .collectLatest { (value, published) ->
                            draftMutex.withLock {
                                if (
                                    published ||
                                        (value.draft == BorradorPerrito() && value.publicationId == null)
                                )
                                    drafts.remove(uid, "PERRITO", shelterId)
                                else drafts.put(uid, "PERRITO", shelterId, ConversoresBorradores.dog(value))
                            }
                            current.update {
                                it.copy(
                                    draftMessage =
                                        if (published) "Borrador eliminado después de publicar."
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
                                "No se pudo recuperar o guardar el borrador. Vuelve a abrir esta pantalla antes de publicar.",
                        )
                    }
                }
            }
    }

    fun edit(field: CampoPerrito, value: String) {
        if (current.value.locked || current.value.importing || current.value.restoring) return
        current.update { old ->
            old.copy(
                draft =
                    with(old.draft) {
                        when (field) {
                            CampoPerrito.Name -> copy(name = value.take(61))
                            CampoPerrito.Age -> copy(age = value.take(4))
                            CampoPerrito.AgeUnit -> copy(ageUnit = value)
                            CampoPerrito.Size -> copy(size = value)
                            CampoPerrito.Sex -> copy(sex = value)
                            CampoPerrito.Description -> copy(description = value.take(2001))
                            CampoPerrito.Photos -> this
                        }
                    },
                errors = old.errors - field,
                error = null,
                review = false,
            )
        }
    }

    fun editHealth(field: CampoSalud, value: EstadoSalud) {
        if (current.value.locked || current.value.importing || current.value.restoring) return
        current.update {
            it.copy(
                draft = it.draft.copy(health = it.draft.health.withValue(field, value)),
                review = false,
                error = null,
            )
        }
    }

    fun addPhotos(uris: List<String>) {
        if (
            current.value.locked ||
                current.value.importing ||
                current.value.restoring ||
                uris.isEmpty()
        )
            return
        current.update { it.copy(importing = true, error = null) }
        job =
            viewModelScope.launch {
                try {
                    val available = 1 - current.value.draft.photos.size
                    for (uri in uris.distinct().take(available)) {
                        val photo = importer.import(uri)
                        current.update {
                            it.copy(
                                draft = it.draft.copy(photos = it.draft.photos + photo),
                                errors = it.errors - CampoPerrito.Photos,
                            )
                        }
                    }
                    if (uris.distinct().size > available)
                        current.update { it.copy(error = "Solo puedes agregar una fotografía.") }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    current.update {
                        it.copy(
                            error =
                                "No se pudo leer una fotografía. Elige una imagen JPEG, PNG o WebP de hasta 20 MB e intenta de nuevo."
                        )
                    }
                } finally {
                    current.update { it.copy(importing = false) }
                }
            }
    }

    fun removePhoto(id: String) {
        if (current.value.locked || current.value.importing || current.value.restoring) return
        current.value.draft.photos.firstOrNull { it.id == id }?.let(importer::remove)
        current.update {
            it.copy(
                draft =
                    it.draft.copy(photos = it.draft.photos.filterNot { photo -> photo.id == id })
            )
        }
    }

    fun review() {
        if (current.value.restoring) return
        val errors = current.value.draft.errors()
        current.update { it.copy(errors = errors, review = errors.isEmpty(), error = null) }
    }

    fun returnToForm() {
        if (!current.value.locked) current.update { it.copy(review = false) }
    }

    fun publish() {
        val state = current.value
        if (
            state.publishing ||
                state.importing ||
                state.published != null ||
                !state.review ||
                state.restoring ||
                state.draftBlocked
        )
            return
        if (!repository.isConfigured) {
            current.update {
                it.copy(error = "La carga de fotografías no está habilitada en esta instalación.")
            }
            return
        }
        if (state.draft.errors().isNotEmpty()) {
            review()
            return
        }
        val attempt =
            publication
                ?: PublicacionPerrito(UUID.randomUUID().toString(), uid, shelterId, state.draft).also {
                    publication = it
                }
        current.update {
            it.copy(
                publishing = true,
                locked = true,
                error = null,
                progress = "Preparando publicación…",
            )
        }
        job =
            viewModelScope.launch {
                try {
                    if (drafts != null)
                        draftMutex.withLock {
                            drafts.put(uid, "PERRITO", shelterId, ConversoresBorradores.dog(saved()))
                        }
                    val result =
                        withTimeout(90_000) {
                            repository.publish(attempt) { progress ->
                                current.update { it.copy(progress = progress) }
                            }
                        }
                    current.update { it.copy(published = result, publishing = false, error = null) }
                } catch (_: TimeoutCancellationException) {
                    current.update {
                        it.copy(
                            publishing = false,
                            error =
                                "La publicación tardó demasiado. Reintenta con los mismos datos para comprobar el resultado sin duplicar el registro.",
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    current.update {
                        it.copy(
                            publishing = false,
                            error =
                                "No pudimos confirmar la publicación. Revisa tu conexión y permisos, y reintenta. Conservamos tus datos y fotografías.",
                        )
                    }
                }
            }
    }

    fun reset() {
        if (current.value.publishing || current.value.importing) return
        current.value.draft.photos.forEach(importer::remove)
        publication = null
        current.value = EstadoRegistro()
    }

    override fun onCleared() {
        job?.cancel()
        if (drafts == null) current.value.draft.photos.forEach(importer::remove)
    }

    companion object {
        fun factory(
            repository: RepositorioPerritos,
            importer: ImportadorFotoPerrito,
            uid: String,
            shelterId: String,
            drafts: AlmacenBorradores? = null,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaRegistroPerrito(repository, importer, uid, shelterId, drafts) as T
            }
    }
}
