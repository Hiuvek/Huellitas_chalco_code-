package com.example.huellitaschalco.core.firebase

interface CallableFunctions {
    suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?>
}

class CallableFailure(val status: String, message: String) : Exception(message)
