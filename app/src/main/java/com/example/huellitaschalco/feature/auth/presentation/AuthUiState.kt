package com.example.huellitaschalco.feature.auth.presentation

import com.example.huellitaschalco.feature.auth.domain.AuthField

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val fieldErrors: Map<AuthField, String> = emptyMap(),
    val error: String? = null,
    val isLoading: Boolean = false,
    val completed: Boolean = false,
)
