package az.kompressor.app.util

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText

private const val PREFIX = "+994 "

/**
 * Attaches a TextWatcher to [field] that:
 *  - Always prepends "+994 " and prevents deletion of the prefix
 *  - Caps total length at prefix(5) + 9 digits = 14 chars
 */
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
                // User deleted into the prefix — restore it
                !raw.startsWith(PREFIX) -> {
                    field.setText(PREFIX)
                    field.setSelection(PREFIX.length)
                }
                // Enforce max length: prefix + 9 digits
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

/**
 * Returns the clean phone number ready for saving:
 * - Strips the "+994 " prefix display space
 * - Removes a leading 0 after the country code (e.g. "+994 050..." → "+99450...")
 * - Returns "+994XXXXXXXXX" format
 */
fun cleanPhoneNumber(raw: String): String {
    val stripped = raw.replace(" ", "").replace("-", "")
    // Remove leading 0 after +994
    return if (stripped.startsWith("+9940")) {
        "+994" + stripped.removePrefix("+9940")
    } else stripped
}
