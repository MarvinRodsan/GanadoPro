package com.ganadopro.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reproduccion",
    foreignKeys = [ForeignKey(
        entity = Animal::class,
        parentColumns = ["id"],
        childColumns = ["animalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Reproduccion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: Long,
    // "Celo", "Inseminación", "Monta", "Diagnóstico Preñez", "Parto"
    val tipoEvento: String,
    val fecha: String,
    val observaciones: String? = null,
    val resultado: String? = null
)
