package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.Alimentacion
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch

class AlimentacionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val _animalId = MutableLiveData<Long>()

    val alimentacion: LiveData<List<Alimentacion>> = _animalId.switchMap {
        repository.getAlimentacionByAnimal(it)
    }

    fun setAnimalId(id: Long) { _animalId.value = id }

    fun insert(a: Alimentacion) = viewModelScope.launch { repository.insertAlimentacion(a) }
    fun update(a: Alimentacion) = viewModelScope.launch { repository.updateAlimentacion(a) }
    fun delete(a: Alimentacion) = viewModelScope.launch { repository.deleteAlimentacion(a) }
}
