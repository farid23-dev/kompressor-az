package az.kompressor.app.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemAdminListingBinding
import az.kompressor.app.domain.model.Car
import az.kompressor.app.util.formatPrice
import com.bumptech.glide.Glide

class AdminListingAdapter(
    private val onApprove: (Car) -> Unit,
    private val onReject:  (Car) -> Unit
) : ListAdapter<Car, AdminListingAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminListingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemAdminListingBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(car: Car) {
            b.tvTitle.text = car.title
            b.tvMeta.text  = "${car.sellerName} · ${car.price.formatPrice()}"

            // Status badge
            val (label, color) = when (car.status) {
                "approved" -> "APPROVED" to Color.parseColor("#34A853")
                "rejected" -> "REJECTED" to Color.parseColor("#EA4335")
                else       -> "PENDING"  to Color.parseColor("#FF6B35")
            }
            b.tvStatus.text = label
            b.tvStatus.setBackgroundColor(color)

            // Thumbnail
            if (car.imageUrls.isNotEmpty()) {
                Glide.with(b.ivThumbnail).load(car.imageUrls.first())
                    .centerCrop().into(b.ivThumbnail)
            }

            // Hide the button that matches current status (no point approving approved / rejecting rejected)
            b.btnApprove.visibility = if (car.status == "approved") android.view.View.GONE else android.view.View.VISIBLE
            b.btnReject.visibility  = if (car.status == "rejected") android.view.View.GONE else android.view.View.VISIBLE

            b.btnApprove.setOnClickListener { onApprove(car) }
            b.btnReject.setOnClickListener  { onReject(car)  }
        }
    }

    class Diff : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(a: Car, b: Car) = a.id == b.id
        override fun areContentsTheSame(a: Car, b: Car) = a == b
    }
}
