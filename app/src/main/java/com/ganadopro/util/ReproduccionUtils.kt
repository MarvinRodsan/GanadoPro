package com.ganadopro.util

import com.ganadopro.data.entities.Reproduccion
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Info de un parto próximo derivada de un evento de reproducción.
 *
 * reproduccionId: id del evento (Monta/Inseminación) que originó el cálculo.
 * Se usa como entidadId al registrar en AlertaEnviada.
 */
data class AlertaPartoInfo(
    val reproduccionId: Long,
    val animalId: Long,
    val fechaEstimadaParto: Date,
    val diasRestantes: Int
)

object ReproduccionUtils {

    private const val GESTACION_DIAS = 283

    /**
     * Única fórmula de "próximos partos" de la app: la usan el Dashboard
     * (DashboardViewModel) y el Worker de alertas, para que lo que se
     * notifica sea exactamente lo que el usuario ve en pantalla.
     *
     * Por cada animal toma el servicio (Monta/Inseminación) más reciente y
     * le suma 283 días de gestación bovina. Esa gestación se descarta si
     * después del servicio se registró:
     *  - un Parto (ya parió: no hay nada que avisar), o
     *  - un Diagnóstico de Preñez con resultado negativo / vacía.
     *
     * animalesValidos: si se pasa, solo se calculan partos para esos ids
     * (hembras activas). null = no filtrar.
     *
     * diasRestantes se cuenta en días de calendario (hoy = 0, mañana = 1),
     * sin importar la hora a la que se haga el cálculo.
     */
    fun calcularProximosPartos(
        eventos: List<Reproduccion>,
        ventanaDiasMin: Int,
        ventanaDiasMax: Int,
        hoy: Date = Date(),
        animalesValidos: Set<Long>? = null
    ): List<AlertaPartoInfo> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        // Una fecha mal escrita no debe tumbar el Dashboard: se ignora ese evento.
        fun fechaDe(e: Reproduccion): Date? = runCatching { sdf.parse(e.fecha) }.getOrNull()

        val hoyInicio = inicioDelDia(hoy)

        return eventos
            .groupBy { it.animalId }
            .mapNotNull { (animalId, lista) ->
                if (animalesValidos != null && animalId !in animalesValidos) return@mapNotNull null

                val servicio = lista
                    .filter { it.tipoEvento == "Inseminación" || it.tipoEvento == "Monta" }
                    .mapNotNull { e -> fechaDe(e)?.let { e to it } }
                    .maxByOrNull { it.second.time }
                    ?: return@mapNotNull null
                val (evento, fechaServicio) = servicio

                val gestacionCerrada = lista.any { e ->
                    val fecha = fechaDe(e) ?: return@any false
                    if (fecha.before(fechaServicio)) return@any false
                    when (e.tipoEvento) {
                        "Parto" -> true
                        "Diagnóstico Preñez" -> esResultadoNegativo(e.resultado)
                        else -> false
                    }
                }
                if (gestacionCerrada) return@mapNotNull null

                val cal = Calendar.getInstance()
                cal.time = fechaServicio
                cal.add(Calendar.DAY_OF_MONTH, GESTACION_DIAS)
                val fechaEstimadaParto = cal.time

                // Se redondea porque un cambio de horario puede dejar la
                // diferencia en 23 o 25 horas; con truncar se perdería un día.
                val diasRestantes = Math.round(
                    (fechaEstimadaParto.time - hoyInicio.time).toDouble() / TimeUnit.DAYS.toMillis(1)
                ).toInt()

                if (diasRestantes in ventanaDiasMin..ventanaDiasMax) {
                    AlertaPartoInfo(
                        reproduccionId = evento.id,
                        animalId = animalId,
                        fechaEstimadaParto = fechaEstimadaParto,
                        diasRestantes = diasRestantes
                    )
                } else null
            }
    }

    private fun esResultadoNegativo(resultado: String?): Boolean {
        val r = resultado?.trim()?.lowercase(Locale.ROOT) ?: return false
        return r.startsWith("neg") || r.startsWith("vac") || r == "no" || r.startsWith("no ")
    }

    private fun inicioDelDia(fecha: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = fecha
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
