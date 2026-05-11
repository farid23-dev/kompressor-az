package az.kompressor.app.data.repository

import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun signIn(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Authentication failed")
            emit(Resource.Success(user.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Sign in failed"))
        }
    }

    override fun signUp(email: String, password: String, fullName: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registration failed")
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
            ).await()
            emit(Resource.Success(user.toDomain().copy(displayName = fullName)))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Sign up failed"))
        }
    }

    override fun signOut() = firebaseAuth.signOut()

    override fun getCurrentUser(): User? = firebaseAuth.currentUser?.toDomain()

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    private fun FirebaseUser.toDomain() = User(
        uid = uid,
        email = email ?: "",
        displayName = displayName ?: "",
        photoUrl = photoUrl?.toString() ?: ""
    )
}
