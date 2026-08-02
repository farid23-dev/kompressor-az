package az.kompressor.app.util

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Modern password validation:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one digit
     * - At least one special character
     */
    fun validatePassword(password: String): PasswordResult {
        if (password.length < 8) {
            return PasswordResult.Invalid("Password must be at least 8 characters long")
        }
        if (!password.any { it.isUpperCase() }) {
            return PasswordResult.Invalid("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isDigit() }) {
            return PasswordResult.Invalid("Password must contain at least one digit")
        }
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        if (!password.any { it in specialChars }) {
            return PasswordResult.Invalid("Password must contain at least one special character")
        }
        return PasswordResult.Valid
    }

    sealed interface PasswordResult {
        object Valid : PasswordResult
        data class Invalid(val message: String) : PasswordResult
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.isNotBlank() && phone.length >= 9
    }
}
