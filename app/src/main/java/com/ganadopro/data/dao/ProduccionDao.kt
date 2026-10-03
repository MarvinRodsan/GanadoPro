package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Produccion

@Dao
interface ProduccionDao {

    @Query("SELECT * FROM produccion WHERE animalId = :animalId ORDER BY fecha DESC")
    fun getByAnimal(animalId: Long): LiveData<List<Produccion>>

    // Producción total del mes (todas las vacas)
    @Query("SELECT SUM(litros) FROM produccion WHERE fecha >= :desde AND fecha <= :hasta")
    fun getTotalMes(desde: String, hasta: String): LiveData<Double?>

    // Producción diaria agrupada para gráficas
    @Query("SELECT fecha, SUM(litros) as litros FROM produccion WHERE fecha >= :desde AND fecha <= :hasta GROUP BY fecha ORDER BY fecha ASC")
    fun getProduccionDiaria(desde: String, hasta: String): LiveData<List<ProduccionDiaria>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produccion: Produccion): Long

    @Update
    suspend fun update(produccion: Produccion)

    @Delete
    suspend fun delete(produccion: Produccion)
}

data class ProduccionDiaria(val fecha: String, val litros: Double)
