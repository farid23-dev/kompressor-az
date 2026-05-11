package az.kompressor.app.domain.repository

import az.kompressor.app.domain.model.Car
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getCars(): Flow<Resource<List<Car>>>
    fun searchCars(query: String): Flow<Resource<List<Car>>>
    fun getCarById(carId: String): Flow<Resource<Car>>
    suspend fun seedDummyData()
}
