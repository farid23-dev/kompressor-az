package az.kompressor.app.domain.repository

import az.kompressor.app.domain.model.User
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signIn(email: String, password: String): Flow<Resource<User>>
    fun signUp(email: String, password: String, fullName: String): Flow<Resource<User>>
    fun signOut()
    fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
}
