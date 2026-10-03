package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.ControlPeso

@Dao
interface ControlPesoDao {

    @Query("SELECT * FROM control_peso WHERE animalId = :animalId ORDER BY fecha ASC")
    fun getByAnimal(animalId: Long): LiveData<List<ControlPeso>>

    @Query("SELECT * FROM control_peso WHERE animalId = :animalId ORDER BY fecha DESC LIMIT 1")
    suspend fun getUltimoPeso(animalId: Long): ControlPeso?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(controlPeso: ControlPeso): Long

    @Update
    suspend fun update(controlPeso: ControlPeso)

    @Delete
    suspend fun delete(controlPeso: ControlPeso)
}
