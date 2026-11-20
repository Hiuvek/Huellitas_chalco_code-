package com.example.huellitaschalco.feature.auth.domain

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val user: StateFlow<AuthUser?>
    val isConfigured: Boolean
    val usesEmulator: Boolean

    suspend fun signUp(name: String, email: String, password: String)

    suspend fun signIn(email: String, password: String)

    fun signOut()
}
