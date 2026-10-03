package com.ganadopro.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registra que ya se envió una notificación para un evento puntual
 * (una vacuna próxima o un parto próximo), para no repetir el mismo
 * aviso todos los días mientras el evento siga dentro de la ventana
 * de aviso (ej. los 3 días previos).
 *
 * tipo: "vacuna" o "parto"
 * entidadId: id de la fila en `vacunas` o en `reproduccion` que originó la alerta
 * fechaAlerta: la fecha del evento que se está avisando (proximaDosis de la
 *   vacuna, o fecha estimada de parto). Es la clave de que una reprogramación
 *   SÍ dispare una alerta nueva: el índice único es (tipo, entidadId,
 *   fechaAlerta), no (tipo, entidadId) a secas. Con (tipo, entidadId) solo,
 *   una vacuna reprogramada a una fecha nueva quedaría bloqueada para
 *   siempre por la alerta de la fecha vieja.
 */
@Entity(
    tableName = "alertas_enviadas",
    indices = [Index(value = ["tipo", "entidadId", "fechaAlerta"], unique = true)]
)
data class AlertaEnviada(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: String,
    val entidadId: Long,
    val fechaAlerta: String,
    val fechaEnviada: String
)
