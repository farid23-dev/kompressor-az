package az.kompressor.app.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.R
import az.kompressor.app.databinding.ItemNotificationBinding
import az.kompressor.app.domain.model.AppNotification
import az.kompressor.app.util.TimeAgo

class NotificationAdapter(
    private val onDelete: (AppNotification) -> Unit,
    private val onItemClick: (AppNotification) -> Unit,
    private val onApprove: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemNotificationBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(n: AppNotification) {
            val context = b.root.context
            b.tvMessage.text = when (n.status) {
                "approved"    -> context.getString(R.string.notif_approved_format, n.carTitle)
                "rejected"    -> context.getString(R.string.notif_rejected_format, n.carTitle)
                "new_listing" -> n.message.ifBlank { context.getString(R.string.notif_new_listing_format, n.carTitle) }
                else          -> n.message.ifBlank { context.getString(R.string.notif_generic_format, n.carTitle) }
            }
            b.tvTime.text         = TimeAgo.format(context, n.timestamp)
            b.unreadDot.isVisible = !n.read
            
            val isPending = n.status == "new_listing" && n.carId.isNotBlank()
            b.btnApprove.isVisible = isPending
            b.btnApprove.setOnClickListener { onApprove(n) }

            b.btnDelete.setOnClickListener { onDelete(n) }
            b.root.setOnClickListener { onItemClick(n) }
        }
    }

    class Diff : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(a: AppNotification, b: AppNotification) = a.id == b.id
        override fun areContentsTheSame(a: AppNotification, b: AppNotification) = a == b
    }
}
