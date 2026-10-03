package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Gasto

@Dao
interface GastoDao {

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun getAll(): LiveData<List<Gasto>>

    @Query("SELECT SUM(monto) FROM gastos WHERE fecha >= :desde AND fecha <= :hasta")
    fun getTotalGastosMes(desde: String, hasta: String): LiveData<Double?>

    // Gastos agrupados por categoría (el campo "concepto" es la categoría:
    // Alimento, Veterinario, Medicinas, Mantenimiento, Transporte, Otro),
    // para el desglose de "en qué se está gastando el dinero" en Reportes.
    @Query("SELECT concepto, SUM(monto) as total FROM gastos WHERE fecha >= :desde AND fecha <= :hasta GROUP BY concepto")
    fun getGastosPorCategoria(desde: String, hasta: String): LiveData<List<GastoCategoria>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gasto: Gasto): Long

    @Update
    suspend fun update(gasto: Gasto)

    @Delete
    suspend fun delete(gasto: Gasto)
}

data class GastoCategoria(val concepto: String, val total: Double)