package az.kompressor.app.data.remote.dto

import az.kompressor.app.domain.model.Car

data class CarDto(
    val id: String = "",
    val title: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val price: Long = 0,
    val mileage: Int = 0,
    val fuelType: String = "",
    val transmission: String = "",
    val city: String = "",
    val description: String = "",
    val phone: String = "",
    val imageUrls: List<String> = emptyList(),
    val sellerUid: String = "",
    val createdAt: Long = 0L,
    val viewCount: Long = 0L,
    val expiresAt: Long = 0L,
    val sellerName: String = "",
    val status: String = "approved"  // "pending" | "approved" | "rejected"
)

fun CarDto.toDomain() = Car(
    id = id, title = title, brand = brand, model = model,
    year = year, price = price, mileage = mileage,
    fuelType = fuelType, transmission = transmission,
    city = city, description = description, phone = phone,
    imageUrls = imageUrls, sellerUid = sellerUid, createdAt = createdAt,
    viewCount = viewCount, expiresAt = expiresAt, sellerName = sellerName,
    status = status
)
