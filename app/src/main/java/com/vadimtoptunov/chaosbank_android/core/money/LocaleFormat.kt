package com.vadimtoptunov.chaosbank_android.core.money

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** A selectable UI locale (Localization cluster). Distinct from the fixed money format. */
enum class LocaleId(val title: String, val javaLocale: Locale) {
    enUS("en-US", Locale.US),
    deDE("de-DE", Locale.GERMANY),
    ar("ar", Locale("ar"));

    companion object {
        fun from(raw: String?): LocaleId = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: enUS
    }
}

/**
 * Locale-aware number grouping. The `numberGroupingIgnoresLocale` defect always uses
 * en-US separators regardless of the selected locale — a classic i18n bug.
 */
object LocaleFormat {
    fun grouped(value: BigDecimal, locale: LocaleId): String {
        val effective = if (Defects.isActive(DefectId.numberGroupingIgnoresLocale)) Locale.US else locale.javaLocale
        val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(effective))
        df.isGroupingUsed = true
        return df.format(value)
    }
}
