package az.kompressor.app.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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

    // Guard: only call startPostponedEnterTransition once
    private var transitionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
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

        binding.viewPagerImages.transitionName = "car_image_${args.carId}"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        viewModel.loadCar(args.carId)
        observeCarState()
        observeFavoriteState()
    }

    /** Safety net — if transition was never started by onStart, force-start it here. */
    override fun onStart() {
        super.onStart()
        safeStartTransition()
    }

    private fun safeStartTransition() {
        if (!transitionStarted && isAdded) {
            transitionStarted = true
            startPostponedEnterTransition()
        }
    }

    private fun observeCarState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.scrollView.isVisible = false
                        // Don't wait forever — start transition immediately on loading
                        safeStartTransition()
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.scrollView.isVisible = true
                        val car = state.data

                        // Image gallery
                        if (car.imageUrls.isNotEmpty()) {
                            val adapter = CarImageAdapter(
                                urls = car.imageUrls,
                                onFirstImageReady = { safeStartTransition() },
                                onImageClick = { position ->
                                    FullScreenImageDialogFragment
                                        .newInstance(car.imageUrls, position)
                                        .show(parentFragmentManager, "fullscreen_image")
                                }
                            )
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
                            safeStartTransition()
                        }

                        // Text fields
                        binding.tvTitle.text = car.title
                        binding.tvPrice.text = car.price.formatPrice()
                        binding.tvYear.text = car.year.toString()
                        binding.tvMileage.text = car.mileage.formatMileage()
                        binding.tvFuelType.text = car.fuelType
                        binding.tvTransmission.text = car.transmission
                        binding.tvCity.text = car.city
                        binding.tvAge.text = TimeAgo.format(car.createdAt)

                        // Seller card
                        if (car.sellerName.isNotBlank()) {
                            binding.layoutSeller.isVisible = true
                            binding.tvSellerName.text = car.sellerName
                        } else {
                            binding.layoutSeller.isVisible = false
                        }

                        // Description
                        if (car.description.isNotBlank()) {
                            binding.tvDescriptionLabel.isVisible = true
                            binding.tvDescription.isVisible = true
                            binding.tvDescription.text = car.description
                        }

                        setupContactButtons(car.title, car.price)
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        // CRITICAL: always unblock the transition even on error
                        safeStartTransition()
                        binding.root.showSnackbar(state.message)
                    }
                }
            }
        }
    }

    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isFavorite.collectLatest { isFav ->
                binding.btnFavorite.setImageResource(
                    if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
            }
        }
    }

    private fun setupContactButtons(title: String, price: Long) {
        binding.btnWhatsapp.setOnClickListener {
            val msg = "Hi, I'm interested in your listing: $title — ${price.formatPrice()}"
            val uri = Uri.parse("https://wa.me/?text=${Uri.encode(msg)}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this car on Kompressor.az: $title — ${price.formatPrice()}"
                )
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        }
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
    ).toInt()

    private fun setupDots(count: Int) {
        binding.dotsLayout.removeAllViews()
        if (count <= 1) return
        val dotSize   = dpToPx(8)
        val dotMargin = dpToPx(4)
        repeat(count) {
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = dotMargin
                }
                setBackgroundResource(R.drawable.dot_inactive)
            }
            binding.dotsLayout.addView(dot)
        }
        updateDots(0, count)
    }

    private fun updateDots(selected: Int, count: Int) {
        for (i in 0 until count) {
            val dot = binding.dotsLayout.getChildAt(i) ?: break
            dot.setBackgroundResource(
                if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
