package com.vadimtoptunov.chaosbank_android.core.money

import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A sample loan offer (banking-breadth cluster). The `loanAprUnderstated` defect
 * advertises a low APR while charging a higher effective rate — so the monthly
 * payment is higher than the advertised APR implies (a misleading-APR dark pattern).
 */
object LoanCalc {
    val principal: BigDecimal = BigDecimal("5000")
    val months: Int = 24
    private val advertisedApr = BigDecimal("7.9")

    /** The APR shown to the user — always the advertised (low) one. */
    fun displayedApr(): BigDecimal = advertisedApr

    /** The rate actually used to compute the payment. */
    fun effectiveApr(): BigDecimal =
        if (Defects.isActive(DefectId.loanAprUnderstated)) BigDecimal("13.9") else advertisedApr

    /** Standard amortised monthly payment for [principal] over [months] at [effectiveApr]. */
    fun monthlyPayment(): BigDecimal {
        val r = effectiveApr().toDouble() / 100.0 / 12.0
        val p = principal.toDouble()
        val payment = if (r == 0.0) p / months
        else p * r * Math.pow(1 + r, months.toDouble()) / (Math.pow(1 + r, months.toDouble()) - 1)
        return BigDecimal(payment).setScale(2, RoundingMode.HALF_EVEN)
    }

    fun totalCost(): BigDecimal = monthlyPayment().multiply(BigDecimal(months)).setScale(2, RoundingMode.HALF_EVEN)
}
