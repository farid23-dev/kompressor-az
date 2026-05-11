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
    val imageUrl: String,   // first image only, for list thumbnail
    val sellerUid: String,
    val createdAt: Long,
    val savedAt: Long = System.currentTimeMillis()
)
