package az.kompressor.app.util

object TimeAgo {

    fun format(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        return when {
            seconds < 60      -> "Just now"
            minutes < 60      -> "${minutes}m ago"
            hours < 24        -> "${hours}h ago"
            days == 1L        -> "Yesterday"
            days < 7          -> "${days} days ago"
            weeks == 1L       -> "1 week ago"
            weeks < 5         -> "${weeks} weeks ago"
            months == 1L      -> "1 month ago"
            months < 12       -> "${months} months ago"
            else              -> "${months / 12}y ago"
        }
    }
}
