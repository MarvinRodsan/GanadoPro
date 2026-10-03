package com.ganadopro.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos")
data class Gasto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val concepto: String,
    val monto: Double,
    val fecha: String,
    val notas: String? = null
)