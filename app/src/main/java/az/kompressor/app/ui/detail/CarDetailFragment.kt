package az.kompressor.app.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

                        // Image gallery
                        if (car.imageUrls.isNotEmpty()) {
                            binding.viewPagerImages.adapter =
                                CarImageAdapter(car.imageUrls)
                            setupDots(car.imageUrls.size)
                            binding.viewPagerImages.registerOnPageChangeCallback(
                                object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        updateDots(position, car.imageUrls.size)
                                    }
                                }
                            )
                        }

                        binding.tvTitle.text = car.title
                        binding.tvPrice.text = "${car.price} AZN"
                        binding.tvYear.text = car.year.toString()
                        binding.tvMileage.text = "${car.mileage} km"
                        binding.tvFuelType.text = car.fuelType
                        binding.tvTransmission.text = car.transmission
                        binding.tvCity.text = car.city

                        // Description (hide if empty)
                        if (car.description.isNotBlank()) {
                            binding.tvDescriptionLabel.isVisible = true
                            binding.tvDescription.isVisible = true
                            binding.tvDescription.text = car.description
                        }

                        // Real seller phone
                        val phoneNumber = car.phone.ifBlank { null }
                        binding.btnContact.setOnClickListener {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${phoneNumber ?: ""}")
                            }
                            if (phoneNumber != null) {
                                startActivity(intent)
                            } else {
                                binding.root.showSnackbar("No phone number available")
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.root.showSnackbar(state.message)
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
                setImageResource(
                    if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive
                )
                val size = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) / 8
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
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
