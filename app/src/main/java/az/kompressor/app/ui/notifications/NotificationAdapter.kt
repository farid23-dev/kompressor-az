package az.kompressor.app.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemNotificationBinding
import az.kompressor.app.domain.model.AppNotification
import az.kompressor.app.util.TimeAgo

class NotificationAdapter : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemNotificationBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(n: AppNotification) {
            b.tvMessage.text = when (n.status) {
                "approved"    -> "✅ Your listing \"${n.carTitle}\" was approved!"
                "rejected"    -> "❌ Your listing \"${n.carTitle}\" was rejected."
                "new_listing" -> if (n.message.isNotBlank()) "🆕 ${n.message}"
                                 else "🆕 New listing submitted: \"${n.carTitle}\""
                else          -> n.message.ifBlank { "Notification about \"${n.carTitle}\"" }
            }
            b.tvTime.text         = TimeAgo.format(n.timestamp)
            b.unreadDot.isVisible = !n.read
        }
    }

    class Diff : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(a: AppNotification, b: AppNotification) = a.id == b.id
        override fun areContentsTheSame(a: AppNotification, b: AppNotification) = a == b
    }
}
