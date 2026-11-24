package com.example.huellitaschalco.funcionalidades.autenticacion.dominio

object ValidacionAutenticacion {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun validate(
        name: String,
        email: String,
        password: String,
        signUp: Boolean,
    ): Map<CampoAutenticacion, String> = buildMap {
        if (signUp && name.isBlank()) put(CampoAutenticacion.Name, "Escribe tu nombre completo.")
        if (email.isBlank()) put(CampoAutenticacion.Email, "Escribe tu correo electrónico.")
        else if (!emailPattern.matches(email.trim()))
            put(CampoAutenticacion.Email, "Escribe un correo electrónico válido.")
        if (password.isBlank()) put(CampoAutenticacion.Password, "Escribe tu contraseña.")
        else if (signUp && password.length < 6)
            put(CampoAutenticacion.Password, "Usa al menos 6 caracteres.")
    }
}
