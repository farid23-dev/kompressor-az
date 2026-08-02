package az.kompressor.app.domain.usecase

import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.util.Resource
import az.kompressor.app.util.ValidationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(
        name: String, surname: String, phone: String,
        email: String, password: String, confirmPassword: String
    ): Flow<Resource<User>> {
        if (name.isBlank())    return flow { emit(Resource.Error("Name cannot be empty")) }
        if (surname.isBlank()) return flow { emit(Resource.Error("Surname cannot be empty")) }
        if (!ValidationUtils.isValidPhone(phone)) return flow { emit(Resource.Error("Invalid phone number")) }
        if (!ValidationUtils.isValidEmail(email)) return flow { emit(Resource.Error("Invalid email address")) }
        
        val passwordResult = ValidationUtils.validatePassword(password)
        if (passwordResult is ValidationUtils.PasswordResult.Invalid) {
            return flow { emit(Resource.Error(passwordResult.message)) }
        }
        
        if (password != confirmPassword)
            return flow { emit(Resource.Error("Passwords do not match")) }
            
        return authRepository.signUp(email.trim(), password, name.trim(), surname.trim(), phone.trim())
    }
}
