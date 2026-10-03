package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.Venta
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VentaViewModel(application: Application) : AndroidViewModel(application) {

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

    // Ojo: esto es SUM(ventas) del mes, no la ganancia/utilidad real.
    // La utilidad real (ventas - gastos) se expone abajo en "utilidadMes".
    val totalVentasMes = repository.getTotalGanancias(primerDiaMes, ultimoDiaMes)
    val totalGastosMes = repository.getTotalGastosMes(primerDiaMes, ultimoDiaMes)
    val countVentasMes = repository.countVentasMes(primerDiaMes, ultimoDiaMes)
    val gastosPorCategoriaMes = repository.getGastosPorCategoriaMes(primerDiaMes, ultimoDiaMes)

    // Utilidad = Ventas - Gastos. Se combina reactivamente: se recalcula
    // cada vez que cambian las ventas o los gastos del mes.
    val utilidadMes = MediatorLiveData<Double>().apply {
        var ventasCache = 0.0
        var gastosCache = 0.0
        addSource(totalVentasMes) {
            ventasCache = it ?: 0.0
            value = ventasCache - gastosCache
        }
        addSource(totalGastosMes) {
            gastosCache = it ?: 0.0
            value = ventasCache - gastosCache
        }
    }

    fun insert(v: Venta) = viewModelScope.launch { repository.insertVenta(v) }
    fun update(v: Venta) = viewModelScope.launch { repository.updateVenta(v) }
    fun delete(v: Venta) = viewModelScope.launch { repository.deleteVenta(v) }
}
