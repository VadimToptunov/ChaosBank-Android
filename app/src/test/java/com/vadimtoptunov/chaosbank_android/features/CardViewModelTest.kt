package com.vadimtoptunov.chaosbank_android.features

import com.vadimtoptunov.chaosbank_android.core.defects.ChaosConfig
import com.vadimtoptunov.chaosbank_android.core.defects.DefectId
import com.vadimtoptunov.chaosbank_android.core.defects.Defects
import com.vadimtoptunov.chaosbank_android.features.card.CardViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardViewModelTest {
    @Before fun clean() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }
    @After fun reset() { Defects.configure(ChaosConfig(0, emptySet(), "clean")) }

    private fun on(vararg d: DefectId) { Defects.configure(ChaosConfig(0, d.toSet(), "test")) }

    @Test fun freeze_readsBackCorrectly() {
        val vm = CardViewModel()
        vm.frozen = true
        assertTrue(vm.frozen)
    }

    @Test fun cardToggleInvert_readsBackInverted() {
        on(DefectId.cardToggleInvert)
        val vm = CardViewModel()
        vm.frozen = true
        assertFalse(vm.frozen)
    }

    @Test fun onlinePayments_defaultOn() {
        assertTrue(CardViewModel().onlinePayments)
    }

    @Test fun onlinePaymentsInverted_readsBackInverted() {
        on(DefectId.onlinePaymentsInverted)
        assertFalse(CardViewModel().onlinePayments)
    }

    @Test fun limitError_rejectsZeroByDefault() {
        val vm = CardViewModel()
        vm.monthlyLimitText = "0"
        assertTrue(vm.limitError != null)
    }

    @Test fun cardLimitAcceptsZero_suppressesError() {
        on(DefectId.cardLimitAcceptsZero)
        val vm = CardViewModel()
        vm.monthlyLimitText = "0"
        assertNull(vm.limitError)
    }

    @Test fun limitError_nullForPositive() {
        val vm = CardViewModel()
        vm.monthlyLimitText = "2000"
        assertNull(vm.limitError)
    }

    @Test fun pan_maskedByDefault() {
        assertTrue(CardViewModel().displayedPAN.startsWith("••••"))
    }

    @Test fun cardNumberFullyVisible_showsFullPan() {
        on(DefectId.cardNumberFullyVisible)
        assertEquals("4916 2043 1188 4291", CardViewModel().displayedPAN)
    }

    @Test fun cvv_hiddenByDefault() {
        assertNull(CardViewModel().visibleCVV)
    }

    @Test fun cardCvvVisible_showsCvv() {
        on(DefectId.cardCvvVisible)
        assertEquals("829", CardViewModel().visibleCVV)
    }

    @Test fun expiry_defaultInFuture() {
        assertEquals("08/29", CardViewModel().expiry)
    }

    @Test fun cardExpiryInPast_showsPastDate() {
        on(DefectId.cardExpiryInPast)
        assertEquals("08/20", CardViewModel().expiry)
    }

    @Test fun pin_maskedByDefault() {
        assertEquals("••••", CardViewModel().pinText)
    }

    @Test fun pinShownPlaintext_revealsPin() {
        on(DefectId.pinShownPlaintext)
        assertEquals("4821", CardViewModel().pinText)
    }
}
