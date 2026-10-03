package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganadopro.data.entities.Gasto
import com.ganadopro.data.entities.Venta
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VentaGastoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

    val ventas = repository.getAllVentas()
    val gastos = repository.getAllGastos()
    val totalGanancias = repository.getTotalGanancias(primerDiaMes, ultimoDiaMes)
    val totalGastosMes = repository.getTotalGastosMes(primerDiaMes, ultimoDiaMes)
    val animalesActivos = repository.getAnimalesActivos()
    // Todos (también los ya vendidos): para mostrar en cada venta qué animal fue.
    val animales = repository.getAllAnimales()

    fun insertVenta(v: Venta) = viewModelScope.launch { repository.insertVenta(v) }
    fun updateVenta(v: Venta) = viewModelScope.launch { repository.updateVenta(v) }
    fun deleteVenta(v: Venta) = viewModelScope.launch { repository.deleteVenta(v) }

    fun insertGasto(g: Gasto) = viewModelScope.launch { repository.insertGasto(g) }
    fun updateGasto(g: Gasto) = viewModelScope.launch { repository.updateGasto(g) }
    fun deleteGasto(g: Gasto) = viewModelScope.launch { repository.deleteGasto(g) }
}