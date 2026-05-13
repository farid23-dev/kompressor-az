package az.kompressor.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        setupThemeButtons()
        setupSignOut()
        observeSignOut()

        binding.btnContactUs.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_contactFragment)
        }
        binding.btnAbout.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
        }
        binding.btnAdminDashboard.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminDashboardFragment)
        }

        observeAdminVisibility()
    }

    private fun observeAdminVisibility() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAdmin.collectLatest { isAdmin ->
                binding.tvAdminLabel.isVisible = isAdmin
                binding.btnAdminDashboard.isVisible = isAdmin
            }
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private fun setupThemeButtons() {
        highlightActiveThemeButton(viewModel.getCurrentTheme())

        binding.btnThemeLight.setOnClickListener {
            viewModel.setTheme(AppCompatDelegate.MODE_NIGHT_NO)
            highlightActiveThemeButton(AppCompatDelegate.MODE_NIGHT_NO)
        }
        binding.btnThemeDark.setOnClickListener {
            viewModel.setTheme(AppCompatDelegate.MODE_NIGHT_YES)
            highlightActiveThemeButton(AppCompatDelegate.MODE_NIGHT_YES)
        }
        binding.btnThemeSystem.setOnClickListener {
            viewModel.setTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            highlightActiveThemeButton(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun highlightActiveThemeButton(mode: Int) {
        val active = 1.0f
        val inactive = 0.45f
        binding.btnThemeLight.alpha  = if (mode == AppCompatDelegate.MODE_NIGHT_NO)            active else inactive
        binding.btnThemeDark.alpha   = if (mode == AppCompatDelegate.MODE_NIGHT_YES)           active else inactive
        binding.btnThemeSystem.alpha = if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) active else inactive
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    private fun setupSignOut() {
        binding.btnSignOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.settings_sign_out))
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton(getString(R.string.settings_sign_out)) { _, _ -> viewModel.signOut() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeSignOut() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSignedOut.collectLatest { signedOut ->
                if (signedOut) {
                    findNavController().navigate(R.id.action_settingsFragment_to_signInFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
