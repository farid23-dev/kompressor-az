package az.kompressor.app.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.usecase.GetCarByIdUseCase
import az.kompressor.app.domain.usecase.PostCarUseCase
import az.kompressor.app.domain.usecase.UpdateCarUseCase
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
    private val updateCarUseCase: UpdateCarUseCase,
    private val getCarByIdUseCase: GetCarByIdUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _postState = MutableStateFlow<Resource<Unit>?>(null)
    val postState: StateFlow<Resource<Unit>?> = _postState

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages

    private val _editCar = MutableStateFlow<Car?>(null)
    val editCar: StateFlow<Car?> = _editCar

    private val _existingImageUrls = MutableStateFlow<List<String>>(emptyList())
    val existingImageUrls: StateFlow<List<String>> = _existingImageUrls

    val isEditMode: Boolean get() = _editCar.value != null

    fun loadCarForEdit(carId: String) {
        if (carId.isBlank()) return
        getCarByIdUseCase(carId).onEach { resource ->
            if (resource is Resource.Success) {
                _editCar.value = resource.data
                _existingImageUrls.value = resource.data.imageUrls
            }
        }.launchIn(viewModelScope)
    }

    fun addImage(uri: Uri) {
        _selectedImages.value += uri
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value -= uri
    }

    fun removeExistingImage(url: String) {
        _existingImageUrls.value -= url
    }

    fun postCar(
        title: String, brand: String, model: String,
        year: Int, price: Long, mileage: Int,
        fuelType: String, transmission: String,
        city: String, phone: String, description: String
    ) {
        val currentUser = authRepository.getCurrentUser()
        val car = Car(
            title = title, brand = brand, model = model,
            year = year, price = price, mileage = mileage,
            fuelType = fuelType, transmission = transmission,
            city = city, phone = phone, description = description,
            sellerUid = currentUser?.uid ?: "",
            sellerName = currentUser?.displayName ?: ""
        )
        postCarUseCase(car, _selectedImages.value)
            .onEach { _postState.value = it }
            .launchIn(viewModelScope)
    }

    fun updateCar(
        brand: String, model: String,
        year: Int, price: Long, mileage: Int,
        fuelType: String, transmission: String,
        city: String, phone: String, description: String
    ) {
        val original = _editCar.value ?: return
        val car = original.copy(
            title = "$brand $model".trim(),
            brand = brand, model = model,
            year = year, price = price, mileage = mileage,
            fuelType = fuelType, transmission = transmission,
            city = city, phone = phone, description = description
        )
        updateCarUseCase(car, _selectedImages.value, _existingImageUrls.value)
            .onEach { _postState.value = it }
            .launchIn(viewModelScope)
    }

    fun resetState() { _postState.value = null }
}
