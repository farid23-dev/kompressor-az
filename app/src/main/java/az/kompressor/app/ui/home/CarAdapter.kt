package az.kompressor.app.ui.home

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.R
import az.kompressor.app.domain.model.Car
import az.kompressor.app.databinding.ItemCarBinding
import az.kompressor.app.util.TimeAgo
import az.kompressor.app.util.formatMileage
import az.kompressor.app.util.formatPrice
import com.bumptech.glide.Glide
import androidx.core.graphics.toColorInt

class CarAdapter(
    private val onItemClick: (Car, View) -> Unit,
    private val onFavoriteClick: (Car) -> Unit
) : ListAdapter<Car, CarAdapter.CarViewHolder>(DiffCallback()) {

    private var favoriteIds: Set<String> = emptySet()

    fun setFavorites(ids: Set<String>) {
        favoriteIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarViewHolder(private val binding: ItemCarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(car: Car) {
            binding.ivCarImage.transitionName = "car_image_${car.id}"

            binding.tvTitle.text = car.title
            binding.tvPrice.text = car.price.formatPrice()
            binding.tvDetails.text = "${car.year} · ${car.mileage.formatMileage()}"
            binding.tvCity.text = car.city

            binding.tvAge.text = TimeAgo.format(binding.root.context, car.createdAt)

            val isFavorite = favoriteIds.contains(car.id)
            binding.btnQuickFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            binding.btnQuickFavorite.setOnClickListener { onFavoriteClick(car) }

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
