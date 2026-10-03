package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.Vacuna
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VacunaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private val _animalId = MutableLiveData<Long>()

    val vacunas: LiveData<List<Vacuna>> = _animalId.switchMap {
        repository.getVacunasByAnimal(it)
    }
    val vacunasPendientes = repository.getVacunasPendientes(hoy)

    fun setAnimalId(id: Long) { _animalId.value = id }

    fun insert(v: Vacuna) = viewModelScope.launch { repository.insertVacuna(v) }
    fun update(v: Vacuna) = viewModelScope.launch { repository.updateVacuna(v) }
    fun delete(v: Vacuna) = viewModelScope.launch { repository.deleteVacuna(v) }
}
