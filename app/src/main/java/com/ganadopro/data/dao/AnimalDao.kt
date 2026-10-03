package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Animal

@Dao
interface AnimalDao {

    @Query("SELECT * FROM animales ORDER BY nombre ASC")
    fun getAll(): LiveData<List<Animal>>

    @Query("SELECT * FROM animales WHERE id = :id")
    suspend fun getById(id: Long): Animal?

    // Para validar código no duplicado antes de guardar (ver FormAnimalFragment)
    @Query("SELECT * FROM animales WHERE codigo = :codigo LIMIT 1")
    suspend fun getByCodigo(codigo: String): Animal?

    @Query("SELECT * FROM animales WHERE codigo LIKE '%' || :query || '%' OR nombre LIKE '%' || :query || '%'")
    fun search(query: String): LiveData<List<Animal>>

    @Query("SELECT COUNT(*) FROM animales WHERE estado = 'Activo'")
    fun countActivos(): LiveData<Int>

    @Query("SELECT * FROM animales WHERE estado = 'Activo'")
    fun getActivos(): LiveData<List<Animal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(animal: Animal): Long

    @Update
    suspend fun update(animal: Animal)

    @Delete
    suspend fun delete(animal: Animal)

    @Query("UPDATE animales SET estado = :estado WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: String)

    // Solo revierte animales que estaban marcados como Vendido: si el usuario
    // lo puso como Fallecido a mano, borrar una venta no lo "revive".
    @Query("UPDATE animales SET estado = 'Activo' WHERE id = :id AND estado = 'Vendido'")
    suspend fun reactivarSiVendido(id: Long)

    // Solo animales activos, igual que el resto de Reportes (total y sexo).
    @Query("SELECT raza, COUNT(*) as total FROM animales WHERE estado = 'Activo' GROUP BY raza")
    fun getCountByRaza(): LiveData<List<RazaCount>>
}

data class RazaCount(val raza: String, val total: Int)
