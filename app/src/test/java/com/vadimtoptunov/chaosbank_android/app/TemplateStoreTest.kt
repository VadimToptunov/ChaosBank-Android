package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TemplateStoreTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    @Test fun clean_prefillsExactAmount() {
        val store = TemplateStore()
        val rent = store.templates.first { it.name == "Rent" }
        assertEquals(0, BigDecimal("1200.00").compareTo(store.prefillAmount(rent)))
    }

    @Test fun templatePrefillsWrongAmount_manglesAmount() {
        Defects.configure(ChaosConfig(0, setOf(DefectId.templatePrefillsWrongAmount), "test"))
        val store = TemplateStore()
        val rent = store.templates.first { it.name == "Rent" }
        assertTrue(store.prefillAmount(rent) > rent.amount)
    }

    @Test fun seedHasTemplates() {
        assertEquals(3, TemplateStore().templates.size)
    }
}
