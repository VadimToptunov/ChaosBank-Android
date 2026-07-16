package com.vadimtoptunov.chaosbank_android.core.money

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class FxRatesTest {
    @Test fun rate_eurToUsd() {
        assertEquals(0, BigDecimal("1.08").compareTo(FxRates.rate(Currency.EUR, Currency.USD)))
    }

    @Test fun rate_sameCurrencyIsOne() {
        assertEquals(0, BigDecimal.ONE.compareTo(FxRates.rate(Currency.USD, Currency.USD)))
    }

    @Test fun rate_isInvertible() {
        val a = FxRates.rate(Currency.EUR, Currency.GBP)
        val b = FxRates.rate(Currency.GBP, Currency.EUR)
        assertEquals(0, BigDecimal.ONE.compareTo((a * b).roundedScale(6)))
    }

    @Test fun convert_multipliesByRate() {
        assertEquals(0, BigDecimal("108.00").compareTo(FxRates.convert(BigDecimal("100"), Currency.EUR, Currency.USD).roundedMoney()))
    }

    @Test fun feeRate_isHalfPercent() {
        assertEquals(0, BigDecimal("0.005").compareTo(FxRates.feeRate))
    }
}

class AmountParserTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    @Test fun parses_usGroupedDecimal() {
        assertEquals(0, BigDecimal("1000.50").compareTo(AmountParser.parse("1,000.50", Locale.US)!!))
    }

    @Test fun parses_plainDecimal() {
        assertEquals(0, BigDecimal("12.5").compareTo(AmountParser.parse("12.5", Locale.US)!!))
    }

    @Test fun rejects_nonNumeric() {
        assertNull(AmountParser.parse("abc", Locale.US))
        assertNull(AmountParser.parse("", Locale.US))
    }

    @Test fun germanLocale_treatsCommaAsDecimal() {
        // Under de_DE, "1.000,50" is one thousand point five; the point is grouping.
        val v = AmountParser.parse("1.000,50", Locale.GERMANY)
        assertTrue(v != null && v.compareTo(BigDecimal("1000.50")) == 0)
    }

    @Test fun localeParseDefect_collapsesSeparators() {
        Defects.configure(ChaosConfig(0, setOf(com.vadimtoptunov.chaosbank_android.core.defects.DefectId.localeParse), "test"))
        // Buggy: every separator becomes a decimal point → 1,000.50 collapses.
        val v = AmountParser.parse("1,000.50", Locale.US)
        assertTrue(v != null && v.compareTo(BigDecimal("1.00050")) == 0)
        Defects.configure(ChaosConfig(0, emptySet(), "clean"))
    }
}
