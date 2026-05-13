package az.kompressor.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.AppNotification
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    val unreadCount: StateFlow<Int> get() = _unreadCount
    private val _unreadCount = MutableStateFlow(0)

    init {
        authRepository.getCurrentUser()?.uid?.let { uid ->
            notificationRepository.getNotifications(uid)
                .onEach { list ->
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.read }
                }
                .launchIn(viewModelScope)
        }
    }

    fun deleteNotification(notifId: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            notificationRepository.deleteNotification(uid, notifId)
        }
    }

    fun markAllRead() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch { notificationRepository.markAllRead(uid) }
    }
}
