package com.example.huellitaschalco.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface CallableFunctions {
    suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?>
}

class CallableFailure(val status: String, message: String) : Exception(message)

class SdkCallableFunctions(private val functions: FirebaseFunctions) : CallableFunctions {
    override suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?> =
        try {
            @Suppress("UNCHECKED_CAST")
            (functions.getHttpsCallable(name).call(data).await().data as Map<String, Any?>)
        } catch (error: FirebaseFunctionsException) {
            throw CallableFailure(error.code.name, error.message ?: "No se confirmó la operación.")
        }
}

/**
 * Protocolo callable oficial, solo en el proyecto demo. No solicita un token FCM fuera del
 * emulador.
 */
class EmulatorCallableFunctions(private val auth: FirebaseAuth, private val host: String) :
    CallableFunctions {
    init {
        require(host in setOf("127.0.0.1", "10.0.2.2", "localhost"))
        require(auth.app.options.projectId == "demo-huellitas-chalco")
    }

    override suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?> {
        require(name.matches(Regex("[A-Za-z][A-Za-z0-9]{0,60}")))
        val user = requireNotNull(auth.currentUser)
        val token = requireNotNull(user.getIdToken(false).await().token)
        return withContext(Dispatchers.IO) {
            val connection =
                URL("http://$host:5001/demo-huellitas-chalco/us-central1/$name").openConnection()
                    as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 5_000
                connection.readTimeout = 20_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.outputStream.use {
                    it.write(
                        JSONObject()
                            .put("data", JSONObject(data))
                            .toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }
                val status = connection.responseCode
                val stream =
                    if (status in 200..299) connection.inputStream else connection.errorStream
                val response =
                    JSONObject(
                        requireNotNull(stream).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    )
                if (response.has("error")) {
                    val error = response.getJSONObject("error")
                    throw CallableFailure(
                        error.optString("status", "UNKNOWN"),
                        error.optString("message", "No se confirmó la operación."),
                    )
                }
                check(status in 200..299)
                val result = response.optJSONObject("result") ?: response.getJSONObject("data")
                result.toMap()
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun JSONObject.toMap(): Map<String, Any?> =
        keys().asSequence().associateWith { unwrap(get(it)) }

    private fun unwrap(value: Any?): Any? =
        when (value) {
            JSONObject.NULL,
            null -> null
            is JSONObject -> value.toMap()
            is JSONArray -> (0 until value.length()).map { unwrap(value.get(it)) }
            else -> value
        }
}
