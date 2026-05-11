package az.kompressor.app.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.usecase.PostCarUseCase
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PostCarViewModel @Inject constructor(
    private val postCarUseCase: PostCarUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _postState = MutableStateFlow<Resource<Unit>?>(null)
    val postState: StateFlow<Resource<Unit>?> = _postState

    // Holds selected image URIs
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages

    fun addImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value + uri
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value - uri
    }

    fun postCar(
        title: String, brand: String, model: String,
        year: Int, price: Long, mileage: Int,
        fuelType: String, transmission: String, city: String
    ) {
        val car = Car(
            title = title, brand = brand, model = model,
            year = year, price = price, mileage = mileage,
            fuelType = fuelType, transmission = transmission,
            city = city, sellerUid = authRepository.getCurrentUser()?.uid ?: ""
        )
        postCarUseCase(car, _selectedImages.value)
            .onEach { _postState.value = it }
            .launchIn(viewModelScope)
    }

    fun resetState() { _postState.value = null }
}
