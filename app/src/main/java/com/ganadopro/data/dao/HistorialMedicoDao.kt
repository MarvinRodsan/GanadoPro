package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.HistorialMedico

@Dao
interface HistorialMedicoDao {

    @Query("SELECT * FROM historial_medico WHERE animalId = :animalId ORDER BY fecha DESC")
    fun getByAnimal(animalId: Long): LiveData<List<HistorialMedico>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(historial: HistorialMedico): Long

    @Update
    suspend fun update(historial: HistorialMedico)

    @Delete
    suspend fun delete(historial: HistorialMedico)
}
