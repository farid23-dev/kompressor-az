package az.kompressor.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.usecase.DeleteCarUseCase
import az.kompressor.app.domain.usecase.GetCarsByUserUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getCarsByUserUseCase: GetCarsByUserUseCase,
    private val deleteCarUseCase: DeleteCarUseCase
) : ViewModel() {

    private val _isSignedOut = MutableStateFlow(false)
    val isSignedOut: StateFlow<Boolean> = _isSignedOut

    private val _myListings = MutableStateFlow<Resource<List<Car>>>(Resource.Loading)
    val myListings: StateFlow<Resource<List<Car>>> = _myListings

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState

    init {
        loadMyListings()
    }

    fun getCurrentUserEmail(): String = authRepository.getCurrentUser()?.email ?: "Unknown"
    fun getCurrentUserUid(): String = authRepository.getCurrentUser()?.uid ?: ""

    fun loadMyListings() {
        val uid = getCurrentUserUid()
        if (uid.isEmpty()) return
        getCarsByUserUseCase(uid)
            .onEach { _myListings.value = it }
            .launchIn(viewModelScope)
    }

    fun deleteCar(carId: String) {
        deleteCarUseCase(carId)
            .onEach { _deleteState.value = it }
            .launchIn(viewModelScope)
    }

    fun resetDeleteState() { _deleteState.value = null }

    fun signOut() {
        authRepository.signOut()
        _isSignedOut.value = true
    }
}
