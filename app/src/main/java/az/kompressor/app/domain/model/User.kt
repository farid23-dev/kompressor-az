package az.kompressor.app.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val surname: String = "",
    val phone: String = "",
    // Derived convenience — "Ali Mammadov"
    val displayName: String = if (name.isNotBlank()) "$name $surname".trim() else ""
)
