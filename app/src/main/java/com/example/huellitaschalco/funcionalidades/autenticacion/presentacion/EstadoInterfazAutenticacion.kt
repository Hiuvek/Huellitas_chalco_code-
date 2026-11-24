package com.example.huellitaschalco.funcionalidades.autenticacion.presentacion

import com.example.huellitaschalco.funcionalidades.autenticacion.dominio.CampoAutenticacion

data class EstadoInterfazAutenticacion(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val fieldErrors: Map<CampoAutenticacion, String> = emptyMap(),
    val error: String? = null,
    val isLoading: Boolean = false,
    val completed: Boolean = false,
)
