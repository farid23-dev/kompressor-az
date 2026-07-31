package az.kompressor.app.ui.detail

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemCarImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class CarImageAdapter(
    private val urls: List<String>,
    private val onFirstImageReady: (() -> Unit)? = null,
    private val onImageClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<CarImageAdapter.ImageViewHolder>() {

    private var firstImageNotified = false

    inner class ImageViewHolder(val binding: ItemCarImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemCarImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val isFirst = position == 0

        holder.binding.ivCarImage.setOnClickListener {
            onImageClick?.invoke(position)
        }

        holder.binding.pbImageLoading.isVisible = true

        Glide.with(holder.binding.ivCarImage.context)
            .load(urls[position])
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    holder.binding.pbImageLoading.isVisible = false
                    if (isFirst && !firstImageNotified) {
                        firstImageNotified = true
                        onFirstImageReady?.invoke()
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any,
                    target: Target<Drawable>, dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.binding.pbImageLoading.isVisible = false
                    if (isFirst && !firstImageNotified) {
                        firstImageNotified = true
                        onFirstImageReady?.invoke()
                    }
                    return false
                }
            })
            .into(holder.binding.ivCarImage)
    }

    override fun getItemCount() = urls.size
}
