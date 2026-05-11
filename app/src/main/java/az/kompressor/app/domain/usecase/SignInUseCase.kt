package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(email: String, password: String): Flow<Resource<User>> {
        if (email.isBlank()) return flow { emit(Resource.Error("Email cannot be empty")) }
        if (password.isBlank()) return flow { emit(Resource.Error("Password cannot be empty")) }
        if (password.length < 6) return flow { emit(Resource.Error("Password must be at least 6 characters")) }
        return authRepository.signIn(email.trim(), password)
    }
}
