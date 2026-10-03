package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.Reproduccion
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch

class ReproduccionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)
    private val _animalId = MutableLiveData<Long>()

    val eventos: LiveData<List<Reproduccion>> = _animalId.switchMap {
        repository.getReproduccionByAnimal(it)
    }

    fun setAnimalId(id: Long) { _animalId.value = id }

    fun insert(r: Reproduccion) = viewModelScope.launch { repository.insertReproduccion(r) }
    fun update(r: Reproduccion) = viewModelScope.launch { repository.updateReproduccion(r) }
    fun delete(r: Reproduccion) = viewModelScope.launch { repository.deleteReproduccion(r) }
}
