package com.ganadopro.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "alimentacion",
    foreignKeys = [ForeignKey(
        entity = Animal::class,
        parentColumns = ["id"],
        childColumns = ["animalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Alimentacion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: Long,
    // "Pasto", "Concentrado", "Ensilaje", "Suplementos"
    val tipoAlimento: String,
    val cantidad: Double,
    val unidad: String = "kg",
    val fecha: String,
    val observaciones: String? = null
)
