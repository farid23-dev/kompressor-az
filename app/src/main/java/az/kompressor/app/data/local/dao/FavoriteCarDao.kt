package az.kompressor.app.data.local.dao

import androidx.room.*
import az.kompressor.app.data.local.entity.FavoriteCarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCarDao {

    @Query("SELECT * FROM favorite_cars ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteCarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(car: FavoriteCarEntity)

    @Query("DELETE FROM favorite_cars WHERE carId = :carId")
    suspend fun deleteFavorite(carId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_cars WHERE carId = :carId)")
    fun isFavorite(carId: String): Flow<Boolean>

    @Query("DELETE FROM favorite_cars")
    suspend fun deleteAll()
}
