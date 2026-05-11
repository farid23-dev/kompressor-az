package az.kompressor.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemMyCarBinding
import az.kompressor.app.domain.model.Car
import com.bumptech.glide.Glide

class MyCarAdapter(
    private val onDelete: (Car) -> Unit
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

        fun bind(car: Car) {
            binding.tvCarTitle.text = car.title
            binding.tvCarPrice.text = "${car.price} AZN"
            binding.tvCarCity.text = car.city
            Glide.with(binding.ivCarThumb.context)
                .load(car.imageUrls.firstOrNull())
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(binding.ivCarThumb)
            binding.btnDelete.setOnClickListener { onDelete(car) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(o: Car, n: Car) = o.id == n.id
        override fun areContentsTheSame(o: Car, n: Car) = o == n
    }
}
