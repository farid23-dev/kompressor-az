package az.kompressor.app.util

import android.content.Context
import az.kompressor.app.R

object TimeAgo {

    private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

    fun daysLeft(createdAt: Long, expiresAt: Long): Int {
        val expiry = if (expiresAt > 0) expiresAt else (createdAt + THIRTY_DAYS_MS)
        val diff = expiry - System.currentTimeMillis()
        return (diff / (24 * 60 * 60 * 1000)).coerceAtLeast(0).toInt()
    }

    fun format(context: Context, timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        return when {
            seconds < 60      -> context.getString(R.string.time_just_now)
            minutes < 60      -> context.getString(R.string.time_minutes_ago, minutes)
            hours < 24        -> context.getString(R.string.time_hours_ago, hours)
            days == 1L        -> context.getString(R.string.time_yesterday)
            days < 7          -> context.getString(R.string.time_days_ago, days)
            weeks == 1L       -> context.getString(R.string.time_1_week_ago)
            weeks < 5         -> context.getString(R.string.time_weeks_ago, weeks)
            months == 1L      -> context.getString(R.string.time_1_month_ago)
            months < 12       -> context.getString(R.string.time_months_ago, months)
            else              -> context.getString(R.string.time_years_ago, months / 12)
        }
    }
}
