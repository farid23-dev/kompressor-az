package az.kompressor.app.data.repository

import az.kompressor.app.data.local.dao.FavoriteCarDao
import az.kompressor.app.data.local.entity.FavoriteCarEntity
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteCarDao
) : FavoritesRepository {

    override fun getFavorites(): Flow<List<Car>> =
        dao.getAllFavorites().map { list -> list.map { it.toCar() } }

    override fun isFavorite(carId: String): Flow<Boolean> =
        dao.isFavorite(carId)

    override suspend fun addFavorite(car: Car) =
        dao.insertFavorite(car.toEntity())

    override suspend fun removeFavorite(carId: String) =
        dao.deleteFavorite(carId)

    // --- Mappers ---
    private fun FavoriteCarEntity.toCar() = Car(
        id = carId,
        title = title,
        brand = brand,
        model = model,
        year = year,
        price = price,
        mileage = mileage,
        fuelType = fuelType,
        transmission = transmission,
        city = city,
        imageUrls = if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList(),
        sellerUid = sellerUid,
        createdAt = createdAt
    )

    private fun Car.toEntity() = FavoriteCarEntity(
        carId = id,
        title = title,
        brand = brand,
        model = model,
        year = year,
        price = price,
        mileage = mileage,
        fuelType = fuelType,
        transmission = transmission,
        city = city,
        imageUrl = imageUrls.firstOrNull() ?: "",
        sellerUid = sellerUid,
        createdAt = createdAt
    )
}
