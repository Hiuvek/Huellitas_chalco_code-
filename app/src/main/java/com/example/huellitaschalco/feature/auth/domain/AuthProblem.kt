package com.example.huellitaschalco.feature.auth.domain

enum class AuthField {
    Name,
    Email,
    Password,
}

enum class AuthProblem(val message: String, val field: AuthField? = null) {
    EmailInUse("Ya existe una cuenta con este correo. Inicia sesión.", AuthField.Email),
    InvalidEmail("Escribe un correo electrónico válido.", AuthField.Email),
    WeakPassword(
        "La contraseña no cumple los requisitos de seguridad. Usa al menos 6 caracteres.",
        AuthField.Password,
    ),
    InvalidCredentials(
        "El correo o la contraseña no coinciden. Revisa tus datos e intenta de nuevo."
    ),
    AccountDisabled("Esta cuenta está inhabilitada. Contacta al responsable de la app."),
    Network(
        "No pudimos conectarnos. Revisa tu conexión e intenta de nuevo; tus datos siguen aquí."
    ),
    TooManyAttempts("Hubo demasiados intentos. Espera un momento antes de volver a intentarlo."),
    Unavailable("El acceso todavía no está disponible en esta instalación. Intenta más tarde."),
    ProfileIncomplete(
        "Tu cuenta se creó, pero no pudimos guardar tu nombre. Pulsa Crear cuenta para reintentar sin duplicarla."
    ),
    Unknown("No pudimos completar la operación. Intenta de nuevo; tus datos siguen aquí."),
}

class AuthFailure(val problem: AuthProblem) : Exception(problem.message)
