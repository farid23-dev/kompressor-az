package az.kompressor.app.ui.post

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.databinding.FragmentPostCarBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostCarFragment : Fragment() {

    private var _binding: FragmentPostCarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostCarViewModel by viewModels()
    private lateinit var imageAdapter: SelectedImageAdapter

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    viewModel.addImage(clipData.getItemAt(i).uri)
                }
            } ?: result.data?.data?.let { uri ->
                viewModel.addImage(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageRecyclerView()
        setupClickListeners()
        observeState()
    }

    private fun setupImageRecyclerView() {
        imageAdapter = SelectedImageAdapter { uri -> viewModel.removeImage(uri) }
        binding.rvImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedImages.collectLatest { uris ->
                imageAdapter.submitList(uris)
                binding.tvImageCount.text = "${uris.size} photo(s) selected"
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            imagePickerLauncher.launch(intent)
        }

        binding.btnPost.setOnClickListener {
            val year = binding.etYear.text.toString().toIntOrNull() ?: 0
            val price = binding.etPrice.text.toString().toLongOrNull() ?: 0
            val mileage = binding.etMileage.text.toString().toIntOrNull() ?: 0

            viewModel.postCar(
                title = "${binding.etBrand.text} ${binding.etModel.text}".trim(),
                brand = binding.etBrand.text.toString(),
                model = binding.etModel.text.toString(),
                year = year,
                price = price,
                mileage = mileage,
                fuelType = binding.spinnerFuel.selectedItem.toString(),
                transmission = binding.spinnerTransmission.selectedItem.toString(),
                city = binding.etCity.text.toString()
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnPost.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnPost.isEnabled = true
                        binding.root.showSnackbar("Car posted successfully!")
                        viewModel.resetState()
                        findNavController().navigateUp()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnPost.isEnabled = true
                        binding.root.showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    null -> {
                        binding.progressBar.isVisible = false
                        binding.btnPost.isEnabled = true
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
