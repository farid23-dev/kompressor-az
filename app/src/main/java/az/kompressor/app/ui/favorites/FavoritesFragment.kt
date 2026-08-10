package az.kompressor.app.ui.favorites

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentFavoritesBinding
import az.kompressor.app.ui.home.CarAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private lateinit var binding: FragmentFavoritesBinding
    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var carAdapter: CarAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFavoritesBinding.bind(view)
        setupRecyclerView()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        carAdapter = CarAdapter(
            onItemClick = { car, sharedImageView ->
                val action = FavoritesFragmentDirections
                    .actionFavoritesFragmentToCarDetailFragment(car.id)
                val extras = androidx.navigation.fragment.FragmentNavigatorExtras(
                    sharedImageView to "car_image_${car.id}"
                )
                findNavController().navigate(action, extras)
            },
            onFavoriteClick = { car ->
                viewModel.removeFavorite(car.id)
            }
        )
        binding.rvFavorites.apply {
            adapter = carAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favorites.collectLatest { cars ->
                carAdapter.setFavorites(cars.map { it.id }.toSet())
                carAdapter.submitList(cars)
                binding.tvEmpty.isVisible = cars.isEmpty()
                binding.rvFavorites.isVisible = cars.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
