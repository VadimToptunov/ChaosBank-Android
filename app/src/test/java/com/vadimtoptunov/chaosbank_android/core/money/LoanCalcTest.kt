package com.vadimtoptunov.chaosbank_android.core.money

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class LoanCalcTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    @Test fun clean_effectiveMatchesDisplayedApr() {
        assertEquals(0, LoanCalc.displayedApr().compareTo(LoanCalc.effectiveApr()))
    }

    @Test fun loanAprUnderstated_effectiveExceedsDisplayed() {
        Defects.configure(ChaosConfig(0, setOf(DefectId.loanAprUnderstated), "test"))
        assertTrue(LoanCalc.effectiveApr() > LoanCalc.displayedApr())
    }

    @Test fun loanAprUnderstated_raisesMonthlyPayment() {
        val cleanPayment = LoanCalc.monthlyPayment()
        Defects.configure(ChaosConfig(0, setOf(DefectId.loanAprUnderstated), "test"))
        assertTrue(LoanCalc.monthlyPayment() > cleanPayment)
    }

    @Test fun monthlyPayment_isPositiveAndTotalConsistent() {
        val monthly = LoanCalc.monthlyPayment()
        assertTrue(monthly > BigDecimal.ZERO)
        assertEquals(0, monthly.multiply(BigDecimal(LoanCalc.months)).setScale(2, java.math.RoundingMode.HALF_EVEN).compareTo(LoanCalc.totalCost()))
    }
}
