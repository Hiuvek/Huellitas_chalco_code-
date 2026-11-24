package com.example.huellitaschalco.nucleo.firebase

interface OperacionesInvocables {
    suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?>
}

class FalloOperacion(val status: String, message: String) : Exception(message)
