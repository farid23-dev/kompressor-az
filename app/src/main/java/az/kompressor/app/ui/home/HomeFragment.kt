package az.kompressor.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var carAdapter: CarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        setupFilterButton()
        setupFilterChip()
        observeCars()
        observeFilter()

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter { car, sharedImageView ->
            val action = HomeFragmentDirections.actionHomeFragmentToCarDetailFragment(car.id)
            val extras = FragmentNavigatorExtras(
                sharedImageView to "car_image_${car.id}"
            )
            findNavController().navigate(action, extras)
        }
        binding.rvCars.apply {
            adapter = carAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText ?: "")
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCars()
        }
    }

    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            FilterBottomSheet().show(childFragmentManager, FilterBottomSheet.TAG)
        }
    }

    private fun setupFilterChip() {
        binding.chipActiveFilter.setOnCloseIconClickListener {
            viewModel.clearFilter()
        }
    }

    private fun observeFilter() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterState.collectLatest { filter ->
                binding.chipActiveFilter.isVisible = filter.isActive
                if (filter.isActive) {
                    binding.chipActiveFilter.text = filter.label()
                }
            }
        }
    }

    private fun observeCars() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carsState.collectLatest { state ->
                // Stop swipe refresh spinner regardless of result
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
