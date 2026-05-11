package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCarByIdUseCase @Inject constructor(
    private val carRepository: CarRepository
) {
    operator fun invoke(carId: String): Flow<Resource<Car>> = carRepository.getCarById(carId)
}
