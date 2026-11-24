package com.example.huellitaschalco.funcionalidades.refugio.dominio

/** Una configuración incompleta no se presenta como una lista de condiciones suficiente. */
data class PoliticaAdopcion(val requirements: List<String>, val steps: List<String>) {
    companion object {
        fun fromStored(requirements: Any?, steps: Any?): PoliticaAdopcion? {
            fun completeList(raw: Any?): List<String>? {
                val list = raw as? List<*> ?: return null
                if (list.isEmpty() || list.any { it !is String || it.isBlank() }) return null
                return list.map { (it as String).trim() }
            }
            val conditions = completeList(requirements) ?: return null
            val process = completeList(steps) ?: return null
            return PoliticaAdopcion(conditions, process)
        }
    }
}
