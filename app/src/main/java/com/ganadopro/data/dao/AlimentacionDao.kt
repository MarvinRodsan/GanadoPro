package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Alimentacion

@Dao
interface AlimentacionDao {

    @Query("SELECT * FROM alimentacion WHERE animalId = :animalId ORDER BY fecha DESC")
    fun getByAnimal(animalId: Long): LiveData<List<Alimentacion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alimentacion: Alimentacion): Long

    @Update
    suspend fun update(alimentacion: Alimentacion)

    @Delete
    suspend fun delete(alimentacion: Alimentacion)
}
