package com.example.huellitaschalco.nucleo.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "cache_refugios")
data class EntidadRefugioCache(
    @PrimaryKey val id: String,
    val nombre: String,
    val zonaPublica: String,
    val horarioPublico: String,
    val requisitosJson: String,
    val actualizadoEn: Long,
)

@Entity(
    tableName = "cache_perritos",
    foreignKeys =
        [
            ForeignKey(
                entity = EntidadRefugioCache::class,
                parentColumns = ["id"],
                childColumns = ["refugioId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("refugioId")],
)
data class EntidadPerritoCache(
    @PrimaryKey val id: String,
    val refugioId: String,
    val nombre: String,
    val edadMeses: Int,
    val tamano: String,
    val sexo: String,
    val descripcion: String,
    val vacunacion: String,
    val desparasitacion: String,
    val esterilizacion: String,
    val estado: String,
    val fotosJson: String,
    val actualizadoEn: Long,
)

@Entity(tableName = "borradores", primaryKeys = ["propietarioLocal", "tipo", "referenciaId"])
data class EntidadBorrador(
    val propietarioLocal: String,
    val tipo: String,
    val referenciaId: String,
    val contenidoJson: String,
    val actualizadoEn: Long,
)

data class FilasCatalogoCache(
    @Embedded val shelter: EntidadRefugioCache,
    @Relation(parentColumn = "id", entityColumn = "refugioId") val dogs: List<EntidadPerritoCache>,
)

@Dao
abstract class ConsultasLocales {
    @Transaction
    @Query("SELECT * FROM cache_refugios ORDER BY id")
    abstract suspend fun catalog(): List<FilasCatalogoCache>

    @Query("DELETE FROM cache_perritos") abstract suspend fun clearDogs()

    @Query("DELETE FROM cache_refugios") abstract suspend fun clearShelters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertShelters(rows: List<EntidadRefugioCache>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDogs(rows: List<EntidadPerritoCache>)

    @Transaction
    open suspend fun replaceCatalog(
        shelters: List<EntidadRefugioCache>,
        dogs: List<EntidadPerritoCache>,
    ) {
        clearDogs()
        clearShelters()
        insertShelters(shelters)
        insertDogs(dogs)
    }

    @Query(
        "SELECT * FROM borradores WHERE propietarioLocal = :owner AND tipo = :type AND referenciaId = :reference LIMIT 1"
    )
    abstract suspend fun draft(owner: String, type: String, reference: String): EntidadBorrador?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putDraft(draft: EntidadBorrador)

    @Query(
        "DELETE FROM borradores WHERE propietarioLocal = :owner AND tipo = :type AND referenciaId = :reference"
    )
    abstract suspend fun deleteDraft(owner: String, type: String, reference: String)
}

@Database(
    entities = [EntidadRefugioCache::class, EntidadPerritoCache::class, EntidadBorrador::class],
    version = 1,
    exportSchema = true,
)
abstract class BaseDatosHuellitas : RoomDatabase() {
    abstract fun local(): ConsultasLocales

    companion object {
        fun open(context: Context, name: String = "huellitas-local.db") =
            Room.databaseBuilder(context.applicationContext, BaseDatosHuellitas::class.java, name)
                .build()
    }
}
