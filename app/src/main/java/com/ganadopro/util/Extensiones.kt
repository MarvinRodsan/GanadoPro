package com.ganadopro.util

import android.app.DatePickerDialog
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Formato único de fechas de la app (así se guardan en la base de datos). */
const val FORMATO_FECHA = "yyyy-MM-dd"

/** Fecha de hoy en "yyyy-MM-dd". Locale.US: siempre dígitos 0-9, sin importar el idioma del teléfono. */
fun fechaHoy(): String = SimpleDateFormat(FORMATO_FECHA, Locale.US).format(Date())

/**
 * Convierte lo que escribió el usuario en un número, aceptando coma o punto
 * como separador decimal ("12,5" y "12.5" valen lo mismo). Devuelve null si
 * no es un número.
 */
fun String.aDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

/**
 * Quita el ".0" de los números enteros para mostrarlos y para precargar
 * formularios: 450.0 -> "450", 12.5 -> "12.5".
 */
fun Double.sinCerosDeMas(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

/** Diálogo estándar de "¿seguro que quieres eliminar?" para todos los listados. */
fun Fragment.confirmarEliminacion(mensaje: String, onConfirmar: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle("Eliminar registro")
        .setMessage(mensaje)
        .setPositiveButton("Eliminar") { _, _ -> onConfirmar() }
        .setNegativeButton("Cancelar", null)
        .show()
}

/**
 * Abre un calendario y escribe la fecha elegida en [campo] como "yyyy-MM-dd".
 * El calendario arranca en la fecha que ya tiene el campo (si es válida) y no
 * en hoy, para que corregir una fecha vieja no obligue a navegar meses.
 *
 * permitirFuturo = false bloquea los días posteriores a hoy (fechas de
 * nacimiento, pesajes, ventas ya hechas...).
 */
fun Fragment.mostrarSelectorFecha(campo: EditText, permitirFuturo: Boolean = true) {
    val cal = Calendar.getInstance()
    runCatching {
        SimpleDateFormat(FORMATO_FECHA, Locale.US).parse(campo.text.toString().trim())
    }.getOrNull()?.let { cal.time = it }

    val dialogo = DatePickerDialog(
        requireContext(),
        { _, y, m, d ->
            campo.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
            campo.error = null
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )
    if (!permitirFuturo) dialogo.datePicker.maxDate = System.currentTimeMillis()
    dialogo.show()
}
