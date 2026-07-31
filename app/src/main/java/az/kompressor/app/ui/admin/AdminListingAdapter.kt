package az.kompressor.app.ui.admin

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemAdminListingBinding
import az.kompressor.app.domain.model.Car
import az.kompressor.app.util.formatPrice
import com.bumptech.glide.Glide
import androidx.core.graphics.toColorInt

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

        @SuppressLint("SetTextI18n")
        fun bind(car: Car) {
            b.tvTitle.text = car.title
            b.tvMeta.text  = "${car.sellerName} · ${car.price.formatPrice()}"

            val (label, color) = when (car.status) {
                "approved" -> "APPROVED" to "#34A853".toColorInt()
                "rejected" -> "REJECTED" to "#EA4335".toColorInt()
                else       -> "PENDING"  to "#FF6B35".toColorInt()
            }
            b.tvStatus.text = label
            b.tvStatus.setBackgroundColor(color)

            if (car.imageUrls.isNotEmpty()) {
                Glide.with(b.ivThumbnail).load(car.imageUrls.first())
                    .centerCrop().into(b.ivThumbnail)
            }

            b.btnApprove.isVisible = car.status != "approved"
            b.btnReject.isVisible  = car.status != "rejected"

            b.btnApprove.setOnClickListener { onApprove(car) }
            b.btnReject.setOnClickListener  { onReject(car)  }
        }
    }

    class Diff : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(a: Car, b: Car) = a.id == b.id
        override fun areContentsTheSame(a: Car, b: Car) = a == b
    }
}
