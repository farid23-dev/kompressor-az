package az.kompressor.app.ui.detail

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import az.kompressor.app.R
import az.kompressor.app.databinding.DialogFullscreenImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class FullScreenImageDialogFragment : DialogFragment(R.layout.dialog_fullscreen_image) {

    private lateinit var binding: DialogFullscreenImageBinding

    companion object {
        private const val ARG_URLS = "urls"
        private const val ARG_START = "start_index"

        fun newInstance(urls: List<String>, startIndex: Int = 0) =
            FullScreenImageDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_URLS, ArrayList(urls))
                    putInt(ARG_START, startIndex)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = android.graphics.Color.BLACK
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DialogFullscreenImageBinding.bind(view)

        val urls = arguments?.getStringArrayList(ARG_URLS) ?: return
        val startIndex = arguments?.getInt(ARG_START, 0) ?: 0

        binding.btnClose.setOnClickListener { dismiss() }

        if (urls.size > 1) {
            binding.tvPageCounter.isVisible = true
            updateCounter(startIndex + 1, urls.size)
        }

        binding.viewPagerFull.adapter = FullImageAdapter(urls)
        binding.viewPagerFull.setCurrentItem(startIndex, false)

        binding.viewPagerFull.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateCounter(position + 1, urls.size)
                }
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateCounter(current: Int, total: Int) {
        binding.tvPageCounter.text = getString(R.string.page_counter_format, current, total)
    }

    private inner class FullImageAdapter(
        private val urls: List<String>
    ) : RecyclerView.Adapter<FullImageAdapter.VH>() {

        inner class VH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.BLACK)
            }
            return VH(iv)
        }

        override fun getItemCount() = urls.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            Glide.with(holder.imageView)
                .load(urls[position])
                .placeholder(R.color.divider)
                .error(R.drawable.ic_avatar_default)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: Target<Drawable>, isFirstResource: Boolean
                    ) = false
                    override fun onResourceReady(
                        resource: Drawable, model: Any, target: Target<Drawable>,
                        dataSource: DataSource, isFirstResource: Boolean
                    ) = false
                })
                .into(holder.imageView)
        }
    }
}
