package az.kompressor.app.data.repository

import az.kompressor.app.domain.model.User
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection = firestore.collection("users")

    override fun signIn(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Authentication failed")
            // Fetch full profile from Firestore
            val doc = usersCollection.document(firebaseUser.uid).get().await()
            val user = if (doc.exists()) {
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = doc.getString("name") ?: "",
                    surname = doc.getString("surname") ?: "",
                    phone = doc.getString("phone") ?: ""
                )
            } else {
                // Legacy account — no Firestore profile yet
                User(uid = firebaseUser.uid, email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "")
            }
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Sign in failed"))
        }
    }

    override fun signUp(
        email: String, password: String,
        name: String, surname: String, phone: String
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")

            // Set displayName in Firebase Auth for quick access
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName("$name $surname".trim())
                    .build()
            ).await()

            // Save full profile to Firestore
            saveUserProfile(firebaseUser.uid, name, surname, phone, email)

            emit(Resource.Success(User(
                uid = firebaseUser.uid,
                email = email,
                name = name, surname = surname, phone = phone
            )))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Sign up failed"))
        }
    }

    override fun getUserProfile(uid: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val doc = usersCollection.document(uid).get().await()
            val user = if (doc.exists()) {
                User(
                    uid = uid,
                    email = doc.getString("email") ?: "",
                    name = doc.getString("name") ?: "",
                    surname = doc.getString("surname") ?: "",
                    phone = doc.getString("phone") ?: ""
                )
            } else {
                // Fallback to Firebase Auth display name
                val fbUser = firebaseAuth.currentUser
                User(uid = uid, email = fbUser?.email ?: "",
                    name = fbUser?.displayName ?: "")
            }
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load profile"))
        }
    }

    override suspend fun saveUserProfile(
        uid: String, name: String, surname: String, phone: String, email: String
    ) {
        usersCollection.document(uid).set(
            mapOf("name" to name, "surname" to surname, "phone" to phone, "email" to email)
        ).await()
    }

    override fun signOut() = firebaseAuth.signOut()

    override fun getCurrentUser(): User? {
        val fbUser = firebaseAuth.currentUser ?: return null
        val displayParts = fbUser.displayName?.split(" ", limit = 2) ?: emptyList()
        return User(
            uid = fbUser.uid,
            email = fbUser.email ?: "",
            name = displayParts.getOrNull(0) ?: "",
            surname = displayParts.getOrNull(1) ?: "",
            phone = ""   // phone fetched async from Firestore when needed
        )
    }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
