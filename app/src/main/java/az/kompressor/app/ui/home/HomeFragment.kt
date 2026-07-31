package az.kompressor.app.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.addCallback
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentHomeBinding
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var carAdapter: CarAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        setupRecyclerView()
        setupSwipeRefresh()

        binding.btnFilter.setOnClickListener {
            FilterBottomSheet().show(childFragmentManager, FilterBottomSheet.TAG)
        }

        binding.chipActiveFilter.setOnCloseIconClickListener {
            viewModel.clearFilter()
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.btnNotification.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }

        binding.btnProfile.setOnLongClickListener {
            if (viewModel.isAdmin.value) {
                findNavController().navigate(R.id.action_homeFragment_to_adminDashboardFragment)
                true
            } else false
        }

        observeCars()
        observeFilter()
        observeAdminState()
        observeNotifBadge()
        setupBackPress()
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter { car, sharedImageView ->
            val action = HomeFragmentDirections.actionHomeFragmentToCarDetailFragment(car.id)
            val extras = FragmentNavigatorExtras(sharedImageView to "car_image_${car.id}")
            findNavController().navigate(action, extras)
        }
        binding.rvCars.apply {
            adapter = carAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadCars() }
    }

    private fun observeFilter() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterState.collectLatest { filter ->
                binding.chipActiveFilter.isVisible = filter.isActive
                if (filter.isActive) binding.chipActiveFilter.text = filter.label()
            }
        }
    }

    private fun observeCars() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carsState.collectLatest { state ->
                binding.swipeRefresh.isRefreshing = false
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.rvCars.isVisible = false
                        binding.tvEmpty.isVisible = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        if (state.data.isEmpty()) {
                            binding.rvCars.isVisible = false
                            binding.tvEmpty.isVisible = true
                        } else {
                            binding.rvCars.isVisible = true
                            binding.tvEmpty.isVisible = false
                            carAdapter.submitList(state.data)
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.root.showSnackbar(state.message)
                    }
                }
            }
        }
    }

    private fun observeAdminState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAdmin.collectLatest { _ -> }
        }
    }

    private fun observeNotifBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.unreadCount.collectLatest { count ->
                if (count > 0) {
                    binding.tvNotifBadge.isVisible = true
                    binding.tvNotifBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    binding.tvNotifBadge.isVisible = false
                }
            }
        }
    }

    private fun setupBackPress() {
        var backPressedOnce = false
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (backPressedOnce) {
                requireActivity().finish()
            } else {
                backPressedOnce = true
                Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, 2000)
            }
        }
    }
}
