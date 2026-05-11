package az.kompressor.app.ui.post

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemSelectedImageBinding
import com.bumptech.glide.Glide

class SelectedImageAdapter(
    private val onRemove: (Uri) -> Unit
) : ListAdapter<Uri, SelectedImageAdapter.ImageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemSelectedImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(private val binding: ItemSelectedImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            Glide.with(binding.ivImage.context).load(uri).centerCrop().into(binding.ivImage)
            binding.btnRemove.setOnClickListener { onRemove(uri) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
    }
}
