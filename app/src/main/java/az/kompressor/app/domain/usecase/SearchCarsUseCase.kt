package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchCarsUseCase @Inject constructor(
    private val carRepository: CarRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<Car>>> {
        if (query.isBlank()) return carRepository.getCars()
        return carRepository.searchCars(query)
    }
}
