package az.kompressor.app.ui.detail

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import az.kompressor.app.databinding.ItemCarImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * ViewPager2 adapter for the image gallery on the car detail screen.
 *
 * @param urls              List of remote image URLs to display.
 * @param onFirstImageReady Called once when the first image finishes loading
 *                          (used to start the shared-element enter transition).
 * @param onImageClick      Called with the tapped position — used to open the
 *                          full-screen viewer.
 */
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

        // Tap → open full-screen viewer at the same position
        holder.binding.ivCarImage.setOnClickListener {
            onImageClick?.invoke(position)
        }

        holder.binding.pbImageLoading.visibility = android.view.View.VISIBLE

        Glide.with(holder.binding.ivCarImage.context)
            .load(urls[position])
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    holder.binding.pbImageLoading.visibility = android.view.View.GONE
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
                    holder.binding.pbImageLoading.visibility = android.view.View.GONE
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
