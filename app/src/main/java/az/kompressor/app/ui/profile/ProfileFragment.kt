package az.kompressor.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentProfileBinding
import az.kompressor.app.ui.profile.ProfileFragmentDirections
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var myCarAdapter: MyCarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmail.text = viewModel.getCurrentUserEmail()
        binding.tvUid.text = "UID: ${viewModel.getCurrentUserUid()}"

        setupMyListings()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSignOut.setOnClickListener { viewModel.signOut() }

        observeSignOut()
        observeMyListings()
        observeDeleteState()
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
            }
        )
        binding.rvMyListings.apply {
            adapter = myCarAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
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
        _binding = null
    }
}
