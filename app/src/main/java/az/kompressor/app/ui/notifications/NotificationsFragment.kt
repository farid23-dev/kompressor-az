package az.kompressor.app.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentNotificationsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private lateinit var binding: FragmentNotificationsBinding
    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            binding = FragmentNotificationsBinding.bind(view)
            binding.btnBack.setOnClickListener { findNavController().navigateUp() }

            adapter = NotificationAdapter(
                onDelete = { notif -> viewModel.deleteNotification(notif.id) },
                onItemClick = { notif ->
                    if (notif.carId.isNotBlank()) {
                        val bundle = bundleOf("carId" to notif.carId)
                        findNavController().navigate(R.id.action_notificationsFragment_to_carDetailFragment, bundle)
                    }
                }
            )
            binding.rvNotifications.adapter = adapter
            binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.notifications.collectLatest { list ->
                    try {
                        adapter.submitList(list)
                        binding.layoutEmpty.isVisible = list.isEmpty()
                        binding.rvNotifications.isVisible = list.isNotEmpty()
                    } catch (e: Exception) {
                        android.util.Log.e("NotifFragment", "Error updating list: ${e.message}")
                    }
                }
            }

            viewModel.markAllRead()
        } catch (e: Exception) {
            android.util.Log.e("NotifFragment", "Critical error in onViewCreated: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroyView() { super.onDestroyView() }
}
