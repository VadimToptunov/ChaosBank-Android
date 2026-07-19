package com.vadimtoptunov.chaosbank_android.app

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class KycGateTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    @Test fun verified_allowsAnyAmount() {
        assertTrue(KycGate.allowsTransfer(BigDecimal("5000"), verified = true))
    }

    @Test fun unverified_allowsSmallBlocksLarge() {
        assertTrue(KycGate.allowsTransfer(BigDecimal("1000"), verified = false))
        assertFalse(KycGate.allowsTransfer(BigDecimal("2000"), verified = false))
    }

    @Test fun kycBypass_allowsLargeWhenUnverified() {
        Defects.configure(ChaosConfig(0, setOf(DefectId.kycBypassAllowsTransfer), "test"))
        assertTrue(KycGate.allowsTransfer(BigDecimal("2000"), verified = false))
    }

    @Test fun store_defaultVerified_andToggle() {
        val store = KycStore()
        assertTrue(store.verified)
        store.applyVerified(false)
        assertFalse(store.verified)
    }
}
