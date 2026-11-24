package com.example.huellitaschalco.nucleo.local

interface AlmacenBorradores {
    suspend fun get(owner: String, type: String, reference: String): String?

    suspend fun put(owner: String, type: String, reference: String, json: String)

    suspend fun remove(owner: String, type: String, reference: String)
}

class AlmacenBorradoresRoom(private val dao: ConsultasLocales) : AlmacenBorradores {
    override suspend fun get(owner: String, type: String, reference: String) =
        dao.draft(owner, type, reference)?.contenidoJson

    override suspend fun put(owner: String, type: String, reference: String, json: String) =
        dao.putDraft(EntidadBorrador(owner, type, reference, json, System.currentTimeMillis()))

    override suspend fun remove(owner: String, type: String, reference: String) =
        dao.deleteDraft(owner, type, reference)
}
