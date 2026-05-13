package az.kompressor.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import az.kompressor.app.databinding.BottomSheetEditProfileBinding
import az.kompressor.app.util.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill current values
        val profile = (viewModel.userProfile.value as? Resource.Success)?.data
        binding.etFirstName.setText(profile?.name ?: "")
        binding.etSurname.setText(profile?.surname ?: "")
        binding.etPhone.setText(profile?.phone ?: "")

        binding.btnSave.setOnClickListener {
            val name    = binding.etFirstName.text?.toString()?.trim() ?: ""
            val surname = binding.etSurname.text?.toString()?.trim() ?: ""
            val phone   = binding.etPhone.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                binding.tilFirstName.error = "Required"; return@setOnClickListener
            }
            binding.tilFirstName.error = null
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Saving…"

            viewModel.updateProfile(name, surname, phone)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateProfileState.collectLatest { state ->
                when (state) {
                    is Resource.Success -> { dismiss() }
                    is Resource.Error   -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save Changes"
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val TAG = "EditProfileSheet"
    }
}
