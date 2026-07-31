package az.kompressor.app.ui.profile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentProfileBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var myCarAdapter: MyCarAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        setupMyListings()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnEditProfile.setOnClickListener {
            EditProfileBottomSheet().show(childFragmentManager, EditProfileBottomSheet.TAG)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        observeUserProfile()
        observeMyListings()
        observeDeleteState()
        observeSignOut()
    }

    private fun setupMyListings() {
        myCarAdapter = MyCarAdapter(
            onEdit = { car ->
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToPostCarFragment(editCarId = car.id)
                findNavController().navigate(action)
            },
            onDelete = { car ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete listing")
                    .setMessage("Remove \"${car.title}\" from your listings?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteCar(car.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onBump = { car ->
                viewModel.bumpCar(car.id)
                binding.root.showSnackbar("\"${car.title}\" bumped to top for 30 days ✅")
            }
        )
        binding.rvMyListings.apply {
            adapter = myCarAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun observeUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userProfile.collectLatest { state ->
                when (state) {
                    is Resource.Success -> {
                        val user = state.data
                        val fullName = "${user.name} ${user.surname}".trim()
                        binding.tvName.text = fullName.ifEmpty { user.email }
                        binding.tvEmail.text = user.email

                        if (user.phone.isNotBlank()) {
                            binding.tvPhone.isVisible = true
                            binding.tvPhone.text = user.phone
                        }
                    }
                    is Resource.Error -> {
                        binding.tvName.text = viewModel.getCurrentUserEmail()
                        binding.tvEmail.text = ""
                    }
                    is Resource.Loading -> { }
                }
            }
        }
    }

    private fun observeMyListings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myListings.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> binding.progressListings.isVisible = true
                    is Resource.Success -> {
                        binding.progressListings.isVisible = false
                        if (state.data.isEmpty()) {
                            binding.tvNoListings.isVisible = true
                            binding.rvMyListings.isVisible = false
                        } else {
                            binding.tvNoListings.isVisible = false
                            binding.rvMyListings.isVisible = true
                            myCarAdapter.submitList(state.data)
                        }
                    }
                    is Resource.Error -> {
                        binding.progressListings.isVisible = false
                        binding.root.showSnackbar(state.message)
                    }
                }
            }
        }
    }

    private fun observeDeleteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteState.collectLatest { state ->
                when (state) {
                    is Resource.Success -> {
                        binding.root.showSnackbar("Listing deleted")
                        viewModel.resetDeleteState()
                        viewModel.loadMyListings()
                    }
                    is Resource.Error -> {
                        binding.root.showSnackbar(state.message)
                        viewModel.resetDeleteState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeSignOut() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSignedOut.collectLatest { signedOut ->
                if (signedOut) {
                    findNavController().navigate(R.id.action_profileFragment_to_signInFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
