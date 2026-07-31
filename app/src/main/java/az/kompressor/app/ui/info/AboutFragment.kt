package az.kompressor.app.ui.info

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentAboutBinding

class AboutFragment : Fragment(R.layout.fragment_about) {

    private lateinit var binding: FragmentAboutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAboutBinding.bind(view)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

}
