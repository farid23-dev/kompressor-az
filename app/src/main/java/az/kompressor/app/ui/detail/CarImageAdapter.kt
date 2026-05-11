package az.kompressor.app.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemCarImageBinding
import com.bumptech.glide.Glide

class CarImageAdapter(private val urls: List<String>) :
    RecyclerView.Adapter<CarImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemCarImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemCarImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.binding.ivCarImage.context)
            .load(urls[position])
            .centerCrop()
            .placeholder(android.R.color.darker_gray)
            .into(holder.binding.ivCarImage)
    }

    override fun getItemCount() = urls.size
}
