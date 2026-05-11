package az.kompressor.app.util

import java.text.NumberFormat
import java.util.Locale

private val priceFormatter = NumberFormat.getNumberInstance(Locale.US)

/** 45000 → "45,000 AZN" */
fun Long.formatPrice(): String = "${priceFormatter.format(this)} AZN"

/** 32000 → "32,000 km" */
fun Int.formatMileage(): String = "${priceFormatter.format(this)} km"
