package az.kompressor.app.ui.home

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import az.kompressor.app.R
import az.kompressor.app.databinding.BottomSheetNotificationsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationsBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_notifications) {

    companion object { const val TAG = "NotificationsBottomSheet" }

    private lateinit var binding: BottomSheetNotificationsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = BottomSheetNotificationsBinding.bind(view)
        binding.btnGoHome.setOnClickListener {
            dismiss()
        }
    }

}
