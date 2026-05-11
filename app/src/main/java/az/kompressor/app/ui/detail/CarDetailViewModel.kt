package az.kompressor.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.usecase.GetCarByIdUseCase
import az.kompressor.app.domain.usecase.ToggleFavoriteUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarDetailViewModel @Inject constructor(
    private val getCarByIdUseCase: GetCarByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _carState = MutableStateFlow<Resource<Car>>(Resource.Loading)
    val carState: StateFlow<Resource<Car>> = _carState

    // Reactively tracks favorite state for the currently loaded car
    val isFavorite: StateFlow<Boolean> = _carState
        .flatMapLatest { state ->
            if (state is Resource.Success) {
                toggleFavoriteUseCase.isFavorite(state.data.id)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadCar(carId: String) {
        getCarByIdUseCase(carId)
            .onEach { _carState.value = it }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite() {
        val state = _carState.value
        if (state !is Resource.Success) return
        viewModelScope.launch {
            if (isFavorite.value) {
                toggleFavoriteUseCase.remove(state.data.id)
            } else {
                toggleFavoriteUseCase.add(state.data)
            }
        }
    }
}
