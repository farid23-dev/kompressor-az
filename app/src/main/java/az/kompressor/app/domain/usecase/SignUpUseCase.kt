package az.kompressor.app.domain.usecase

import android.util.Patterns
import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Flow<Resource<User>> {
        if (fullName.isBlank()) return flow { emit(Resource.Error("Full name cannot be empty")) }
        if (email.isBlank()) return flow { emit(Resource.Error("Email cannot be empty")) }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return flow { emit(Resource.Error("Invalid email address")) }
        if (password.length < 6) return flow { emit(Resource.Error("Password must be at least 6 characters")) }
        if (password != confirmPassword) return flow { emit(Resource.Error("Passwords do not match")) }
        return authRepository.signUp(email.trim(), password, fullName.trim())
    }
}
