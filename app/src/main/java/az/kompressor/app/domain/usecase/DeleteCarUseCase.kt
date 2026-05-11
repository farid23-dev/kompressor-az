package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.repository.CarRepository
import javax.inject.Inject

class DeleteCarUseCase @Inject constructor(
    private val carRepository: CarRepository
) {
    operator fun invoke(carId: String) = carRepository.deleteCarById(carId)
}
