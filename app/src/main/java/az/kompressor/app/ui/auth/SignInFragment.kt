package az.kompressor.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentSignInBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import az.kompressor.app.util.AdminSetup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private lateinit var binding: FragmentSignInBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)

        binding.btnSignIn.setOnClickListener {
            viewModel.signIn(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        binding.tvGoToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnSignIn.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignIn.isEnabled = true
                        lifecycleScope.launch { AdminSetup.registerCurrentUserAsAdminIfNeeded() }
                        findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
                        viewModel.resetState()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignIn.isEnabled = true
                        binding.root.showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    null -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignIn.isEnabled = true
                    }
                }
            }
        }
    }
}
