package az.kompressor.app.ui.post

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import az.kompressor.app.databinding.FragmentPostCarBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import az.kompressor.app.util.attachPhonePrefix
import az.kompressor.app.util.cleanPhoneNumber
import az.kompressor.app.util.ValidationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostCarFragment : Fragment(R.layout.fragment_post_car) {

    private lateinit var binding: FragmentPostCarBinding
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPostCarBinding.bind(view)

        setupSpinners()
        setupImageRecyclerView()
        setupValidationClearers()
        setupClickListeners()
        observeState()

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
                binding.rvImages.isVisible = total > 0
                binding.tvImageCount.text = if (total == 0) getString(R.string.no_photos_selected)
                else {
                    val selectedText = getString(R.string.photo_count_selected, total)
                    if (existingCount > 0) {
                        "$selectedText ${getString(R.string.existing_photos_count, existingCount)}"
                    } else selectedText
                }
            }
        }
    }

    private fun setupValidationClearers() {
        binding.etBrand.doOnTextChanged { _, _, _, _ -> binding.tilBrand.error = null }
        binding.etModel.doOnTextChanged { _, _, _, _ -> binding.tilModel.error = null }
        binding.etYear.doOnTextChanged { _, _, _, _ -> binding.tilYear.error = null }
        binding.etPrice.doOnTextChanged { _, _, _, _ -> binding.tilPrice.error = null }
        binding.etMileage.doOnTextChanged { _, _, _, _ -> binding.tilMileage.error = null }
        attachPhonePrefix(binding.etPhone)
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
            val phone = cleanPhoneNumber(binding.etPhone.text.toString())
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

    @SuppressLint("SetTextI18n")
    private fun observeEditCar() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.editCar.collectLatest { car ->
                car ?: return@collectLatest
                binding.tvHeader.text = getString(R.string.edit_title)
                binding.btnPost.text = getString(R.string.btn_save_changes)

                binding.etBrand.setText(car.brand)
                binding.etModel.setText(car.model)
                binding.etYear.setText(car.year.toString())
                binding.etPrice.setText(car.price.toString())
                binding.etMileage.setText(car.mileage.toString())
                binding.etPhone.setText(car.phone.ifBlank { "" })
                binding.etCity.setText(car.city)
                binding.etDescription.setText(car.description)

                val fuels = resources.getStringArray(R.array.fuel_types)
                val fuelIdx = fuels.indexOfFirst { it.equals(car.fuelType, ignoreCase = true) }
                if (fuelIdx >= 0) binding.spinnerFuel.setSelection(fuelIdx)

                val trans = resources.getStringArray(R.array.transmission_types)
                val transIdx = trans.indexOfFirst { it.equals(car.transmission, ignoreCase = true) }
                if (transIdx >= 0) binding.spinnerTransmission.setSelection(transIdx)

                val existingCount = car.imageUrls.size
                if (existingCount > 0) {
                    binding.tvImageCount.text = getString(R.string.existing_photos_count, existingCount)
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (binding.etBrand.text.isNullOrBlank()) {
            binding.tilBrand.error = getString(R.string.error_brand_required); valid = false
        }
        if (binding.etModel.text.isNullOrBlank()) {
            binding.tilModel.error = getString(R.string.error_model_required); valid = false
        }
        val year = binding.etYear.text.toString().toIntOrNull()
        if (year == null || year < 1900 || year > 2100) {
            binding.tilYear.error = getString(R.string.error_invalid_year); valid = false
        }
        val price = binding.etPrice.text.toString().toLongOrNull()
        if (price == null || price <= 0) {
            binding.tilPrice.error = getString(R.string.error_invalid_price); valid = false
        }
        val mileage = binding.etMileage.text.toString().toIntOrNull()
        if (mileage == null || mileage < 0) {
            binding.tilMileage.error = getString(R.string.error_invalid_mileage); valid = false
        }
        if (!ValidationUtils.isValidPhone(binding.etPhone.text.toString())) {
            binding.tilPhone.error = getString(R.string.error_invalid_phone); valid = false
        }
        if (binding.etCity.text.isNullOrBlank()) {
            binding.tilCity.error = getString(R.string.error_city_required); valid = false
        }
        val hasNewImages      = viewModel.selectedImages.value.isNotEmpty()
        val hasExistingImages = viewModel.existingImageUrls.value.isNotEmpty()
        if (!hasNewImages && !hasExistingImages) {
            binding.root.showSnackbar(getString(R.string.error_no_photos))
            valid = false
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
                        
                        if (viewModel.isAdmin.value) {
                            // Admin posts/updates directly without any messages
                            findNavController().navigateUp()
                        } else {
                            val isEdit = args.editCarId.isNotBlank()
                            if (isEdit) {
                                binding.root.showSnackbar(getString(R.string.listing_updated_msg))
                                findNavController().navigateUp()
                            } else {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.listing_submitted_title))
                                    .setMessage(getString(R.string.listing_submitted_msg))
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        findNavController().navigateUp()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                        }
                        viewModel.resetState()
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
}
