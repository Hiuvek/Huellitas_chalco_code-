package com.example.huellitaschalco.feature.shelter.dogs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DogEditState(
    val base: RegisteredDog,
    val draft: DogEditDraft = DogEditDraft.from(base),
    val errors: Map<DogField, String> = emptyMap(),
    val importing: Boolean = false,
    val saving: Boolean = false,
    val locked: Boolean = false,
    val review: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val conflict: RegisteredDog? = null,
    val saved: RegisteredDog? = null,
) {
    val dirty
        get() = draft != DogEditDraft.from(base)
}

class DogEditViewModel(
    private val repository: DogRepository,
    private val importer: DogPhotoImporter,
    private val uid: String,
    initial: RegisteredDog,
) : ViewModel() {
    private val current = MutableStateFlow(DogEditState(initial))
    val state = current.asStateFlow()
    private val temporary = mutableListOf<LocalDogPhoto>()
    private val editable
        get() = !current.value.locked && !current.value.importing && current.value.saved == null

    fun edit(field: DogField, value: String) {
        if (!editable) return
        current.update { old ->
            val form = old.draft.form
            old.copy(
                draft =
                    old.draft.copy(
                        form =
                            when (field) {
                                DogField.Name -> form.copy(name = value.take(61))
                                DogField.Age -> form.copy(age = value.take(4))
                                DogField.AgeUnit -> form.copy(ageUnit = value)
                                DogField.Size -> form.copy(size = value)
                                DogField.Sex -> form.copy(sex = value)
                                DogField.Description -> form.copy(description = value.take(2001))
                                DogField.Photos -> form
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
                val available = 6 - current.value.draft.photos.size
                for (uri in uris.distinct().take(available)) {
                    val photo = importer.import(uri)
                    temporary += photo
                    current.update {
                        it.copy(
                            draft =
                                it.draft.copy(
                                    photos = it.draft.photos + EditableDogPhoto(local = photo)
                                ),
                            errors = it.errors - DogField.Photos,
                        )
                    }
                }
                if (uris.distinct().size > available)
                    current.update { it.copy(error = "Solo puedes conservar 6 fotografías.") }
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
                        repository.updateDog(DogEditAttempt(uid, before.base, before.draft)) {
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
            } catch (conflict: DogEditConflict) {
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
        current.value = DogEditState(actual)
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
            repository: DogRepository,
            importer: DogPhotoImporter,
            uid: String,
            initial: RegisteredDog,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DogEditViewModel(repository, importer, uid, initial) as T
            }
    }
}
