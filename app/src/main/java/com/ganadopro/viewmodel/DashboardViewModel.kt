package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import com.ganadopro.data.entities.Animal
import com.ganadopro.data.entities.Reproduccion
import com.ganadopro.repository.GanaderaRepository
import com.ganadopro.util.ReproduccionUtils
import java.text.SimpleDateFormat
import java.util.*

data class AlertaParto(
    val animalId: Long,
    val nombreAnimal: String,
    val fechaProbableParto: Date,
    val diasRestantes: Int
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val hoy: String get() = sdf.format(Date())
    private val primerDiaMes: String get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return sdf.format(cal.time)
    }
    private val ultimoDiaMes: String get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return sdf.format(cal.time)
    }

    val totalAnimales = repository.countAnimalesActivos()
    val vacunasPendientes = repository.countVacunasPendientes(hoy)
    val produccionMes = repository.getTotalProduccionMes(primerDiaMes, ultimoDiaMes)

    private val eventosGestacion = repository.getEventosGestacion()
    private val animales = repository.getAllAnimales()

    // Calcula partos próximos combinando eventos + nombres de animales.
    // La fórmula de gestación (283 días) vive en un único lugar,
    // ReproduccionUtils.calcularProximosPartos, que es también lo que usa
    // AlertaWorker para las notificaciones. Así el Dashboard y las alertas
    // nunca pueden desincronizarse por tener dos fórmulas separadas.
    val proximosPartos = MediatorLiveData<List<AlertaParto>>().apply {
        var eventosCache: List<Reproduccion>? = null
        var animalesCache: List<Animal>? = null

        fun calcular() {
            val eventos = eventosCache ?: return
            val listaAnimales = animalesCache ?: return

            // Solo hembras activas: un animal vendido o fallecido no debe
            // seguir apareciendo como "próximo parto".
            val hembrasActivas = listaAnimales
                .filter { it.estado == "Activo" && it.sexo == "Hembra" }
                .map { it.id }
                .toSet()

            // Solo mostrar partos dentro de los próximos 30 días (y no ya pasados hace mucho)
            val infos = ReproduccionUtils.calcularProximosPartos(
                eventos = eventos,
                ventanaDiasMin = -7,
                ventanaDiasMax = 30,
                animalesValidos = hembrasActivas
            )

            val alertas = infos.map { info ->
                val nombreAnimal = listaAnimales.find { it.id == info.animalId }?.nombre
                    ?: "Animal #${info.animalId}"
                AlertaParto(info.animalId, nombreAnimal, info.fechaEstimadaParto, info.diasRestantes)
            }.sortedBy { it.diasRestantes }

            value = alertas
        }

        addSource(eventosGestacion) {
            eventosCache = it
            calcular()
        }
        addSource(animales) {
            animalesCache = it
            calcular()
        }
    }
}