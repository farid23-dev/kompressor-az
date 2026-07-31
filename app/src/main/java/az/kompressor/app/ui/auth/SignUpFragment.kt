package az.kompressor.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentSignUpBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import az.kompressor.app.util.attachPhonePrefix
import az.kompressor.app.util.cleanPhoneNumber
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private lateinit var binding: FragmentSignUpBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignUpBinding.bind(view)

        attachPhonePrefix(binding.etPhone)

        binding.btnSignUp.setOnClickListener {
            viewModel.signUp(
                name            = binding.etName.text.toString().trim(),
                surname         = binding.etSurname.text.toString().trim(),
                phone           = cleanPhoneNumber(binding.etPhone.text.toString().trim()),
                email           = binding.etEmail.text.toString().trim(),
                password        = binding.etPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString()
            )
        }

        binding.tvGoToSignIn.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnSignUp.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignUp.isEnabled = true
                        findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                        viewModel.resetState()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignUp.isEnabled = true
                        binding.root.showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    null -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignUp.isEnabled = true
                    }
                }
            }
        }
    }
}
