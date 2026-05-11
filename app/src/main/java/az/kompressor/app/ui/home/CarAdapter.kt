package az.kompressor.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.domain.model.Car
import az.kompressor.app.databinding.ItemCarBinding
import az.kompressor.app.util.TimeAgo
import az.kompressor.app.util.formatMileage
import az.kompressor.app.util.formatPrice
import com.bumptech.glide.Glide

class CarAdapter(
    // Exposes the shared ImageView for the scene transition animation
    private val onItemClick: (Car, View) -> Unit
) : ListAdapter<Car, CarAdapter.CarViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarViewHolder(private val binding: ItemCarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            // Unique transitionName per item so the system knows which view to animate
            binding.ivCarImage.transitionName = "car_image_${car.id}"

            binding.tvTitle.text = car.title
            binding.tvPrice.text = car.price.formatPrice()
            binding.tvDetails.text = "${car.year} · ${car.mileage.formatMileage()} · ${car.fuelType}"
            binding.tvCity.text = "📍 ${car.city}"
            binding.tvTransmission.text = car.transmission
            binding.tvAge.text = TimeAgo.format(car.createdAt)

            Glide.with(binding.ivCarImage.context)
                .load(car.imageUrls.firstOrNull())
                .placeholder(android.R.color.darker_gray)
                .centerCrop()
                .into(binding.ivCarImage)

            binding.root.setOnClickListener { onItemClick(car, binding.ivCarImage) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Car, newItem: Car) = oldItem == newItem
    }
}
