package az.kompressor.app.domain.repository

import az.kompressor.app.domain.model.User
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signIn(email: String, password: String): Flow<Resource<User>>
    fun signUp(
        email: String, password: String,
        name: String, surname: String, phone: String
    ): Flow<Resource<User>>
    fun signOut()
    fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
    fun getUserProfile(uid: String): Flow<Resource<User>>
    suspend fun saveUserProfile(uid: String, name: String, surname: String, phone: String, email: String)
}
