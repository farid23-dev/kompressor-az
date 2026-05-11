package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.repository.CarRepository
import javax.inject.Inject

class GetCarsByUserUseCase @Inject constructor(
    private val carRepository: CarRepository
) {
    operator fun invoke(uid: String) = carRepository.getCarsByUser(uid)
}
