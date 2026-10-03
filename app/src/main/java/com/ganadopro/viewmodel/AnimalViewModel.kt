package com.ganadopro.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.ganadopro.data.entities.Animal
import com.ganadopro.repository.GanaderaRepository
import kotlinx.coroutines.launch

class AnimalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GanaderaRepository(application)

    private val _searchQuery = MutableLiveData<String>("")
    val animales: LiveData<List<Animal>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) repository.getAllAnimales()
        else repository.searchAnimales(query)
    }

    val totalActivos: LiveData<Int> = repository.countAnimalesActivos()
    val animalesActivos: LiveData<List<Animal>> = repository.getAnimalesActivos()
    val countByRaza = repository.getCountByRaza()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insert(animal: Animal) = viewModelScope.launch {
        repository.insertAnimal(animal)
    }

    fun update(animal: Animal) = viewModelScope.launch {
        repository.updateAnimal(animal)
    }

    fun delete(animal: Animal) = viewModelScope.launch {
        repository.deleteAnimal(animal)
    }

    suspend fun getById(id: Long) = repository.getAnimalById(id)
    suspend fun getByCodigo(codigo: String) = repository.getAnimalByCodigo(codigo)
    suspend fun getUltimoPeso(animalId: Long) = repository.getUltimoPeso(animalId)
}
