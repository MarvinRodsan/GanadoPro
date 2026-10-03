package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Venta

@Dao
interface VentaDao {

    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAll(): LiveData<List<Venta>>

    @Query("SELECT * FROM ventas WHERE animalId = :animalId")
    fun getByAnimal(animalId: Long): LiveData<List<Venta>>

    @Query("SELECT SUM(precio) FROM ventas WHERE fecha >= :desde AND fecha <= :hasta")
    fun getTotalGanancias(desde: String, hasta: String): LiveData<Double?>

    @Query("SELECT COUNT(*) FROM ventas WHERE fecha >= :desde AND fecha <= :hasta")
    fun countVentasMes(desde: String, hasta: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM ventas WHERE animalId = :animalId")
    suspend fun countByAnimal(animalId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: Venta): Long

    @Update
    suspend fun update(venta: Venta)

    @Delete
    suspend fun delete(venta: Venta)
}
