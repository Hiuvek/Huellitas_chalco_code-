package com.example.huellitaschalco.funcionalidades.autenticacion.dominio

data class UsuarioAutenticado(val uid: String, val email: String, val displayName: String) {
    val label: String
        get() = displayName.ifBlank { email.substringBefore('@') }
}
