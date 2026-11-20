package com.example.huellitaschalco.feature.auth

import com.example.huellitaschalco.feature.auth.domain.AuthField
import com.example.huellitaschalco.feature.auth.domain.AuthValidation
import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {
    @Test
    fun registrationRequiresAllFields() {
        assertEquals(
            setOf(AuthField.Name, AuthField.Email, AuthField.Password),
            AuthValidation.validate(" ", "", "", true).keys,
        )
    }

    @Test
    fun invalidEmailIsRejected() {
        listOf("usuario", "usuario@", "@ejemplo.mx", "usuario @ejemplo.mx", "usuario@@ejemplo.mx")
            .forEach {
                assertTrue(
                    it,
                    AuthField.Email in AuthValidation.validate("Usuario", it, "123456", true),
                )
            }
    }

    @Test
    fun surroundingEmailSpacesAreAccepted() {
        assertTrue(
            AuthValidation.validate("Usuario", " usuario+chalco@ejemplo.mx ", "123456", true)
                .isEmpty()
        )
    }

    @Test
    fun registrationRequiresSixCharacters() {
        assertTrue(
            AuthField.Password in AuthValidation.validate("Usuario", "u@ejemplo.mx", "12345", true)
        )
        assertTrue(AuthValidation.validate("Usuario", "u@ejemplo.mx", "123456", true).isEmpty())
    }

    @Test
    fun loginDoesNotRequireNameOrReapplyRegistrationPasswordPolicy() {
        assertTrue(AuthValidation.validate("", "u@ejemplo.mx", "123", false).isEmpty())
    }

    @Test
    fun whitespaceOnlyPasswordIsRejected() {
        assertTrue(
            AuthField.Password in AuthValidation.validate("Usuario", "u@ejemplo.mx", "      ", true)
        )
    }
}
