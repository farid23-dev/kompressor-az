package az.kompressor.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.AppNotification
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.repository.NotificationRepository
import az.kompressor.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val carRepository: az.kompressor.app.domain.repository.CarRepository
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
                    android.util.Log.d("NotifVM", "Loaded ${list.size} notifications, ${_unreadCount.value} unread")
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

    fun approveCar(n: AppNotification) {
        if (n.carId.isBlank()) return
        viewModelScope.launch {
            val resource = carRepository.getCarById(n.carId).first { it !is Resource.Loading }
            if (resource is Resource.Success) {
                val car = resource.data
                carRepository.updateCarStatus(
                    carId = car.id,
                    status = "approved",
                    sellerUid = car.sellerUid,
                    carTitle = car.title
                )
                deleteNotification(n.id)
            }
        }
    }
}
