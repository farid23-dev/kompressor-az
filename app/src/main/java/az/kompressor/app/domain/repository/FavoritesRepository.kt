package az.kompressor.app.domain.repository

import az.kompressor.app.domain.model.Car
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavorites(): Flow<List<Car>>
    fun isFavorite(carId: String): Flow<Boolean>
    suspend fun addFavorite(car: Car)
    suspend fun removeFavorite(carId: String)
    suspend fun clearAll()
}
