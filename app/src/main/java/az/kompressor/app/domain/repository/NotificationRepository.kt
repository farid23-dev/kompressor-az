package az.kompressor.app.domain.repository

import az.kompressor.app.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(uid: String): Flow<List<AppNotification>>
    suspend fun markAllRead(uid: String)
}
