package az.kompressor.app.ui.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.FragmentContactBinding
import androidx.core.net.toUri

class ContactFragment : Fragment(R.layout.fragment_contact) {

    private lateinit var binding: FragmentContactBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentContactBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, "tel:+994513019971".toUri())
            startActivity(intent)
        }

        binding.btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:ismayilovf@outlook.com".toUri()
                putExtra(Intent.EXTRA_SUBJECT, "Kompressor.az - Support")
            }
            startActivity(Intent.createChooser(intent, getString(R.string.btn_send_email)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
