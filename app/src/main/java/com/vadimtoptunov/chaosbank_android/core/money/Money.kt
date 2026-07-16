package com.vadimtoptunov.chaosbank_android.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Round to 2 fractional digits with banker's rounding (half-to-even). */
fun BigDecimal.roundedMoney(): BigDecimal = setScale(2, RoundingMode.HALF_EVEN)

/** Round to an arbitrary scale with banker's rounding. */
fun BigDecimal.roundedScale(scale: Int): BigDecimal = setScale(scale, RoundingMode.HALF_EVEN)

/** A monetary amount tied to a currency. Amounts are always exact [BigDecimal]. */
data class Money(val amount: BigDecimal, val currency: Currency) {

    val rounded: Money get() = Money(amount.roundedMoney(), currency)

    /** "€1,234.56" — grouping fixed for the sandbox, regardless of device locale. */
    val formatted: String
        get() = currency.symbol + MoneyFormat.decimal(amount.roundedMoney())

    /** Signed with an explicit leading + or − (used for deltas / P&L). */
    val formattedSigned: String
        get() {
            val r = amount.roundedMoney()
            val sign = if (r.signum() < 0) "−" else "+"
            return sign + currency.symbol + MoneyFormat.decimal(r.abs())
        }

    companion object {
        fun zero(currency: Currency) = Money(BigDecimal.ZERO, currency)
    }
}

object MoneyFormat {
    private fun formatter(fractionDigits: Int): DecimalFormat {
        val df = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
        df.minimumFractionDigits = fractionDigits
        df.maximumFractionDigits = fractionDigits
        df.isGroupingUsed = true
        return df
    }

    /** Fixed en_US-style grouping: comma thousands, dot decimal. */
    fun decimal(value: BigDecimal, fractionDigits: Int = 2): String =
        formatter(fractionDigits).format(value)

    fun price(value: BigDecimal, fractionDigits: Int = 2): String = decimal(value, fractionDigits)

    fun percent(value: BigDecimal): String {
        val r = value.roundedScale(2)
        val sign = if (r.signum() < 0) "−" else "+"
        return sign + decimal(r.abs()) + "%"
    }
}
