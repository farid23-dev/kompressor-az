package az.kompressor.app.util

import java.text.NumberFormat
import java.util.Locale

private val priceFormatter = NumberFormat.getNumberInstance(Locale.US)

fun Long.formatPrice(): String = "${priceFormatter.format(this)} AZN"

fun Int.formatMileage(): String = "${priceFormatter.format(this)} km"
