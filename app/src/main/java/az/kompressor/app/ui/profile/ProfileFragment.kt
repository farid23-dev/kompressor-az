package az.kompressor.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show user info
        binding.tvEmail.text = viewModel.getCurrentUserEmail()
        binding.tvUid.text = "UID: ${viewModel.getCurrentUserUid()}"

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSignOut.setOnClickListener { viewModel.signOut() }

        // Observe sign-out → navigate to sign in and clear back stack
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSignedOut.collectLatest { signedOut ->
                if (signedOut) {
                    findNavController().navigate(
                        R.id.action_profileFragment_to_signInFragment
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
