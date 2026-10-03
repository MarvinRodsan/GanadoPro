package com.ganadopro.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animales")
data class Animal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val codigo: String,
    val nombre: String,
    val raza: String,
    val sexo: String,           // "Macho" / "Hembra"
    val fechaNacimiento: String, // formato "yyyy-MM-dd"
    val pesoInicial: Double,
    val estado: String,         // "Activo", "Vendido", "Fallecido"
    val foto: String? = null    // ruta local de la imagen
)
