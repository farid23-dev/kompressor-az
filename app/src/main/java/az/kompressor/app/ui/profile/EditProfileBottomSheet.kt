package az.kompressor.app.ui.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import az.kompressor.app.R
import az.kompressor.app.databinding.BottomSheetEditProfileBinding
import az.kompressor.app.util.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import az.kompressor.app.util.attachPhonePrefix
import az.kompressor.app.util.cleanPhoneNumber
import az.kompressor.app.util.ValidationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_edit_profile) {

    private lateinit var binding: BottomSheetEditProfileBinding
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = BottomSheetEditProfileBinding.bind(view)

        val profile = (viewModel.userProfile.value as? Resource.Success)?.data
        attachPhonePrefix(binding.etPhone)
        binding.etFirstName.setText(profile?.name ?: "")
        binding.etSurname.setText(profile?.surname ?: "")
        binding.etPhone.setText(profile?.phone ?: "")

        binding.btnSave.setOnClickListener {
            val name    = binding.etFirstName.text?.toString()?.trim() ?: ""
            val surname = binding.etSurname.text?.toString()?.trim() ?: ""
            val phone   = cleanPhoneNumber(binding.etPhone.text?.toString()?.trim() ?: "")

            if (name.isEmpty()) {
                binding.tilFirstName.error = getString(R.string.field_required); return@setOnClickListener
            }
            binding.tilFirstName.error = null

            if (!ValidationUtils.isValidPhone(phone)) {
                binding.tilPhone.error = getString(R.string.error_invalid_phone); return@setOnClickListener
            }
            binding.tilPhone.error = null
            binding.btnSave.isEnabled = false
            binding.btnSave.text = getString(R.string.saving_msg)

            viewModel.updateProfile(name, surname, phone)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateProfileState.collectLatest { state ->
                when (state) {
                    is Resource.Success -> { dismiss() }
                    is Resource.Error   -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = getString(R.string.btn_save_changes)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView() }

    companion object {
        const val TAG = "EditProfileSheet"
    }
}
