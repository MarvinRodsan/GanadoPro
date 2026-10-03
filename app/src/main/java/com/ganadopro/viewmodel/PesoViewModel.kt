    package com.ganadopro.viewmodel

    import android.app.Application
    import androidx.lifecycle.*
    import com.ganadopro.data.entities.ControlPeso
    import com.ganadopro.repository.GanaderaRepository
    import kotlinx.coroutines.launch

    class PesoViewModel(application: Application) : AndroidViewModel(application) {

        private val repository = GanaderaRepository(application)
        private val _animalId = MutableLiveData<Long>()

        val pesos: LiveData<List<ControlPeso>> = _animalId.switchMap {
            repository.getPesosByAnimal(it)
        }

        fun setAnimalId(id: Long) { _animalId.value = id }

        fun insert(p: ControlPeso) = viewModelScope.launch { repository.insertPeso(p) }
        fun update(p: ControlPeso) = viewModelScope.launch { repository.updatePeso(p) }
        fun delete(p: ControlPeso) = viewModelScope.launch { repository.deletePeso(p) }
    }
