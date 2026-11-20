package com.example.huellitaschalco.feature.auth.domain

data class AuthUser(val uid: String, val email: String, val displayName: String) {
    val label: String
        get() = displayName.ifBlank { email.substringBefore('@') }
}
