package az.kompressor.app.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import az.kompressor.app.databinding.FragmentCarDetailBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CarDetailFragment : Fragment() {

    private var _binding: FragmentCarDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CarDetailViewModel by viewModels()
    private val args: CarDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        viewModel.loadCar(args.carId)

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

                        Glide.with(this@CarDetailFragment)
                            .load(car.imageUrls.firstOrNull())
                            .centerCrop()
                            .into(binding.ivCarImage)

                        binding.tvTitle.text = car.title
                        binding.tvPrice.text = "${car.price} AZN"
                        binding.tvYear.text = car.year.toString()
                        binding.tvMileage.text = "${car.mileage} km"
                        binding.tvFuelType.text = car.fuelType
                        binding.tvTransmission.text = car.transmission
                        binding.tvCity.text = car.city

                        binding.btnContact.setOnClickListener {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+994501234567")
                            }
                            startActivity(intent)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
