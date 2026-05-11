package az.kompressor.app.domain.usecase

import android.net.Uri
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PostCarUseCase @Inject constructor(
    private val carRepository: CarRepository
) {
    operator fun invoke(car: Car, imageUris: List<Uri>): Flow<Resource<Unit>> {
        if (car.brand.isBlank()) return flow { emit(Resource.Error("Brand is required")) }
        if (car.model.isBlank()) return flow { emit(Resource.Error("Model is required")) }
        if (car.year < 1900 || car.year > 2100) return flow { emit(Resource.Error("Enter a valid year")) }
        if (car.price <= 0) return flow { emit(Resource.Error("Price must be greater than 0")) }
        if (car.mileage < 0) return flow { emit(Resource.Error("Enter a valid mileage")) }
        if (car.phone.isBlank()) return flow { emit(Resource.Error("Phone number is required")) }
        if (car.city.isBlank()) return flow { emit(Resource.Error("City is required")) }
        return carRepository.postCar(car, imageUris)
    }
}
