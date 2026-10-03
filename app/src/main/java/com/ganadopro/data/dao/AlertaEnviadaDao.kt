package com.ganadopro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadopro.data.entities.AlertaEnviada

@Dao
interface AlertaEnviadaDao {

    @Query("SELECT EXISTS(SELECT 1 FROM alertas_enviadas WHERE tipo = :tipo AND entidadId = :entidadId AND fechaAlerta = :fechaAlerta)")
    suspend fun yaFueEnviada(tipo: String, entidadId: Long, fechaAlerta: String): Boolean

    // OnConflictStrategy.IGNORE: si por alguna carrera de tareas se intenta
    // registrar dos veces la misma alerta, el índice único
    // (tipo, entidadId, fechaAlerta) hace que la segunda inserción
    // simplemente no haga nada, en vez de fallar. Si la fecha cambió
    // (vacuna reprogramada / nueva estimación de parto), esta combinación
    // es distinta y SÍ se permite una alerta nueva.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registrar(alerta: AlertaEnviada)
}
