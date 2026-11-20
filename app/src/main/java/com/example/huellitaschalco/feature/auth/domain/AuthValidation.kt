package com.example.huellitaschalco.feature.auth.domain

object AuthValidation {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun validate(
        name: String,
        email: String,
        password: String,
        signUp: Boolean,
    ): Map<AuthField, String> = buildMap {
        if (signUp && name.isBlank()) put(AuthField.Name, "Escribe tu nombre completo.")
        if (email.isBlank()) put(AuthField.Email, "Escribe tu correo electrónico.")
        else if (!emailPattern.matches(email.trim()))
            put(AuthField.Email, "Escribe un correo electrónico válido.")
        if (password.isBlank()) put(AuthField.Password, "Escribe tu contraseña.")
        else if (signUp && password.length < 6)
            put(AuthField.Password, "Usa al menos 6 caracteres.")
    }
}
