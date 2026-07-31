package az.kompressor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_cars")
data class FavoriteCarEntity(
    @PrimaryKey val carId: String,
    val title: String,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Long,
    val mileage: Int,
    val fuelType: String,
    val transmission: String,
    val city: String,
    val imageUrl: String,
    val sellerUid: String,
    val sellerName: String = "",
    val createdAt: Long,
    val expiresAt: Long = 0L,
    val savedAt: Long = System.currentTimeMillis()
)
