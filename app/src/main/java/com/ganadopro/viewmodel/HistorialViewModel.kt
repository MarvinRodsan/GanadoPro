package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.HistorialMedico
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val _animalId = MutableLiveData<Long>()

    val historial: LiveData<List<HistorialMedico>> = _animalId.switchMap {
        repository.getHistorialByAnimal(it)
    }

    fun setAnimalId(id: Long) { _animalId.value = id }

    fun insert(h: HistorialMedico) = viewModelScope.launch { repository.insertHistorial(h) }
    fun update(h: HistorialMedico) = viewModelScope.launch { repository.updateHistorial(h) }
    fun delete(h: HistorialMedico) = viewModelScope.launch { repository.deleteHistorial(h) }
}
