package com.example.huellitaschalco.feature.shelter.description

import com.example.huellitaschalco.feature.shelter.description.data.descriptionServiceFailure
import com.example.huellitaschalco.feature.shelter.description.domain.DescriptionFailureKind
import com.google.firebase.ai.type.PermissionMissingException
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class DescriptionFailureMapperTest {
    @Test
    fun connectionInsideSdkWrapperIsNotMistakenForAppAuthorization() {
        assertEquals(
            DescriptionFailureKind.Connection,
            descriptionServiceFailure(
                    Exception("detalle privado", IOException("red")),
                    checkingApp = true,
                )
                .kind,
        )
    }

    @Test
    fun permissionsAndAppCheckErrorsRequireAuthorization() {
        assertEquals(
            DescriptionFailureKind.AppVerification,
            descriptionServiceFailure(PermissionMissingException("detalle privado")).kind,
        )
        assertEquals(
            DescriptionFailureKind.AppVerification,
            descriptionServiceFailure(Exception("403 detalle privado"), checkingApp = true).kind,
        )
    }

    @Test
    fun unknownServerErrorDoesNotLeakItsMessage() {
        val failure = descriptionServiceFailure(Exception("clave privada y respuesta del servidor"))
        assertEquals(DescriptionFailureKind.Unavailable, failure.kind)
        assertFalse(failure.message!!.contains("clave privada"))
    }
}
