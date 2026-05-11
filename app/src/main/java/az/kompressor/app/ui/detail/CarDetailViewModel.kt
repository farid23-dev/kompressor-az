package az.kompressor.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.usecase.GetCarByIdUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CarDetailViewModel @Inject constructor(
    private val getCarByIdUseCase: GetCarByIdUseCase
) : ViewModel() {

    private val _carState = MutableStateFlow<Resource<Car>>(Resource.Loading)
    val carState: StateFlow<Resource<Car>> = _carState

    fun loadCar(carId: String) {
        getCarByIdUseCase(carId)
            .onEach { _carState.value = it }
            .launchIn(viewModelScope)
    }
}
