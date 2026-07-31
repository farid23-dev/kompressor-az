package az.kompressor.app.util

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText

private const val PREFIX = "+994 "

fun attachPhonePrefix(field: TextInputEditText) {
    if (field.text.isNullOrEmpty()) {
        field.setText(PREFIX)
        field.setSelection(PREFIX.length)
    }

    field.addTextChangedListener(object : TextWatcher {
        private var isUpdating = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (isUpdating || s == null) return
            isUpdating = true

            val raw = s.toString()
            when {
                !raw.startsWith(PREFIX) -> {
                    field.setText(PREFIX)
                    field.setSelection(PREFIX.length)
                }
                raw.length > PREFIX.length + 9 -> {
                    val trimmed = raw.substring(0, PREFIX.length + 9)
                    field.setText(trimmed)
                    field.setSelection(trimmed.length)
                }
            }
            isUpdating = false
        }
    })
}

fun cleanPhoneNumber(raw: String): String {
    val stripped = raw.replace(" ", "").replace("-", "")
    return if (stripped.startsWith("+9940")) {
        "+994" + stripped.removePrefix("+9940")
    } else stripped
}
