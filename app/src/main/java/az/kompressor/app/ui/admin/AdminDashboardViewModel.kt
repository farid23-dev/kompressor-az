package az.kompressor.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val carRepository: CarRepository
) : ViewModel() {

    private val _carsState = MutableStateFlow<Resource<List<Car>>>(Resource.Loading)
    val carsState: StateFlow<Resource<List<Car>>> = _carsState

    private val _actionState = MutableStateFlow<String?>(null)
    val actionState: StateFlow<String?> = _actionState

    init { loadAll() }

    fun loadAll() {
        carRepository.getAllCarsAdmin()
            .onEach { _carsState.value = it }
            .launchIn(viewModelScope)
    }

    fun approve(car: Car) = updateStatus(car, "approved")
    fun reject(car: Car)  = updateStatus(car, "rejected")

    private fun updateStatus(car: Car, status: String) {
        viewModelScope.launch {
            carRepository.updateCarStatus(car.id, status, car.sellerUid, car.title)
            _actionState.value = if (status == "approved") "SUCCESS_APPROVED:${car.title}"
                                 else "SUCCESS_REJECTED:${car.title}"
            loadAll()
        }
    }

    fun clearAction() { _actionState.value = null }
}
