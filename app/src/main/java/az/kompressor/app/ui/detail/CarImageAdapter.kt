package az.kompressor.app.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemCarImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable

class CarImageAdapter(
    private val urls: List<String>,
    private val onFirstImageReady: (() -> Unit)? = null
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
        Glide.with(holder.binding.ivCarImage.context)
            .load(urls[position])
            .centerCrop()
            .placeholder(android.R.color.darker_gray)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?,
                    target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    if (isFirst && !firstImageNotified) {
                        firstImageNotified = true
                        onFirstImageReady?.invoke()
                    }
                    return false
                }
                override fun onResourceReady(resource: Drawable, model: Any,
                    target: Target<Drawable>, dataSource: DataSource,
                    isFirstResource: Boolean): Boolean {
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
