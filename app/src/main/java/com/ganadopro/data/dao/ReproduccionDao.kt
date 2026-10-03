package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Reproduccion

@Dao
interface ReproduccionDao {

    @Query("SELECT * FROM reproduccion WHERE animalId = :animalId ORDER BY fecha DESC")
    fun getByAnimal(animalId: Long): LiveData<List<Reproduccion>>

    // Trae los eventos que definen una gestación: el servicio (Inseminación/Monta)
    // y los que la cierran (Parto, o un Diagnóstico de Preñez negativo). Ver
    // ReproduccionUtils.calcularProximosPartos.
    @Query("""
        SELECT * FROM reproduccion 
        WHERE tipoEvento IN ('Inseminación', 'Monta', 'Diagnóstico Preñez', 'Parto')
        ORDER BY fecha DESC
    """)
    fun getEventosGestacion(): LiveData<List<Reproduccion>>

    // Versión "de una sola vez" (no LiveData), misma consulta, para el Worker
    // de alertas que corre en background sin un Observer de UI.
    @Query("""
        SELECT * FROM reproduccion 
        WHERE tipoEvento IN ('Inseminación', 'Monta', 'Diagnóstico Preñez', 'Parto')
        ORDER BY fecha DESC
    """)
    suspend fun getEventosGestacionSync(): List<Reproduccion>

    @Query("SELECT COUNT(*) FROM reproduccion WHERE tipoEvento = 'Parto' AND fecha >= :desde AND fecha <= :hasta")
    fun countPartosMes(desde: String, hasta: String): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reproduccion: Reproduccion): Long

    @Update
    suspend fun update(reproduccion: Reproduccion)

    @Delete
    suspend fun delete(reproduccion: Reproduccion)
}