package com.example.huellitaschalco.funcionalidades.refugio.dominio

import kotlinx.coroutines.flow.Flow

data class RefugioPublico(
    val id: String,
    val name: String,
    val municipality: String,
    val state: String,
    val zone: String,
    val hours: String,
    val demonstration: Boolean,
    val policy: PoliticaAdopcion? = null,
    val location: CoordenadasPublicas? = null,
)

data class EstadoRefugioPublico(
    val loading: Boolean = true,
    val shelter: RefugioPublico? = null,
    val error: String? = null,
    val stale: Boolean = false,
)

interface RepositorioRefugioPublico {
    fun observe(id: String): Flow<EstadoRefugioPublico>

    suspend fun update(uid: String, expected: RefugioPublico, draft: BorradorEdicionRefugio) {
        throw UnsupportedOperationException("La edición no está disponible.")
    }
}
