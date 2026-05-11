package az.kompressor.app.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentCarDetailBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.TimeAgo
import az.kompressor.app.util.formatMileage
import az.kompressor.app.util.formatPrice
import az.kompressor.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CarDetailFragment : Fragment() {

    private var _binding: FragmentCarDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CarDetailViewModel by viewModels()
    private val args: CarDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Shared element enter transition
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        // Delay until image loads so the transition has content to animate
        postponeEnterTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Match the transitionName set on ivCarImage in the adapter
        binding.viewPagerImages.transitionName = "car_image_${args.carId}"

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        viewModel.loadCar(args.carId)
        observeCarState()
        observeFavoriteState()
    }

    private fun observeCarState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.scrollView.isVisible = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.scrollView.isVisible = true
                        val car = state.data

                        // Image gallery — start postponed transition after first image loads
                        if (car.imageUrls.isNotEmpty()) {
                            val adapter = CarImageAdapter(car.imageUrls) {
                                // Called back when first image is ready
                                startPostponedEnterTransition()
                            }
                            binding.viewPagerImages.adapter = adapter
                            setupDots(car.imageUrls.size)
                            binding.viewPagerImages.registerOnPageChangeCallback(
                                object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        updateDots(position, car.imageUrls.size)
                                    }
                                }
                            )
                        } else {
                            // No images — start immediately
                            startPostponedEnterTransition()
                        }

                        binding.tvTitle.text = car.title
                        // Age — e.g. "3 hours ago", shown right-aligned next to title
                        binding.tvAge.text = TimeAgo.format(car.createdAt)
                        binding.tvPrice.text = car.price.formatPrice()

                        // View count — hidden until at least 1 view recorded
                        if (car.viewCount > 0) {
                            binding.tvViewCount.isVisible = true
                            binding.tvViewCount.text = "👁 ${car.viewCount} views"
                        }

                        binding.tvYear.text = car.year.toString()
                        binding.tvMileage.text = car.mileage.formatMileage()
                        binding.tvFuelType.text = car.fuelType
                        binding.tvTransmission.text = car.transmission
                        binding.tvCity.text = car.city

                        if (car.description.isNotBlank()) {
                            binding.tvDescriptionLabel.isVisible = true
                            binding.tvDescription.isVisible = true
                            binding.tvDescription.text = car.description
                        }

                        val phoneNumber = car.phone.ifBlank { null }

                        // Call
                        binding.btnContact.setOnClickListener {
                            if (phoneNumber != null) {
                                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
                            } else {
                                binding.root.showSnackbar("No phone number available")
                            }
                        }

                        // WhatsApp deep link
                        binding.btnWhatsApp.setOnClickListener {
                            if (phoneNumber != null) {
                                // Strip non-digits, ensure international format
                                val digits = phoneNumber.replace(Regex("[^\\d]"), "")
                                val url = "https://wa.me/$digits"
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    binding.root.showSnackbar("WhatsApp not installed")
                                }
                            } else {
                                binding.root.showSnackbar("No phone number available")
                            }
                        }

                        // Share
                        binding.btnShare.setOnClickListener {
                            val deepLink = "https://kompressor.az/car/${car.id}"
                            val text = buildString {
                                append("🚗 ${car.title}\n")
                                append("💰 ${car.price.formatPrice()}\n")
                                append("📍 ${car.city} · ${car.year} · ${car.mileage.formatMileage()}\n")
                                append("⛽ ${car.fuelType} · ${car.transmission}\n")
                                if (phoneNumber != null) append("📞 $phoneNumber\n")
                                append("\n$deepLink")
                            }
                            startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }, "Share listing"
                                )
                            )
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.root.showSnackbar(state.message)
                        startPostponedEnterTransition()
                    }
                }
            }
        }
    }

    private fun setupDots(count: Int) {
        binding.dotsLayout.removeAllViews()
        if (count <= 1) return
        repeat(count) { i ->
            val dot = ImageView(requireContext()).apply {
                setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                val size = 20 // dp → px
                val px = (size * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(px, px).apply {
                    marginStart = 6; marginEnd = 6
                }
            }
            binding.dotsLayout.addView(dot)
        }
    }

    private fun updateDots(selected: Int, count: Int) {
        if (count <= 1) return
        for (i in 0 until binding.dotsLayout.childCount) {
            (binding.dotsLayout.getChildAt(i) as? ImageView)?.setImageResource(
                if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isFavorite.collectLatest { isFav ->
                binding.btnFavorite.setImageResource(
                    if (isFav) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
