package az.kompressor.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.domain.usecase.GetCarByIdUseCase
import az.kompressor.app.domain.usecase.ToggleFavoriteUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _carState = MutableStateFlow<Resource<Car>>(Resource.Loading)
    val carState: StateFlow<Resource<Car>> = _carState

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private var viewCountIncremented = false

    init {
        checkAdminStatus()
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            _isAdmin.value = carRepository.isAdmin(uid)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> = _carState
        .flatMapLatest { state ->
            if (state is Resource.Success) {
                toggleFavoriteUseCase.isFavorite(state.data.id)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showApproveButton: StateFlow<Boolean> = combine(_isAdmin, _carState) { isAdmin, state ->
        isAdmin && state is Resource.Success && state.data.status == "pending"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadCar(carId: String) {
        getCarByIdUseCase(carId)
            .onEach { resource ->
                _carState.value = resource
                if (resource is Resource.Success && !viewCountIncremented) {
                    viewCountIncremented = true
                    viewModelScope.launch {
                        carRepository.incrementViewCount(carId)
                    }
                }
            }
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

    fun approveCar() {
        val state = _carState.value
        if (state !is Resource.Success) return
        viewModelScope.launch {
            carRepository.updateCarStatus(
                carId = state.data.id,
                status = "approved",
                sellerUid = state.data.sellerUid,
                carTitle = state.data.title
            )
            loadCar(state.data.id)
        }
    }
}
