package az.kompressor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room cache of a favorited car.
 * Only the fields needed for the list card + basic detail preview are stored.
 * Full car data (description, phone, etc.) is always re-fetched from Firestore
 * when opening the detail screen.
 */
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
    val imageUrl: String,       // first image URL for thumbnail
    val sellerUid: String,
    val sellerName: String = "",
    val createdAt: Long,
    val expiresAt: Long = 0L,   // 0 = legacy (TimeAgo falls back to createdAt + 30d)
    val savedAt: Long = System.currentTimeMillis()
)
