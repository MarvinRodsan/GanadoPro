package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.Produccion
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProduccionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val _animalId = MutableLiveData<Long>()

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

    val produccionAnimal: LiveData<List<Produccion>> = _animalId.switchMap {
        repository.getProduccionByAnimal(it)
    }
    val totalMes = repository.getTotalProduccionMes(primerDiaMes, ultimoDiaMes)
    val produccionDiaria = repository.getProduccionDiaria(primerDiaMes, ultimoDiaMes)

    fun setAnimalId(id: Long) { _animalId.value = id }

    fun insert(p: Produccion) = viewModelScope.launch { repository.insertProduccion(p) }
    fun update(p: Produccion) = viewModelScope.launch { repository.updateProduccion(p) }
    fun delete(p: Produccion) = viewModelScope.launch { repository.deleteProduccion(p) }
}
