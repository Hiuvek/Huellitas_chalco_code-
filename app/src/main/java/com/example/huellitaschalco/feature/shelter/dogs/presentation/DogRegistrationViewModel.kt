package com.example.huellitaschalco.feature.shelter.dogs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.huellitaschalco.feature.shelter.dogs.domain.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RegistrationState(
    val draft: DogDraft = DogDraft(),
    val errors: Map<DogField, String> = emptyMap(),
    val importing: Boolean = false,
    val publishing: Boolean = false,
    val review: Boolean = false,
    val locked: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val published: RegisteredDog? = null,
)

class DogRegistrationViewModel(
    private val repository: DogRepository,
    private val importer: DogPhotoImporter,
    private val uid: String,
    private val shelterId: String,
) : ViewModel() {
    private val current = MutableStateFlow(RegistrationState())
    val state = current.asStateFlow()
    private var publication: DogPublication? = null
    private var job: Job? = null

    fun edit(field: DogField, value: String) {
        if (current.value.locked || current.value.importing) return
        current.update { old ->
            old.copy(
                draft =
                    with(old.draft) {
                        when (field) {
                            DogField.Name -> copy(name = value.take(61))
                            DogField.Age -> copy(age = value.take(4))
                            DogField.AgeUnit -> copy(ageUnit = value)
                            DogField.Size -> copy(size = value)
                            DogField.Sex -> copy(sex = value)
                            DogField.Description -> copy(description = value.take(2001))
                            DogField.Photos -> this
                        }
                    },
                errors = old.errors - field,
                error = null,
                review = false,
            )
        }
    }

    fun editHealth(field: HealthField, value: HealthStatus) {
        if (current.value.locked || current.value.importing) return
        current.update {
            it.copy(
                draft = it.draft.copy(health = it.draft.health.withValue(field, value)),
                review = false,
                error = null,
            )
        }
    }

    fun addPhotos(uris: List<String>) {
        if (current.value.locked || current.value.importing || uris.isEmpty()) return
        current.update { it.copy(importing = true, error = null) }
        job = viewModelScope.launch {
            try {
                val available = 6 - current.value.draft.photos.size
                for (uri in uris.distinct().take(available)) {
                    val photo = importer.import(uri)
                    current.update {
                        it.copy(
                            draft = it.draft.copy(photos = it.draft.photos + photo),
                            errors = it.errors - DogField.Photos,
                        )
                    }
                }
                if (uris.distinct().size > available)
                    current.update { it.copy(error = "Solo puedes agregar 6 fotografías.") }
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
        if (current.value.locked || current.value.importing) return
        current.value.draft.photos.firstOrNull { it.id == id }?.let(importer::remove)
        current.update {
            it.copy(
                draft =
                    it.draft.copy(photos = it.draft.photos.filterNot { photo -> photo.id == id })
            )
        }
    }

    fun review() {
        val errors = current.value.draft.errors()
        current.update { it.copy(errors = errors, review = errors.isEmpty(), error = null) }
    }

    fun returnToForm() {
        if (!current.value.locked) current.update { it.copy(review = false) }
    }

    fun publish() {
        val state = current.value
        if (state.publishing || state.importing || state.published != null || !state.review) return
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
                ?: DogPublication(UUID.randomUUID().toString(), uid, shelterId, state.draft).also {
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
        job = viewModelScope.launch {
            try {
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
        current.value = RegistrationState()
    }

    override fun onCleared() {
        job?.cancel()
        current.value.draft.photos.forEach(importer::remove)
    }

    companion object {
        fun factory(
            repository: DogRepository,
            importer: DogPhotoImporter,
            uid: String,
            shelterId: String,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DogRegistrationViewModel(repository, importer, uid, shelterId) as T
            }
    }
}
