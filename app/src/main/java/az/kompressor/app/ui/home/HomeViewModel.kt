package az.kompressor.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.domain.usecase.GetCarsUseCase
import az.kompressor.app.domain.usecase.SearchCarsUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCarsUseCase: GetCarsUseCase,
    private val searchCarsUseCase: SearchCarsUseCase,
    private val carRepository: CarRepository
) : ViewModel() {

    private val _carsState = MutableStateFlow<Resource<List<Car>>>(Resource.Loading)
    val carsState: StateFlow<Resource<List<Car>>> = _carsState

    init {
        seedAndLoad()
    }

    private fun seedAndLoad() {
        viewModelScope.launch {
            carRepository.seedDummyData()
            loadCars()
        }
    }

    fun loadCars() {
        getCarsUseCase().onEach { _carsState.value = it }.launchIn(viewModelScope)
    }

    fun search(query: String) {
        searchCarsUseCase(query).onEach { _carsState.value = it }.launchIn(viewModelScope)
    }
}
