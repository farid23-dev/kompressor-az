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
        if (car.title.isBlank()) return flow { emit(Resource.Error("Title cannot be empty")) }
        if (car.brand.isBlank()) return flow { emit(Resource.Error("Brand cannot be empty")) }
        if (car.model.isBlank()) return flow { emit(Resource.Error("Model cannot be empty")) }
        if (car.price <= 0) return flow { emit(Resource.Error("Price must be greater than 0")) }
        if (car.year < 1900) return flow { emit(Resource.Error("Invalid year")) }
        return carRepository.postCar(car, imageUris)
    }
}
