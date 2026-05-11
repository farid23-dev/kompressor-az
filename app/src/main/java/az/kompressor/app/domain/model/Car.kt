package az.kompressor.app.domain.model

data class Car(
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
    val createdAt: Long = 0L
)
