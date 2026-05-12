package az.kompressor.app.data.repository

import az.kompressor.app.domain.model.AppNotification
import az.kompressor.app.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map { doc ->
                AppNotification(
                    id        = doc.id,
                    carId     = doc.getString("carId") ?: "",
                    carTitle  = doc.getString("carTitle") ?: "",
                    status    = doc.getString("status") ?: "",
                    message   = doc.getString("message") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    read      = doc.getBoolean("read") ?: false
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun markAllRead(uid: String) {
        try {
            val items = firestore.collection("notifications")
                .document(uid).collection("items")
                .whereEqualTo("read", false).get().await()
            items.documents.forEach { it.reference.update("read", true).await() }
        } catch (_: Exception) {}
    }
}
