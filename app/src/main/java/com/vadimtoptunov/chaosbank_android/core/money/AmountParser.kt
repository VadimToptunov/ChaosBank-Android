package com.vadimtoptunov.chaosbank_android.core.money

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Parses a user-typed amount string into an exact [BigDecimal] under the active
 * locale. The single injection point for the `localeParse` defect.
 */
object AmountParser {
    fun parse(raw: String, locale: Locale = Locale.getDefault()): BigDecimal? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        if (Defects.isActive(DefectId.localeParse)) {
            // Buggy: treat every separator as a decimal point. "1,000.50" -> "1.00050".
            val normalized = trimmed.replace(",", ".")
            val parts = normalized.split(".")
            return if (parts.size > 1) {
                val intPart = parts.first().ifEmpty { "0" }
                val fraction = parts.drop(1).joinToString("")
                "$intPart.$fraction".toBigDecimalOrNull()
            } else {
                normalized.toBigDecimalOrNull()
            }
        }

        return runCatching {
            val nf = NumberFormat.getInstance(locale)
            if (nf is DecimalFormat) nf.isParseBigDecimal = true
            (nf.parse(trimmed) as? BigDecimal) ?: BigDecimal(nf.parse(trimmed).toString())
        }.getOrElse {
            trimmed.toBigDecimalOrNull()
        }
    }
}
