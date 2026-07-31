package az.kompressor.app.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.domain.repository.FavoritesRepository
import az.kompressor.app.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val carRepository: CarRepository,
    private val favoritesRepository: FavoritesRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _isSignedOut = MutableStateFlow(false)
    val isSignedOut: StateFlow<Boolean> = _isSignedOut

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    init {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            _isAdmin.value = carRepository.isAdmin(uid)
        }
    }

    fun getCurrentTheme(): Int = LocaleHelper.getDarkMode(context)

    fun setTheme(mode: Int) {
        LocaleHelper.saveDarkMode(context, mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                favoritesRepository.clearAll()
            } catch (_: Exception) {
            }
            authRepository.signOut()
            _isSignedOut.value = true
        }
    }
}
