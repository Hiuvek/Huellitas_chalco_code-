package com.example.huellitaschalco.feature.refuge.domain

import kotlinx.coroutines.flow.Flow

data class PublicShelter(
    val id: String,
    val name: String,
    val municipality: String,
    val state: String,
    val zone: String,
    val hours: String,
    val demonstration: Boolean,
    val policy: AdoptionPolicy? = null,
)

data class PublicShelterState(
    val loading: Boolean = true,
    val shelter: PublicShelter? = null,
    val error: String? = null,
    val stale: Boolean = false,
)

interface PublicShelterRepository {
    fun observe(id: String): Flow<PublicShelterState>
}
