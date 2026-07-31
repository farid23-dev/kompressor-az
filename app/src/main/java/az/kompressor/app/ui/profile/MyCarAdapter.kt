package az.kompressor.app.ui.profile

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemMyCarBinding
import az.kompressor.app.domain.model.Car
import az.kompressor.app.util.TimeAgo
import az.kompressor.app.util.formatPrice
import com.bumptech.glide.Glide

class MyCarAdapter(
    private val onEdit: (Car) -> Unit,
    private val onDelete: (Car) -> Unit,
    private val onBump: (Car) -> Unit
) : ListAdapter<Car, MyCarAdapter.MyCarViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCarViewHolder {
        val binding = ItemMyCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyCarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyCarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MyCarViewHolder(private val binding: ItemMyCarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(car: Car) {
            binding.tvCarTitle.text = car.title
            binding.tvCarPrice.text = car.price.formatPrice()

            val days = TimeAgo.daysLeft(car.createdAt, car.expiresAt)
            val daysLabel = when {
                days <= 0 -> "⚠ Last day!"
                days == 1 -> "⚠ 1 day left"
                days <= 5 -> "⚠ $days days left"
                else      -> "$days days left"
            }
            binding.tvStats.text = "👁 ${car.viewCount} views · $daysLabel"

            Glide.with(binding.ivCarThumb.context)
                .load(car.imageUrls.firstOrNull())
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(binding.ivCarThumb)

            binding.btnEdit.setOnClickListener { onEdit(car) }
            binding.btnDelete.setOnClickListener { onDelete(car) }
            binding.btnBump.setOnClickListener { onBump(car) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(o: Car, n: Car) = o.id == n.id
        override fun areContentsTheSame(o: Car, n: Car) = o == n
    }
}
