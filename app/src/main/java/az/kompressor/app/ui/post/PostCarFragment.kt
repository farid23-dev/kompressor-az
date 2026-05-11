package az.kompressor.app.ui.post

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
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
    private val args: PostCarFragmentArgs by navArgs()
    private lateinit var imageAdapter: SelectedImageAdapter

    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) viewModel.addImage(clipData.getItemAt(i).uri)
            } ?: result.data?.data?.let { uri -> viewModel.addImage(uri) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupImageRecyclerView()
        setupValidationClearers()
        setupClickListeners()
        observeState()

        // Edit mode: load existing car if carId was passed
        val editCarId = args.editCarId
        if (editCarId.isNotBlank()) {
            viewModel.loadCarForEdit(editCarId)
            observeEditCar()
        }
    }

    private fun setupSpinners() {
        val fuels = resources.getStringArray(R.array.fuel_types)
        val transmissions = resources.getStringArray(R.array.transmission_types)
        binding.spinnerFuel.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, fuels)
        binding.spinnerTransmission.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, transmissions)
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
                val existingCount = viewModel.existingImageUrls.value.size
                val total = existingCount + uris.size
                binding.tvImageCount.text = if (total == 0) "No photos selected"
                    else "$total photo(s) selected${if (existingCount > 0) " ($existingCount existing)" else ""}"
            }
        }
    }

    private fun setupValidationClearers() {
        binding.etBrand.doOnTextChanged { _, _, _, _ -> binding.tilBrand.error = null }
        binding.etModel.doOnTextChanged { _, _, _, _ -> binding.tilModel.error = null }
        binding.etYear.doOnTextChanged { _, _, _, _ -> binding.tilYear.error = null }
        binding.etPrice.doOnTextChanged { _, _, _, _ -> binding.tilPrice.error = null }
        binding.etMileage.doOnTextChanged { _, _, _, _ -> binding.tilMileage.error = null }
        binding.etPhone.doOnTextChanged { _, _, _, _ -> binding.tilPhone.error = null }
        binding.etCity.doOnTextChanged { _, _, _, _ -> binding.tilCity.error = null }
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
            if (!validateForm()) return@setOnClickListener

            val year = binding.etYear.text.toString().toIntOrNull() ?: 0
            val price = binding.etPrice.text.toString().toLongOrNull() ?: 0
            val mileage = binding.etMileage.text.toString().toIntOrNull() ?: 0
            val brand = binding.etBrand.text.toString()
            val model = binding.etModel.text.toString()
            val fuelType = binding.spinnerFuel.selectedItem.toString()
            val transmission = binding.spinnerTransmission.selectedItem.toString()
            val city = binding.etCity.text.toString()
            val phone = binding.etPhone.text.toString()
            val description = binding.etDescription.text.toString()

            if (args.editCarId.isNotBlank()) {
                viewModel.updateCar(brand, model, year, price, mileage,
                    fuelType, transmission, city, phone, description)
            } else {
                viewModel.postCar("$brand $model".trim(), brand, model, year, price, mileage,
                    fuelType, transmission, city, phone, description)
            }
        }
    }

    /** Pre-fill all fields when in edit mode */
    private fun observeEditCar() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.editCar.collectLatest { car ->
                car ?: return@collectLatest
                binding.tvPostTitle.text = "Edit Listing"
                binding.btnPost.text = "Save Changes"

                binding.etBrand.setText(car.brand)
                binding.etModel.setText(car.model)
                binding.etYear.setText(car.year.toString())
                binding.etPrice.setText(car.price.toString())
                binding.etMileage.setText(car.mileage.toString())
                binding.etPhone.setText(car.phone)
                binding.etCity.setText(car.city)
                binding.etDescription.setText(car.description)

                // Select correct spinner values
                val fuels = resources.getStringArray(R.array.fuel_types)
                val fuelIdx = fuels.indexOfFirst { it.equals(car.fuelType, ignoreCase = true) }
                if (fuelIdx >= 0) binding.spinnerFuel.setSelection(fuelIdx)

                val trans = resources.getStringArray(R.array.transmission_types)
                val transIdx = trans.indexOfFirst { it.equals(car.transmission, ignoreCase = true) }
                if (transIdx >= 0) binding.spinnerTransmission.setSelection(transIdx)

                val existingCount = car.imageUrls.size
                if (existingCount > 0) {
                    binding.tvImageCount.text = "$existingCount existing photo(s)"
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (binding.etBrand.text.isNullOrBlank()) {
            binding.tilBrand.error = "Brand is required"; valid = false
        }
        if (binding.etModel.text.isNullOrBlank()) {
            binding.tilModel.error = "Model is required"; valid = false
        }
        val year = binding.etYear.text.toString().toIntOrNull()
        if (year == null || year < 1900 || year > 2100) {
            binding.tilYear.error = "Enter a valid year"; valid = false
        }
        val price = binding.etPrice.text.toString().toLongOrNull()
        if (price == null || price <= 0) {
            binding.tilPrice.error = "Enter a valid price"; valid = false
        }
        val mileage = binding.etMileage.text.toString().toIntOrNull()
        if (mileage == null || mileage < 0) {
            binding.tilMileage.error = "Enter valid mileage"; valid = false
        }
        if (binding.etPhone.text.isNullOrBlank()) {
            binding.tilPhone.error = "Phone number is required"; valid = false
        }
        if (binding.etCity.text.isNullOrBlank()) {
            binding.tilCity.error = "City is required"; valid = false
        }
        return valid
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
                        val msg = if (args.editCarId.isNotBlank()) "Listing updated!" else "Car posted successfully!"
                        binding.root.showSnackbar(msg)
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
