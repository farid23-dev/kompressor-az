package az.kompressor.app.data.repository

import az.kompressor.app.domain.model.AppNotification
import az.kompressor.app.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val ref = firestore.collection("notifications")
            .document(uid)
            .collection("items")

        val listener = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                try {
                    AppNotification(
                        id        = doc.id,
                        carId     = doc.getString("carId") ?: "",
                        carTitle  = doc.getString("carTitle") ?: "",
                        status    = doc.getString("status") ?: "",
                        message   = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        read      = doc.getBoolean("read") ?: false
                    )
                } catch (_: Exception) {
                    null
                }
            }?.sortedByDescending { it.timestamp } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun markAllRead(uid: String) {
        try {
            val snapshot = firestore.collection("notifications")
                .document(uid).collection("items")
                .whereEqualTo("read", false).get().await()
            
            if (snapshot.isEmpty) return
            
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("NotifRepo", "Error marking as read: ${e.message}")
        }
    }

    override suspend fun deleteNotification(uid: String, notifId: String) {
        try {
            firestore.collection("notifications")
                .document(uid).collection("items")
                .document(notifId).delete().await()
        } catch (_: Exception) {}
    }
}
