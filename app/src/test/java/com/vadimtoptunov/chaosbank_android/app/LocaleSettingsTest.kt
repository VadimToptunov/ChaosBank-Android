package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocaleSettingsTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    @Test fun setRtl_updatesFlag() {
        val locale = LocaleSettings()
        assertFalse(locale.rtl)
        locale.enableRtl(true)
        assertTrue(locale.rtl)
    }

    @Test fun clean_neverForcesLtr() {
        assertFalse(LocaleSettings.forcesLtrRow(rtl = true))
        assertFalse(LocaleSettings.forcesLtrRow(rtl = false))
    }

    @Test fun rtlBreaksLayout_forcesLtrOnlyWhenRtl() {
        Defects.configure(ChaosConfig(0, setOf(DefectId.rtlBreaksLayout), "test"))
        assertTrue(LocaleSettings.forcesLtrRow(rtl = true))
        assertFalse(LocaleSettings.forcesLtrRow(rtl = false))
    }
}
