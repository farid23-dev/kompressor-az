package az.kompressor.app.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
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
        binding = FragmentNotificationsBinding.bind(view)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        adapter = NotificationAdapter(
            onDelete = { notif -> viewModel.deleteNotification(notif.id) },
            onItemClick = { notif ->
                if (notif.carId.isNotBlank()) {
                    val action = NotificationsFragmentDirections.actionNotificationsFragmentToCarDetailFragment(notif.carId)
                    findNavController().navigate(action)
                }
            }
        )
        binding.rvNotifications.adapter = adapter
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collectLatest { list ->
                adapter.submitList(list)
                binding.layoutEmpty.isVisible        = list.isEmpty()
                binding.rvNotifications.isVisible    = list.isNotEmpty()
            }
        }

        viewModel.markAllRead()
    }

    override fun onDestroyView() { super.onDestroyView() }
}
