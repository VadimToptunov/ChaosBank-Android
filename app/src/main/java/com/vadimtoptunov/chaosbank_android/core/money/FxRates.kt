package com.vadimtoptunov.chaosbank_android.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/** Fixed mid-market FX rates for the sandbox. Deterministic; not a live feed. */
object FxRates {
    /** Units of currency per 1 EUR. */
    private val perEUR: Map<Currency, BigDecimal> = mapOf(
        Currency.EUR to BigDecimal.ONE,
        Currency.USD to BigDecimal("1.08"),
        Currency.GBP to BigDecimal("0.85"),
    )

    /** Exchange fee applied on the sold amount (0.5%). */
    val feeRate: BigDecimal = BigDecimal("0.005")

    /** Mid-market rate to convert 1 unit of [from] into [to]. */
    fun rate(from: Currency, to: Currency): BigDecimal {
        val f = perEUR.getValue(from)
        val t = perEUR.getValue(to)
        return t.divide(f, 10, RoundingMode.HALF_EVEN)
    }

    /** Gross converted amount before fees. */
    fun convert(amount: BigDecimal, from: Currency, to: Currency): BigDecimal =
        amount * rate(from, to)
}
