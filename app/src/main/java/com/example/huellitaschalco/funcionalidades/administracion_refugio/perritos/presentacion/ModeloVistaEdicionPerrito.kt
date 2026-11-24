package com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.funcionalidades.administracion_refugio.perritos.dominio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EstadoEdicionPerrito(
    val base: PerritoRegistrado,
    val draft: BorradorEdicionPerrito = BorradorEdicionPerrito.from(base),
    val errors: Map<CampoPerrito, String> = emptyMap(),
    val importing: Boolean = false,
    val saving: Boolean = false,
    val locked: Boolean = false,
    val review: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val conflict: PerritoRegistrado? = null,
    val saved: PerritoRegistrado? = null,
) {
    val dirty
        get() = draft != BorradorEdicionPerrito.from(base)
}

class ModeloVistaEdicionPerrito(
    private val repository: RepositorioPerritos,
    private val importer: ImportadorFotoPerrito,
    private val uid: String,
    initial: PerritoRegistrado,
) : ViewModel() {
    private val current = MutableStateFlow(EstadoEdicionPerrito(initial))
    val state = current.asStateFlow()
    private val temporary = mutableListOf<FotoLocalPerrito>()
    private val editable
        get() = !current.value.locked && !current.value.importing && current.value.saved == null

    fun edit(field: CampoPerrito, value: String) {
        if (!editable) return
        current.update { old ->
            val form = old.draft.form
            old.copy(
                draft =
                    old.draft.copy(
                        form =
                            when (field) {
                                CampoPerrito.Name -> form.copy(name = value.take(61))
                                CampoPerrito.Age -> form.copy(age = value.take(4))
                                CampoPerrito.AgeUnit -> form.copy(ageUnit = value)
                                CampoPerrito.Size -> form.copy(size = value)
                                CampoPerrito.Sex -> form.copy(sex = value)
                                CampoPerrito.Description -> form.copy(description = value.take(2001))
                                CampoPerrito.Photos -> form
                            }
                    ),
                errors = old.errors - field,
                review = false,
                error = null,
            )
        }
    }

    fun addPhotos(uris: List<String>) {
        if (!editable || uris.isEmpty()) return
        current.update { it.copy(importing = true, error = null, review = false) }
        viewModelScope.launch {
            try {
                val available = 1 - current.value.draft.photos.size
                for (uri in uris.distinct().take(available)) {
                    val photo = importer.import(uri)
                    temporary += photo
                    current.update {
                        it.copy(
                            draft =
                                it.draft.copy(
                                    photos = it.draft.photos + FotoPerritoEditable(local = photo)
                                ),
                            errors = it.errors - CampoPerrito.Photos,
                        )
                    }
                }
                if (uris.distinct().size > available)
                    current.update { it.copy(error = "Solo puedes conservar una fotografía.") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.update {
                    it.copy(
                        error =
                            "No se pudo leer la fotografía. Selecciona JPEG, PNG o WebP de hasta 20 MB; conservamos las demás."
                    )
                }
            } finally {
                current.update { it.copy(importing = false) }
            }
        }
    }

    fun removePhoto(id: String) {
        if (!editable) return
        current.update {
            it.copy(
                draft = it.draft.copy(photos = it.draft.photos.filterNot { p -> p.id == id }),
                review = false,
            )
        }
    }

    fun cover(id: String) {
        if (!editable) return
        current.update { old ->
            val photo = old.draft.photos.firstOrNull { it.id == id } ?: return@update old
            old.copy(
                draft =
                    old.draft.copy(
                        photos = listOf(photo) + old.draft.photos.filterNot { it.id == id }
                    ),
                review = false,
            )
        }
    }

    fun review() {
        if (!editable) return
        val errors = current.value.draft.errors()
        current.update { it.copy(errors = errors, review = errors.isEmpty(), error = null) }
    }

    fun returnToForm() {
        if (editable) current.update { it.copy(review = false) }
    }

    fun save() {
        val before = current.value
        if (
            before.saving ||
                before.importing ||
                !before.review ||
                before.conflict != null ||
                before.saved != null ||
                before.draft.errors().isNotEmpty()
        )
            return
        current.update { it.copy(saving = true, locked = true, error = null) }
        viewModelScope.launch {
            try {
                val result =
                    withTimeout(90_000) {
                        repository.updateDog(IntentoEdicionPerrito(uid, before.base, before.draft)) {
                            progress ->
                            current.update { it.copy(progress = progress) }
                        }
                    }
                current.update { it.copy(saving = false, saved = result, error = null) }
            } catch (_: TimeoutCancellationException) {
                current.update {
                    it.copy(
                        saving = false,
                        error =
                            "No pudimos confirmar a tiempo. Reintenta con los mismos datos para comprobar si se guardaron.",
                    )
                }
            } catch (conflict: ConflictoEdicionPerrito) {
                current.update {
                    it.copy(
                        saving = false,
                        conflict = conflict.actual,
                        error =
                            "Otro responsable cambió esta ficha. Conservamos tus cambios; carga el registro actual antes de editar de nuevo.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.update {
                    it.copy(
                        saving = false,
                        error =
                            "No pudimos guardar. Revisa tu conexión y permisos; conservamos tus datos y fotografías para reintentar.",
                    )
                }
            }
        }
    }

    fun reloadConflict() {
        val actual = current.value.conflict ?: return
        if (current.value.saving) return
        releasePhotos()
        current.value = EstadoEdicionPerrito(actual)
    }

    fun releasePhotos() {
        temporary.forEach(importer::remove)
        temporary.clear()
    }

    override fun onCleared() {
        releasePhotos()
    }

    companion object {
        fun factory(
            repository: RepositorioPerritos,
            importer: ImportadorFotoPerrito,
            uid: String,
            initial: PerritoRegistrado,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModeloVistaEdicionPerrito(repository, importer, uid, initial) as T
            }
    }
}
