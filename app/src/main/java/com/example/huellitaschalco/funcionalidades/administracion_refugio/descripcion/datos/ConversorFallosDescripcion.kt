package com.example.huellitaschalco.funcionalidades.administracion_refugio.descripcion.datos

import com.example.huellitaschalco.funcionalidades.administracion_refugio.descripcion.dominio.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.type.*
import java.io.IOException

/** Interpreta tipos del SDK; no muestra ni registra los mensajes originales. */
internal fun descriptionServiceFailure(
    error: Exception,
    checkingApp: Boolean = false,
): FalloServicioDescripcion {
    val causes = generateSequence<Throwable>(error) { it.cause }.take(8).toList()
    val kind =
        when {
            causes.any { it is IOException || it is FirebaseNetworkException } ->
                TipoFalloDescripcion.Connection
            error is QuotaExceededException -> TipoFalloDescripcion.Quota
            error is RequestTimeoutException -> TipoFalloDescripcion.Timeout
            checkingApp || error is PermissionMissingException ->
                TipoFalloDescripcion.AppVerification
            error is APINotConfiguredException ||
                error is InvalidAPIKeyException ||
                error is ServiceDisabledException ||
                error is UnsupportedUserLocationException -> TipoFalloDescripcion.Configuration
            error is PromptBlockedException ||
                error is ContentBlockedException ||
                error is ResponseStoppedException -> TipoFalloDescripcion.Blocked
            else -> TipoFalloDescripcion.Unavailable
        }
    return FalloServicioDescripcion(kind)
}
