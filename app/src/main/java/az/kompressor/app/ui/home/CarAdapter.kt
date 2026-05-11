package az.kompressor.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.domain.model.Car
import az.kompressor.app.databinding.ItemCarBinding
import com.bumptech.glide.Glide

class CarAdapter(
    private val onItemClick: (Car) -> Unit
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
            binding.tvTitle.text = car.title
            binding.tvPrice.text = "${car.price} AZN"
            binding.tvDetails.text = "${car.year} • ${car.mileage} km • ${car.fuelType}"
            binding.tvCity.text = car.city
            binding.tvTransmission.text = car.transmission

            Glide.with(binding.ivCarImage.context)
                .load(car.imageUrls.firstOrNull())
                .placeholder(android.R.color.darker_gray)
                .centerCrop()
                .into(binding.ivCarImage)

            binding.root.setOnClickListener { onItemClick(car) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Car, newItem: Car) = oldItem == newItem
    }
}
