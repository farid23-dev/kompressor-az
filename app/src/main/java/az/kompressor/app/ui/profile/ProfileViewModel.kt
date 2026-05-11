package az.kompressor.app.ui.profile

import androidx.lifecycle.ViewModel
import az.kompressor.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isSignedOut = MutableStateFlow(false)
    val isSignedOut: StateFlow<Boolean> = _isSignedOut

    fun getCurrentUserEmail(): String =
        authRepository.getCurrentUser()?.email ?: "Unknown"

    fun getCurrentUserUid(): String =
        authRepository.getCurrentUser()?.uid ?: ""

    fun signOut() {
        authRepository.signOut()
        _isSignedOut.value = true
    }
}
