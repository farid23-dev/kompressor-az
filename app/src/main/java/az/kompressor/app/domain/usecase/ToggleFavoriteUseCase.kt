package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    suspend fun add(car: Car) = favoritesRepository.addFavorite(car)
    suspend fun remove(carId: String) = favoritesRepository.removeFavorite(carId)
    fun isFavorite(carId: String) = favoritesRepository.isFavorite(carId)
}
