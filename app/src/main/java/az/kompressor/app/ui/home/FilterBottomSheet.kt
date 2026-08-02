package az.kompressor.app.ui.home

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import az.kompressor.app.R
import az.kompressor.app.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_filter) {

    private lateinit var binding: BottomSheetFilterBinding

    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = BottomSheetFilterBinding.bind(view)

        setupSpinners()
        restoreCurrentFilter()

        binding.btnApplyFilter.setOnClickListener { applyAndDismiss() }
        binding.btnClearFilter.setOnClickListener { clearAndDismiss() }
    }

    private fun setupSpinners() {
        val anyLabel = getString(R.string.filter_any)
        val fuels = listOf(anyLabel) + resources.getStringArray(R.array.fuel_types).toList()
        val transmissions = listOf(anyLabel) + resources.getStringArray(R.array.transmission_types).toList()

        binding.spinnerFuel.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, fuels
        )
        binding.spinnerTransmission.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, transmissions
        )
    }

    private fun restoreCurrentFilter() {
        val current = viewModel.filterState.value
        val anyLabel = getString(R.string.filter_any)

        val fuels = listOf(anyLabel) + resources.getStringArray(R.array.fuel_types).toList()
        val fuelIdx = if (current.fuelType.isBlank()) 0
            else fuels.indexOfFirst { it.equals(current.fuelType, ignoreCase = true) }.coerceAtLeast(0)
        binding.spinnerFuel.setSelection(fuelIdx)

        val trans = listOf(anyLabel) + resources.getStringArray(R.array.transmission_types).toList()
        val transIdx = if (current.transmission.isBlank()) 0
            else trans.indexOfFirst { it.equals(current.transmission, ignoreCase = true) }.coerceAtLeast(0)
        binding.spinnerTransmission.setSelection(transIdx)

        binding.etCity.setText(current.city)

        when (current.sortBy) {
            "price_asc"  -> binding.rbPriceAsc.isChecked = true
            "price_desc" -> binding.rbPriceDesc.isChecked = true
            else         -> binding.rbNewest.isChecked = true
        }
    }

    private fun applyAndDismiss() {
        val anyLabel = getString(R.string.filter_any)
        val fuels = listOf(anyLabel) + resources.getStringArray(R.array.fuel_types).toList()
        val trans = listOf(anyLabel) + resources.getStringArray(R.array.transmission_types).toList()

        val selectedFuel = fuels[binding.spinnerFuel.selectedItemPosition].let {
            if (it == anyLabel) "" else it
        }
        val selectedTrans = trans[binding.spinnerTransmission.selectedItemPosition].let {
            if (it == anyLabel) "" else it
        }
        val city = binding.etCity.text?.toString()?.trim() ?: ""

        val sortBy = when (binding.rgSort.checkedChipId) {
            R.id.rbPriceAsc  -> "price_asc"
            R.id.rbPriceDesc -> "price_desc"
            else             -> "newest"
        }

        viewModel.applyFilter(FilterState(selectedFuel, selectedTrans, city, sortBy))
        dismiss()
    }

    private fun clearAndDismiss() {
        viewModel.clearFilter()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        const val TAG = "FilterBottomSheet"
    }
}
