package com.ganadopro.repository

import android.content.Context
import androidx.room.withTransaction
import com.ganadopro.data.database.GanaderaDatabase
import com.ganadopro.data.entities.*

class GanaderaRepository(context: Context) {

    private val db = GanaderaDatabase.getDatabase(context)
    private val animalDao = db.animalDao()
    private val historialDao = db.historialMedicoDao()
    private val vacunaDao = db.vacunaDao()
    private val reproduccionDao = db.reproduccionDao()
    private val alimentacionDao = db.alimentacionDao()
    private val pesoDao = db.controlPesoDao()
    private val produccionDao = db.produccionDao()
    private val ventaDao = db.ventaDao()
    private val alertaEnviadaDao = db.alertaEnviadaDao()

    // ─── Animales ───────────────────────────────────────────────────────────
    fun getAllAnimales() = animalDao.getAll()
    fun searchAnimales(q: String) = animalDao.search(q)
    fun countAnimalesActivos() = animalDao.countActivos()
    fun getAnimalesActivos() = animalDao.getActivos()
    fun getCountByRaza() = animalDao.getCountByRaza()
    suspend fun getAnimalById(id: Long) = animalDao.getById(id)
    suspend fun insertAnimal(a: Animal) = animalDao.insert(a)
    suspend fun updateAnimal(a: Animal) = animalDao.update(a)
    suspend fun deleteAnimal(a: Animal) = animalDao.delete(a)

    // ─── Gastos ─────────────────────────────────────────────────────────────
    private val gastoDao = db.gastoDao()
    fun getAllGastos() = gastoDao.getAll()
    fun getTotalGastosMes(desde: String, hasta: String) = gastoDao.getTotalGastosMes(desde, hasta)
    suspend fun insertGasto(g: Gasto) = gastoDao.insert(g)
    suspend fun updateGasto(g: Gasto) = gastoDao.update(g)
    suspend fun deleteGasto(g: Gasto) = gastoDao.delete(g)

    // ─── Historial Médico ───────────────────────────────────────────────────
    fun getHistorialByAnimal(id: Long) = historialDao.getByAnimal(id)
    suspend fun insertHistorial(h: HistorialMedico) = historialDao.insert(h)
    suspend fun updateHistorial(h: HistorialMedico) = historialDao.update(h)
    suspend fun deleteHistorial(h: HistorialMedico) = historialDao.delete(h)

    // ─── Vacunas ────────────────────────────────────────────────────────────
    fun getVacunasByAnimal(id: Long) = vacunaDao.getByAnimal(id)
    fun getVacunasPendientes(hoy: String) = vacunaDao.getPendientes(hoy)
    fun countVacunasPendientes(hoy: String) = vacunaDao.countPendientes(hoy)
    suspend fun getVacunasProximasEnVentana(desde: String, hasta: String) = vacunaDao.getProximasEnVentana(desde, hasta)
    suspend fun insertVacuna(v: Vacuna) = vacunaDao.insert(v)
    suspend fun updateVacuna(v: Vacuna) = vacunaDao.update(v)
    suspend fun deleteVacuna(v: Vacuna) = vacunaDao.delete(v)

    // ─── Reproducción ───────────────────────────────────────────────────────
    fun getReproduccionByAnimal(id: Long) = reproduccionDao.getByAnimal(id)
    fun getEventosGestacion() = reproduccionDao.getEventosGestacion()
    suspend fun getEventosGestacionSync() = reproduccionDao.getEventosGestacionSync()
    suspend fun insertReproduccion(r: Reproduccion) = reproduccionDao.insert(r)
    suspend fun updateReproduccion(r: Reproduccion) = reproduccionDao.update(r)
    suspend fun deleteReproduccion(r: Reproduccion) = reproduccionDao.delete(r)

    // ─── Alimentación ───────────────────────────────────────────────────────
    fun getAlimentacionByAnimal(id: Long) = alimentacionDao.getByAnimal(id)
    suspend fun insertAlimentacion(a: Alimentacion) = alimentacionDao.insert(a)
    suspend fun updateAlimentacion(a: Alimentacion) = alimentacionDao.update(a)
    suspend fun deleteAlimentacion(a: Alimentacion) = alimentacionDao.delete(a)

    // ─── Control de Peso ────────────────────────────────────────────────────
    fun getPesosByAnimal(id: Long) = pesoDao.getByAnimal(id)
    suspend fun getUltimoPeso(id: Long) = pesoDao.getUltimoPeso(id)
    suspend fun insertPeso(p: ControlPeso) = pesoDao.insert(p)
    suspend fun updatePeso(p: ControlPeso) = pesoDao.update(p)
    suspend fun deletePeso(p: ControlPeso) = pesoDao.delete(p)

    // ─── Producción Lechera ─────────────────────────────────────────────────
    fun getProduccionByAnimal(id: Long) = produccionDao.getByAnimal(id)
    fun getTotalProduccionMes(desde: String, hasta: String) = produccionDao.getTotalMes(desde, hasta)
    fun getProduccionDiaria(desde: String, hasta: String) = produccionDao.getProduccionDiaria(desde, hasta)
    suspend fun insertProduccion(p: Produccion) = produccionDao.insert(p)
    suspend fun updateProduccion(p: Produccion) = produccionDao.update(p)
    suspend fun deleteProduccion(p: Produccion) = produccionDao.delete(p)

    // ─── Ventas ─────────────────────────────────────────────────────────────
    fun getAllVentas() = ventaDao.getAll()
    fun getVentasByAnimal(id: Long) = ventaDao.getByAnimal(id)
    fun getTotalGanancias(desde: String, hasta: String) = ventaDao.getTotalGanancias(desde, hasta)
    fun countVentasMes(desde: String, hasta: String) = ventaDao.countVentasMes(desde, hasta)
    suspend fun updateVenta(v: Venta) = ventaDao.update(v)

    // Registrar una venta y marcar el animal como "Vendido" es una sola
    // operación: o se hacen las dos cosas o ninguna (transacción). Así el
    // animal sale de los conteos de activos, de las alertas y de la lista
    // de animales disponibles para vender, sin que el usuario tenga que
    // ir a editarlo a mano.
    suspend fun insertVenta(v: Venta): Long = db.withTransaction {
        val id = ventaDao.insert(v)
        animalDao.updateEstado(v.animalId, "Vendido")
        id
    }

    // Borrar una venta (registrada por error) devuelve el animal a "Activo",
    // siempre que no le quede otra venta registrada.
    suspend fun deleteVenta(v: Venta) = db.withTransaction {
        ventaDao.delete(v)
        if (ventaDao.countByAnimal(v.animalId) == 0) {
            animalDao.reactivarSiVendido(v.animalId)
        }
    }

    // ─── Alertas enviadas (control de spam de notificaciones) ──────────────
    suspend fun yaFueEnviada(tipo: String, entidadId: Long, fechaAlerta: String) =
        alertaEnviadaDao.yaFueEnviada(tipo, entidadId, fechaAlerta)
    suspend fun registrarAlertaEnviada(alerta: AlertaEnviada) = alertaEnviadaDao.registrar(alerta)

    // ─── Gastos por categoría (Reportes) ────────────────────────────────────
    fun getGastosPorCategoriaMes(desde: String, hasta: String) = gastoDao.getGastosPorCategoria(desde, hasta)

    // ─── Animal por código (validación de duplicados) ───────────────────────
    suspend fun getAnimalByCodigo(codigo: String) = animalDao.getByCodigo(codigo)

}
