package az.kompressor.app.domain.usecase

import android.net.Uri
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateCarUseCase @Inject constructor(
    private val carRepository: CarRepository
) {
    operator fun invoke(
        car: Car,
        newImageUris: List<Uri>,
        existingImageUrls: List<String>
    ): Flow<Resource<Unit>> = carRepository.updateCar(car, newImageUris, existingImageUrls)
}
