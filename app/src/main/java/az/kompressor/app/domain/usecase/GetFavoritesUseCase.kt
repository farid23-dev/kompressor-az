package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    operator fun invoke(): Flow<List<Car>> = favoritesRepository.getFavorites()
}
