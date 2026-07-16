package com.vadimtoptunov.chaosbank_android.ui.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Transaction date/label formatting. Hosts the `dateTimezoneShift` defect. */
object TxFormat {
    private val homeTimeZone: TimeZone = TimeZone.getTimeZone("Europe/Berlin")
    private val shiftedTimeZone: TimeZone = TimeZone.getTimeZone("Pacific/Midway")

    private fun formatter(pattern: String, shifted: Boolean): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = if (shifted) shiftedTimeZone else homeTimeZone
        }

    fun rowTime(dateSec: Long, shifted: Boolean): String =
        formatter("d MMM · HH:mm", shifted).format(Date(dateSec * 1000))

    fun dayHeader(dateSec: Long, shifted: Boolean): String =
        formatter("EEEE, d MMM", shifted).format(Date(dateSec * 1000))

    fun emoji(category: String): String = when (category) {
        "Income", "Top-up" -> "＋"
        "Transfer" -> "⇄"
        "Exchange" -> "⇆"
        "Groceries" -> "🛒"
        "Dining" -> "🍽"
        "Utilities" -> "⚡️"
        "Health" -> "＋"
        "Transport" -> "🚗"
        "Shopping" -> "🛍"
        "Digital" -> "📱"
        "Trade" -> "📈"
        else -> "•"
    }
}
