package com.ganadopro.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ganadopro.data.entities.AlertaEnviada
import com.ganadopro.repository.GanaderaRepository
import com.ganadopro.util.NotificationHelper
import com.ganadopro.util.ReproduccionUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Corre ~1 vez al día (ver programación en MainActivity). Revisa:
 *  - Vacunas cuya próxima dosis cae dentro de los próximos VENTANA_DIAS días.
 *  - Partos estimados (Monta/Inseminación + 283 días) dentro de esa misma ventana.
 *
 * Cada alerta se registra en AlertaEnviada para no repetirla todos los días
 * mientras siga dentro de la ventana.
 */
class AlertaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "alerta_diaria_ganadopro"
        private const val VENTANA_DIAS = 3
    }

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override suspend fun doWork(): Result {
        return try {
            val repository = GanaderaRepository(applicationContext)

            val hoy = sdf.format(Date())
            val hasta = sdf.format(
                Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, VENTANA_DIAS) }.time
            )

            revisarVacunas(repository, hoy, hasta)
            revisarPartos(repository, hoy)

            Result.success()
        } catch (e: Exception) {
            // No dejamos que una excepción tumbe el trabajo silenciosamente:
            // WorkManager reintentará según su política de backoff por defecto.
            Result.retry()
        }
    }

    private suspend fun revisarVacunas(repository: GanaderaRepository, hoy: String, hasta: String) {
        val proximas = repository.getVacunasProximasEnVentana(hoy, hasta)

        for (vacuna in proximas) {
            // La fecha que identifica la alerta es la próxima dosis, no "hoy":
            // así, si la vacuna se reprograma a otra fecha, la combinación
            // (tipo, entidadId, fechaAlerta) es distinta y se permite un
            // aviso nuevo en vez de quedar bloqueada por la alerta anterior.
            val fechaAlerta = vacuna.proximaDosis ?: hoy
            if (repository.yaFueEnviada("vacuna", vacuna.id, fechaAlerta)) continue

            val animal = repository.getAnimalById(vacuna.animalId)
            val nombreAnimal = animal?.nombre ?: "Animal #${vacuna.animalId}"

            NotificationHelper.notificarVacuna(
                context = applicationContext,
                vacunaId = vacuna.id,
                animalId = vacuna.animalId,
                nombreAnimal = nombreAnimal,
                nombreVacuna = vacuna.nombre,
                fechaProximaDosis = fechaAlerta
            )

            repository.registrarAlertaEnviada(
                AlertaEnviada(tipo = "vacuna", entidadId = vacuna.id, fechaAlerta = fechaAlerta, fechaEnviada = hoy)
            )
        }
    }

    private suspend fun revisarPartos(repository: GanaderaRepository, hoy: String) {
        val eventos = repository.getEventosGestacionSync()
        val proximos = ReproduccionUtils.calcularProximosPartos(
            eventos = eventos,
            ventanaDiasMin = 0,
            ventanaDiasMax = VENTANA_DIAS
        )

        for (info in proximos) {
            // Igual que con vacunas: la fechaAlerta es la fecha estimada de
            // parto. Si el evento de reproducción cambia (se corrige la fecha
            // de monta/inseminación) y la estimación se mueve, es una
            // combinación distinta y se permite un aviso nuevo.
            val fechaAlerta = sdf.format(info.fechaEstimadaParto)
            if (repository.yaFueEnviada("parto", info.reproduccionId, fechaAlerta)) continue

            // Mismo filtro que el Dashboard: solo hembras activas. No se
            // avisa del parto de un animal vendido, fallecido o eliminado.
            val animal = repository.getAnimalById(info.animalId) ?: continue
            if (animal.estado != "Activo" || animal.sexo != "Hembra") continue
            val nombreAnimal = animal.nombre

            NotificationHelper.notificarParto(
                context = applicationContext,
                reproduccionId = info.reproduccionId,
                animalId = info.animalId,
                nombreAnimal = nombreAnimal,
                diasRestantes = info.diasRestantes
            )

            repository.registrarAlertaEnviada(
                AlertaEnviada(tipo = "parto", entidadId = info.reproduccionId, fechaAlerta = fechaAlerta, fechaEnviada = hoy)
            )
        }
    }
}
