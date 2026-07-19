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

class LocaleFormatTest {
    private val value = BigDecimal("1234567.89")

    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    @Test fun enUS_usesCommaAndDot() {
        assertEquals("1,234,567.89", LocaleFormat.grouped(value, LocaleId.enUS))
    }

    @Test fun deDE_usesDotAndComma() {
        assertEquals("1.234.567,89", LocaleFormat.grouped(value, LocaleId.deDE))
    }

    @Test fun defect_alwaysUsesEnUsRegardlessOfLocale() {
        Defects.configure(ChaosConfig(0, setOf(DefectId.numberGroupingIgnoresLocale), "test"))
        assertEquals("1,234,567.89", LocaleFormat.grouped(value, LocaleId.deDE))
        assertEquals("1,234,567.89", LocaleFormat.grouped(value, LocaleId.enUS))
    }

    @Test fun localeId_fromParsesNames() {
        assertEquals(LocaleId.deDE, LocaleId.from("deDE"))
        assertEquals(LocaleId.enUS, LocaleId.from(null))
        assertEquals(LocaleId.enUS, LocaleId.from("nope"))
    }

    @Test fun money_symbolPlacementFollowsLocale() {
        val amount = BigDecimal("1234.56")
        assertTrue(LocaleFormat.money(amount, "EUR", LocaleId.enUS).trim().startsWith("€"))
        assertTrue(LocaleFormat.money(amount, "EUR", LocaleId.deDE).trim().endsWith("€"))
    }

    @Test fun currencySymbolPlacementIgnoresLocale_alwaysEnUsStyle() {
        Defects.configure(ChaosConfig(0, setOf(DefectId.currencySymbolPlacementIgnoresLocale), "test"))
        val amount = BigDecimal("1234.56")
        assertTrue(LocaleFormat.money(amount, "EUR", LocaleId.deDE).trim().startsWith("€"))
    }
}
