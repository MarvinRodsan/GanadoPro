package com.ganadopro.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ganadopro.R
import com.ganadopro.ui.MainActivity

/**
 * Notificaciones locales para vacunas y partos próximos.
 *
 * El tap en la notificación navega directo a la ficha del animal
 * correspondiente (ver MainActivity.EXTRA_ANIMAL_ID y manejarIntentDeepLink),
 * respetando la sesión: si no hay sesión activa, MainActivity redirige a
 * Login sin perder el destino.
 */
object NotificationHelper {

    const val CANAL_SALUD = "canal_alertas_salud"
    const val CANAL_REPRODUCCION = "canal_alertas_reproduccion"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CANAL_SALUD,
                "Salud y vacunas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos de vacunas próximas a vencer" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CANAL_REPRODUCCION,
                "Reproducción",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos de partos próximos" }
        )
    }

    fun notificarVacuna(
        context: Context,
        vacunaId: Long,
        animalId: Long,
        nombreAnimal: String,
        nombreVacuna: String,
        fechaProximaDosis: String
    ) {
        val notification = NotificationCompat.Builder(context, CANAL_SALUD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Vacunación próxima")
            .setContentText("$nombreAnimal: $nombreVacuna programada para el $fechaProximaDosis")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(
                pendingIntentAnimal(context, requestCode = ("vacuna_$vacunaId").hashCode(), animalId = animalId)
            )
            .build()

        mostrar(context, id = ("vacuna_$vacunaId").hashCode(), notification = notification)
    }

    fun notificarParto(
        context: Context,
        reproduccionId: Long,
        animalId: Long,
        nombreAnimal: String,
        diasRestantes: Int
    ) {
        val mensaje = when {
            diasRestantes <= 0 -> "$nombreAnimal: la fecha estimada de parto es hoy"
            diasRestantes == 1 -> "$nombreAnimal: parto estimado en 1 día"
            else -> "$nombreAnimal: parto estimado en $diasRestantes días"
        }

        val notification = NotificationCompat.Builder(context, CANAL_REPRODUCCION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Parto próximo")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(
                pendingIntentAnimal(context, requestCode = ("parto_$reproduccionId").hashCode(), animalId = animalId)
            )
            .build()

        mostrar(context, id = ("parto_$reproduccionId").hashCode(), notification = notification)
    }

    private fun pendingIntentAnimal(context: Context, requestCode: Int, animalId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ANIMAL_ID, animalId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun mostrar(context: Context, id: Int, notification: Notification) {
        val tienePermiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        // Si el usuario negó el permiso, no se rompe nada: simplemente no se
        // muestra la notificación push. El Dashboard sigue siendo el
        // respaldo pasivo que ya existía antes de este cambio.
        if (tienePermiso) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
