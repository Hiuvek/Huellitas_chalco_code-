package com.example.huellitaschalco.core.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "cache_refugios")
data class CachedShelterEntity(
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
                entity = CachedShelterEntity::class,
                parentColumns = ["id"],
                childColumns = ["refugioId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("refugioId")],
)
data class CachedDogEntity(
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
data class DraftEntity(
    val propietarioLocal: String,
    val tipo: String,
    val referenciaId: String,
    val contenidoJson: String,
    val actualizadoEn: Long,
)

data class CachedCatalogRows(
    @Embedded val shelter: CachedShelterEntity,
    @Relation(parentColumn = "id", entityColumn = "refugioId") val dogs: List<CachedDogEntity>,
)

@Dao
abstract class LocalDao {
    @Transaction
    @Query("SELECT * FROM cache_refugios ORDER BY id")
    abstract suspend fun catalog(): List<CachedCatalogRows>

    @Query("DELETE FROM cache_perritos") abstract suspend fun clearDogs()

    @Query("DELETE FROM cache_refugios") abstract suspend fun clearShelters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertShelters(rows: List<CachedShelterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDogs(rows: List<CachedDogEntity>)

    @Transaction
    open suspend fun replaceCatalog(
        shelters: List<CachedShelterEntity>,
        dogs: List<CachedDogEntity>,
    ) {
        clearDogs()
        clearShelters()
        insertShelters(shelters)
        insertDogs(dogs)
    }

    @Query(
        "SELECT * FROM borradores WHERE propietarioLocal = :owner AND tipo = :type AND referenciaId = :reference LIMIT 1"
    )
    abstract suspend fun draft(owner: String, type: String, reference: String): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putDraft(draft: DraftEntity)

    @Query(
        "DELETE FROM borradores WHERE propietarioLocal = :owner AND tipo = :type AND referenciaId = :reference"
    )
    abstract suspend fun deleteDraft(owner: String, type: String, reference: String)
}

@Database(
    entities = [CachedShelterEntity::class, CachedDogEntity::class, DraftEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HuellitasDatabase : RoomDatabase() {
    abstract fun local(): LocalDao

    companion object {
        fun open(context: Context, name: String = "huellitas-local.db") =
            Room.databaseBuilder(context.applicationContext, HuellitasDatabase::class.java, name)
                .build()
    }
}
