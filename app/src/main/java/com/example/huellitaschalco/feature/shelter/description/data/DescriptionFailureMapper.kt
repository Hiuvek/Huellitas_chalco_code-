package com.example.huellitaschalco.feature.shelter.description.data

import com.example.huellitaschalco.feature.shelter.description.domain.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.type.*
import java.io.IOException

/** Interpreta tipos del SDK; no muestra ni registra los mensajes originales. */
internal fun descriptionServiceFailure(
    error: Exception,
    checkingApp: Boolean = false,
): DescriptionServiceFailure {
    val causes = generateSequence<Throwable>(error) { it.cause }.take(8).toList()
    val kind =
        when {
            causes.any { it is IOException || it is FirebaseNetworkException } ->
                DescriptionFailureKind.Connection
            error is QuotaExceededException -> DescriptionFailureKind.Quota
            error is RequestTimeoutException -> DescriptionFailureKind.Timeout
            checkingApp || error is PermissionMissingException ->
                DescriptionFailureKind.AppVerification
            error is APINotConfiguredException ||
                error is InvalidAPIKeyException ||
                error is ServiceDisabledException ||
                error is UnsupportedUserLocationException -> DescriptionFailureKind.Configuration
            error is PromptBlockedException ||
                error is ContentBlockedException ||
                error is ResponseStoppedException -> DescriptionFailureKind.Blocked
            else -> DescriptionFailureKind.Unavailable
        }
    return DescriptionServiceFailure(kind)
}
