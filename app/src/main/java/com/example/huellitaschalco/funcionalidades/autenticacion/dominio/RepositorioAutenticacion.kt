package com.example.huellitaschalco.funcionalidades.autenticacion.dominio

import kotlinx.coroutines.flow.StateFlow

interface RepositorioAutenticacion {
    val user: StateFlow<UsuarioAutenticado?>
    val isConfigured: Boolean
    val usesEmulator: Boolean

    suspend fun signUp(name: String, email: String, password: String)

    suspend fun signIn(email: String, password: String)

    fun signOut()
}
