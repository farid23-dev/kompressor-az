package az.kompressor.app.ui.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentAdminDashboardBinding
import az.kompressor.app.domain.model.Car
import az.kompressor.app.util.Resource
import az.kompressor.app.util.showSnackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private lateinit var binding: FragmentAdminDashboardBinding
    private val viewModel: AdminDashboardViewModel by viewModels()
    private lateinit var adapter: AdminListingAdapter

    private var allCars: List<Car> = emptyList()
    private var currentTab = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAdminDashboardBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) { findNavController().navigateUp(); return@launch }
            val isAdmin = try {
                FirebaseFirestore.getInstance()
                    .collection("admins").document(uid).get().await().exists()
            } catch (_: Exception) { false }
            if (!isAdmin) {
                binding.root.showSnackbar("Access denied")
                findNavController().navigateUp()
                return@launch
            }
        }

        adapter = AdminListingAdapter(
            onApprove = { car -> viewModel.approve(car) },
            onReject  = { car -> viewModel.reject(car) }
        )
        binding.rvListings.adapter = adapter
        binding.rvListings.layoutManager = LinearLayoutManager(requireContext())

        listOf("All", "Pending", "Approved", "Rejected").forEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { currentTab = tab.position; filterAndShow() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        observeState()
        observeActions()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carsState.collectLatest { state ->
                binding.progressBar.isVisible = state is Resource.Loading
                when (state) {
                    is Resource.Success -> {
                        allCars = state.data
                        filterAndShow()
                        val pending = allCars.count { it.status == "pending" }
                        binding.tvPendingCount.isVisible = pending > 0
                        binding.tvPendingCount.text = "$pending pending"
                    }
                    is Resource.Error -> binding.root.showSnackbar(state.message)
                    else -> {}
                }
            }
        }
    }

    private fun observeActions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collectLatest { msg ->
                if (msg != null) {
                    binding.root.showSnackbar(msg)
                    viewModel.clearAction()
                }
            }
        }
    }

    private fun filterAndShow() {
        val filtered = when (currentTab) {
            1    -> allCars.filter { it.status == "pending" }
            2    -> allCars.filter { it.status == "approved" }
            3    -> allCars.filter { it.status == "rejected" }
            else -> allCars
        }
        adapter.submitList(filtered)
        binding.tvEmpty.isVisible    = filtered.isEmpty()
        binding.rvListings.isVisible = filtered.isNotEmpty()
    }
}
