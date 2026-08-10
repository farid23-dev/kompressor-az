package az.kompressor.app.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentSettingsBinding
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val isLoggedIn = viewModel.isUserLoggedIn()
        binding.tvAccountLabel.isVisible = isLoggedIn
        binding.cardAccount.isVisible    = isLoggedIn

        setupLanguageButtons()
        setupThemeButtons()
        setupSignOut()
        observeSignOut()
        observeLanguageChange()

        binding.btnContactUs.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_contactFragment)
        }
        binding.btnAbout.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
        }
    }

    private fun setupLanguageButtons() {
        highlightActiveLanguageButton(viewModel.getCurrentLanguage())

        binding.btnLangEn.setOnClickListener {
            viewModel.setLanguage("en")
            highlightActiveLanguageButton("en")
        }
        binding.btnLangAz.setOnClickListener {
            viewModel.setLanguage("az")
            highlightActiveLanguageButton("az")
        }
    }

    private fun highlightActiveLanguageButton(lang: String) {
        binding.btnLangEn.isSelected = lang == "en"
        binding.btnLangAz.isSelected = lang == "az"
    }

    private fun observeLanguageChange() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.languageChanged.collectLatest { changed ->
                if (changed) {
                    viewModel.resetLanguageChange()
                    requireActivity().recreate()
                }
            }
        }
    }

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
        binding.btnThemeLight.isSelected  = mode == AppCompatDelegate.MODE_NIGHT_NO
        binding.btnThemeDark.isSelected   = mode == AppCompatDelegate.MODE_NIGHT_YES
        binding.btnThemeSystem.isSelected = mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun setupSignOut() {
        binding.btnSignOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.sign_out_confirm_title))
                .setMessage(getString(R.string.sign_out_confirm_msg))
                .setPositiveButton(getString(R.string.btn_sign_out)) { _, _ -> viewModel.signOut() }
                .setNegativeButton(getString(R.string.cancel), null)
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
    }
}
