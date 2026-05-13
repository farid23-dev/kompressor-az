package az.kompressor.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.domain.usecase.DeleteCarUseCase
import az.kompressor.app.domain.usecase.GetCarsByUserUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getCarsByUserUseCase: GetCarsByUserUseCase,
    private val deleteCarUseCase: DeleteCarUseCase,
    private val carRepository: CarRepository
) : ViewModel() {

    private val _isSignedOut = MutableStateFlow(false)
    val isSignedOut: StateFlow<Boolean> = _isSignedOut

    private val _myListings = MutableStateFlow<Resource<List<Car>>>(Resource.Loading)
    val myListings: StateFlow<Resource<List<Car>>> = _myListings

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState

    private val _userProfile = MutableStateFlow<Resource<User>>(Resource.Loading)
    val userProfile: StateFlow<Resource<User>> = _userProfile

    init {
        loadMyListings()
        loadUserProfile()
    }

    fun getCurrentUserEmail(): String = authRepository.getCurrentUser()?.email ?: ""
    fun getCurrentUserUid(): String = authRepository.getCurrentUser()?.uid ?: ""

    fun loadUserProfile() {
        val uid = getCurrentUserUid()
        if (uid.isEmpty()) return
        authRepository.getUserProfile(uid)
            .onEach { _userProfile.value = it }
            .launchIn(viewModelScope)
    }

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

    private val _updateProfileState = MutableStateFlow<Resource<Unit>?>(null)
    val updateProfileState: StateFlow<Resource<Unit>?> = _updateProfileState

    fun updateProfile(name: String, surname: String, phone: String) {
        val uid = getCurrentUserUid()
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _updateProfileState.value = Resource.Loading
            try {
                authRepository.saveUserProfile(uid, name, surname, phone, getCurrentUserEmail())
                _updateProfileState.value = Resource.Success(Unit)
                loadUserProfile() // refresh displayed name
            } catch (e: Exception) {
                _updateProfileState.value = Resource.Error(e.message ?: "Update failed")
            }
        }
    }

    fun bumpCar(carId: String) {
        viewModelScope.launch {
            carRepository.bumpCar(carId)
            loadMyListings()
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isSignedOut.value = true
    }
}
