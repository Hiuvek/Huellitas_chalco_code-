package com.example.huellitaschalco.feature.refuge.domain

/** Una configuración incompleta no se presenta como una lista de condiciones suficiente. */
data class AdoptionPolicy(val requirements: List<String>, val steps: List<String>) {
    companion object {
        fun fromStored(requirements: Any?, steps: Any?): AdoptionPolicy? {
            fun completeList(raw: Any?): List<String>? {
                val list = raw as? List<*> ?: return null
                if (list.isEmpty() || list.any { it !is String || it.isBlank() }) return null
                return list.map { (it as String).trim() }
            }
            val conditions = completeList(requirements) ?: return null
            val process = completeList(steps) ?: return null
            return AdoptionPolicy(conditions, process)
        }
    }
}
