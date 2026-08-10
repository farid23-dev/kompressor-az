package az.kompressor.app.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.TypedValue
import android.view.View
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
class CarDetailFragment : Fragment(R.layout.fragment_car_detail) {

    private lateinit var binding: FragmentCarDetailBinding
    private val viewModel: CarDetailViewModel by viewModels()
    private val args: CarDetailFragmentArgs by navArgs()

    private var transitionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCarDetailBinding.bind(view)

        binding.viewPagerImages.transitionName = "car_image_${args.carId}"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.btnAdminApprove.setOnClickListener { viewModel.approveCar() }
        viewModel.loadCar(args.carId)
        observeCarState()
        observeFavoriteState()
        observeAdminStatus()
    }

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
                        safeStartTransition()
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.scrollView.isVisible = true
                        val car = state.data

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

                        binding.tvTitle.text = car.title
                        binding.tvPrice.text = car.price.formatPrice()
                        binding.tvYear.text = car.year.toString()
                        binding.tvMileage.text = car.mileage.formatMileage()
                        binding.tvFuelType.text = car.fuelType
                        binding.tvTransmission.text = car.transmission
                        binding.tvCity.text = car.city
                        binding.tvAge.text = TimeAgo.format(requireContext(), car.createdAt)

                        binding.tvPendingBanner.isVisible = car.status == "pending"

                        if (car.sellerName.isNotBlank()) {
                            binding.layoutSeller.isVisible = true
                            binding.tvSellerName.text = car.sellerName
                        } else {
                            binding.layoutSeller.isVisible = false
                        }

                        if (car.description.isNotBlank()) {
                            binding.tvDescriptionLabel.isVisible = true
                            binding.tvDescription.isVisible = true
                            binding.tvDescription.text = car.description
                        }

                        setupContactButtons(car.title, car.price, car.phone)
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
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

    private fun observeAdminStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAdmin.collectLatest { isAdmin ->
                val state = viewModel.carState.value
                val isPending = state is Resource.Success && state.data.status == "pending"
                binding.btnAdminApprove.isVisible = isAdmin && isPending
            }
        }
    }

    private fun setupContactButtons(title: String, price: Long, phone: String) {
        binding.btnWhatsapp.setOnClickListener {
            val clean = phone.filter { it.isDigit() || it == '+' }
            val priceStr = price.formatPrice()
            val msg = Uri.encode("Hi, I'm interested in your listing: $title — $priceStr")
            val uri = if (clean.isNotEmpty())
                Uri.parse("https://wa.me/$clean?text=$msg")
            else
                Uri.parse("https://wa.me/?text=$msg")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        binding.btnCall.setOnClickListener {
            val clean = phone.filter { it.isDigit() || it == '+' }
            if (clean.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
            }
        }
        binding.btnShare.setOnClickListener {
            val priceStr = price.formatPrice()
            val shareText = getString(R.string.share_car_format, title, priceStr)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
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
}
