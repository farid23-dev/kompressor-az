package az.kompressor.app.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminSetup {

    private const val ADMIN_EMAIL = "admin@kompressor.az"

    suspend fun registerCurrentUserAsAdminIfNeeded() {
        val auth = FirebaseAuth.getInstance()
        val db   = FirebaseFirestore.getInstance()
        val user = auth.currentUser ?: return
        if (user.email != ADMIN_EMAIL) return

        val ref = db.collection("admins").document(user.uid)
        if (!ref.get().await().exists()) {
            ref.set(mapOf(
                "email"     to user.email,
                "grantedAt" to System.currentTimeMillis()
            )).await()
        }
    }
}
