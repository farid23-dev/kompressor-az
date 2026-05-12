package az.kompressor.app.domain.model

data class AppNotification(
    val id: String = "",
    val carId: String = "",
    val carTitle: String = "",
    val status: String = "",   // "approved" | "rejected"
    val timestamp: Long = 0L,
    val read: Boolean = false
)
