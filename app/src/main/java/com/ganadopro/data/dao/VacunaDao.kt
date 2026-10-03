package com.ganadopro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ganadopro.data.entities.Vacuna

@Dao
interface VacunaDao {

    @Query("SELECT * FROM vacunas WHERE animalId = :animalId ORDER BY fechaAplicacion DESC")
    fun getByAnimal(animalId: Long): LiveData<List<Vacuna>>

    // Vacunas pendientes: la próxima dosis es hoy o antes. Dos condiciones
    // para que la lista (y el contador del Dashboard) reflejen la realidad:
    //  - el animal sigue Activo (no tiene sentido avisar de un animal vendido
    //    o fallecido);
    //  - la dosis no fue aplicada ya: si existe un registro posterior de la
    //    misma vacuna para el mismo animal, este refuerzo queda cumplido y
    //    deja de contarse como pendiente sin tener que editar el registro viejo.
    @Query("""
        SELECT v.* FROM vacunas v
        INNER JOIN animales a ON a.id = v.animalId
        WHERE a.estado = 'Activo'
          AND v.proximaDosis IS NOT NULL AND v.proximaDosis != ''
          AND v.proximaDosis <= :hoy
          AND NOT EXISTS (
              SELECT 1 FROM vacunas v2
              WHERE v2.animalId = v.animalId
                AND v2.id != v.id
                AND v2.nombre = v.nombre COLLATE NOCASE
                AND v2.fechaAplicacion > v.fechaAplicacion
          )
        ORDER BY v.proximaDosis ASC
    """)
    fun getPendientes(hoy: String): LiveData<List<Vacuna>>

    @Query("""
        SELECT COUNT(*) FROM vacunas v
        INNER JOIN animales a ON a.id = v.animalId
        WHERE a.estado = 'Activo'
          AND v.proximaDosis IS NOT NULL AND v.proximaDosis != ''
          AND v.proximaDosis <= :hoy
          AND NOT EXISTS (
              SELECT 1 FROM vacunas v2
              WHERE v2.animalId = v.animalId
                AND v2.id != v.id
                AND v2.nombre = v.nombre COLLATE NOCASE
                AND v2.fechaAplicacion > v.fechaAplicacion
          )
    """)
    fun countPendientes(hoy: String): LiveData<Int>

    // Versión "de una sola vez" (no LiveData) para el Worker de alertas:
    // vacunas cuya próxima dosis cae dentro de la ventana de aviso (ej. hoy..hoy+3).
    // Mismos filtros que getPendientes (animal Activo, dosis aún no aplicada).
    @Query("""
        SELECT v.* FROM vacunas v
        INNER JOIN animales a ON a.id = v.animalId
        WHERE a.estado = 'Activo'
          AND v.proximaDosis IS NOT NULL
          AND v.proximaDosis BETWEEN :desde AND :hasta
          AND NOT EXISTS (
              SELECT 1 FROM vacunas v2
              WHERE v2.animalId = v.animalId
                AND v2.id != v.id
                AND v2.nombre = v.nombre COLLATE NOCASE
                AND v2.fechaAplicacion > v.fechaAplicacion
          )
        ORDER BY v.proximaDosis ASC
    """)
    suspend fun getProximasEnVentana(desde: String, hasta: String): List<Vacuna>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vacuna: Vacuna): Long

    @Update
    suspend fun update(vacuna: Vacuna)

    @Delete
    suspend fun delete(vacuna: Vacuna)
}
