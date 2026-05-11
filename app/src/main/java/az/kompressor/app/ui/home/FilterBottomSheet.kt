package az.kompressor.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import az.kompressor.app.R
import az.kompressor.app.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Filter + Sort bottom sheet.
 * Scoped to the parent (HomeFragment) so it shares the exact same ViewModel instance.
 * activityViewModels() would create a separate instance — don't use it here.
 *
 * NOTE: fuel_types / transmission_types arrays stay in English because they match
 * the values stored in Firestore. Only UI labels are localized via strings.xml.
 */
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    // requireParentFragment() = HomeFragment → same VM instance as HomeFragment's viewModels()
    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        restoreCurrentFilter()

        binding.btnApplyFilter.setOnClickListener { applyAndDismiss() }
        binding.btnClearFilter.setOnClickListener { clearAndDismiss() }
    }

    private fun setupSpinners() {
        // Localized "Any/Hamısı" as the first (no-filter) option
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

    /** Pre-select whatever filter is currently active */
    private fun restoreCurrentFilter() {
        val current = viewModel.filterState.value
        val anyLabel = getString(R.string.filter_any)

        // Fuel spinner — match against English values (stored in Firestore)
        val fuels = listOf(anyLabel) + resources.getStringArray(R.array.fuel_types).toList()
        val fuelIdx = if (current.fuelType.isBlank()) 0
            else fuels.indexOfFirst { it.equals(current.fuelType, ignoreCase = true) }.coerceAtLeast(0)
        binding.spinnerFuel.setSelection(fuelIdx)

        // Transmission spinner
        val trans = listOf(anyLabel) + resources.getStringArray(R.array.transmission_types).toList()
        val transIdx = if (current.transmission.isBlank()) 0
            else trans.indexOfFirst { it.equals(current.transmission, ignoreCase = true) }.coerceAtLeast(0)
        binding.spinnerTransmission.setSelection(transIdx)

        // City
        binding.etCity.setText(current.city)

        // Sort radio
        when (current.sortBy) {
            "price_asc"  -> binding.rgSort.check(R.id.rbPriceAsc)
            "price_desc" -> binding.rgSort.check(R.id.rbPriceDesc)
            else         -> binding.rgSort.check(R.id.rbNewest)
        }
    }

    private fun applyAndDismiss() {
        val anyLabel = getString(R.string.filter_any)
        val fuels = listOf(anyLabel) + resources.getStringArray(R.array.fuel_types).toList()
        val trans = listOf(anyLabel) + resources.getStringArray(R.array.transmission_types).toList()

        // Position 0 = "Any/Hamısı" → empty string means no filter
        val selectedFuel = fuels[binding.spinnerFuel.selectedItemPosition].let {
            if (it == anyLabel) "" else it
        }
        val selectedTrans = trans[binding.spinnerTransmission.selectedItemPosition].let {
            if (it == anyLabel) "" else it
        }
        val city = binding.etCity.text?.toString()?.trim() ?: ""

        val sortBy = when (binding.rgSort.checkedRadioButtonId) {
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
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheet"
    }
}
