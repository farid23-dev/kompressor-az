package az.kompressor.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.usecase.SignInUseCase
import az.kompressor.app.domain.usecase.SignUpUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState

    fun signIn(email: String, password: String) {
        signInUseCase(email, password).onEach { _authState.value = it }.launchIn(viewModelScope)
    }

    fun signUp(fullName: String, email: String, password: String, confirmPassword: String) {
        signUpUseCase(fullName, email, password, confirmPassword).onEach { _authState.value = it }.launchIn(viewModelScope)
    }

    fun resetState() { _authState.value = null }
}
